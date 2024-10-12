// scala format
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.2")

// coverage
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.1.1")
addSbtPlugin("org.scoverage" % "sbt-coveralls" % "1.3.13")

// release
addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.7.0")

addSbtPlugin("pl.project13.scala" % "sbt-jmh" % "0.4.7")

//addSbtPlugin("ch.epfl.scala" % "sbt-scala3-migrate" % "0.5.0")

addDependencyTreePlugin
