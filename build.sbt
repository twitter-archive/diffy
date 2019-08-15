import ReleaseTransformations._

lazy val buildSettings = Seq(
  organization := "com.twitter",
  scalaVersion := "2.11.7",
  crossScalaVersions := Seq("2.10.5", "2.11.7")
)

lazy val compilerOptions = Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-unchecked",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Xfuture"
)

lazy val testDependencies = Seq(
  "junit" % "junit" % "4.8.1",
  "org.mockito" % "mockito-all" % "1.8.5",
  "org.scalacheck" %% "scalacheck" % "1.12.4",
  "org.scalatest" %% "scalatest" % "2.2.5"
)

lazy val finatraVersion = "2.0.0.M2"

lazy val finatraDependencies = Seq(
  "com.twitter.finatra" %% "finatra-http" % finatraVersion,
  "com.twitter.finatra" %% "finatra-http" % finatraVersion % "test" classifier "tests",
  "com.twitter.inject" %% "inject-app" % finatraVersion % "test",
  "com.twitter.inject" %% "inject-app" % finatraVersion % "test" classifier "tests",
  "com.twitter.inject" %% "inject-core" % finatraVersion % "test",
  "com.twitter.inject" %% "inject-core" % finatraVersion % "test" classifier "tests",
  "com.twitter.inject" %% "inject-modules" % finatraVersion % "test",
  "com.twitter.inject" %% "inject-modules" % finatraVersion % "test" classifier "tests",
  "com.twitter.inject" %% "inject-server" % finatraVersion % "test",
  "com.twitter.inject" %% "inject-server" % finatraVersion % "test" classifier "tests"
)

lazy val baseSettings = Seq(
  resolvers += "Twitter's Repository" at "https://maven.twttr.com/",
  scalacOptions ++= compilerOptions ++ (
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 11)) => Seq("-Ywarn-unused-import")
      case _ => Nil
    }
  ),
  scalacOptions in (Compile, console) := compilerOptions,
  libraryDependencies ++= Seq(
    "io.netty" % "netty-tcnative-boringssl-static" % "2.0.25.Final",
    "com.twitter" %% "finagle-http" % "6.28.0",
    "com.twitter" %% "finagle-thriftmux" % "6.28.0",
    "com.twitter" %% "scrooge-generator" % "4.0.0",
    "javax.mail" % "mail" % "1.4.7",
    "org.jsoup" % "jsoup" % "1.7.2",
    "org.scala-lang" % "scala-compiler" % scalaVersion.value
  ) ++ finatraDependencies ++ testDependencies.map(_ % "test"),
  assemblyMergeStrategy in assembly := {
    case "BUILD" => MergeStrategy.discard
    case PathList("scala", "tools", _*) => MergeStrategy.last
    case other => MergeStrategy.defaultMergeStrategy(other)
  }
)

lazy val docSettings = site.settings ++ ghpages.settings ++ site.includeScaladoc("docs") :+ (
  git.remoteRepo := "git@github.com:twitter/diffy.git"
)

lazy val diffy = project.in(file("."))
  .settings(
    moduleName := "diffy",
    assemblyJarName := "diffy-server.jar",
    excludeFilter in unmanagedResources := HiddenFileFilter || "BUILD",
    unmanagedResourceDirectories in Compile +=
      baseDirectory.value / "src" / "main" / "webapp"
  )
  .settings(buildSettings ++ baseSettings ++ docSettings ++ publishSettings)

lazy val publishSettings = Seq(
  releaseCrossBuild := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  homepage := Some(url("https://github.com/twitter/diffy")),
  licenses := Seq("Apache 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  },
  autoAPIMappings := true,
  apiURL := Some(url("https://twitter.github.io/diffy/docs/")),
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/twitter/diffy"),
      "scm:git:git@github.com:twitter/diffy.git"
    )
  ),
  pomExtra := (
    <developers>
      <developer>
        <id>puneetkhanduri</id>
        <name>Puneet Khanduri</name>
        <url>https://twitter.com/pzdk</url>
      </developer>
    </developers>
  )
)

lazy val sharedReleaseProcess = Seq(
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    publishArtifacts,
    setNextVersion,
    commitNextVersion,
    ReleaseStep(action = Command.process("sonatypeReleaseAll", _)),
    pushChanges
  )
)

credentials ++= (
  for {
    username <- Option(System.getenv().get("SONATYPE_USERNAME"))
    password <- Option(System.getenv().get("SONATYPE_PASSWORD"))
  } yield Credentials(
    "Sonatype Nexus Repository Manager",
    "oss.sonatype.org",
    username,
    password
  )
).toSeq
