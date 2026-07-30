import laika.ast.LengthUnit.px
import laika.ast.Path.Root
import laika.ast.{Image, InlineSVGIcon}
import laika.config.SyntaxHighlighting
import laika.helium.config.*
import laika.sbt.LaikaPlugin.autoImport.*
import laika.theme.config.Color
import org.typelevel.sbt.TypelevelSitePlugin.autoImport.*
import mdoc.MdocPlugin.autoImport.mdocVariables
import sbt.*
import sbt.Keys.{isSnapshot, version}
import sbtdynver.DynVerPlugin.autoImport.dynverGitDescribeOutput

import scala.sys.process.*

//noinspection TypeAnnotation
object DocConfig {

  // latest stable release across all tags, not just those reachable from HEAD:
  // v6.x maintenance releases live on the series/6.x branch, so sbt-dynver's
  // previousStableVersion (which walks ancestry from HEAD) can't see them.
  private val latestStableRelease = Def.setting {
    val current = version.value
    if (!isSnapshot.value && current.matches("""\d+\.\d+\.\d+""")) Some(current)
    else {
      val StableTag = """v(\d+)\.(\d+)\.(\d+)""".r
      "git tag".!!.linesIterator
        .collect { case StableTag(maj, min, patch) => (maj.toInt, min.toInt, patch.toInt) }
        .reduceOption(Ordering[(Int, Int, Int)].max)
        .map { case (ma, mi, pa) => s"$ma.$mi.$pa" }
    }
  }

  // temporary hack until we get v7 past RC
  private val latestPreRelease = Def.setting {
    val PreRelease    = """(\d+)\.(\d+)\.(\d+)-\w[\w.]*""".r
    val StableVersion = """(\d+)\.(\d+)\.(\d+)""".r
    val stable        = latestStableRelease.value.collect { case StableVersion(ma, mi, pa) =>
      (ma.toInt, mi.toInt, pa.toInt)
    }
    dynverGitDescribeOutput.value.map(_.ref.dropPrefix).collect {
      case v @ PreRelease(ma, mi, pa)
          if stable.forall(Ordering[(Int, Int, Int)].gt((ma.toInt, mi.toInt, pa.toInt), _)) =>
        v
    }
  }

  // maybe see if the laika project wants this
  private val mavenCentralIcon = InlineSVGIcon(
    """<svg class="svg-icon" width="100%" height="100%" viewBox="0 0 100 100" version="1.1" xmlns="http://www.w3.org/2000/svg">
      |  <g class="svg-shape">
      |    <path fill-rule="evenodd" d="M50 4 L89.8 27 L89.8 73 L50 96 L10.2 73 L10.2 27 Z M50 18 L77.7 34 L77.7 66 L50 82 L22.3 66 L22.3 34 Z"/>
      |  </g>
      |</svg>""".stripMargin,
    title = Some("Maven Central")
  )

