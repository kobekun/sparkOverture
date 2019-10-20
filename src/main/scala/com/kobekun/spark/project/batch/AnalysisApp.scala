package com.kobekun.spark.project.batch

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hbase.CellUtil
import org.apache.hadoop.hbase.client.{Result, Scan}
import org.apache.hadoop.hbase.io.ImmutableBytesWritable
import org.apache.hadoop.hbase.mapreduce.TableInputFormat
import org.apache.hadoop.hbase.protobuf.ProtobufUtil
import org.apache.hadoop.hbase.util.{Base64, Bytes}
import org.apache.spark.deploy.history.LogInfo
import org.apache.spark.internal.Logging
import org.apache.spark.sql.SparkSession


/**
  * 用spark對hbase中的數據做統計分析操作
  *
  * 1) 統計每個國家每個省份的訪問量
  * 2) 統計不同瀏覽器的訪問量
  */
object AnalysisApp extends Logging{

  def main(args: Array[String]): Unit = {

    val spark = SparkSession.builder()
      .config("spark.serializer","org.apache.spark.serializer.KryoSerializer")
      .appName("AnalysisApp")
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

//    hbaseRDD.take(10).foreach(x => {
//
//      val rowkey = Bytes.toString(x._1.get())
//      for(cell <- x._2.rawCells()){
//
//        val cf = Bytes.toString(CellUtil.cloneFamily(cell))
//        val qualifier = Bytes.toString(CellUtil.cloneQualifier(cell))
//        val value = Bytes.toString(CellUtil.cloneValue(cell))
//
//        println(s"$rowkey : $cf : $qualifier : $value")
//      }
//
//    })

    /**
      * spark中一個常用的優化點：cache
      */
    hbaseRDD.cache()

    //TODO... 統計每個國家每個省份的訪問量
    hbaseRDD.map(x => {
      val country = Bytes.toString(x._2.getValue("o".getBytes(),Bytes.toBytes("country")))
      val province = Bytes.toString(x._2.getValue("o".getBytes(),Bytes.toBytes("province")))

      ((country,province),1)
    }).reduceByKey(_+_)
        .collect()
        .foreach(println)

    logInfo("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")

    //用DataFrame API或者SparkSQL API來完成上面需求操作
    import spark.implicits._

    hbaseRDD.map(x => {
      val country = Bytes.toString(x._2.getValue("o".getBytes(),Bytes.toBytes("country")))
      val province = Bytes.toString(x._2.getValue("o".getBytes(),Bytes.toBytes("province")))
      val browserName = Bytes.toString(x._2.getValue("o".getBytes(),Bytes.toBytes("browserName")))
      CountryProvince(country,province)
      Browser(browserName)
    }).toDF.select("country","province")
        .groupBy("country","province")
        .count().show(false)

    logInfo("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")

    // 擴展：求上面需求的top10
    hbaseRDD.map(x => {
      val country = Bytes.toString(x._2.getValue("o".getBytes(),Bytes.toBytes("country")))
      val province = Bytes.toString(x._2.getValue("o".getBytes(),Bytes.toBytes("province")))

      ((country,province),1)
    }).reduceByKey(_+_)
        .map(x => (x._2,x._1))  // (hello,3)=>(3,hello)
        .sortByKey(false)
        .map(x => (x._2,x._1)) // (3,hello) => (hello,3)
        .take(10)
        .foreach(println)

    logInfo("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")

    //統計每個瀏覽器的訪問量
    hbaseRDD.map(x => {
      val browserName = Bytes.toString(x._2.getValue("o".getBytes(),Bytes.toBytes("browserName")))
      (browserName,1)
    }).reduceByKey(_+_)
      .map(x => (x._2,x._1))  // (hello,3)=>(3,hello)
      .sortByKey(false)
      .map(x => (x._2,x._1)) // (3,hello) => (hello,3)
      .foreach(println)

    logInfo("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")

    //統計每個瀏覽器的訪問量 DataFrame API 或者 SparkSQL API
    hbaseRDD.map(x => {
      val browserName = Bytes.toString(x._2.getValue("o".getBytes(),Bytes.toBytes("browserName")))
      Browser(browserName)
    }).toDF.select("browserName")
      .groupBy("browserName")
      .count().show(false)

    logInfo("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")

    //統計每個瀏覽器的訪問量 DataFrame API 或者 SparkSQL API
    hbaseRDD.map(x => {
      val browserName = Bytes.toString(x._2.getValue("o".getBytes(),Bytes.toBytes("browserName")))
      Browser(browserName)
    }).toDF.createOrReplaceTempView("tmp")
    spark.sql("select browserName,count(1) cnt from tmp group by browserName order by cnt desc").show(false)

    hbaseRDD.unpersist(true)

    spark.stop()


  }

  case class CountryProvince(country: String, province: String)
  case class Browser(browser: String)
}

/**
  * Get an RDD for a given Hadoop file with an arbitrary new API InputFormat
  * and extra configuration options to pass to the input format.
  *
  */

// @param fClass storage format of the data to be read
// @param kClass `Class` of the key associated with the `fClass` parameter
// @param vClass `Class` of the value associated with the `fClass` parameter

//def newAPIHadoopRDD[K, V, F <: NewInputFormat[K, V]](
//conf: Configuration = hadoopConfiguration,
//fClass: Class[F],
//kClass: Class[K],
//vClass: Class[V]): RDD[(K, V)] = withScope {
//  assertNotStopped()
//
//  // This is a hack to enforce loading hdfs-site.xml.
//  // See SPARK-11227 for details.
//  FileSystem.getLocal(conf)
//
//  // Add necessary security credentials to the JobConf. Required to access secure HDFS.
//  val jconf = new JobConf(conf)
//  SparkHadoopUtil.get.addCredentials(jconf)
//  new NewHadoopRDD(this, fClass, kClass, vClass, jconf)
//}