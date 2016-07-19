package cache

import java.net.InetAddress
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import org.scalatest.{BeforeAndAfterAll, FunSuite}
import org.scalatest.concurrent.ScalaFutures


/**
  * Test suite for ConcurrentAddressCache implementation of AddressCache.
  */
class ConcurrentAddressCacheSuite extends FunSuite with ScalaFutures with BeforeAndAfterAll {
  implicit val system = ActorSystem()

  override def afterAll(): Unit = {
    system.terminate()
  }

  test("Must add a non-existent address") {
    val address = InetAddress.getByName("127.0.0.1")
    val cache = new ConcurrentAddressCache(1, TimeUnit.DAYS)

    whenReady(cache.add(address)) { result =>
      assert(result)
    }
  }

  test("Must return false when attempting to insert already present address") {
    val address1 = InetAddress.getByName("127.0.0.1")
    val address2 = InetAddress.getByName("127.0.0.1")
    val cache = new ConcurrentAddressCache(1, TimeUnit.DAYS)

    cache.add(address1)

    whenReady(cache.add(address2)) { result =>
      assert(!result)
    }
  }

  test("Must remove element if it's present in the cache") {
    val address = InetAddress.getByName("127.0.0.1")
    val cache = new ConcurrentAddressCache(1, TimeUnit.DAYS)

    cache.add(address)
    whenReady(cache.remove(address)) { result =>
      assert(result)
    }
  }

  test("Must return false when attempting to remove element not present in the cache") {
    val address = InetAddress.getByName("127.0.0.1")
    val cache = new ConcurrentAddressCache(1, TimeUnit.DAYS) //Empty

    whenReady(cache.remove(address)) { result =>
      assert(!result)
    }
  }

  test("Must peek most recent element without deleting it") {
    val olderAddress = InetAddress.getByName("127.0.0.1")
    val newestAddress = InetAddress.getByName("200.1.8.156")

    val cache = new ConcurrentAddressCache(1, TimeUnit.DAYS) //Empty
    cache.add(olderAddress)
    cache.add(newestAddress)

    whenReady(cache.peek()) { result =>
      assert(result.isDefined)
      assert(result.get === newestAddress)
    }

    //Trying to add the address should be forbidden.
    whenReady(cache.add(newestAddress)) { result =>
      assert(!result)
    }
  }

  test("Peeking from an empty cache should return None") {
    val cache = new ConcurrentAddressCache(1, TimeUnit.DAYS)

    whenReady(cache.peek()) { result =>
      assert(result.isEmpty)
    }
  }

  test("take() should return the most recent element and delete it from the cache") {
    val olderAddress = InetAddress.getByName("127.0.0.1")
    val newestAddress = InetAddress.getByName("200.1.8.156")

    val cache = new ConcurrentAddressCache(1, TimeUnit.DAYS) //Empty
    cache.add(olderAddress)
    cache.add(newestAddress)

    whenReady(cache.take()) { result =>
      assert(result.isDefined)
      assert(result.get === newestAddress)
    }

    //Trying to add the address should be allowed.
    whenReady(cache.add(newestAddress)) { result =>
      assert(result)
    }
  }

  test("taking from an empty cache return None") {
    val cache = new ConcurrentAddressCache(1, TimeUnit.DAYS)

    whenReady(cache.take()) { result =>
      assert(result.isEmpty)
    }
  }
}
