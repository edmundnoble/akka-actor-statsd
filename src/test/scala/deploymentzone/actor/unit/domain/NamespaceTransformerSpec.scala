package deploymentzone.actor.unit.domain

import org.scalatest.{Matchers, WordSpec}
import deploymentzone.actor.Metric
import java.lang.IllegalArgumentException

import akka.util.ByteString
import deploymentzone.actor.domain.NamespaceTransformer

class NamespaceTransformerSpec
  extends WordSpec
  with Matchers {

  "invoking NamespaceTransformer" when {
    "given an empty string argument" should {
      "return only the bucket name" in new NamespaceTest {
        NamespaceTransformer("")(counter) should be (counter.bytes)
      }
    }
    "given a valid namespace" should {
      "return the namespace.bucket" in new NamespaceTest {
        NamespaceTransformer("x.y")(counter) should be (ByteString("x.y.") ++ counter.bytes)
      }
    }
    "given a namespace that ends with a period" should {
      "throw an exception" in new NamespaceTest {
        an [IllegalArgumentException] should be thrownBy NamespaceTransformer("x.y.")(counter)
      }
    }
    "given a namespace that starts with a period" should {
      "throw an exception" in new NamespaceTest {
        an [IllegalArgumentException] should be thrownBy NamespaceTransformer(".x.y")(counter)
      }
    }
    "given a namespace that contains a reserved character" should {
      "throw an exception" in new NamespaceTest {
        an [IllegalArgumentException] should be thrownBy NamespaceTransformer("x@y")(counter)
      }
    }
  }

  private class NamespaceTest {
    val counter = Metric[Long]("&", "bucket", 1.0)(1)
  }

}
