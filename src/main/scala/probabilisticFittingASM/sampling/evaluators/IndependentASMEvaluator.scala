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
package probabilisticFittingASM.sampling.evaluators

import probabilisticFittingASM.sampling.parameters.ModelFittingParameters
import scalismo.sampling.DistributionEvaluator
import scalismo.statisticalmodel.asm.{ActiveShapeModel, PreprocessedImage}

case class IndependentASMEvaluator(asm: ActiveShapeModel,
                                   target: PreprocessedImage,
                                   likelihood: breeze.stats.distributions.ContinuousDistr[Double])
  extends ASMEvaluator(asm, target, likelihood) with DistributionEvaluator[ModelFittingParameters] with EvaluationCaching {


  override def computeLogValue(theta: ModelFittingParameters): Double = {
    computeMahalanobisDistances(theta).map(likelihood.logPdf).sum
  }
}
