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
import scalismo.sampling.proposals.MixtureProposal
import scalismo.sampling.proposals.MixtureProposal.implicits._
import scalismo.sampling.{ProposalGenerator, TransitionProbability}
import scalismo.utils.Random

/**
  * Standard random pose proposal with translation and rotation.
  */
case class PoseProposal(scaleFactor: Double) (implicit random: Random)
  extends ProposalGenerator[ModelFittingParameters] with TransitionProbability[ModelFittingParameters] {

  // proposal for translation and rotation
  val randomGaussianPoseProposal = {

    // euler rotations mixture proposal
    val yawProposalC = GaussianAxisRotationProposal(0.1 * scaleFactor, YawAxis)
    val yawProposalI = GaussianAxisRotationProposal(0.05 * scaleFactor, YawAxis)
    val yawProposalF = GaussianAxisRotationProposal(0.01 * scaleFactor, YawAxis)
    val rotationYaw = MixtureProposal(0.2 *: yawProposalC + 0.3 *: yawProposalI + 0.4 *: yawProposalF)

    val pitchProposalC = GaussianAxisRotationProposal(0.1 * scaleFactor, PitchAxis)
    val pitchProposalI = GaussianAxisRotationProposal(0.05 * scaleFactor, PitchAxis)
    val pitchProposalF = GaussianAxisRotationProposal(0.01 * scaleFactor, PitchAxis)
    val rotationPitch = MixtureProposal(0.2 *: pitchProposalC + 0.3 *: pitchProposalI + 0.4 *: pitchProposalF)

    val rollProposalC = GaussianAxisRotationProposal(0.1 * scaleFactor, RollAxis)
    val rollProposalI = GaussianAxisRotationProposal(0.05 * scaleFactor, RollAxis)
    val rollProposalF = GaussianAxisRotationProposal(0.01 * scaleFactor, RollAxis)
    val rotationRoll = MixtureProposal(0.2 *: rollProposalC + 0.3 *: rollProposalI + 0.4 *: rollProposalF)
    val rotationProposal = MixtureProposal(0.3 *: rotationRoll + 0.3 *: rotationPitch + 0.3 *: rotationYaw)

    // translation mixture proposal
    val translationProposalC = Gaussian3DTranslationProposal(10.0 * scaleFactor)
    val translationProposalI = Gaussian3DTranslationProposal(5.0 * scaleFactor)
    val translationProposalF = Gaussian3DTranslationProposal(1.0 * scaleFactor)
    val translationProposal = MixtureProposal(0.2 *: translationProposalC + 0.3 *: translationProposalI + 0.4 *: translationProposalF)

    MixtureProposal(0.6 *: rotationProposal + 0.4 *: translationProposal)
  }

  override def propose(current: ModelFittingParameters): ModelFittingParameters = {
    randomGaussianPoseProposal.propose(current)
  }

  override def logTransitionProbability(from: ModelFittingParameters, to: ModelFittingParameters): Double = {
    randomGaussianPoseProposal.logTransitionProbability(from, to)
  }
}
