package cache

import java.net.InetAddress
import java.util.Calendar
import java.util.concurrent.TimeUnit

import akka.actor.{Actor, Props}
import cache.AddressCacheKeeper.{OperationResult, OperationStatus}

import scala.collection.immutable.SortedMap
import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
  * @param maxAge Max aged allowed for a member of the cache.
  * @param unit Time unit related to `maxAge`
  * @param frequency Time frequency used to clean the cache (e.g. every 15 minutes)
  */
class AddressCacheKeeper(maxAge: Long, unit: TimeUnit, frequency: FiniteDuration = 15 minutes) extends Actor {
  /**
    * Max allowed age of an address expressed as a finite duration object.
    */
  private val age = FiniteDuration(maxAge, unit)

  /**
    * Maps timestamp to address. This mapping is sorted so the take and peek operations are performed
    * in effective constant time.
    */
  private var stampToAddress: SortedMap[Long, InetAddress] = SortedMap()

  /**
    * Maps addresses to timestamp. This mapping is for adding and removing in effective constant time.
    */
  private val addressToStamp: mutable.HashMap[InetAddress, Long] = mutable.HashMap()

  /**
    * Filters out the oldest elements in cache every `frequency` units of time.
    * We keep the reference so we can cancel it when this actor dies/stops.
    */
  private val scheduler = context.system.scheduler.schedule(
    frequency,
    frequency,
    context.self,
    AddressCacheKeeper.Clean)

  /**
    * Cancels the scheduled clean task after the actor has stopped.
    */
  override def postStop(): Unit = {
    if (!scheduler.isCancelled) {
      scheduler.cancel()
    }
    super.postStop()
  }

  override def receive: Receive = {
    case AddressCacheKeeper.Add(address) =>
      val originalSender = sender()

      if (!addressToStamp.contains(address)) {
        val now = Calendar.getInstance().getTimeInMillis

        addressToStamp(address) = now
        stampToAddress = stampToAddress + (now -> address)

        originalSender ! OperationStatus(true)
      } else {
        originalSender ! OperationStatus(false)
      }
    case AddressCacheKeeper.Remove(address) =>
      val originalSender = sender()

      if (addressToStamp.contains(address)) {
        val timestamp = addressToStamp(address)

        stampToAddress = stampToAddress - timestamp
        addressToStamp -= address

        originalSender ! OperationStatus(true)
      } else {
        originalSender ! OperationStatus(false)
      }
    case AddressCacheKeeper.Peek =>
      sender() ! OperationResult(stampToAddress.headOption.map(_._2))
    case AddressCacheKeeper.Take =>
      val originalSender = sender()

      if (stampToAddress.isEmpty) {
        originalSender ! OperationResult(None)
      } else {
        val (_, address) = stampToAddress.head
        stampToAddress = stampToAddress.tail
        addressToStamp -= address

        originalSender ! OperationResult(Some(address))
      }
    case AddressCacheKeeper.Clean =>
      val now = Calendar.getInstance().getTimeInMillis

      //Filter out elements older than 'maxAge'
      val (keepThis, removeThis) = stampToAddress.span {
        //Remember than first element of the pair contains the date (in milliseconds) when de address was added.
        now - _._1 < age.toMillis
      }

      stampToAddress = keepThis
      addressToStamp --= removeThis.values
  }

}

object AddressCacheKeeper {
  //Inbound messages.
  case object Peek
  case object Take
  case object Clean
  case class Add(address: InetAddress)
  case class Remove(address: InetAddress)

  //Outbound messages.
  case class OperationStatus(status: Boolean)
  case class OperationResult(result: Option[InetAddress])

  /**
    * Factory method for the cache.AddressCacheKeeper actor.
    * @param maxAge Max aged allowed for a member of the cache.
    * @param unit Time unit related to `maxAge`
    * @param frequency Time frequency used to clean the cache (e.g. every 15 minutes)
    * @return Props instance of an AddressCacheKeeper actor.
    */
  def props(maxAge: Long, unit: TimeUnit, frequency: FiniteDuration = 15 minutes): Props =
    Props(new AddressCacheKeeper(maxAge, unit, frequency))
}