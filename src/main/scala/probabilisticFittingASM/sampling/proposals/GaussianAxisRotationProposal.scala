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

import probabilisticFittingASM.sampling.parameters.ModelFittingParameters
import scalismo.sampling.{ProposalGenerator, TransitionProbability}


/**
  * Euler rotation axis definitions.
  */
sealed trait RotationAxis
case object RollAxis extends RotationAxis
case object PitchAxis extends RotationAxis
case object YawAxis extends RotationAxis

/**
  * Gaussian rotation proposal around one rotation axis.
  */
case class GaussianAxisRotationProposal(sdevRot: Double, axis: RotationAxis)
  extends ProposalGenerator[ModelFittingParameters] with TransitionProbability[ModelFittingParameters] {

  val perturbationDistr = new breeze.stats.distributions.Gaussian(0, sdevRot)


  override def propose(theta: ModelFittingParameters): ModelFittingParameters = {
    val rotParams = theta.poseParameters.rotation
    val newRotParams = axis match {
      case RollAxis => rotParams.copy(_1 = rotParams._1 + perturbationDistr.sample())
      case PitchAxis => rotParams.copy(_2 = rotParams._2 + perturbationDistr.sample())
      case YawAxis => rotParams.copy(_3 = rotParams._3 + perturbationDistr.sample())
    }

    theta.copy(poseParameters = theta.poseParameters.copy(rotation = newRotParams))
  }

  override def logTransitionProbability(from: ModelFittingParameters, to: ModelFittingParameters): Double = {
    val rotParamsFrom = from.poseParameters.rotation
    val rotParamsTo = to.poseParameters.rotation
    val residual = axis match {
      case RollAxis => rotParamsTo._1 - rotParamsFrom._1
      case PitchAxis => rotParamsTo._2 - rotParamsFrom._2
      case YawAxis => rotParamsTo._3 - rotParamsFrom._3
    }
    perturbationDistr.logPdf(residual)
  }
}











