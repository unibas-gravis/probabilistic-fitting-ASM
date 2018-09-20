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

import breeze.stats.distributions.ContinuousDistr
import probabilisticFittingASM.sampling.parameters.ModelFittingParameters
import scalismo.geometry.{Point, _3D}
import scalismo.mesh.TriangleMesh
import scalismo.sampling.DistributionEvaluator
import scalismo.statisticalmodel.StatisticalMeshModel

/**
  * Likelihood of the model instance closely representing some lines.
  * The lines are given as a sequence of points. Each point is evaluated independently
  * under a likelihood model for the distance of the points to the surface.
  */
case class LineDistanceIPEvaluator(model: StatisticalMeshModel,
                                   lines: Seq[Seq[Point[_3D]]],
                                   likelihoodModel: ContinuousDistr[Double])
  extends DistributionEvaluator[ModelFittingParameters]
  with EvaluationCaching {

  override def computeLogValue(theta: ModelFittingParameters): Double = {
    val currentMesh = ModelFittingParameters.transformedMesh(model, theta)

    lines.map { line =>
      lineLikelihood(currentMesh, line)
    }sum
  }

  def lineLikelihood(currentMesh: TriangleMesh[_3D], linePoints: Seq[Point[_3D]]): Double = {
      linePoints.map{ linePoint =>
        val closestPoint = currentMesh.pointSet.findClosestPoint(linePoint).point
        val distance = (closestPoint - linePoint).norm
        likelihoodModel.logPdf(distance)
      }.sum
  }
}