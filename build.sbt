import scala.sys.process._

name := "simple-scala-generator"
organization := "io.grhodes"
version := "git describe --tags --dirty --always".!!.stripPrefix("v").trim

scalaVersion := "2.12.2"

//crossScalaVersions := Seq(scalaVersion.value, "2.11.11", "2.10.6")

javacOptions in doc := Seq("-encoding", "UTF-8")

licenses += ("MIT", url("http://opensource.org/licenses/MIT"))

libraryDependencies ++= Seq(
  "io.swagger.codegen.v3" % "swagger-codegen-generators" % "1.0.8",
  "io.swagger.codegen.v3" % "swagger-codegen" % "3.0.8",
  "org.scalatest" %% "scalatest" % "3.0.1" % Test,
  "org.scalacheck" %% "scalacheck" % "1.13.4" % Test
)
