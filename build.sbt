//import com.trueaccord.scalapb.compiler.Version.scalapbVersion

/* scala versions and options */
scalaVersion := "2.12.7"

// These options will be used for *all* versions.
scalacOptions ++= Seq(
  "-deprecation",
  "-unchecked",
  "-encoding", "UTF-8",
  "-Xlint",
)

//val akka = "2.5.23"
val akka = "2.6.4"
//val kammon = "1.1.3"
//val kamon = "1.0.0"
val kamon = "2.1.0"
//val akkaHttp = "10.1.9"
val akkaHttp = "10.1.11"

/* dependencies */
libraryDependencies ++= Seq (
  // -- Logging --
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  // -- Akka --
  "com.typesafe.akka" %% "akka-actor"   % akka,
  "com.typesafe.akka" %% "akka-remote" % akka,
  "com.typesafe.akka" %% "akka-slf4j"   % akka,
  "com.typesafe.akka" %% "akka-cluster" % akka,
  "com.typesafe" % "config" % "1.3.2",
  "com.typesafe.akka" % "akka-cluster-metrics_2.12" % akka,
  "com.typesafe.akka" %% "akka-testkit" % akka % "test",
  "io.aeron" % "aeron-driver" % "1.26.0",
  "io.aeron" % "aeron-client" % "1.26.0"
)


//Publish/Subscribe tools
libraryDependencies += "com.typesafe.akka" %% "akka-cluster-tools" % akka

//HTTP REST
libraryDependencies += "com.typesafe.akka" %% "akka-http"   % akkaHttp
libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json"  % akkaHttp
libraryDependencies += "com.typesafe.akka" %% "akka-http-xml"  % akkaHttp


//--Alpakk--
libraryDependencies += "com.lightbend.akka" %% "akka-stream-alpakka-file" % "1.0.2"

libraryDependencies += "com.typesafe.akka" %% "akka-stream" % akka

// Kamon
libraryDependencies += "io.kamon" %% "kamon-akka" % kamon
//libraryDependencies += "io.kamon" %% "kamon-core" % kamon
//libraryDependencies += "io.kamon" %% "kamon-akka-2.5" % kamon
libraryDependencies += "io.kamon" %% "kamon-prometheus" % kamon
//libraryDependencies += "io.kamon" %% "kamon-zipkin" % "1.0.0"
//libraryDependencies += "io.kamon" %% "kamon-system-metrics" % kamon
//libraryDependencies += "io.kamon" %% "kamon-log-reporter" % "0.6.8"
//libraryDependencies += "io.kamon" %% "sigar-loader" % "1.6.5-rev003"


// RServe dependencies

libraryDependencies += "org.rosuda.REngine" % "REngine" % "2.1.0"
libraryDependencies += "org.rosuda.REngine" % "Rserve" % "1.8.1"

// ScalaPB (Protobuffer required for serialization)

//libraryDependencies += "com.trueaccord.scalapb"      %% "scalapb-runtime"  % scalapbVersion  % "protobuf"
libraryDependencies += "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf"

//Slack
libraryDependencies += "com.slack.api" % "slack-api-model" % "1.0.6"
libraryDependencies += "com.slack.api" % "slack-api-client" % "1.0.6"
libraryDependencies += "com.slack.api" % "slack-app-backend" % "1.0.6"

//Figaro
//libraryDependencies += "com.cra.figaro" %% "figaro" % "5.0.0.0"


//javaOptions += "-javaagent:/home/mep53/workspace/scala/kamon/aspectj-1.9.2/lib/aspectjweaver.jar"

javaOptions += "--add-opens java.base/java.lang=ALL-UNNAMED"

lazy val logback = "ch.qos.logback" % "logback-classic" % "1.2.3" % "runtime"
lazy val root = (project in file("."))
  .enablePlugins(JavaAppPackaging, JavaAgent) // (1)

javaAgents += "org.aspectj" % "aspectjweaver" % "1.9.2" % "dist"// (2)
javaOptions in Universal += "-Dorg.aspectj.tracing.factory=default" // (3)

//ensimeIgnoreScalaMismatch := true

//sbt:docker
import com.typesafe.sbt.packager.docker._


version in Docker := "0.6"

