package deploymentzone.actor

import java.util.concurrent.TimeUnit
import java.net.InetSocketAddress

import deploymentzone.actor.validation.StatsDBucketValidator

import scala.concurrent.duration._

private[actor] case class StatsConfig(hostname: String,
                                      port: Int,
                                      namespace: String,
                                      packetSize: Int,
                                      transmitInterval: FiniteDuration) {

  require(StatsDBucketValidator(namespace),
    s"""reserved characters (${StatsDBucketValidator.RESERVED_CHARACTERS}) may not be used in namespaces and namespaces may not start or end with a period (".")""")

  val address = new InetSocketAddress(hostname, port)
}


private[actor] object StatsConfig {
  val path = "deploymentzone.akka-actor-statsd"

  def apply(underlyingConfig: com.typesafe.config.Config) =
    typesafeStatsConfig(underlyingConfig)
  private[actor] def apply(hostname: String, port: Int = Defaults.STATSD_UDP_PORT) =
    defaultStatsConfig(hostname, port)
  private[actor] def apply(address: InetSocketAddress) =
    defaultStatsConfig(address.getHostName, address.getPort)

  private[actor] def typesafeStatsConfig(underlyingConfig: com.typesafe.config.Config): StatsConfig =
    StatsConfig(
      hostname = underlyingConfig.getString(s"$path.hostname"),
      port = underlyingConfig.getInt(s"$path.port"),
      namespace = underlyingConfig.getString(s"$path.namespace"),
      packetSize = underlyingConfig.getInt(s"$path.packet-size"),
      transmitInterval =
        underlyingConfig.getDuration(s"$path.transmit-interval", TimeUnit.MILLISECONDS).millis
    )

  private[actor] def defaultStatsConfig(hostname: String, port: Int): StatsConfig =
    StatsConfig(
      hostname = hostname,
      port = port,
      namespace = Defaults.NAMESPACE,
      packetSize = Defaults.PACKET_SIZE,
      transmitInterval = Defaults.TRANSMIT_INTERVAL
    )
}

object Defaults {
  val STATSD_UDP_PORT = 8125
  val PACKET_SIZE = PacketSize.FAST_ETHERNET
  val TRANSMIT_INTERVAL = 100.milliseconds
  val NAMESPACE = ""
}
