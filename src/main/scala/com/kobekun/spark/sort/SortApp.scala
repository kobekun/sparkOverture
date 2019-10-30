package com.kobekun.spark.sort

import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}

object SortApp {

  def main(args: Array[String]): Unit = {

    val sparkConf = new SparkConf().setAppName(this.getClass.getName)
      .setMaster("local[2]")
    val sc = new SparkContext(sparkConf)

    val data = sc.parallelize(List("米家激光投影电视4k套装 17999 1000",
      "小米充电宝 200 10",
      "小米云台监控 200 1000",
      "米兔 9 2000"),1)

    //按照价格的降序，按照库存的降序排列(默认按升序排列)
    //第一种方式：通过元组
//    data.map(x =>{
//      val splits = x.split(" ")
//      val name = splits(0)
//      val price = splits(1).toInt
//      val store = splits(2).toInt
//
//      (name,price,store)
//    }).sortBy(x => (-x._2,-x._3)).foreach(println)

    //第二种方式：通过类
//    data.map(x =>{
//      val splits = x.split(" ")
//      val name = splits(0)
//      val price = splits(1).toInt
//      val store = splits(2).toInt
//
//      new Product(name,price,store)
//    }).sortBy(x => x).foreach(println)

    //第三种方式：通过case class
//    data.map(x =>{
//      val splits = x.split(" ")
//      val name = splits(0)
//      val price = splits(1).toInt
//      val store = splits(2).toInt
//
//      ProductCaseClass(name,price,store)
//    }).sortBy(x => x).foreach(println)

    //第四种方式：使用隐式转换
    val products: RDD[Product] = data.map(x =>{
      val splits = x.split(" ")
      val name = splits(0)
      val price = splits(1).toInt
      val store = splits(2).toInt

      new Product(name,price,store)
    })

    products.sortBy(x => x).foreach(println)
    /**
      * 什么是隐式转换：偷偷摸摸的给某个类的方法进行增强
      */
    implicit def product2Ordered(product: Product): Ordered[Product] =
      new Ordered[Product] {
        override def compare(that: Product): Int = {
          that.price - product.price
        }
      }

    sc.stop()
  }
}

//使用case class不用序列化  ******
case class ProductCaseClass(val name: String, val price: Int, val store: Int)
  extends Ordered[ProductCaseClass]{

  //实现自定义排序规则
  override def compare(that: ProductCaseClass): Int = {
    //    this.price - that.price //升序
    that.price - this.price  //降序
  }
}