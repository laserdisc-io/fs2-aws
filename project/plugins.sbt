// scala format
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.6")

// coverage
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.4.2")
addSbtPlugin("org.scoverage" % "sbt-coveralls" % "1.3.15")

// release
addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.11.2")

addSbtPlugin("pl.project13.scala" % "sbt-jmh" % "0.4.8")

//addSbtPlugin("ch.epfl.scala" % "sbt-scala3-migrate" % "0.5.0")

addDependencyTreePlugin
