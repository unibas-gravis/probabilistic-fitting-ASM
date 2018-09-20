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

import breeze.linalg.{DenseMatrix, convert}
import scalismo.geometry._3D
import scalismo.image.DiscreteScalarImage
import scalismo.io.{ImageIO, MeshIO}
import scalismo.mesh.TriangleMesh
import scalismo.numerics.{Sampler, UniformMeshSampler3D}
import scalismo.statisticalmodel.asm._
import scalismo.statisticalmodel.{MultivariateNormalDistribution, StatisticalMeshModel}
import scalismo.utils.Random

import scala.collection.immutable

object ASMBuilder {

  type TrainingData = (DiscreteScalarImage[_3D, Float], TriangleMesh[_3D])
  type TrainingDataIterator = Iterator[TrainingData]
  
  def buildModel(data: DataProvider, ids: Seq[String], statisticalMeshModel: StatisticalMeshModel)(implicit rng: Random): ActiveShapeModel = {

    val trainingData: TrainingDataIterator = data.ids.iterator.map{ basename =>
      (
        ImageIO.read3DScalarImage[Float](data.volumeFile(basename)).get,
        MeshIO.readMesh(data.registrationFile(basename)).get
      )
    }

    trainModel(
      statisticalMeshModel,
      trainingData,
      GaussianGradientImagePreprocessor(8.0f),
      NormalDirectionFeatureExtractor(7, 8.0),
      (m: TriangleMesh[_3D]) => UniformMeshSampler3D(m, 1000)
    )
  }

  /**
    * Train an active shape model using an existing PCA model
    */
  def trainModel(statisticalModel: StatisticalMeshModel,
                 trainingData: TrainingDataIterator,
                 preprocessor: ImagePreprocessor,
                 featureExtractor: FeatureExtractor,
                 sampler: TriangleMesh[_3D] => Sampler[_3D])
                (implicit rand: Random)
  : ActiveShapeModel = {

    val sampled = sampler(statisticalModel.referenceMesh).sample.map(_._1).to[immutable.IndexedSeq]
    val pointIds = sampled.map(statisticalModel.referenceMesh.pointSet.findClosestPoint(_).id)

    // preprocessed images can be expensive in terms of memory, so we go through them one at a time.
    val imageFeatures = trainingData.flatMap {
      case (image, registeredMesh)
        if ( registeredMesh.pointSet.numberOfPoints == statisticalModel.referenceMesh.pointSet.numberOfPoints &&
          registeredMesh.triangulation == statisticalModel.referenceMesh.triangulation ) =>
        print("... loading data")
        val (pimg, mesh) = (preprocessor(image), registeredMesh)
        pointIds.map { pointId => featureExtractor(pimg, mesh.pointSet.point(pointId), mesh, pointId) }
      case (image, registeredMesh) =>
        if ( registeredMesh.pointSet.numberOfPoints != statisticalModel.referenceMesh.pointSet.numberOfPoints) println("Warning: Skip mesh which is not in correspondence with the reference. Number of points is not identical!")
        if ( registeredMesh.triangulation != statisticalModel.referenceMesh.triangulation) println("Warning: Skip mesh which is not in correspondence with the reference. Triangulation is not identical!")
        None
    }.toIndexedSeq
    println(" ... all data loaded")

    val pointsLength = pointIds.length
    val imageRange = (0 until imageFeatures.length / pointsLength)
    val pointFeatures = (0 until pointsLength).map { pointIndex =>
      val featuresForPoint = imageRange.flatMap { imageIndex =>
        imageFeatures(imageIndex * pointsLength + pointIndex).map(convert(_, Double))
      }
      val dataDistribution = MultivariateNormalDistribution.estimateFromData(featuresForPoint)

      val epsilon = 1.0E-8
      dataDistribution.copy(cov = dataDistribution.cov+epsilon*DenseMatrix.eye[Double](dataDistribution.cov.cols))
    }

    val profiles = new Profiles(pointIds.zip(pointFeatures).map { case (i, d) => Profile(i, d) })
    ActiveShapeModel(statisticalModel, profiles, preprocessor, featureExtractor)
  }
}
