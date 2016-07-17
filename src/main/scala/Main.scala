import java.net.InetAddress
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import cache.ConcurrentAddressCache
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by jxx706 on 17/07/16.
  */
object Main extends App {
  implicit val system = ActorSystem()

  val cache = new ConcurrentAddressCache(3, TimeUnit.MINUTES)

  val address = InetAddress.getByName("127.0.0.1")
  val address2 = InetAddress.getByName("127.0.0.1")
  cache.add(address).foreach(println(_)) //true
  cache.add(address2).foreach(println(_))  //false
  cache.remove(address).foreach(println(_))  //true
  cache.remove(address2).foreach(println) //false
  cache.add(address).foreach(println) //true
  cache.peek().foreach(println) //Some
  Thread.sleep(70000)
  cache.peek().foreach(println) //Some
  Thread.sleep(70000)
  cache.peek().foreach(println) //Some
  Thread.sleep(70000)
  cache.peek().foreach(println) //None

}
