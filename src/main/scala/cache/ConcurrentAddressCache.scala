package cache

import java.net.InetAddress
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._


/**
  * Cache for Internet addresses specially suited for multithreading scenarios.
  * @param maxAge Max age allowed for the elements in the cache.
  * @param unit Time unit associated with `maxAge`
  * @param actorSystem Actor system used to spawn internal actor.
  */
class ConcurrentAddressCache(maxAge: Long, unit: TimeUnit)(implicit actorSystem: ActorSystem)
  extends AddressCache(maxAge, unit) {
  private val cacheKeeper = actorSystem.actorOf(AddressCacheKeeper.props(maxAge, unit))
  private implicit val timeout = Timeout(5 seconds)  //TODO: Export to config file.


  /**
    * Stores unique elements only (existing elements are be ignored).
    * This will return true if the element was successfully added.
    *
    * @param address Address to be stored.
    * @return Boolean wrapped in a future manifesting the status of the insertion.
    */
  override def add(address: InetAddress): Future[Boolean] =
  (cacheKeeper ? AddressCacheKeeper.Add(address))
      .mapTo[AddressCacheKeeper.OperationStatus]
      .map(_.status)

  /**
    * Returns true if the address was successfully removed
    *
    * @param address Address to be removed.
    * @return Boolean wrapped in a future manifesting the status of the removal.
    */
  override def remove(address: InetAddress): Future[Boolean] =
  (cacheKeeper ? AddressCacheKeeper.Remove(address))
      .mapTo[AddressCacheKeeper.OperationStatus]
      .map(_.status)

  /**
    * Returns the most recently added element.
    *
    * @return Returns an Option with the most recent added element (in a Some if exists, None otherwise)
    *         wrapped in a Future.
    */
  override def peek(): Future[Option[InetAddress]] =
  (cacheKeeper ? AddressCacheKeeper.Peek)
      .mapTo[AddressCacheKeeper.OperationResult]
      .map(_.result)

  /**
    * Retrieves and removes the most recently added element
    * from the cache and waits if necessary until an element becomes available.
    *
    * @return Returns an Option with the most recent added element (in a Some if exists, None otherwise)
    *         wrapped in a Future.
    */
  override def take(): Future[Option[InetAddress]] =
    (cacheKeeper ? AddressCacheKeeper.Take)
      .mapTo[AddressCacheKeeper.OperationResult]
      .map(_.result)
}
