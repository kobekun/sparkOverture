package com.kobekun.spark.project.batch

import java.util.zip.CRC32
import java.util.{Date, Locale}

import com.kobekun.spark.sort.ProductClass
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.time.FastDateFormat
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hbase.{HColumnDescriptor, HTableDescriptor, TableName}
import org.apache.hadoop.hbase.client.{Admin, Connection, ConnectionFactory, Put}
import org.apache.hadoop.hbase.io.ImmutableBytesWritable
import org.apache.hadoop.hbase.mapreduce.TableOutputFormat
import org.apache.hadoop.hbase.util.Bytes
import org.apache.spark.SparkConf
import org.apache.spark.internal.Logging
import org.apache.spark.sql.SparkSession


/**
  * 對日誌進行ETL操作：把數據從文件系統(本地、HDFS)清洗(ip/ua/time)之後最終存儲到hbase中
  *
  * 批處理：一天處理一次，今天凌晨來處理昨天的數據
  * 需要傳給ImoocLog一個處理時間：yyyyMMdd
  *
  * Hbase表：一天一個  logs_yyyyMMdd
  *   創建表 : 表名和cf，不用關注具體有多少列，只需關注有多少cf即可
  *   rowkey的設計
  *     結合項目的業務需求
  *     通常是組合使用：時間作為rowkey的前綴_字段(MD5/CRC32編碼)
  *     cf: o
  *     column: 把文件系統中解析出來的字段放到map中，然後循環拼成這個rowkey對應的cf下的所有列
  * 後續進行業務分析統計時，也是一天一個批次，直接從hbase表中讀取數據，然後使用spark進行業務統計
  *
  *
  */
object ImoocLog extends Logging{


  def main(args: Array[String]): Unit = {

    if(args.length != 1){
      println("param error: usage -> ImoocLog <time>")
      System.exit(1)
    }

    val day = args(0)
//    val day = "20190130"  //先寫死，後面通過shell腳本將時間傳進來
    val input = s"hdfs://hadoop001:8020/access/$day/*"

//    val spark = SparkSession.builder()
//      .config("spark.serializer","org.apache.spark.serializer.KryoSerializer")
//      .appName("ImoocLog")
//      .master("local[2]")
//      .getOrCreate()
//    val sparkConf = new SparkConf()
//    sparkConf.set("spark.serializer","org.apache.spark.serializer.KryoSerializer")
//      .registerKryoClasses(Array(classOf[Product]))



    val spark = SparkSession.builder().getOrCreate()

    System.setProperty("icode", "EE09C17060084BD7")

//    val path = "E:\\IdeaProjects\\sparkOverture\\src\\data\\test-access.log"
    var logDF = spark.read.format("com.imooc.bigdata.spark.pk")
      .option("path", input)
      .load()

    logDF.show()

    /**
      * sparksql 自定义函数的使用
      */
    import org.apache.spark.sql.functions._
    def formatTime() = udf((time: String) => {
      FastDateFormat.getInstance("yyyyMMddHHmm").format(
        new Date(FastDateFormat.getInstance("dd/MM/yyyy:HH:mm:ss Z", Locale.ENGLISH)
          .parse(time.substring(time.indexOf("[") + 1, time.lastIndexOf("]"))).getTime
        )
      )
    })

    //在已有的DF之上添加或者修改字段
    logDF = logDF.withColumn("formattime", formatTime()(logDF("time")))

//    logDF.show(false)

    val product = new ProductClass("test",1,1)
    val bc = spark.sparkContext.broadcast[ProductClass](product)

    val hbaseInfoRDD = logDF.rdd.map(x => {

      val bcValue = bc.value

      val ip = x.getAs[String]("ip")
      val country = x.getAs[String]("country")
      val province = x.getAs[String]("province")
      val city = x.getAs[String]("city")
      val formattime = x.getAs[String]("formattime")
      val method = x.getAs[String]("method")
      val url = x.getAs[String]("url")
      val protocal = x.getAs[String]("protocal")
      val status = x.getAs[String]("status")
      val bytessent = x.getAs[String]("bytessent")
      val referer = x.getAs[String]("referer")
      val browsername = x.getAs[String]("browsername")
      val browserversion = x.getAs[String]("browserversion")
      val osname = x.getAs[String]("osname")
      val osversion = x.getAs[String]("osversion")
      val ua = x.getAs[String]("ua")

      val columns = scala.collection.mutable.HashMap[String,String]()
      columns.put("ip",ip)
      columns.put("country",country)
      columns.put("province",province)
      columns.put("city",city)
      columns.put("formattime",formattime)
      columns.put("method",method)
      columns.put("url",url)
      columns.put("protocal",protocal)
      columns.put("status",status)
      columns.put("bytessent",bytessent)
      columns.put("referer",referer)
      columns.put("browsername",browsername)
      columns.put("browserversion",browserversion)
      columns.put("osname",osname)
      columns.put("osversion",osversion)

      //hbase api put
      val rowkey = getRowkey(day,referer+url+ip+ua)  //hbase的rowkey
      val put = new Put(Bytes.toBytes(rowkey))  //要保存到hbase的put對象

      //每個rowkey對應的cf的每一列
      for((k,v) <- columns){
        put.addColumn(Bytes.toBytes("o"), Bytes.toBytes(k.toString), Bytes.toBytes(v.toString))
      }

      (new ImmutableBytesWritable(rowkey.getBytes()),put)

    })//.collect().foreach(println)

    val conf = new Configuration()
    conf.set("hbase.rootdir","hdfs://hadoop001:8020/hbase")
    conf.set("hbase.zookeeper.quorum","hadoop001:2181")

    val tableName = createTable(day,conf)

    //設置寫數據到哪個表中
    conf.set(TableOutputFormat.OUTPUT_TABLE,tableName)

    //保存數據
    hbaseInfoRDD.saveAsNewAPIHadoopFile(
      "hdfs://hadoop001:8020/etl/access/hbase",
      classOf[ImmutableBytesWritable],
      classOf[Put],
      classOf[TableOutputFormat[ImmutableBytesWritable]],
      conf
    )

    logInfo(s"本次作業執行成功。。$day")

    spark.stop()
  }

  def getRowkey(time: String, info: String) ={

    /**
      * 由於rowkey採用time_crc32(info)進行拼接
      * 只要是字符串拼接，盡量不要使用+號  --> 經典面試題
      */

    val builder = new StringBuilder(time)
    builder.append("_")

    val crc32 = new CRC32()
    crc32.reset()
    if(StringUtils.isNotEmpty(info)){
      crc32.update(Bytes.toBytes(info))
    }

    builder.append(crc32.getValue)

    builder.toString()

  }
  def createTable(day: String, conf: Configuration) ={

    val table = "access_" + day
    var conn: Connection = null
    var admin: Admin = null
    try{

      conn = ConnectionFactory.createConnection(conf)
      admin = conn.getAdmin

      //判斷表是否存在，如果存在刪除(一天生成一個表，如果中間過程處理有問題，需要刪除重新生成)
      val tableName = TableName.valueOf(table)
      if(admin.tableExists(tableName)){
        admin.disableTable(tableName)
        admin.deleteTable(tableName)
      }

      val tableDescriptor = new HTableDescriptor(tableName)
      val columnDescriptor = new HColumnDescriptor("o")
      tableDescriptor.addFamily(columnDescriptor)
      admin.createTable(tableDescriptor)

    }catch {
      case e: Exception => e.printStackTrace()
    }finally {

      if(null != admin){
        admin.close()
      }
      if(null != conn){
        conn.close()
      }
    }

    table
  }
}
