package deploymentzone.actor.validation

import scala.util.matching.Regex

/**
 * Validates that a provided name won't cause downstream problems with statsd; that is no reserved characters are
 * present in the name.
 *
 * Periods (".") in the name are okay as these are statsd folder name separators. Periods at the end of start of
 * a name are not permitted.
 *
 * The reserved characters are colon (":"), pipe ("|"), at-symbol ("@") and backslash ("\").
 */
private[actor] object StatsDBucketValidator extends (String ⇒ Boolean) {
  val RESERVED_CHARACTERS = Seq(":", "|", "@", "\\")
  val RESERVED_CHARACTERS_STRING = RESERVED_CHARACTERS.mkString("\"", "\", \"", "\"")
  private val RESERVED_CHARACTERS_PATTERN = "[" + Regex.quoteReplacement(RESERVED_CHARACTERS.mkString) + "]"
  private val reserved = RESERVED_CHARACTERS_PATTERN.r

  /**
   * Validates that a string contains no special characters.
   *
   * @param name string to validate.
   */
  def apply(name: String): Boolean =
      reserved.findFirstIn(name).fold(true)(_ => false) &&
      !name.startsWith(".") &&
      !name.endsWith(".")

}
