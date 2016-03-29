package deploymentzone.actor.unit

import deploymentzone.actor._
import org.scalatest.FunSuiteLike
import akka.testkit.ImplicitSender
import akka.actor.{Actor, ActorLogging, ActorRef, Props, Stash}
import akka.io.UdpConnected
import akka.util.ByteString

import scala.concurrent.duration._
import com.typesafe.config.ConfigFactory

class StatsProtocolImplementationSuite
  extends TestKit("stats-protocol-implementation-suite")
  with FunSuiteLike
  with ImplicitSender {

  test("connects and then relays a message") {
    val stats = system.actorOf(NoOpStatsActor.props(testActor))
    expectMsg(UdpConnected.Connect)
    stats ! UdpConnected.Connected
    val msg = Count.increment("ninjas")
    stats ! msg
    expectMsg(msg.bytes)
  }

  test("stashes messages until connection is established") {
    val stats = system.actorOf(NoOpStatsActor.props(testActor))
    expectMsg(UdpConnected.Connect)
    val msgs = Seq(Count.decrement("turtles"),
                   Gauge("ninjas", 5.0)(4000L),
                   Timing("eric.likes.haskell")(9.seconds))
    msgs.foreach(msg => stats ! msg)
    stats ! UdpConnected.Connected
    expectMsg(ByteString(msgs.map(_.bytes.utf8String).mkString("\n").stripLineEnd))
  }

  private class NoOpStatsActor(val connection : ActorRef)
    extends Actor
    with Stash
    with ActorLogging
    with StatsProtocolImplementation {

    override protected[this] val config = StatsConfig("invalid_hostname")

    override protected def process(msg: MaterializedMetric) = msg.bytes

  }

  private object NoOpStatsActor {
    def props(connection: ActorRef) = Props(new NoOpStatsActor(connection))
  }

}
