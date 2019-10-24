package com.kobekun.spark.project.streaming.util

import java.util
import java.util.{Date, UUID}

import com.alibaba.fastjson.JSONObject
import org.apache.commons.lang3.time.FastDateFormat

import scala.util.Random

/**
  * 付费日志产生器
  */
object MockData {

  def main(args: Array[String]): Unit = {

    val random = new Random()
    val dataFormat = FastDateFormat.getInstance("yyyyMMddHHmmss")

    //time,userid,courseid,orderid,fee,flag
    for(i <- 0 to 9){

      val time = dataFormat.format(new Date())
      val userid = random.nextInt(10000) + ""
      val courseid = random.nextInt(100) + ""
      val orderid = UUID.randomUUID().toString
      val fee = random.nextInt(400) + ""

      val result = Array("0","1") // 0为未支付，1为支付
      val flag = result(random.nextInt(2))

      val map = new util.HashMap[String,Object]()
      map.put("time",time)
      map.put("userid",userid)
      map.put("courseid",courseid)
      map.put("orderid",orderid)
      map.put("fee",fee)
      map.put("flag",flag)

      val json = new JSONObject(map)
      println(json)
    }
  }
}
