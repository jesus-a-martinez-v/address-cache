package cache

import java.net.InetAddress
import java.util.concurrent.TimeUnit

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by jxx706 on 17/07/16.
  */
abstract class AddressCache(maxAge: Long, unit: TimeUnit)(implicit executionContext: ExecutionContext) {

  /**
    * add() method must store unique elements only (existing elements must be ignored).
    * This will return true if the element was successfully added.
    * @param address
    * @return
    */
  def add(address: InetAddress): Future[Boolean]

  /**
    * remove() method will return true if the address was successfully removed
    * @param address
    * @return
    */
  def remove(address: InetAddress): Future[Boolean]

  /**
    * The peek() method will return the most recently added element,
    * null if no element exists.
    * @return
    */
  def peek(): Future[Option[InetAddress]]

  /**
    * take() method retrieves and removes the most recently added element
    * from the cache and waits if necessary until an element becomes available.
    * @return
    */
  def take(): Future[Option[InetAddress]]

}
