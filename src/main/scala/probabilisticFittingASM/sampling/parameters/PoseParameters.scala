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
import scalismo.geometry.{Point, EuclideanVector, _3D}
import scalismo.registration.{RigidTransformation, RotationTransform, TranslationTransform}


/**
  * Parameters describing the pose of an object.
  * @param translation Translation EuclideanVector.
  * @param rotation Rotations angles aroung the x-, y- and z-axis.
  * @param rotationCenter Center used for the rotation.
  */
case class PoseParameters(translation: EuclideanVector[_3D], rotation: (Double, Double, Double), rotationCenter: Point[_3D]) {
  def parameters: DenseVector[Double] = {
    DenseVector.vertcat(translation.toBreezeVector, DenseVector[Double](rotation._1, rotation._2, rotation._3)
      , DenseVector[Double](rotationCenter.x, rotationCenter.y, rotationCenter.z))
  }
}

/**
  * Helper methods around the pose parameters.
  */
object PoseParameters {
  def createFromRigidTransform(r: RigidTransformation[_3D]): PoseParameters = {
    val rotParams = r.rotation.parameters
    PoseParameters(r.translation.t, (rotParams(0), rotParams(1), rotParams(2)), r.rotation.center)
  }

  def toTransform(poseParameters: PoseParameters) = {
    val translation = TranslationTransform[_3D](poseParameters.translation)
    val (phi, theta, psi) = poseParameters.rotation
    val center = poseParameters.rotationCenter
    val rotation = RotationTransform(phi, theta, psi, center)
    RigidTransformation[_3D](translation, rotation)
  }
}
