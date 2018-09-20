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
import scalismo.geometry.{Vector, _3D}
import scalismo.sampling.evaluators.GaussianEvaluator
import scalismo.sampling.{ProposalGenerator, TransitionProbability}
import scalismo.utils.Random

/**
  * Gaussian translation proposal for the 3d space.
  */
case class Gaussian3DTranslationProposal(sdev: Vector[_3D])(implicit rnd: Random)
  extends ProposalGenerator[ModelFittingParameters] with TransitionProbability[ModelFittingParameters] {
  override def propose(current: ModelFittingParameters): ModelFittingParameters = {
    val cpose = current.poseParameters
    val npose = cpose.copy(
      translation = Vector(
        cpose.translation.x + rnd.scalaRandom.nextGaussian() * sdev.x,
        cpose.translation.y + rnd.scalaRandom.nextGaussian() * sdev.y,
        cpose.translation.z + rnd.scalaRandom.nextGaussian() * sdev.z)
    )
    current.copy(poseParameters = npose)
  }

  /** rate of transition from to (log value) */
  override def logTransitionProbability(from: ModelFittingParameters, to: ModelFittingParameters): Double = {
    val toPose = to.poseParameters
    val fromPose = from.poseParameters
    if (toPose.copy(translation = fromPose.translation) != fromPose)
      Double.NegativeInfinity
    else {
      val diff = toPose.translation - fromPose.translation
      val px = GaussianEvaluator.logDensity(diff.x, 0, sdev.x)
      val py = GaussianEvaluator.logDensity(diff.y, 0, sdev.y)
      val pz = GaussianEvaluator.logDensity(diff.z, 0, sdev.z)
      px + py + pz
    }
  }
}

/**
  * Factory method with uses same standard deviation in all three directions.
  */
object Gaussian3DTranslationProposal{
  def apply(sdev: Double)(implicit rng: Random) = new Gaussian3DTranslationProposal(Vector(sdev,sdev,sdev))
}