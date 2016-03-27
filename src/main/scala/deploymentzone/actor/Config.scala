package deploymentzone.actor

import java.util.concurrent.TimeUnit
import java.net.InetSocketAddress
import com.typesafe.config.ConfigFactory
import scala.concurrent.duration._

private[actor] trait Config {
  def hostname: String
  def port: Int
  def address = new InetSocketAddress(hostname, port)
  def namespace: String
  def packetSize: Int
  def transmitInterval: FiniteDuration
}

private[actor] class TypesafeConfig(val underlyingConfig: com.typesafe.config.Config) extends Config {
  import Config.path
  override lazy val hostname = underlyingConfig.getString(s"$path.hostname")
  override lazy val port = underlyingConfig.getInt(s"$path.port")
  override lazy val namespace = underlyingConfig.getString(s"$path.namespace")
  override lazy val packetSize = underlyingConfig.getInt(s"$path.packet-size")
  override lazy val transmitInterval =
    underlyingConfig.getDuration(s"$path.transmit-interval", TimeUnit.MILLISECONDS).millis
}

private[actor] class DefaultConfig(val hostname: String,
                                   val port: Int) extends Config {
  override val namespace: String = ""
  override val packetSize: Int = Defaults.PACKET_SIZE
  override val transmitInterval = Defaults.TRANSMIT_INTERVAL
}

private[actor] object Config {
  val path = "deploymentzone.akka-actor-statsd"

  def apply(underlyingConfig: com.typesafe.config.Config) =
    new TypesafeConfig(underlyingConfig)
  private[actor] def apply(hostname: String, port: Int) =
    new DefaultConfig(hostname, port)
  private[actor] def apply(address: InetSocketAddress) =
    new DefaultConfig(address.getHostName, address.getPort)
}

object Defaults {
  val STATSD_UDP_PORT = 8125
  val PACKET_SIZE = PacketSize.FAST_ETHERNET
  val TRANSMIT_INTERVAL = 100.milliseconds
  
  private[actor] lazy val underlyingConfig = ConfigFactory.load()

  lazy val config: Config = Config(underlyingConfig)
}
