package deploymentzone.actor.unit.domain

import akka.util.ByteString
import org.scalatest.FunSpec
import deploymentzone.actor.domain.MultiMetricQueue
import deploymentzone.actor.{ImplicitActorSystem, PacketSize}

class MultiMetricQueueFunSpec
  extends FunSpec
  with ImplicitActorSystem {

  describe("A MultiMetricQueue") {
    def mkQueue = MultiMetricQueue(PacketSize.GIGABIT_ETHERNET)
    def enqueueStr(q: MultiMetricQueue, str: String): Unit = { q.enqueue(ByteString(str)) }
    describe("when empty") {
      it("returns a None payload") {
        assert(mkQueue.payload().isEmpty)
      }
    }

    describe("when having a single element") {
      it("returns that element with no newline") {
        val subject = mkQueue
        val message = ByteString("message")
        subject.enqueue(message)
        assert(subject.payload().contains(message))
      }
    }

    describe("when having two elements") {
      it("returns the elements separated by a newline") {
        val subject = mkQueue
        enqueueStr(subject, "message1")
        enqueueStr(subject, "message2")
        assert(subject.payload().contains(ByteString(
          """message1
            |message2""".stripMargin)))
      }
    }

    describe("when the elements cross the packetSize boundary") {
      it("first returns one element then another") {
        val subject = MultiMetricQueue(4)
        enqueueStr(subject, "dog")
        enqueueStr(subject, "cat")
        assert(subject.payload().contains(ByteString("dog")))
        assert(subject.payload().contains(ByteString("cat")))
      }
    }

    describe("when a UTF-8 character crosses the packetSize boundary") {
      it("first returns one element then another") {
        val subject = MultiMetricQueue(2)
        enqueueStr(subject, "ü")
        enqueueStr(subject, "u")
        assert(subject.payload().contains(ByteString("ü")))
        assert(subject.payload().contains(ByteString("u")))
      }
    }

    describe("when a single message goes over the packetSize boundary") {
      it("drops the message") {
        val subject = MultiMetricQueue(4)
        enqueueStr(subject, "12345")
        assert(subject.payload().isEmpty)
        assert(subject.size == 0)
      }
    }

    describe("when the first message goes over the packetSize boundary") {
      it("drops the oversized message but continues with other messages") {
        val subject = MultiMetricQueue(4)
        enqueueStr(subject, "12345")
        enqueueStr(subject, "1")
        enqueueStr(subject, "2")
        assert(subject.payload().contains(ByteString(
          """1
            |2""".stripMargin)))
      }
    }

    describe("when any message goes over the packetSize boundary") {
      it("drops the oversized message but continues with other messages") {
        val subject = MultiMetricQueue(4)
        enqueueStr(subject, "1")
        enqueueStr(subject, "12345")
        enqueueStr(subject, "2")
        assert(subject.payload().contains(ByteString(
          """1
            |2""".stripMargin)))
      }
    }
  }
}
