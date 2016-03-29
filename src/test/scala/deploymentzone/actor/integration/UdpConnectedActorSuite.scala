package deploymentzone.actor.integration

import deploymentzone.actor.{StatsConfig, TestKit, UdpConnectedActor, UdpListenerActor}
import org.scalatest.FunSuiteLike
import akka.testkit.ImplicitSender
import java.net.InetSocketAddress

import akka.io.{Udp, UdpConnected}
import akka.util.ByteString

class UdpConnectedActorSuite
  extends TestKit("udp-connected-actor-suite")
  with FunSuiteLike
  with ImplicitSender {

  test("sends data") {
    val listener = system.actorOf(UdpListenerActor.props(testActor))
    val address = expectMsgClass(classOf[InetSocketAddress])
    val connected = system.actorOf(UdpConnectedActor.props(StatsConfig(address), testActor))
    connected ! UdpConnected.Connect
    expectMsg(UdpConnected.Connected)
    connected ! ByteString("data")
    expectMsg("data")
    connected ! UdpConnected.Disconnect
    listener ! Udp.Unbind
  }

}
