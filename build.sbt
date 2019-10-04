organization := "ch.unibas.cs.gravis"

name := "shape18-asm-sampling"

licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))

scalaVersion := "2.12.8"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8", "-target:jvm-1.8")

resolvers += Resolver.jcenterRepo

resolvers += Resolver.bintrayRepo("unibas-gravis", "maven")

libraryDependencies ++= Seq(
  "ch.unibas.cs.gravis" % "scalismo-native-all" % "4.0.+",
  "ch.unibas.cs.gravis" %% "scalismo" % "0.17.1",

  "com.github.tototoshi" %% "scala-csv" % "1.3.4",

  "org.rogach" %% "scallop" % "2.1.3",

  "io.github.pityka" %% "nspl-awt" % "0.0.19",
  "io.github.pityka" %% "nspl-scalatags-jvm" % "0.0.19"
)

assemblyMergeStrategy in assembly := {
 case PathList("META-INF", xs @ _*) => MergeStrategy.discard
 case x => MergeStrategy.first
}

mainClass in assembly := Some("shape18.Liver")
assemblyJarName in assembly := "shape18-asm-sampling.jar"

enablePlugins(GitVersioning)

enablePlugins(BuildInfoPlugin)
buildInfoKeys := BuildInfoKey.ofN(name, version, scalaVersion, sbtVersion)
buildInfoPackage := "probabilisticFittingASM.utils"
