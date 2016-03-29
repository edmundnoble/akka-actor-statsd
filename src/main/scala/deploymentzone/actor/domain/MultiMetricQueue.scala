package deploymentzone.actor.domain

import scala.collection.{immutable, mutable}
import deploymentzone.actor.PacketSize

import akka.event.{Logging, LoggingAdapter}
import akka.util.ByteString

/**
  * Logic for combining messages so they won't cross a predetermined packet size boundary.
  *
  * Takes UTF-8 byte size into account for messages.
  *
  * When a single message is larger than the provided [[packetSize]] that message is dropped
  * with a log warning.
  *
  * This class is not thread-safe.
  *
  * @param packetSize maximum byte size that is permitted for any given payload
  */
private[actor] class MultiMetricQueue(val packetSize: Int,
                                      val handleDroppedMessage: ByteString => Unit) {

  private val queue = mutable.Queue[ByteString]()
  private var last: Option[ByteString] = None

  /**
    * Enqueues a message for future dispatch.
    *
    * @param message message to enqueue
    * @return this instance (for convenience chaining)
    */
  def enqueue(message: ByteString): Unit = {
    if (message.length > packetSize) {
      handleDroppedMessage(message)
    } else {
      last = last.fold { Some(message) } { h => Some(merge(message, h)) }
    }
  }

  private def merge(message: ByteString, h: ByteString): ByteString = {
    if (message.length + h.length > packetSize) {
      queue.enqueue(h)
      message
    } else {
      h ++ MultiMetricQueue.NEWLINE ++ message
    }
  }

  /**
    * The remaining number of messages in the queue.
    *
    * Primarily provided for instrumentation and testing.
    *
    * @return number of messages remaining in the queue.
    */
  def size: Int = queue.size

  /**
    * Creates a StatsD payload message from the next items in the queue, which will be as large as possible up to
    * [[packetSize]].
    *
    * Items that are added to the payload are also removed from the queue.
    *
    * @return Newline separated list of StatsD messages up to the maximum [[packetSize]]
    */
  def payload(): Option[ByteString] = {
    if (queue.nonEmpty) {
      Some(queue.dequeue())
    } else {
      last.fold[Option[ByteString]] { None } { l => last = None; Some(l) }
    }
  }

}

object MultiMetricQueue {
  /**
    * Creates an instance of MultiMetricQueue.
    *
    * @param packetSize maximum packet size for a single aggregated message
    */
  def apply(packetSize: Int, logger: LoggingAdapter) =
    new MultiMetricQueue(packetSize, droppedMessageWarning(packetSize, logger))

  private[actor] def apply(packetSize: Int) = new MultiMetricQueue(packetSize, _ => ())

  def droppedMessageWarning(packetSize: Int, logger: LoggingAdapter): (ByteString => Unit) = { message =>
    if (logger.isWarningEnabled) {
      val DISCARD_MSG_MAX_LENGTH = 30
      val discardMsgLength = message.length
      val ellipsis = if (discardMsgLength > DISCARD_MSG_MAX_LENGTH) "..." else ""
      val discardMsg = message.slice(0, Math.min(DISCARD_MSG_MAX_LENGTH, discardMsgLength)).utf8String + ellipsis
      logger.warning(s"""Message dropped, length $discardMsgLength larger than max. packet size $packetSize: $discardMsg""")
    }
  }

  val NEWLINE = ByteString('\n')
}
