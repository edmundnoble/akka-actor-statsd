package deploymentzone.actor

import akka.actor._
import deploymentzone.actor.validation.StatsDBucketValidator

import akka.util.ByteString
import deploymentzone.actor.domain.NamespaceTransformer

/**
 * An actor which sends counters to a StatsD instance via connected UDP.
  *
  * @param config configuration settings
 */
class StatsActor(val config: StatsConfig)
  extends Actor
  with Stash
  with ActorLogging
  with StatsProtocolImplementation {

  val namespaceTx = NamespaceTransformer(config.namespace)

  lazy val _connection: ActorRef = context.actorOf(UdpConnectedActor.props(config, self), "udp")

  override def connection = _connection

  override def process(msg: MaterializedMetric): Option[ByteString] = namespaceTx(msg, config.packetSize)

}

object StatsActor {
  def props(config: StatsConfig): Props = {
    Props(new StatsActor(config))
  }
}
