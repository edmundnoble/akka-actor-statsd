package deploymentzone.actor

import scala.concurrent.duration._
import akka.actor._
import deploymentzone.actor.domain.MultiMetricQueue
import java.util.concurrent.TimeUnit

private[actor] class ScheduledDispatcherActor(config: Config, val receiver: ActorRef)
  extends Actor
  with ActorLogging {

  import ScheduledDispatcherActor._
  import context.system

  val packetSize = config.packetSize
  val transmitInterval = config.transmitInterval

  require(packetSize > 0, PACKET_SIZE_NEGATIVE_ZERO_MESSAGE(packetSize))
  require(transmitInterval.toMillis > 0, TRANSMIT_INTERVAL_NEGATIVE_ZERO_MESSAGE)

  log.debug(s"packetSize: $packetSize")
  log.debug(s"transmitInterval (millis): ${transmitInterval.toMillis}")

  if (transmitInterval.toMillis > 1.second.toMillis) {
    log.warning("Transmit interval set to a large value of {} milliseconds", transmitInterval)
  }

  val mmq = MultiMetricQueue(packetSize)(system)

  val recurringTransmit: Cancellable = system.scheduler.schedule(transmitInterval, transmitInterval, self, Transmit)(system.dispatcher, self)

  def receive = {
    case msg: String => mmq.enqueue(msg)
    case Transmit =>
      mmq.payload().foreach(payload => receiver ! payload)
  }

  override def postStop() {
    recurringTransmit.cancel()
  }

  private object Transmit
}

private[actor] object ScheduledDispatcherActor {
  def PACKET_SIZE_NEGATIVE_ZERO_MESSAGE(packetSize: Int) = s"packetSize $packetSize cannot be negative or 0"
  val TRANSMIT_INTERVAL_NEGATIVE_ZERO_MESSAGE = "transmitInterval cannot be negative or 0"

  def props(config: Config, receiver: ActorRef): Props = Props(new ScheduledDispatcherActor(config, receiver))
}