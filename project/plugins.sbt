// scala format
addSbtPlugin("com.geirsson" % "sbt-scalafmt" % "1.5.1")

// coverage
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.1")
addSbtPlugin("org.scoverage" % "sbt-coveralls" % "1.2.5")

// release
addSbtPlugin("com.typesafe.sbt"  % "sbt-git"         % "1.0.0")
addSbtPlugin("org.scala-sbt"     % "sbt-autoversion" % "1.0.0")
addSbtPlugin("com.github.gseitz" % "sbt-release"     % "1.0.9")
addSbtPlugin("com.jsuereth"      % "sbt-pgp"         % "1.1.1")
