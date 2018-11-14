name := "fs2-aws-testkit"

scalaVersion := "2.12.7"

pgpPublicRing := file("../.travis/local.pubring.asc")
pgpSecretRing := file("../.travis/local.secring.asc")
releasePublishArtifactsAction := PgpKeys.publishSigned.value
pgpPassphrase := sys.env.get("PGP_PASS").map(_.toCharArray)

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest"   % "3.0.4",
  "org.mockito"   % "mockito-core" % "2.23.0"
)
