
name := """play_redis_websocket"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion in ThisBuild := "2.13.1"

libraryDependencies ++= Seq(
  jdbc,
  ws,
  guice,
  "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % Test,
  "redis.clients" % "jedis" % "3.6.1"
)

