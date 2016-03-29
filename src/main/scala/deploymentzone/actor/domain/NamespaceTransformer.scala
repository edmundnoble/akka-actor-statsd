package deploymentzone.actor.domain

import akka.util.ByteString
import deploymentzone.actor.{MaterializedMetric, Metric}
import deploymentzone.actor.validation.StatsDBucketValidator

/**
 * Transforms the toString result value of a CounterMessage instance to include an
 * optional namespace.
 */
private[actor] class NamespaceTransformer(val namespace: ByteString) extends (MaterializedMetric => ByteString) {

  import NamespaceTransformer._

  require(StatsDBucketValidator(namespace.utf8String))

  override def apply(metric: MaterializedMetric): ByteString = {
    if (namespace.isEmpty) {
      metric.bytes
    } else {
      namespace ++ NAMESPACE_SEPARATOR ++ metric.bytes
    }
  }
}

private[actor] object NamespaceTransformer {
  val NAMESPACE_SEPARATOR = ByteString('.')
  def apply(namespace: String) = new NamespaceTransformer(ByteString(namespace))
}
