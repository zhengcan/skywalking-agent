inThisBuild(Seq(
  organization := "org.apache.skywalking",
  scalaVersion := "2.12.8",
  javacOptions in Compile ++= Seq("-encoding", "UTF-8", "-source", "1.8"),
  javacOptions in(Compile, compile) ++= Seq("-target", "1.8"),
  scalacOptions ++= Seq("-encoding", "UTF-8"),
  skyWalkingVersion := "6.6.0",
  licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
  bintrayOrganization := Some("can2020"),
))

lazy val `apm-asynchttpclient-v25x` = (project in file("plugins/asynchttpclient-2.5.x-plugin"))
  .enablePlugins(SkyWalkingAgent)
  .settings(
    name := "apm-asynchttpclient-2.5.x-plugin",
    normalizedName := name.value,
    libraryDependencies ++= Seq(
      "org.asynchttpclient" % "async-http-client" % "2.5.2" % Provided
    ),
    packageBin in Compile := (assembly in Compile).value
  )
lazy val `apm-play-v2x` = (project in file("plugins/play-2.x-plugin"))
  .enablePlugins(SkyWalkingAgent)
  .settings(
    name := "apm-play-2.x-plugin",
    normalizedName := name.value,
    libraryDependencies ++= Seq(
      component("play") % Provided
    ),
    packageBin in Compile := (assembly in Compile).value
  )

lazy val `skywalking-agents` = (project in file("."))
  .settings(
    skip in publish := true,
  )
  .aggregate(
    `apm-asynchttpclient-v25x`,
    `apm-play-v2x`,
  )