dockerExposedPorts in Docker := Seq(1600,9095,9411)
dockerRepository := Some("ngcdi")
packageName := "sckl-demo"
//dockerChmodType := DockerChmodType.UserGroupWriteExecute

//enablePlugins(JavaAppPackaging)
//enablePlugins(JavaAgent)
//enablePlugins(AspectJWeaver)
enablePlugins(AshScriptPlugin)

lazy val myuser = "vcf"
lazy val userid = "1003"
lazy val mygroup = "dial"
lazy val javadir = "openjdk-12"
lazy val jdkimg = "openjdk:12-alpine"
lazy val linuximg = "alpine:3.10"
lazy val javamodules = "java.base,java.instrument,java.net.http,java.logging,java.sql,java.xml,java.naming,java.security.sasl,jdk.unsupported,java.management,jdk.jfr"
lazy val sckldatadir = "/data"
lazy val entryScript = "demo-app"


dockerCommands := Seq(
  Cmd("FROM",jdkimg, "as", "stage0"),
  Cmd("ENV","USRNAME="+myuser),
  Cmd("ENV","GNAME="+mygroup),
  Cmd("WORKDIR","/opt/docker"),
  Cmd("RUN","mkdir"," -p /opt/docker"),
  Cmd("RUN","mkdir"," -p /opt/jre"),
  Cmd("WORKDIR","/opt/jre"),
  Cmd("RUN","jlink", "--module-path /opt/"+javadir+"/jmods --add-modules "+javamodules+" --output "+javadir),
  Cmd("WORKDIR","/opt/"),
  Cmd("RUN","rm -rf "+javadir),
  Cmd("RUN","mv /opt/jre/"+javadir+" /opt/"),
  Cmd("RUN","rm -rf jre"),
  Cmd("WORKDIR","/opt/docker"),
  Cmd("COPY","opt/docker /opt/docker"),
  Cmd("USER","root"),
  ExecCmd("RUN","chmod", "-R", "u=rwX,g=rwX", "/opt/docker"),
  ExecCmd("RUN","chmod", "u+x,g+x", "/opt/docker/bin/demo-app"),
  DockerStageBreak,
  Cmd("FROM",linuximg),
  Cmd("ENV","JAVA_HOME=/opt/"+javadir),
  Cmd("ENV","PATH=$JAVA_HOME/bin:$PATH"),
  Cmd("USER","root"),
  Cmd("RUN","addgroup","-S",mygroup),
  Cmd("RUN","id"," -u",myuser,"2>", "/dev/null", "||", "adduser", "-S", "-h "+myuser, "-u", userid, "-G", mygroup, myuser),
  Cmd("WORKDIR","/opt/docker"),
  Cmd("COPY","--from=stage0 --chown="+myuser+":"+mygroup+" /opt /opt"),
  ExecCmd("RUN","mkdir", "-p", "/opt/docker/logs", "/opt/docker/native", sckldatadir),
  ExecCmd("RUN","chown","-R",myuser+":"+mygroup, "/opt/docker/logs", "/opt/docker/native", sckldatadir),
  ExecCmd("VOLUME","/opt/docker/logs", "/opt/docker/native", sckldatadir),
  Cmd("USER",userid),
  ExecCmd("ENTRYPOINT","sh", "-c", "bin/"+entryScript+" $*"),
  //ExecCmd("ENTRYPOINT","/bin/sh"),
  //ExecCmd("CMD",""),
)




//dockerCommands += Cmd("COPY", "*.csv", s"${(defaultLinuxInstallLocation in Docker).value}/")

licenses := Seq(("CC0", url("http://creativecommons.org/publicdomain/zero/1.0")))

PB.targets in Compile := Seq(
  scalapb.gen() -> (sourceManaged in Compile).value
)


// Assembly config
logLevel in assembly := Level.Debug
assemblyJarName in assembly := "ngcdi-sckl-fat-1.0.jar"
assemblyMergeStrategy in assembly := {
  case "META-INF/aop.xml"                                => MergeStrategy.concat
  case PathList("reference.conf") => MergeStrategy.concat
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}
assemblyExcludedJars in assembly := {
  val cp = (fullClasspath in assembly).value
  cp filter {_.data.getName == "compile-0.1.0.jar"}
}
