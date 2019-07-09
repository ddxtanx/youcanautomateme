val Http4sVersion = "0.20.3"

lazy val root = (project in file("."))
  .settings(
    organization := "com.ddxtanx",
    name := "youcanautomateme",
    version := "1",
    scalaVersion := "2.12.8",
    libraryDependencies ++= Seq(
      "org.http4s"      %% "http4s-blaze-client" % Http4sVersion,
      "org.http4s" %% "http4s-dsl" % Http4sVersion,
      "net.ruippeixotog" %% "scala-scraper" % "2.1.0",
      "org.slf4j" % "slf4j-api" % "1.7.26",
      "org.slf4j" % "slf4j-simple" % "1.7.26",
    )
  )

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-language:higherKinds",
  "-language:postfixOps",
  "-feature",
  "-Ypartial-unification",
  "-Xfatal-warnings",
)
