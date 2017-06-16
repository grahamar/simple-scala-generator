name := "simple-scala-generator"
organization := "io.grhodes"
version := "git describe --tags --dirty --always".!!.stripPrefix("v").trim

scalaVersion := "2.12.2"

crossScalaVersions := Seq(scalaVersion.value, "2.11.11", "2.10.6")

javacOptions in doc := Seq("-encoding", "UTF-8")

licenses += ("MIT", url("http://opensource.org/licenses/MIT"))

libraryDependencies ++= Seq(
  "io.swagger" % "swagger-codegen" % "2.2.0",
  "org.scalatest" %% "scalatest" % "2.2.6" % "test",
  "org.scalacheck" %% "scalacheck" % "1.11.5" % "test"
)
