/**
 * DOS Project1
 */

package dos.project.remote

import akka.actor._
import akka.routing.RoundRobinRouter
import com.typesafe.config.ConfigFactory
import scala.math._
import akka.kernel.Bootable
import scala.concurrent.duration._
import scala.collection.mutable.HashMap
import scala.concurrent.duration.Duration
import scala.util.Random
import java.security.MessageDigest
import java.text

class Master(k: Int, usrName : String, ip_address : String) extends Actor {

    var numOfWorkers: Int = 0
    var numOfReplies: Int = 0
    var numOfRemoteActors: Int = 4
    var workerCount  = 0   // count of workers

    var actorRefArr: Array[ActorRef] = Array()

    val reqId:Int = 1

	context.actorSelection("akka.tcp://RemoteActor@" + ip_address + "/user/remoteWorker") ! Identify(reqId)
    
    var totalCoins:Long = 0

    def receive = {
        case ActorIdentity(`reqId`, Some(actor)) =>
            actorRefArr :+= actor
            numOfWorkers += 1
            numOfReplies += 1

        case ActorIdentity(`reqId`, None) =>
            println("\tOne of the remote actors is not available. Currently available worker # = %d".format(numOfWorkers))
            numOfReplies += 1

        case Minecoins =>
            if(numOfWorkers == 0 && numOfReplies == numOfRemoteActors) {
                println ("\n\tCould not find any workers to perform the task. !!EXITING!!\n")
                context.system.shutdown()
            }
            var arrayIdx:Int = 0
            for (i <-0 until numOfWorkers) {
              actorRefArr(arrayIdx) ! Work(k,usrName)
              arrayIdx += 1
            }

        case Coin(key,value)=>
            println( "\tReceived coin (string,bitcoin) => (" + key + ","+ value + ")")
            totalCoins += 1

        case Done =>
            // keep track of worker actors who have finished their work
            workerCount += 1
            sender ! Done
            if (workerCount >= numOfWorkers) {
                // print total bitcoins collected and shutdown the system
                println(" Total BitCoins FOUND = " + totalCoins)
                println(" === !!! End of Mining !!! === ")
                context.system.shutdown()
             }

        case _ => println("\t unknown msg")
     }
}

class MasterApplication(theK:Int,userName:String,ip_address:String) extends Bootable {
    // Create an Akka system
    val system = ActorSystem("MasterActor", ConfigFactory.load.getConfig("masterconfig"))

    // create the master
      val master = system.actorOf(Props(new Master(theK, userName,ip_address)), name = "master")

    //Give some time for remote to come up
    Thread.sleep(3000)
    println(" === !!! Starting Mining !!! === ")

    master ! Minecoins

    def startup() {
    }

    def shutdown() {
        system.shutdown()
    }
}

object MasterApp {
    def main(args: Array[String]) {
        var theK: Int = 0
        var userName:String = ""
		var ip_address:String = ""

        try {
            theK = args(0).toInt
            userName = args(1)
			ip_address = args(2)
        }
        catch {
            case e:ArrayIndexOutOfBoundsException =>
                   println ("Please provide arguments. E.g. Project1 3 geets56 192.168.1.109.10000")
                   System.exit(0)
        }
        val app = new MasterApplication(theK,userName,ip_address)
    }
}
