package dos.project.remote

import akka.actor.Actor

trait CalBitCoin
case object Minecoins extends CalBitCoin
case object Done extends CalBitCoin
case class Work(begin: Int,usrname: String) extends CalBitCoin
case class Coin (key:String,value:String) extends CalBitCoin
