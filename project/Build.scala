import sbt._
import sbt.Keys._

object Build extends sbt.Build {

  lazy val project = Project(
    id = "akka-actor-statsd",
    base = file("."),
    settings = Defaults.coreDefaultSettings ++ Seq(
      name                  := "akka-actor-statsd",
      organization          := "com.deploymentzone",
      version               := "0.4-SNAPSHOT",
      licenses              := Seq("MIT" -> url("http://opensource.org/licenses/MIT")),
      homepage              := Some(url("https://github.com/cfeduke/akka-actor-statsd/")),
      scalaVersion          := "2.11.7",
      scalacOptions         := Seq("-deprecation", "-feature", "-encoding", "utf8"),
      libraryDependencies   ++= Dependencies.allDeps,
      publishMavenStyle     := true,
      publishTo := {
        val nexus = "https://oss.sonatype.org/"
        if (version.value.trim.endsWith("SNAPSHOT"))
          Some("snapshots" at nexus + "content/repositories/snapshots")
        else
          Some("releases"  at nexus + "service/local/staging/deploy/maven2")
      },
      pomIncludeRepository := { _ => false },
      pomExtra :=
          <scm>
            <url>git@github.com:cfeduke/akka-actor-statsd.git</url>
            <connection>scm:git:git@github.com:cfeduke/akka-actor-statsd.git</connection>
          </scm>
          <developers>
            <developer>
              <id>cfeduke</id>
              <name>Charles Feduke</name>
              <url>http://www.deploymentzone.com</url>
            </developer>
          </developers>
    )
  )

  object Dependencies {

    object Versions {
      val akka              = "2.3.11"
      val scalatest         = "2.2.6"
      val logback           = "1.1.6"
    }

    val compileDependencies = Seq(
      "com.typesafe.akka"   %%  "akka-actor"      % Versions.akka,
      "com.typesafe.akka"   %%  "akka-slf4j"      % Versions.akka,
      "ch.qos.logback"      %   "logback-classic" % Versions.logback
    )

    val testDependencies = Seq(
      "com.typesafe.akka"   %% "akka-testkit"     % Versions.akka         % "test",
      "org.scalatest"       %% "scalatest"        % Versions.scalatest    % "test"
    )

    val allDeps: Seq[ModuleID] = compileDependencies ++ testDependencies

  }

}
