package deploymentzone.actor.domain

import scala.collection.{mutable, immutable}
import deploymentzone.actor.PacketSize
import scala.annotation.tailrec
import java.nio.charset.Charset
import akka.event.Logging
import akka.actor.ActorSystem

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
private[actor] class MultiMetricQueue(val packetSize: Int)(implicit system: ActorSystem) {
  val logger = Logging(system.eventStream, classOf[MultiMetricQueue])

  private val queue = mutable.Queue[String]()

  /**
   * Enqueues a message for future dispatch.
    *
    * @param message message to enqueue
   * @return this instance (for convenience chaining)
   */
  def enqueue(message: String): Unit = {
    queue.enqueue(message)
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
   * Creates a StatsD payload message from a list of messages up to the [[packetSize]] limit
   * in bytes taking UTF-8 size into account.
   *
   * Items that are added to the payload are also removed from the queue.
   *
   * @return Newline separated list of StatsD messages up to the maximum [[packetSize]]
   */
  def payload(): Option[String] = {
    val UTF8 = Charset.forName("utf-8")
    val builder = new StringBuilder

    @tailrec
    def recurse(utf8Length: Int = 0): String = {
      if (queue.isEmpty) {
        builder.toString()
      } else {
        val proposedAddition = queue.head.getBytes(UTF8).length
          if (proposedAddition > packetSize) {
            DroppedMessageWarning(proposedAddition, queue.head)
            queue.dequeue
            recurse(utf8Length)
          } else if (proposedAddition + utf8Length + 1 > (packetSize + 1)) {
            builder.toString()
          } else {
            val item = queue.dequeue
            builder.append(item)
            builder.append("\n")
            recurse(proposedAddition + utf8Length)
        }
      }
    }

    val result = recurse().stripLineEnd
    if (result.length > 0) {
      Some(result)
    } else {
      None
    }
  }

  private object DroppedMessageWarning extends ((Int, String) => Unit) {
    def apply(proposedAddition: Int, message: String) {
      if (!logger.isWarningEnabled)
        return

      val DISCARD_MSG_MAX_LENGTH = 25
      val discardMsgLength = message.length
      val ellipsis = if (discardMsgLength > DISCARD_MSG_MAX_LENGTH) "..." else ""
      val discardMsg = message.substring(0, Math.min(DISCARD_MSG_MAX_LENGTH, discardMsgLength)) + ellipsis
      logger.warning(s"""Message dropped, length $proposedAddition larger than max. packet size $packetSize: $discardMsg""")
    }
  }
}

object MultiMetricQueue {
  /**
   * Creates an instance of MultiMetricQueue.
   *
   * @param packetSize maximum packet size for a single aggregated message
   */
  def apply(packetSize: Int)(implicit system: ActorSystem) =
    new MultiMetricQueue(packetSize)(system)
}
