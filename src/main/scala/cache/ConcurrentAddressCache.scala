package cache

import java.net.InetAddress
import java.util.concurrent.{TimeUnit, TimeoutException}

import akka.actor.ActorSystem
import akka.pattern.{AskTimeoutException, ask}
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

  /**
    * Actor that handles the internal representation of the cache. We will perform each operation by passing him a
    * message and waiting for his reply.
    */
  private val cacheKeeper = actorSystem.actorOf(AddressCacheKeeper.props(maxAge, unit))

  /**
    * Implicit timeout for the ask operation.
    */
  private implicit val timeout = Timeout(5.seconds)


  /**
    * Stores unique elements only (existing elements are be ignored).
    * This will return true if the element was successfully added.
    *
    * @param address Address to be stored.
    * @return Boolean wrapped in a future manifesting the status of the insertion.
    */
  override def add(address: InetAddress): Future[Boolean] = withErrorHandling {
    (cacheKeeper ? AddressCacheKeeper.Add(address))
      .mapTo[AddressCacheKeeper.OperationStatus]
      .map(_.status)
  }


  /**
    * Returns true if the address was successfully removed
    *
    * @param address Address to be removed.
    * @return Boolean wrapped in a future manifesting the status of the removal.
    */
  override def remove(address: InetAddress): Future[Boolean] = withErrorHandling {
    (cacheKeeper ? AddressCacheKeeper.Remove(address))
      .mapTo[AddressCacheKeeper.OperationStatus]
      .map(_.status)
  }

  /**
    * Returns the most recently added element.
    *
    * @return Returns an Option with the most recent added element (in a Some if exists, None otherwise)
    *         wrapped in a Future.
    */
  override def peek(): Future[Option[InetAddress]] = withErrorHandling {
    (cacheKeeper ? AddressCacheKeeper.Peek)
      .mapTo[AddressCacheKeeper.OperationResult]
      .map(_.result)
  }

  /**
    * Retrieves and removes the most recently added element
    * from the cache and waits if necessary until an element becomes available.
    *
    * @return Returns an Option with the most recent added element (in a Some if exists, None otherwise)
    *         wrapped in a Future.
    */
  override def take(): Future[Option[InetAddress]] = withErrorHandling {
    (cacheKeeper ? AddressCacheKeeper.Take)
      .mapTo[AddressCacheKeeper.OperationResult]
      .map(_.result)
  }

  //Wraps the evaluation of a future into a error handling context.
  private def withErrorHandling[T](f: => Future[T]) =
    try {
      f
    } catch {
      case _: AskTimeoutException => throw new TimeoutException(s"Couldn't complete operation within ${timeout.duration}")
      //Add more cases here...
    }
}
