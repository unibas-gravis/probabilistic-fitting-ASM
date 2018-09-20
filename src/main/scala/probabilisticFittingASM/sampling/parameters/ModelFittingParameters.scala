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
package probabilisticFittingASM.sampling.parameters

import breeze.linalg.DenseVector
import scalismo.common.PointId
import scalismo.geometry.{Point, _3D}
import scalismo.mesh.TriangleMesh
import scalismo.registration._
import scalismo.statisticalmodel.StatisticalMeshModel

/**
  * Parameters describing a full model fit.
  * @param scalaParameter Parameters for scaling the model.
  * @param poseParameters Parameters describing the pose of the model.
  * @param shapeParameters Parameters describing the shape instance of the model.
  */
case class ModelFittingParameters(
  scalaParameter: ScaleParameter,
  poseParameters: PoseParameters,
  shapeParameters: ShapeParameters,
  generatedBy: String = "Anonymous") {

  def canEqual(other: Any): Boolean = {
    other.isInstanceOf[ModelFittingParameters]
  }

  override def equals(that: Any): Boolean = {
    that match {
      case that: ModelFittingParameters => that.canEqual(this) && this.hashCode == that.hashCode
      case _ => false
    }
  }

  override def hashCode(): Int = allParameters.hashCode()


  val allParameters = DenseVector.vertcat(scalaParameter.parameters, poseParameters.parameters, shapeParameters.parameters)

}

/**
  * Helper functions around the model fitting parameters.
  */
object ModelFittingParameters {

  /**
    * Create ModelFittingParameters using pose and shape only (scale is fixed to 1)
    */
  def apply(poseParameters: PoseParameters, shapeParameters: ShapeParameters): ModelFittingParameters = {
    ModelFittingParameters(ScaleParameter(1.0), poseParameters, shapeParameters)
  }


  def poseTransform(parameter: ModelFittingParameters): RigidTransformation[_3D] = {
    val poseParameters = parameter.poseParameters
    val translation = TranslationTransform[_3D](poseParameters.translation)
    val (phi, theta, psi) = poseParameters.rotation
    val center = poseParameters.rotationCenter
    val rotation = RotationTransform(phi, theta, psi, center)
    RigidTransformation[_3D](translation, rotation)
  }

  def scaleTransform(parameters: ModelFittingParameters): ScalingTransformation[_3D] = {
    ScalingTransformation(parameters.scalaParameter.s)
  }


  def shapeTransform(model: StatisticalMeshModel, parameters: ModelFittingParameters): Transformation[_3D] = {
    val coeffs = parameters.shapeParameters.parameters
    //model.instance(coeffs)
    val gpModel = model.gp.interpolateNearestNeighbor
    val instance = gpModel.instance(coeffs)
    Transformation((pt: Point[_3D]) => pt + instance(pt))
  }

  def poseAndShapeTransform(model: StatisticalMeshModel, parameters: ModelFittingParameters): Transformation[_3D] = {
    Transformation(poseTransform(parameters).compose(shapeTransform(model, parameters)))
  }

  def fullTransform(model: StatisticalMeshModel, parameters: ModelFittingParameters): Transformation[_3D] = {
    Transformation(scaleTransform(parameters).compose(poseTransform(parameters).compose(shapeTransform(model, parameters))))
  }

  def transformedMesh(model: StatisticalMeshModel, parameters: ModelFittingParameters): TriangleMesh[_3D] = {
    model.referenceMesh.transform(fullTransform(model, parameters))
  }

  def transformedPoints(model: StatisticalMeshModel, parameters: ModelFittingParameters, points: Seq[Point[_3D]]): Seq[Point[_3D]] = {
    val t = fullTransform(model, parameters)
    points.map(t)
  }

  def transformedPointsWithIds(model: StatisticalMeshModel, parameters: ModelFittingParameters, pointIds: Seq[PointId]): Seq[Point[_3D]] = {
    val t = fullTransform(model, parameters)
    val ps = model.referenceMesh.pointSet
    pointIds.map(id => t(ps.point(id)))
  }


}
