package com.kobekun.spark

import com.kobekun.spark.project.batch.ImoocLog

object Rowkey {

  def main(args: Array[String]): Unit = {

    val url = "/course/list?c=cb"
    val referer = "https://www.imooc.com/course/list?c=data"
    val ip = "110.85.18.234"
    val rowkey = ImoocLog.getRowkey("20190516",referer+url+ip)

    println(rowkey)
  }
}
