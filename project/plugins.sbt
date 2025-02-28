// scala format
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.4")

// coverage
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.3.1")
addSbtPlugin("org.scoverage" % "sbt-coveralls" % "1.3.15")

// release
addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.9.2")

addSbtPlugin("pl.project13.scala" % "sbt-jmh" % "0.4.7")

//addSbtPlugin("ch.epfl.scala" % "sbt-scala3-migrate" % "0.5.0")

addDependencyTreePlugin
