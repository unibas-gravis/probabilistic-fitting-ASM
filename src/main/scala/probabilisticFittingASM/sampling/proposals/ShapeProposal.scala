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
package probabilisticFittingASM.sampling.proposals

import breeze.linalg.DenseVector
import probabilisticFittingASM.sampling.parameters.ModelFittingParameters
import scalismo.sampling.proposals.MixtureProposal
import scalismo.sampling.proposals.MixtureProposal.implicits._
import scalismo.sampling.{ProposalGenerator, TransitionProbability}
import scalismo.statisticalmodel.asm.ActiveShapeModel
import scalismo.statisticalmodel.{MultivariateNormalDistribution, StatisticalMeshModel}
import scalismo.utils.Random


/**
  * Shape proposal to randomly change the shape of a statistical model.
  * @param model Model for which to produce the shape coefficients.
  * @param stdev Variance of the Gaussian from which we sample the update.
  * @param components Number of components we want to sample (first n components are sampled).
  */
case class ShapeProposal(model: StatisticalMeshModel, stdev: Double, components: Int = -1)(implicit random: Random)
  extends ProposalGenerator[ModelFittingParameters]
  with TransitionProbability[ModelFittingParameters] {

  val rank = model.rank
  val ones = if (components>=0) math.min(components,rank) else rank
  val sigma = breeze.linalg.diag(DenseVector.vertcat(
    DenseVector.ones[Double](ones),
    DenseVector.ones[Double](rank-ones)
  ))
  val perturbationDistr = new MultivariateNormalDistribution(DenseVector.zeros(rank), sigma * stdev)


  override def propose(theta: ModelFittingParameters): ModelFittingParameters = {
    val currentCoeffs = theta.shapeParameters.parameters
    theta.copy(shapeParameters = theta.shapeParameters.copy(parameters = currentCoeffs + perturbationDistr.sample))
  }

  override def logTransitionProbability(from: ModelFittingParameters, to: ModelFittingParameters) = {
    val residual = to.shapeParameters.parameters - from.shapeParameters.parameters
    perturbationDistr.logpdf(residual)
  }
}


/**
  * Object with factory methods for multi-scale shape proposal mixture.
  */
object ShapeProposal {
  def stdShapeNProposal(asm: ActiveShapeModel, n: Int)(implicit rng: Random): ProposalGenerator[ModelFittingParameters] with TransitionProbability[ModelFittingParameters] = {
    MixtureProposal[ProposalGenerator[ModelFittingParameters] with TransitionProbability[ModelFittingParameters]](
      0.3 *: ShapeProposal(asm.statisticalModel, 0.05, n) +
        0.3 *: ShapeProposal(asm.statisticalModel, 0.01, n) +
        0.3 *: ShapeProposal(asm.statisticalModel, 0.001, n)
    )
  }

  def stdShapeProposal(asm: ActiveShapeModel)(implicit rng: Random) = {
    MixtureProposal[ProposalGenerator[ModelFittingParameters] with TransitionProbability[ModelFittingParameters]](
      0.3 *: ShapeProposal(asm.statisticalModel, 0.1) +
        0.3 *: ShapeProposal(asm.statisticalModel, 0.05) +
        0.3 *: ShapeProposal(asm.statisticalModel, 0.01)
    )
  }
}