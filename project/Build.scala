import sbt._
import Keys._

object NetLogoBuild extends Build {

  lazy val root =
    Project(id = "NetLogo", base = file("."))
      .configs(Testing.configs: _*)
      .settings(Defaults.defaultSettings ++
                Testing.settings ++
                Packaging.settings ++
                ChecksumsAndPreviews.settings: _*)
      .aggregate(headless)
      .dependsOn(headless % "test->test;compile->compile")

  lazy val headless =
    Project(id = "headless",
            base = file("headless"))
      .configs(Testing.configs: _*)
      .settings(Testing.settings: _*)

}
