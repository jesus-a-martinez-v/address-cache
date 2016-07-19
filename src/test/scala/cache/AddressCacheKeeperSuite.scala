package cache

import java.net.InetAddress
import java.util.concurrent.TimeUnit

import akka.actor.{ActorRef, ActorSystem, PoisonPill}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest._

import scala.concurrent.duration._

/**
  * Test suite for AddressCacheKeeper actor.
  */
class AddressCacheKeeperSuite extends TestKit(ActorSystem("KeeperSpec"))
  with FunSuiteLike with BeforeAndAfterAll with ImplicitSender {

  private def addAddress(actor: ActorRef, address: InetAddress) = {
    actor ! AddressCacheKeeper.Add(address)
  }

  private def removeAddress(actor: ActorRef, address: InetAddress) = {
    actor ! AddressCacheKeeper.Remove(address)
  }

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  test("Should insert an address if it's unique") {
    val actor = system.actorOf(AddressCacheKeeper.props(1, TimeUnit.DAYS))
    addAddress(actor, InetAddress.getByName("127.0.0.1"))
    expectMsg(AddressCacheKeeper.OperationStatus(true))
  }

  test("Shouldn't insert an address if it's already present in the cache") {
    val actor = system.actorOf(AddressCacheKeeper.props(1, TimeUnit.DAYS))
    val address1 = InetAddress.getByName("127.0.0.1")
    val address2 = InetAddress.getByName("127.0.0.1")

    addAddress(actor, address1)
    expectMsg(AddressCacheKeeper.OperationStatus(true))

    addAddress(actor, address2)
    expectMsg(AddressCacheKeeper.OperationStatus(false))
  }

  test("Should remove an element only if it exist") {
    val actor = system.actorOf(AddressCacheKeeper.props(1, TimeUnit.DAYS))

    addAddress(actor, InetAddress.getByName("127.0.0.1"))
    expectMsg(AddressCacheKeeper.OperationStatus(true))

    removeAddress(actor, InetAddress.getByName("127.0.0.1"))
    expectMsg(AddressCacheKeeper.OperationStatus(true))
  }

  test("Should not remove non existing elements") {
    val actor = system.actorOf(AddressCacheKeeper.props(1, TimeUnit.DAYS))
    removeAddress(actor, InetAddress.getLocalHost)
    expectMsg(AddressCacheKeeper.OperationStatus(false))
  }

  test("Must peek and return the most recent added element without removing it") {
    val actor = system.actorOf(AddressCacheKeeper.props(1, TimeUnit.DAYS))
    val newAddress = InetAddress.getByName("123.123.123.1")
    addAddress(actor, newAddress)
    expectMsg(AddressCacheKeeper.OperationStatus(true))

    //First, let's peek
    actor ! AddressCacheKeeper.Peek
    assert(expectMsgClass(classOf[AddressCacheKeeper.OperationResult]).result.get === newAddress)

    //Then, let's check the element still exist by trying to add it again.
    addAddress(actor, newAddress)
    expectMsg(AddressCacheKeeper.OperationStatus(false))
  }

  test("Must return None when peeking an empty cache") {
    val emptyCacheActor = system.actorOf(AddressCacheKeeper.props(1, TimeUnit.DAYS))
    emptyCacheActor ! AddressCacheKeeper.Peek
    assert(expectMsgClass(classOf[AddressCacheKeeper.OperationResult]).result.isEmpty)
  }

  test("Must take and return the most recent added element and remove it") {
    val actor = system.actorOf(AddressCacheKeeper.props(1, TimeUnit.DAYS))
    val newAddress = InetAddress.getByName("123.123.123.1")
    addAddress(actor, newAddress)
    expectMsg(AddressCacheKeeper.OperationStatus(true))

    //First, let's peek
    actor ! AddressCacheKeeper.Take
    assert(expectMsgClass(classOf[AddressCacheKeeper.OperationResult]).result.get === newAddress)

    //Then, let's check the element doesn't exist by trying to add it again.
    addAddress(actor, newAddress)
    expectMsg(AddressCacheKeeper.OperationStatus(true))
  }

  test("Must return None when taking from an empty cache") {
    val emptyCacheActor = system.actorOf(AddressCacheKeeper.props(1, TimeUnit.DAYS))
    emptyCacheActor ! AddressCacheKeeper.Take
    assert(expectMsgClass(classOf[AddressCacheKeeper.OperationResult]).result.isEmpty)
  }

  test("Must clean cache after an specified period of time") {
    val impatientActor = system.actorOf(AddressCacheKeeper.props(10, TimeUnit.SECONDS, 5 seconds))

    val oldestAddress = InetAddress.getByName("1.2.3.4")
    val newestAddress = InetAddress.getByName("3.4.5.6")

    //This address will only live for 10 seconds.
    addAddress(impatientActor, oldestAddress)
    expectMsg(AddressCacheKeeper.OperationStatus(true))

    //We wait 15 seconds.
    Thread.sleep(15000)

    //We add a new address
    addAddress(impatientActor, newestAddress)
    expectMsg(AddressCacheKeeper.OperationStatus(true))

    //Given around 15 seconds has passed, oldestAddress has already been cleaned, so trying to remove it should return false.
    removeAddress(impatientActor, oldestAddress)
    expectMsg(AddressCacheKeeper.OperationStatus(false))

    //newestAddress should still be alive, so removing it should return true.
    removeAddress(impatientActor, newestAddress)
    expectMsg(AddressCacheKeeper.OperationStatus(true))
  }

}
