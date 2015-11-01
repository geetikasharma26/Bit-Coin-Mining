package dos.project.remote

import akka.actor.Actor
import akka.actor._
import akka.kernel.Bootable
import akka.routing.RoundRobinRouter
import com.typesafe.config.ConfigFactory
import scala.math._
import scala.collection.mutable.HashMap
import scala.concurrent.duration.Duration
import scala.util.Random
import java.security.MessageDigest
import java.text

import akka.actor.Props

class RemoteActor extends Actor {

    var birthTime:Long = 0

    def calculateBitCoins(k : Int , usrname: String ) {
      println("=== Starting calculate bit coin ===")
      while(System.currentTimeMillis() - birthTime < 30000) {
        var ranstring = Random.alphanumeric.take(100).mkString
        var hashinput = usrname + ranstring
        val sha = MessageDigest.getInstance("SHA-256")
        var bitcoins =  sha.digest(hashinput.getBytes).foldLeft("")((hashinput: String, b: Byte) => hashinput +
                        Character.forDigit((b & 0xf0) >> 4, 16) +
                        Character.forDigit(b & 0x0f, 16))

        // Check if the leading number of zeros are equal to k
        if (checkPrefix(bitcoins, k))
          sender ! Coin(hashinput,bitcoins)
      }
    }

    //Method to check whether the number of zeros of bitcoins is equal to k
    def checkPrefix(bitcoins:String,k : Int): Boolean = {
        var i = 0
        while (i<k) {
          var c = bitcoins(i)
          if (c != '0') {
            return false
          }
          i += 1
        }
        return true
    }

    def receive = {
        case "Hello" =>
            sender ! "HelloBack"
        case Work(k , usrname) =>
            birthTime = System.currentTimeMillis()
            calculateBitCoins(k , usrname)
            sender ! Done
        case Done =>
            context.system.shutdown()
        case _ =>
            println("\t unknown msg")
    }
}

class RemoteWorker(workerNum: Int) extends Bootable {

    val numOfWorkers = 4
    val system = ActorSystem("RemoteActor",ConfigFactory.load.getConfig("remoteworker"+workerNum+"config"))
    val local = system.actorOf(Props[RemoteActor].withRouter(RoundRobinRouter(numOfWorkers)), name = "remoteWorker")

    def startup() {
    }

    def shutdown() {
        system.shutdown()
    }
}

object RemoteWorkerApp {
    def main(args: Array[String]) {
        try {
            new RemoteWorker(args(0).toInt)
            println("Started RemoteWorker" + args(0).toInt + " Application")
        }
        catch {
            case e:ArrayIndexOutOfBoundsException =>
               println ("Please correct number for the worker. e.g. \"run 1\"")
               System.exit(0)
        }
    }
}