  val FS2AWS = Seq(
    tlSiteApiUrl := Some(url("https://fs2aws.laserdisc.io/api/")),
    // sbt-typelevel resolves VERSION to a pre-release when no stable release is bin-compatible with it;
    // we always want the stable release there, with the pre-release surfaced on the landing page
    mdocVariables += "VERSION" -> latestStableRelease.value.getOrElse(version.value),
    laikaIncludeAPI            := true,
    laikaExtensions += SyntaxHighlighting,
    tlSiteIsTypelevelProject := None,
    tlSiteHelium             :=
      tlSiteHelium.value.all
        .themeColors(
          primary = Color.hex("cc6600"),
          primaryLight = Color.hex("fdf4ea"),
          primaryMedium = Color.hex("fbe9d6"),
          secondary = Color.hex("5b7980"),
          text = Color.hex("5f5f5f"),
          background = Color.hex("ffffff"),
          bgGradient = (Color.hex("77420d"), Color.hex("b85c00"))
        )
        .site
        // fit 140 chars in code blocks: 140 * 8.4px (Fira Mono @ 14px) + 24px pre padding + 2*45px content padding
        .layout(contentWidth = px(1300))
        .site
        .internalCSS(Root / "landing.css")
        .site
        .internalCSS(Root / "site.css")
        .site
        .darkMode
        .disabled
        .site
        .topNavigationBar(
          homeLink = IconLink.external("https://fs2aws.laserdisc.io/", HeliumIcon.home, text = Some("fs2-aws"))
        )
        .site
        .pageNavigation(enabled = true, depth = 1, keepOnSmallScreens = false)
        .site
        .mainNavigation(
          appendLinks = Seq(
            ThemeNavigationSection(
              "Related Projects",
              TextLink.external("https://github.com/aws/aws-sdk-java-v2", "aws-sdk-java-v2"),
              TextLink.external("https://github.com/awslabs/amazon-kinesis-client", "amazon-kinesis-client"),
              TextLink.external("https://github.com/awslabs/amazon-kinesis-producer", "amazon-kinesis-producer"),
              TextLink.external("https://fs2.io", "fs2")
            )
          )
        )
        .site
        .footer(
          """fs2-aws is a <a href="https://github.com/laserdisc-io">LaserDisc</a> project released under the <a href="https://opensource.org/licenses/MIT">MIT licence</a>."""
        )
        .site
        .landingPage(
          logo = Some(Image.internal(Root / "fs2-aws-logo.png", alt = Some("fs2-aws logo"), height = Some(px(150)))),
          title = Some("fs2-aws"),
          subtitle = Some("FS2 streaming wrappers for the AWS SDK"),
          latestReleases = (
            latestStableRelease.value.map { v =>
              ReleaseInfo(
                "Latest Release",
                s"""<a href="https://github.com/laserdisc-io/fs2-aws/releases/tag/v$v">$v</a>"""
              )
            } ++
              latestPreRelease.value.map { v =>
                ReleaseInfo(
                  "Latest Pre-Release",
                  s"""<a href="https://github.com/laserdisc-io/fs2-aws/releases/tag/v$v">$v</a>"""
                )
              }
          ).toSeq,
          license = Some("""<a href="https://github.com/laserdisc-io/fs2-aws/blob/main/LICENSE">MIT</a>"""),
          titleLinks = Seq(
            VersionMenu.create(unversionedLabel = "Getting Started"),
            LinkGroup.create(
              IconLink.external("https://github.com/laserdisc-io/fs2-aws", HeliumIcon.github),
              IconLink.external("https://central.sonatype.com/search?q=io.laserdisc.fs2-aws", mavenCentralIcon)
            )
          ),
          linkPanel = Some(
            LinkPanel(
              "Documentation",
              TextLink.internal(Root / "modules" / "fs2-aws-s3.md", "fs2-aws-s3"),
              TextLink.internal(Root / "modules" / "fs2-aws-kinesis.md", "fs2-aws-kinesis"),
              TextLink.internal(Root / "modules" / "fs2-aws-sqs.md", "fs2-aws-sqs"),
              TextLink.internal(Root / "modules" / "fs2-aws-sns.md", "fs2-aws-sns"),
              TextLink.internal(Root / "modules" / "fs2-aws-dynamodb.md", "fs2-aws-dynamodb"),
              TextLink.internal(Root / "modules" / "fs2-aws-testkit.md", "fs2-aws-testkit"),
              TextLink.internal(Root / "modules" / "pure-aws.md", "pure-aws"),
              TextLink.internal(Root / "migration.md", "Migrating to 7.x")
            )
          ),
          teasers = Seq(
            Teaser(
              "Purely Functional",
              "fs2-aws wraps the AWS SDK v2 async clients (and, for Kinesis, the KCL/KPL) in purely " +
                "functional, resource-safe APIs built on cats-effect and FS2, sharing FS2's design goals: " +
                "compositionality, expressiveness, resource safety, and speed."
            ),
            Teaser(
              "Streaming",
              "Read and write S3 objects, consume Kinesis and SQS, publish to SNS, and scan DynamoDB " +
                "tables as back-pressured FS2 streams and pipes — processing large payloads in constant space."
            ),
            Teaser(
              "Batteries Included",
              "Modules are published for Scala 2.13 and 3, alongside generated tagless-final wrappers " +
                "for the AWS SDK v2 async clients."
            )
          )
        )
  )

}
