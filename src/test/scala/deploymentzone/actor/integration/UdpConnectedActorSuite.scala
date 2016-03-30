package deploymentzone.actor.integration

import deploymentzone.actor._
import org.scalatest.FunSuiteLike
import akka.testkit.ImplicitSender
import java.net.InetSocketAddress

import akka.io.{Udp, UdpConnected}
import akka.util.ByteString

class UdpConnectedActorSuite
  extends TestKit("udp-connected-actor-suite")
  with FunSuiteLike
  with ImplicitSender
  with TestBase {

  test("sends data") {
    val listener = system.actorOf(UdpListenerActor.props(testActor))
    val address = expectMsgClass(classOf[InetSocketAddress])
    val connected = system.actorOf(UdpConnectedActor.props(StatsConfig(address), testActor))
    connected ! UdpConnected.Connect
    expectMsg(UdpConnected.Connected)
    connected ! ByteString("data")
    expectMsg(ByteString("data"))
    connected ! UdpConnected.Disconnect
    listener ! Udp.Unbind
  }

}
