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

class ExperimentOptions(args: Seq[String]) extends ScalismoOptions(args) {
  val root = opt[String](default = Some("./data"), descr = "Directory where the data of for the experiment resides.")
  val debug               = opt[Boolean](default = Some(false), descr = "Turn on debug mode. (needs display to be available)")
  val leaveOneOut     = opt[Boolean](default = Some(false), descr = "Uses test example in the model.")
}