# README

This library provides cache for Internet addresses specially suited for multithreading scenarios. The cache comes with a self cleaning ability, removing the older elements (according to some user specified threshold) periodically (also specified by the user. The default is 15 minutes). The present document explains how to use the library.

### Dependencies ###

The following dependencies will be installed:
  
  * [Scala Test](http://www.scalatest.org/)
  * [Akka](http://akka.io/)
  * [Akka Testkit](http://doc.akka.io/docs/akka/snapshot/scala/testing.html)

To build the project, just run sbt:

```
sbt
```

To run the tests and see if everything's working:

```
sbt test
```

### Usage ###

First, you need to import the package into your code:

```
...
import java.util.concurrent.TimeUnit
import akka.actor.ActorSystem
import cache.ConcurrentAddressCache

implicit val system = ActorSystem("my-actor-system")

//Only elements aged 1 day or less will be kept in the cache.
val myCache = new ConcurrentAddressCache(1, TimeUnit.DAYS)

...
```

Then, follow these instructions for each method:

#### add(address: InetAddress)

Adds a new address to the cache only if it wasn't already there. In that case, returns true; otherwise false.

#### remove(address: InetAddress)

Removes an address already contained in the cache. If the removal was successful (i.e., the address existed) returns true; otherwise false.

#### peek()

Returns the newest element in the cache without removing it. If the cache is empty, returns None.

#### take()

Returns the newest element in the cache and deletes it. If the cache is empty, returns None.