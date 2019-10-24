package com.kobekun.spark.project.streaming.util

import java.util
import java.util.{Date, Properties, UUID}

import com.alibaba.fastjson.JSONObject
import org.apache.commons.lang3.time.FastDateFormat
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}

import scala.util.Random

object KafkaProducerApp {

  def main(args: Array[String]): Unit = {

    val prop = new Properties()

    prop.put("key.serializer","org.apache.kafka.common.serialization.StringSerializer")
    prop.put("value.serializer","org.apache.kafka.common.serialization.StringSerializer")
    prop.put("bootstrap.servers",ParamsConf.brokers)
    prop.put("request.required.acks","1")

    val topic = ParamsConf.topic

    val producer = new KafkaProducer[String,String](prop)

    val random = new Random()
    val dataFormat = FastDateFormat.getInstance("yyyyMMddHHmmss")

    for(i <- 1 to 100){

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

      producer.send(new ProducerRecord[String,String](topic, i+"", json.toString))
    }

    println("kobeun kafka生产者生产数据完毕。")
  }
}

//private static final String ACKS_DOC = "The number of acknowledgments the producer requires the leader to have received before considering a request complete. This controls the "
// durability of records that are sent. The following settings are allowed: "
// <li><code>acks=0</code> If set to zero then the producer will not wait for any acknowledgment from the"
// server at all. The record will be immediately added to the socket buffer and considered sent. No guarantee can be"
// made that the server has received the record in this case, and the <code>retries</code> configuration will not"
// take effect (as the client won't generally know of any failures). The offset given back for each record will"
// always be set to -1."
//如果设置为零，那么生产者将根本不等待服务器的任何确认。 该记录将立即添加到套接字缓冲区中并视为已发送。
//在这种情况下，不能保证服务器已收到记录，并且<code> retries </ code>配置将不会生效（因为客户端通常不会知道任何故障）。
//每条记录返回的偏移量将被设置为-1
// <li><code>acks=1</code> This will mean the leader will write the record to its local log but will respond"
// without awaiting full acknowledgement from all followers. In this case should the leader fail immediately after"
// acknowledging the record but before the followers have replicated it then the record will be lost."
//这意味着领导者会将记录写入其本地日志，但会在不等待所有关注者的完全确认的情况下做出响应。 在这种情况下，如果领导者在确认记
//录后立即失败，但是在跟随者复制记录之前，则记录将丢失。
// <li><code>acks=all</code> This means the leader will wait for the full set of in-sync replicas to"
// acknowledge the record. This guarantees that the record will not be lost as long as at least one in-sync replica"
// remains alive. This is the strongest available guarantee. This is equivalent to the acks=-1 setting.";
//这意味着领导者将等待全套同步副本确认记录。 这保证了只要至少一个同步副本仍处于活动状态，记录就不会丢失。
//这是最有力的保证。 这等效于acks = -1设置。