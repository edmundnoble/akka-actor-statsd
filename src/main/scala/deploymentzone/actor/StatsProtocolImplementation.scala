package deploymentzone.actor

import akka.actor.{Actor, ActorLogging, ActorRef, Stash}
import akka.io.UdpConnected
import akka.util.ByteString

private[actor] trait StatsProtocolImplementation
  { this: Actor with Stash with ActorLogging =>

  protected def connection: ActorRef
  private var scheduledDispatcher: ActorRef = _
  protected[this] val config: StatsConfig

  protected def process(msg: MaterializedMetric): ByteString

  override def preStart() {
    connection ! UdpConnected.Connect
    scheduledDispatcher = context.actorOf(ScheduledDispatcherActor.props(config, connection), "scheduled")
  }

  override def receive = connectionPending

  protected def connectionPending: Actor.Receive = {
    case UdpConnected.Connected =>
      unstashAll()
      context.become(connected)
    case x => stash()
  }
  
  protected def connected: Actor.Receive = {
    case msg: MaterializedMetric =>
      scheduledDispatcher ! process(msg)
  }
}
