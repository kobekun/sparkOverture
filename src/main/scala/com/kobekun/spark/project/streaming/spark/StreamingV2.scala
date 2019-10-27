package com.kobekun.spark.project.streaming.spark

import com.alibaba.fastjson.JSON
import com.kobekun.spark.project.streaming.util.{ParamsConf, RedisPool}
import org.apache.spark.SparkConf
import org.apache.spark.streaming.kafka010.{ConsumerStrategies, KafkaUtils, LocationStrategies}
import org.apache.spark.streaming.{Seconds, StreamingContext}

object StreamingV2 {

  def main(args: Array[String]): Unit = {

    val sparkConf = new SparkConf().setMaster("local[2]").setAppName("StreamingV2")
    val ssc = new StreamingContext(sparkConf,Seconds(5))

    val stream = KafkaUtils.createDirectStream(ssc,
      LocationStrategies.PreferConsistent,
      ConsumerStrategies.Subscribe[String,String](ParamsConf.topic,
        ParamsConf.kafkaParams
      )
    )

    stream.map(x => x.value()).print

    /**
      * 统计每天付费成功的总订单数
      * 统计每天付费成功的总订单金额
      */
    stream.foreachRDD(rdd => {

      // flag fee time
      val data = rdd.map(x => JSON.parseObject(x.value()))
      .map(x => {

        val flag = x.getString("flag")
        val time = x.getString("time")
        val fee = x.getLong("fee")

        val day = time.substring(0,8)
        val hour = time.substring(8,10)
        val minute = time.substring(10,12)

        val success: (Long,Long) = if(flag == "1") (1, fee) else (0,0)

        /**
          * 把上面数据规整成一个数据结构
          * day,hour,minute：分别是粒度  success._1 订单数 success._2 费用
          */
        (day, hour, minute, List[Long](1,success._1,success._2))
      })

      //天  zip链式对应
      // val list = List(1,2,3).zip(List("a","b","c"))
//        println(list(0)._1)
//      println(list(0)._2)
//      list(0)即为：(1,a)
//      所以打印出：
//      1
//      a
//      hkeys 表名
//      hget 表名 total  => 获取key的值

      data.map(x => (x._1,x._4))
        .reduceByKey((a,b) =>{

          a.zip(b).map(x => (x._1 + x._2))
        }).foreachPartition(partition => {

        val jedis = RedisPool.getJedis()
        partition.foreach(x => {
          jedis.hincrBy("Imooc_"+x._1, "total", x._2(0))  //总数
          jedis.hincrBy("Imooc_"+x._1, "success", x._2(1))  //成功的订单数
          jedis.hincrBy("Imooc_"+x._1, "fee", x._2(2))  //成功订单数费用

        })
      })

      //小时
      data.map(x => ((x._1,x._2),x._4))
        .reduceByKey((a,b) =>{

          a.zip(b).map(x => (x._1 + x._2))
        }).foreachPartition(partition => {

        val jedis = RedisPool.getJedis()
        partition.foreach(x => {
          // Imooc20190822, total10, 5382
          jedis.hincrBy("Imooc_"+x._1._1, "total"+x._1._2, x._2(0))  //总数
          jedis.hincrBy("Imooc_"+x._1._1, "success"+x._1._2, x._2(1))  //成功的订单数
          jedis.hincrBy("Imooc_"+x._1._1, "fee"+x._1._2, x._2(2))  //成功订单数费用

        })
      })
    })
    ssc.start()
    ssc.awaitTermination()
  }
}
