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
package probabilisticFittingASM

object RunAll extends App {

  ImportData.main(Seq().toArray)

  println(
    """| According to http://www.sliver07.org/rules.php we are not allowed to provide our registered meshes.
       | Please put your registered meshes in the appropriate folder (see README.md).
       | If you have copied your registered data, remove the line: System.exit(0)""".stripMargin)

  System.exit(0)

  BuildModels.main(Seq().toArray)

  // test in model experiment
  FitSampling.main(Seq().toArray)
  FitSamplingWithLines.main(Seq().toArray)
  FitStandardASM.main(Seq().toArray)
  EvaluateFittings.main(Seq().toArray)


  // leave one out experiment
  AugmentModels.main(Seq().toArray)
  FitSampling.main(Seq("-l").toArray)
  FitSamplingWithLines.main(Seq("-l").toArray)
  FitStandardASM.main(Seq("-l").toArray)
  EvaluateFittings.main(Seq("-l").toArray)

  // for generating the plots of our paper you have to install R
  // see resources/scripts/paper_plots.R
}
