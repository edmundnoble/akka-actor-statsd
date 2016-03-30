package deploymentzone.actor.unit.domain

import org.scalatest.{Matchers, WordSpec}
import deploymentzone.actor.{MaterializedMetric, Metric, TestBase}
import java.lang.IllegalArgumentException

import akka.util.ByteString
import deploymentzone.actor.domain.NamespaceTransformer

class NamespaceTransformerSpec
  extends WordSpec
  with Matchers
  with TestBase {

  "invoking NamespaceTransformer" when {
    "given an empty string argument" should {
      "return only the bucket name" in new NamespaceTest {
        NamespaceTransformer("")(counter, Int.MaxValue) should be (Some(noLengthLimit(counter)))
      }
    }
    "given a valid namespace" should {
      "return the namespace.bucket" in new NamespaceTest {
        NamespaceTransformer("x.y")(counter, Int.MaxValue) should be (Some(ByteString("x.y.") ++ noLengthLimit(counter)))
      }
    }
    "given a namespace that ends with a period" should {
      "throw an exception" in new NamespaceTest {
        an [IllegalArgumentException] should be thrownBy NamespaceTransformer("x.y.")(counter, Int.MaxValue)
      }
    }
    "given a namespace that starts with a period" should {
      "throw an exception" in new NamespaceTest {
        an [IllegalArgumentException] should be thrownBy NamespaceTransformer(".x.y")(counter, Int.MaxValue)
      }
    }
    "given a namespace that contains a reserved character" should {
      "throw an exception" in new NamespaceTest {
        an [IllegalArgumentException] should be thrownBy NamespaceTransformer("x@y")(counter, Int.MaxValue)
      }
    }
  }

  private class NamespaceTest {
    val counter = Metric[Long]("&", "bucket", 1.0)(1)
  }

}
