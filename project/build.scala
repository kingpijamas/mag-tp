import sbt._
import Keys._
import org.scalatra.sbt._
import org.scalatra.sbt.PluginKeys._
import com.earldouglas.xwp.JettyPlugin
import com.mojolly.scalate.ScalatePlugin._
import ScalateKeys._


object MagTpBuild extends Build {
  val Organization     = "org.mag"
  val Name             = "MAG-TP"
  val Version          = "0.1.0-SNAPSHOT"
  val ScalaVersion     = "2.11.8"
  val ScalatraVersion  = "2.4.1"
  val JettyVersion     = "9.2.15.v20160210"
  val AkkaVersion      = "2.4.10"
  val MacwireVersion   = "2.2.5"
  val ScalatestVersion = "3.0.1"

  lazy val project = Project (
    "mag-tp",
    file("."),
    settings = ScalatraPlugin.scalatraSettings ++ scalateSettings ++ Seq(
      organization := Organization,
      name := Name,
      version := Version,
      scalaVersion := ScalaVersion,
      resolvers += Classpaths.typesafeReleases,
      resolvers += "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases",
      libraryDependencies ++= Seq(
        // Actors
        "com.typesafe.akka"           %% "akka-actor"                   % AkkaVersion,
        "com.typesafe.akka"           %% "akka-testkit"                 % AkkaVersion        % "test",
        // DI
        "com.softwaremill.macwire"    %% "macros"                       % MacwireVersion,
        "com.softwaremill.macwire"    %% "util"                         % MacwireVersion,
        "com.softwaremill.macwire"    %% "proxy"                        % MacwireVersion,
        // math & statistics
        "org.scalanlp"                %% "breeze"                       % "0.12", // FIXME: kill, unused!
        // Web
        "org.scalatra"                %% "scalatra"                     % ScalatraVersion,
        "org.scalatra"                %% "scalatra-scalate"             % ScalatraVersion,
        "org.scalatra"                %% "scalatra-atmosphere"          % ScalatraVersion,
        // "org.scalatra" %% "scalatra-specs2" % ScalatraVersion % "test",
        "org.eclipse.jetty"           % "jetty-plus"                    % JettyVersion       % "compile;provided",
        "org.eclipse.jetty"           % "jetty-webapp"                  % JettyVersion       % "compile",
        "org.eclipse.jetty.websocket" % "websocket-server"              % JettyVersion       % "compile;provided",
        // "org.eclipse.jetty" % "jetty-server" % JettyVersion,
        "javax.servlet"               % "javax.servlet-api"             % "3.1.0"            % "compile;provided;test",
        // Logging
        "ch.qos.logback"              % "logback-classic"               % "1.1.5"            % "runtime",
        // JSON
        "org.json4s"                  %% "json4s-jackson"               % "3.4.2",
        "io.spray"                    %% "spray-json"                   % "1.3.2",
        "com.github.fommil"           %% "spray-json-shapeless"         % "1.3.0",
        // Atmosphere client
        "org.webjars"                 % "jquery"                        % "1.11.1",
        "org.webjars"                 % "jquery-atmosphere"             % "2.2.13",
        // Style
        "org.webjars"                 % "bootstrap"                     % "3.3.7-1",
        // Plots
        "org.webjars"                 % "d3js"                          % "3.4.13",
        "org.webjars.bower"           % "epoch"                         % "0.8.4",
        // Tests
        "org.scalactic"               %% "scalactic"                    % ScalatestVersion,
        "org.scalatest"               %% "scalatest"                    % ScalatestVersion  % "test",
        "org.scalamock"               %% "scalamock-scalatest-support"  % "3.4.1"           % "test"
  ),
      scalateTemplateConfig in Compile <<= (sourceDirectory in Compile) { base =>
        Seq(
          TemplateConfig(
            base / "webapp" / "WEB-INF" / "templates",
            Seq.empty,  /* default imports should be added here */
            Seq(
              Binding(
                "context",
                "_root_.org.scalatra.scalate.ScalatraRenderContext",
                importMembers = true,
                isImplicit = true
              )
            ),  /* add extra bindings here */
            Some("templates")
          )
        )
      }
    )
  ).enablePlugins(JettyPlugin)


}
