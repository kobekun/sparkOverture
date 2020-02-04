package com.kobekun.spark.datasource

import org.apache.spark.sql.SparkSession

object TestCustomDataSource {

  def main(args: Array[String]): Unit = {

    val spark = SparkSession.builder()
      .appName(this.getClass.getSimpleName).master("local[2]")
      .getOrCreate()


    val df = spark
      .read
      .format("com.kobekun.spark.datasource.DefaultDataSource")
      .load("data/kobekunDataSource")

    df.show()

    spark.stop()
  }
}
