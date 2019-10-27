package com.kobekun.spark.project.streaming.spark

import com.alibaba.fastjson.JSON
import com.kobekun.spark.project.streaming.util.{ParamsConf, RedisPool}
import org.apache.spark.SparkConf
import org.apache.spark.streaming.kafka010.{ConsumerStrategies, KafkaUtils, LocationStrategies}
import org.apache.spark.streaming.{Seconds, StreamingContext}

object Streaming {

  def main(args: Array[String]): Unit = {

    val sparkConf = new SparkConf().setMaster("local[2]").setAppName("Streaming")
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
      data.cache()

      data.map(x => {

        val time = x.getString("time")
        val day = time.substring(0,8)
        val flag = x.getString("flag")
        val flagResult = if(flag == "1") 1 else 0
        (day,flagResult)
      }).reduceByKey(_+_)//.collect().foreach(println)
          .foreachPartition(partition => {
        val jedis = RedisPool.getJedis()
        partition.foreach(x =>{
          jedis.incrBy("ImoocCount-" + x._1, x._2)
        })
      })
      data.map(x => {

        val time = x.getString("time")
        val day = time.substring(0,8)
        val flag = x.getString("flag")
        val fee = if(flag == "1") x.getString("fee").toLong else 0
        (day,fee)
      }).reduceByKey(_+_)
        .foreachPartition(partition => {
          val jedis = RedisPool.getJedis()
          partition.foreach(x =>{
            jedis.incrBy("ImoocFee-" + x._1, x._2)
          })
        })

      data.unpersist(true)
    })
    ssc.start()
    ssc.awaitTermination()
  }
}
