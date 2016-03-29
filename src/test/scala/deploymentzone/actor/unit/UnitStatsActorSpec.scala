package deploymentzone.actor.unit

import deploymentzone.actor._
import org.scalatest.{WordSpecLike, Matchers}
import akka.testkit.{ImplicitSender, TestActorRef}
import com.typesafe.config.ConfigFactory
import akka.actor.ActorInitializationException

class UnitStatsActorSpec
  extends TestKit("stats-actor-unit-spec")
  with WordSpecLike
  with Matchers
  with ImplicitSender {
  "StatsActor" when {
    "using configuration-only props" should {
      "get initialized with the expected values when all values are specified" in {
        import scala.concurrent.duration._
        val `stats-actor.conf` =
          """deploymentzone {
            |    akka-actor-statsd {
            |        port = 9999
            |        namespace = "mango"
            |        hostname = "127.0.0.1"
            |        packet-size = 100
            |        transmit-interval = 10000 ms
            |    }
            |}""".stripMargin
        val config = StatsConfig(ConfigFactory.parseString(`stats-actor.conf`))
        val subject = TestActorRef[StatsActor](StatsActor.props(config))
        subject.underlyingActor.config.address.getAddress.getHostAddress should be("127.0.0.1")
        subject.underlyingActor.config.address.getPort should be(9999)
        subject.underlyingActor.config.transmitInterval should be(10000.millis)
        subject.underlyingActor.config.packetSize should be(100)
        subject.underlyingActor.config.namespace should be("mango")
      }
    }
  }

}
