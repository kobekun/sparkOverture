package com.kobekun.spark.project.streaming.util

import com.typesafe.config.ConfigFactory

/**
  * 项目参数读取类
  */
object ParamsConf {

  private lazy val config = ConfigFactory.load()

  val topic = config.getString("kafka.topic")
  val groupId = config.getString("kafka.group.id")
  val brokerList = config.getString("kafka.broker.list")

  val redisHost = config.getString("redis.host")
  val redisDb = config.getString("redis.db")

  def main(args: Array[String]): Unit = {

    println(ParamsConf.topic)
    println(ParamsConf.groupId)
    println(ParamsConf.brokerList)

    println(ParamsConf.redisHost)
    println(ParamsConf.redisDb)
  }
}
