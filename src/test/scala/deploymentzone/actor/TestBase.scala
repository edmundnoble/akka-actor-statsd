package deploymentzone.actor

import akka.util.ByteString

trait TestBase {

  import scala.language.implicitConversions

  implicit def noLengthLimit(m: MaterializedMetric): ByteString = m.render(Int.MaxValue).get

}
