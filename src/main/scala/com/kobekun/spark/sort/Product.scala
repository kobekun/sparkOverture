package com.kobekun.spark.sort

//普通的类，不带比较规则，如何进行排序
class Product(val name: String, val price: Int, val store: Int)
extends Serializable {


  override def toString = s"Product($name, $price, $store)"

//  //实现自定义排序规则
//  override def compare(that: Product): Int = {
////    this.price - that.price //升序
//    that.price - this.price
//  }
}
