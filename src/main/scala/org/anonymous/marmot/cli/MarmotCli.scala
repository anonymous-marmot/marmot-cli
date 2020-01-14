// Copyright (C) 2018 The Marmot Team.
// See the LICENCE file distributed with this work for additional
// information regarding copyright ownership.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.anonymous.marmot.cli

import com.softwaremill.sttp._
import org.anonymous.marmot.cli.commands.{FeaturesCommand, RetrieveCommand, SearchCommand, TestCommand}

/**
  * The application class for the Marmot command line interface
  */
object MarmotCli {


  def main(args: Array[String]): Unit = {

    def getEnvOrElse(envVar: String, defaultPath: String) = sys.env.getOrElse(envVar, defaultPath)

    val javaLibPath = getEnvOrElse("JAVA_LIB_PATH", "/usr/lib/jvm/default-java/lib/")

    val trustStorePath = getEnvOrElse("JAVA_TRUSTSTORE", "/usr/lib/jvm/default-java/lib/security/cacerts")

    // This only is allowed to be set for GraalVM compiles...
    //System.setProperty("java.library.path", javaLibPath)
    //System.setProperty("javax.net.ssl.trustStore", trustStorePath)

    cliParser.parse(args, Config()) match {
      case Some(c) =>


        implicit val config: Config = c
        implicit val backend: SttpBackend[Id, Nothing] = HttpURLConnectionBackend()

        if (!config.silent && config.mode != "") cliParser.showHeader()

        config.mode match {
          case "test" => TestCommand.execute
          case "retrieve" => RetrieveCommand.execute
          case "search" => SearchCommand.execute
          case "features" => FeaturesCommand.execute
          case "" => cliParser.showUsage()
          case x => config.consoleOutput.outputError(s"Unknown command: $x")
        }


      case None =>
    }

  }

  private def cliParser = {
    val parser = {
      new scopt.OptionParser[Config]("marmot-cli") {
        head("Marmot Command Line Tool", s"(${BuildInfo.version})")

        version("version").text("Prints the version of the command line tool.")

        help("help").text("Prints this help text.")

        override def showUsageOnError = true

        opt[String]("server").action((x, c) => c.copy(server = x)).text("The url to the Marmot server")
        opt[Unit](name = "raw").action((_, c) => c.copy(raw = true)).text("Output the raw results")
        opt[Unit](name = "silent").action((_, c) => c.copy(silent = true)).text("Suppress non-result output")

        checkConfig(c => if (c.server.isEmpty()) failure("Option server is required.") else success)

        cmd("test").action((_, c) => c.copy(mode = "test"))

        cmd("features").action((_, c) => c.copy(mode = "features"))
            .text("Retrieve the current list of features.")

        cmd("retrieve").action((s, c) => c.copy(mode = "retrieve"))
          .text("Retrieve a project's description, specified by ID.")
          .children(
            arg[String]("id").action((x, c) => c.copy(id = x)).text("The ID of the project to retrieve"),
            opt[String]("csv").action((x, c) => c.copy(csv = x)).text("Path to the output .csv file (overwrites existing file)"),
            opt[Unit]('f', "file").action((_, c) => c.copy(opts = List("file"))).text("Use to load the ID from file, " +
              "with the filepath given in place of the ID")
          )

        cmd("search").action((s, c) => c.copy(mode = "search"))
          .text("Search artifact using a query.")
          .children(
            arg[String]("query").action((x, c) => c.copy(query = x)).text("The query to be used."),
            opt[String]("csv").action((x, c) => c.copy(csv = x)).text("Path to the output .csv file (overwrites existing file)"),
            opt[Int]("limit").action((x, c) => c.copy(limit = Some(x))).text("The maximal number of results returned."),
            opt[Unit](name = "list").action((_, c) => c.copy(list = true)).text("Output results as list (raw option overrides this)"),
            opt[Int]("timeout").action((x, c) => c.copy(timeout = Some(x))).text("Timeout in seconds.")
          )
      }
    }
    parser
  }
}