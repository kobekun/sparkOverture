package com.kobekun.spark.project.batch

import java.sql.DriverManager

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hbase.client.{Result, Scan}
import org.apache.hadoop.hbase.io.ImmutableBytesWritable
import org.apache.hadoop.hbase.mapreduce.TableInputFormat
import org.apache.hadoop.hbase.protobuf.ProtobufUtil
import org.apache.hadoop.hbase.util.{Base64, Bytes}
import org.apache.spark.internal.Logging
import org.apache.spark.sql.SparkSession

import scala.util.{Failure, Success, Try}


/**
  * 用spark對hbase中的數據做統計分析操作
  *
  * 1) 統計每個國家每個省份的訪問量
  * 2) 統計不同瀏覽器的訪問量
  */
object AnalysisV2App extends Logging{

  def main(args: Array[String]): Unit = {

    val spark = SparkSession.builder()
      .config("spark.serializer","org.apache.spark.serializer.KryoSerializer")
      .appName("AnalysisV2App")
      .master("local[2]")
      .getOrCreate()

    val day = "20190130"

    val conf = new Configuration()
    conf.set("hbase.rootdir","hdfs://hadoop001:8020/hbase")
    conf.set("hbase.zookeeper.quorum","hadoop001:2181")

    val tableName = "access_" + day
    conf.set(TableInputFormat.INPUT_TABLE,tableName) //從哪個表裡面讀數據

    val scan = new Scan()

    //設置要查詢的cf
    scan.addFamily(Bytes.toBytes("o"))
    //設置要查詢的列
    scan.addColumn(Bytes.toBytes("o"),Bytes.toBytes("country"))
    scan.addColumn(Bytes.toBytes("o"),Bytes.toBytes("province"))

    scan.addColumn(Bytes.toBytes("o"),Bytes.toBytes("browserName"))
    //設置scan
    conf.set(TableInputFormat.SCAN,Base64.encodeBytes(ProtobufUtil.toScan(scan).toByteArray))

    //通過spark的newAPIHadoopRDD方法讀取數據
    val hbaseRDD = spark.sparkContext.newAPIHadoopRDD(
      conf,
      classOf[TableInputFormat],
      classOf[ImmutableBytesWritable],
      classOf[Result]
    )


    /**
      * spark中一個常用的優化點：cache
      */
    hbaseRDD.cache()


    //用DataFrame API或者SparkSQL API來完成上面需求操作
    import spark.implicits._


    //統計每個瀏覽器的訪問量
    val resultRDD = hbaseRDD.map(x => {
      val browserName = Bytes.toString(x._2.getValue("o".getBytes(),Bytes.toBytes("browserName")))
      (browserName,1)
    }).reduceByKey(_+_)

    resultRDD.collect().foreach(println)


    //coalesce减少分区数
    resultRDD.coalesce(2).foreachPartition(part => {

      Try{
        val connection = {

          Class.forName("com.mysql.jdbc.Driver")
          val url = "jdbc:mysql://hadoop001:3306/spark?characterEncoding=UTF-8"
          val user = "root"
          val password = "root"

          DriverManager.getConnection(url,user,password)
        }

        val preAutoCommit = connection.getAutoCommit
        connection.setAutoCommit(false)

        val sql = "insert into browser_stat(day,browser,cnt) values(?,?,?)"
        val pstmt = connection.prepareStatement(sql)
        pstmt.addBatch(s"delete from browser_stat where day=$day")

        part.foreach(x => {
          pstmt.setString(1, day)
          pstmt.setString(2, x._1)
          pstmt.setInt(3, x._2)

          pstmt.addBatch()
        })

        pstmt.executeBatch()
        connection.commit()

        (connection, preAutoCommit)
      }match {
        case Success((connection,preAutoCommit)) => {

          connection.setAutoCommit(preAutoCommit)
          if(null != connection) connection.close
        }
        case Failure(e) => throw e
      }

    })

    hbaseRDD.unpersist(true)

    spark.stop()


  }

  case class CountryProvince(country: String, province: String)
  case class Browser(browser: String)
}
