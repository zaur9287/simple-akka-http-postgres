name := "simplehttpslickpostgres"

scalaVersion := "2.11.8"

organization := "Zaur"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
resolvers += "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"

libraryDependencies ++= List(
    "com.typesafe"            %    "config"               %     "1.2.0",
    "org.postgresql"          %    "postgresql"           %     "9.4-1201-jdbc41",
    "org.json4s"              %%   "json4s-native"        %     "3.4.0",

    "com.typesafe.akka"       %%   "akka-http"            %     "10.1.9",
    "com.typesafe.akka"       %%   "akka-stream-testkit"  %     "2.5.23",
    "com.typesafe.akka"       %%   "akka-http-testkit"    %     "10.1.9",
    "org.scalatest"           %    "scalatest_2.10"       %     "2.0"       % "test"
)

// set the main class for packaging the main jar
mainClass in (Compile, packageBin) := Some("com.base.app.WebServer")

// set the main class for the main 'sbt run' task
mainClass in (Compile, run) := Some("com.base.app.WebServer")