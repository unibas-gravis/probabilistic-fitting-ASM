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

import breeze.linalg.DenseMatrix
import breeze.stats.distributions.ContinuousDistr
import probabilisticFittingASM.sampling.parameters.ModelFittingParameters
import scalismo.statisticalmodel.asm.{ActiveShapeModel, PreprocessedImage}


abstract class ASMEvaluator(asm: ActiveShapeModel, target: PreprocessedImage, likelihood: ContinuousDistr[Double]) {

  val referenceMesh = asm.statisticalModel.referenceMesh

  def computeMahalanobisDistances(theta: ModelFittingParameters): Seq[Double] = {

    val currentMesh = ModelFittingParameters.transformedMesh(asm.statisticalModel, theta)

    val mahalDistances = for {
      profileID <- asm.profiles.ids.par
      profile = asm.profiles(profileID)
      profilePointOnMesh = currentMesh.pointSet.point(profile.pointId)
      featureOpt = asm.featureExtractor(target, profilePointOnMesh, currentMesh, profile.pointId)
      if (featureOpt.isDefined)
    } yield {

      val featureAtPoint = featureOpt.get
      val cov = profile.distribution.cov.map(_.toDouble)
      val myCovInv = breeze.linalg.inv(profile.distribution.cov.map(_.toDouble) + DenseMatrix.eye[Double](cov.rows) * 1e-5)

      val x0 = featureAtPoint - profile.distribution.mean
      math.sqrt(math.abs(x0 dot (myCovInv * x0)))
    }

    mahalDistances.toIndexedSeq
  }
}


