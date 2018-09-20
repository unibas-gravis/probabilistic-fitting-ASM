/*
 * Copyright University of Basel, Graphics and Vision Research Group
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package probabilisticFittingASM.utils

import org.rogach.scallop.ScallopConf
import org.rogach.scallop.exceptions.ScallopException

class ScalismoOptions(args: Seq[String]) extends ScallopConf(args) {
  version(s"${BuildInfo.name} version ${BuildInfo.version}\n© University of Basel")

  override def onError(e: Throwable) = e match {
    case ScallopException(message) =>
      printHelp
      println("Your args: "+args.mkString(" "))
      println(message)
      sys.exit(128)
    case ex => super.onError(ex)
  }
}