// scala format
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.0")

// coverage
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.0.7")
addSbtPlugin("org.scoverage" % "sbt-coveralls" % "1.3.6")

// release
addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.5.10")

addSbtPlugin("pl.project13.scala" % "sbt-jmh" % "0.4.4")

//addSbtPlugin("ch.epfl.scala" % "sbt-scala3-migrate" % "0.5.0")

addDependencyTreePlugin
