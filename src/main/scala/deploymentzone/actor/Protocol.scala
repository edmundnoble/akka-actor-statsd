package deploymentzone.actor

import akka.util.ByteString
import deploymentzone.actor.validation.StatsDBucketValidator

private[actor] class Metric[@specialized(Int, Long, Double) T](symbol: String, bucket: String, samplingRate: Double) {
  require(StatsDBucketValidator(bucket),
    s"""reserved characters (${StatsDBucketValidator.RESERVED_CHARACTERS}) may not be used in buckets and buckets may not start or end with a period (".")""")

  final val suffix = ByteString('|') ++ {
    if (samplingRate != 1.0)
      ByteString(symbol) ++ ByteString("|@") ++ ByteString(samplingRate.toString)
    else
      ByteString(symbol)
  }
  final val prefix = ByteString(bucket) ++ ByteString(':')

  def renderValue(t: T): ByteString = ByteString(String.valueOf(t))

  def apply(t: T): MaterializedMetric =
    MaterializedMetric(prefix ++ renderValue(t) ++ suffix)

}

object Metric {
  private[actor] def apply[@specialized T](symbol: String, bucket: String, samplingRate: Double): Metric[T] =
    new Metric(symbol, bucket, samplingRate)
}

case class MaterializedMetric(private[actor] val bytes: ByteString) {
  final def render(maxLength: Int): Option[ByteString] = {
    if (bytes.length < maxLength) {
      Some(bytes)
    } else {
      None
    }
  }
}

class Count(bucket: String, samplingRate: Double = 1.0)
  extends Metric[Int](Count.SYMBOL, bucket, samplingRate) {
  override def renderValue(i: Int) = ByteString(String.valueOf(i))
}

object Count {
  val SYMBOL = "c"

  def apply(bucket: String, samplingRate: Double = 1.0) =
    new Count(bucket, samplingRate)

  def increment(bucket: String): MaterializedMetric = apply(bucket)(1)
  def decrement(bucket: String): MaterializedMetric = apply(bucket)(-1)
}

class Gauge(bucket: String, samplingRate: Double)
  extends Metric[Long](Gauge.SYMBOL, bucket, samplingRate) {
  override def renderValue(l: Long) = ByteString(String.valueOf(l))
}

object Gauge {
  val SYMBOL = "g"
  def apply(bucket: String, samplingRate: Double = 1.0) =
    new Gauge(bucket, samplingRate)
}

class GaugeAdd(bucket: String, samplingRate: Double)
  extends Gauge(bucket, samplingRate) {
  override def renderValue(t: Long) = ByteString('+') ++ ByteString(String.valueOf(Math.abs(t)))
}

object GaugeAdd {
  def apply(bucket: String, samplingRate: Double = 1.0) =
    new GaugeAdd(bucket, samplingRate)
}

class GaugeSubtract(bucket: String, samplingRate: Double)
  extends Gauge(bucket, samplingRate) {
  override def renderValue(t: Long) = ByteString('-') ++ ByteString(String.valueOf(Math.abs(t)))
}

object GaugeSubtract {
  def apply(bucket: String, samplingRate: Double = 1.0) =
    new GaugeSubtract(bucket, samplingRate)
}

class Set(bucket: String, samplingRate: Double = 1.0)
  extends Metric[Long](Set.SYMBOL, bucket, samplingRate) {
  override def renderValue(l: Long) = ByteString(String.valueOf(l))
}

object Set {
  val SYMBOL = "s"

  def apply(bucket: String, samplingRate: Double = 1.0) =
    new Set(bucket, samplingRate)
}

import scala.concurrent.duration.Duration

class Timing(bucket: String, samplingRate: Double = 1.0)
  extends Metric[Duration](Timing.SYMBOL, bucket, samplingRate) {
  override def renderValue(t: Duration) = ByteString(String.valueOf(t.toMillis))
}

object Timing {

  val SYMBOL = "ms"

  def apply(bucket: String, samplingRate: Double = 1.0) =
    new Timing(bucket, samplingRate)
}
