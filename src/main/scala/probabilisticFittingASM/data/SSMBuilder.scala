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

package probabilisticFittingASM.data

import scalismo.common.{DiscreteField, UnstructuredPointsDomain}
import scalismo.geometry.{Vector, _3D}
import scalismo.io.MeshIO
import scalismo.mesh.TriangleMesh
import scalismo.statisticalmodel.{DiscreteLowRankGaussianProcess, StatisticalMeshModel}

object SSMBuilder {

  /** The calculation step. */
  def buildModel(dataProvider: DataProvider, trainingIds: Seq[String]): StatisticalMeshModel = {
    val meshFiles = trainingIds.map(id => dataProvider.registrationFile(id))
    val meshes = meshFiles.map(f => MeshIO.readMesh(f).get)
    require(meshes.forall(m => m.pointSet.numberOfPoints == meshes.head.pointSet.numberOfPoints),"a mesh has different number of points")
    val referenceMesh = meshes.head
    calculateModel(referenceMesh, meshes)
  }

  def calculateModel(referenceMesh: TriangleMesh[_3D], meshes: Seq[TriangleMesh[_3D]]): StatisticalMeshModel = {
    val statisticalExamples = meshes.map { mesh =>
      DiscreteField[_3D,UnstructuredPointsDomain[_3D],Vector[_3D]](
        referenceMesh.pointSet,
        mesh.pointSet.points.zip(referenceMesh.pointSet.points).map { case (p, r) => p - r }.toIndexedSeq
      ).interpolateNearestNeighbor()
    }

    val dlrgp = DiscreteLowRankGaussianProcess.createUsingPCA[_3D, UnstructuredPointsDomain[_3D],Vector[_3D]](referenceMesh.pointSet, statisticalExamples)

    val ssm = StatisticalMeshModel(referenceMesh, dlrgp.interpolateNearestNeighbor)
    ssm
  }

}
