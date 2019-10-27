package com.kobekun.spark.project.streaming.util

import org.apache.commons.pool2.impl.GenericObjectPoolConfig
import redis.clients.jedis.JedisPool

object RedisPool {

  val poolConfig = new GenericObjectPoolConfig()
  poolConfig.setMaxIdle(10)
  poolConfig.setMaxTotal(1000)

  private lazy val jedisPool = new JedisPool(poolConfig, ParamsConf.redisHost)

  //启动redis需要指定redis.conf  其中将该配置文件中保护模式改为no
  def getJedis() ={
    val jedis = jedisPool.getResource
    jedis.select(ParamsConf.redisDb)
    jedis
  }


}
