package deploymentzone.actor.domain

import akka.util.ByteString
import deploymentzone.actor.{MaterializedMetric, Metric}
import deploymentzone.actor.validation.StatsDBucketValidator

/**
  * Transforms the toString result value of a CounterMessage instance to include an
  * optional namespace.
  */
private[actor] class NamespaceTransformer(val namespace: ByteString) extends ((MaterializedMetric, Int) => Option[ByteString]) {

  import NamespaceTransformer._

  require(StatsDBucketValidator(namespace.utf8String))

  override def apply(metric: MaterializedMetric, maxLength: Int): Option[ByteString] = {
    if (namespace.isEmpty) {
      metric.render(maxLength)
    } else {
      for {
        m <- metric.render(maxLength)
      } yield namespace ++ NAMESPACE_SEPARATOR ++ m
    }
  }
}

private[actor] object NamespaceTransformer {
  val NAMESPACE_SEPARATOR = ByteString('.')
  def apply(namespace: String) = new NamespaceTransformer(ByteString(namespace))
}
