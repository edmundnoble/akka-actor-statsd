package deploymentzone.actor.integration

import deploymentzone.actor._
import org.scalatest.{Matchers, WordSpecLike}
import java.net.InetSocketAddress

import akka.testkit.{ImplicitSender, TestProbe}
import akka.io.Udp

import scala.concurrent.duration._
import akka.actor.Terminated
import akka.util.ByteString

class IntegrationStatsActorSpec
  extends TestKit("stats-actor-integration-spec")
    with WordSpecLike
    with Matchers
    with ImplicitSender {

  "StatsActor" when {
    "initialized with an empty namespace" should {
      "send the expected message" in new Environment {
        val stats = system.actorOf(StatsActor.props(StatsConfig(address)), "stats")
        val msg = Count.increment("dog")
        stats ! msg
        expectMsg(msg.bytes)

        shutdown()
      }
    }
    "initialized with a namespace" should {
      "send the expected message" in new Environment {
        val stats = system.actorOf(StatsActor.props(StatsConfig(address).copy(namespace = "name.space")), "stats-ns")
        val msg = Gauge("gauge")(340L)
        stats ! msg
        expectMsg(ByteString("name.space.") ++ msg.bytes)

        shutdown()
      }
      "sending the same message over and over again does not alter the message" in new Environment {
        val stats = system.actorOf(StatsActor.props(StatsConfig(address).copy(namespace = "name.space")), "stats-ns-repeat")
        val msg = Count.increment("kittens")
        stats ! msg
        expectMsg(ByteString("name.space.") ++ msg.bytes)
        stats ! msg
        expectMsg(ByteString("name.space.") ++ msg.bytes)
      }
    }
    "sending multiple messages quickly in sequence" should {
      "transmit all the messages" in new Environment {
        val stats = system.actorOf(StatsActor.props(StatsConfig(address).copy(transmitInterval = 1.second)), "stats-mmsg")
        val msgs = Seq(Timing("xyz")(40.seconds),
          Count.increment("ninjas"),
          Count.decrement("pirates"),
          Gauge("ratchet")(0xDEADBEEF))
        msgs.foreach(stats ! _)
        expectMsg(ByteString(msgs.map(_.bytes.utf8String).mkString("\n").stripLineEnd))

        shutdown()
      }
    }
    "multiple instances" should {
      "all deliver their messages" in new Environment {
        val stats1 = system.actorOf(StatsActor.props(StatsConfig(address)), "mi-stats1")
        val stats2 = system.actorOf(StatsActor.props(StatsConfig(address)), "mi-stats2")
        val msg1 = Count.increment("count")
        val msg2 = Count.decrement("count")
        stats1 ! msg1
        stats2 ! msg2
        expectMsgAllOf(msg1.bytes, msg2.bytes)
      }
    }
  }

  private class Environment {
    val listener = system.actorOf(UdpListenerActor.props(testActor))
    val address = expectMsgClass(classOf[InetSocketAddress])

    def shutdown() {
      listener ! Udp.Unbind
    }
  }


}
