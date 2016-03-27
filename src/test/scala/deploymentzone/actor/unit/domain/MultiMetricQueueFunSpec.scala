package deploymentzone.actor.unit.domain

import org.scalatest.FunSpec
import deploymentzone.actor.domain.MultiMetricQueue
import deploymentzone.actor.{PacketSize, ImplicitActorSystem}

class MultiMetricQueueFunSpec
  extends FunSpec
  with ImplicitActorSystem {

  describe("A MultiMetricQueue") {
    def mkQueue = MultiMetricQueue(PacketSize.GIGABIT_ETHERNET)
    describe("when empty") {
      it("returns a None payload") {
        assert(mkQueue.payload().isEmpty)
      }
    }

    describe("when having a single element") {
      it("returns that element with no newline") {
        val subject = mkQueue
        subject.enqueue("message")
        assert(subject.payload() == Some("message"))
      }
    }

    describe("when having two elements") {
      it("returns the elements separated by a newline") {
        val subject = mkQueue
        subject.enqueue("message1")
        subject.enqueue("message2")
        assert(subject.payload() ==
          Some("""message1
            |message2""".stripMargin))
      }
    }

    describe("when the elements cross the packetSize boundary") {
      it("first returns one element then another") {
        val subject = MultiMetricQueue(4)
        subject.enqueue("dog")
        subject.enqueue("cat")
        assert(subject.payload() == Some("dog"))
        assert(subject.payload() == Some("cat"))
      }
    }

    describe("when a UTF-8 character crosses the packetSize boundary") {
      it("first returns one element then another") {
        val subject = MultiMetricQueue(2)
        subject.enqueue("ü")
        subject.enqueue("u")
        assert(subject.payload() == Some("ü"))
        assert(subject.payload() == Some("u"))
      }
    }

    describe("when a single message goes over the packetSize boundary") {
      it("drops the message") {
        val subject = MultiMetricQueue(4)
        subject.enqueue("12345")
        assert(subject.payload().isEmpty)
        assert(subject.size == 0)
      }
    }

    describe("when the first message goes over the packetSize boundary") {
      it("drops the oversized message but continues with other messages") {
        val subject = MultiMetricQueue(4)
        subject.enqueue("12345")
        subject.enqueue("1")
        subject.enqueue("2")
        assert(subject.payload() ==
          Some("""1
            |2""".stripMargin))
      }
    }

    describe("when any message goes over the packetSize boundary") {
      it("drops the oversized message but continues with other messages") {
        val subject = MultiMetricQueue(4)
        subject.enqueue("1")
        subject.enqueue("12345")
        subject.enqueue("2")
        assert(subject.payload() ==
          Some("""1
            |2""".stripMargin))
      }
    }
  }
}
