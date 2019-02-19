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

package scalismo.statisticalmodel.asm

import breeze.linalg.{DenseMatrix, DenseVector}
import scalismo.common._
import scalismo.geometry.{Dim, NDSpace, Point, EuclideanVector, _3D}
import scalismo.image.DiscreteScalarImage
import scalismo.mesh.TriangleMesh
import scalismo.registration.{LandmarkRegistration, RigidTransformation, RigidTransformationSpace}
import scalismo.statisticalmodel.{DiscreteLowRankGaussianProcess, MultivariateNormalDistribution, StatisticalMeshModel}
import scalismo.utils.Random

import scala.util.{Failure, Try}

case class ActiveShapeModelOptimized(statisticalModel: StatisticalMeshModel, profiles: Profiles, preprocessor: ImagePreprocessor, featureExtractor: FeatureExtractor) {

  /**
   * Returns the mean mesh of the shape model, along with the mean feature profiles at the profile points
   */
  def mean(): ASMSample = {
    val smean = statisticalModel.mean
    val meanProfilePoints = profiles.data.map(p => smean.pointSet.point(p.pointId))
    val meanFeatures = profiles.data.map(_.distribution.mean)
    val featureField = DiscreteFeatureField[_3D, UnstructuredPointsDomain[_3D]](new UnstructuredPointsDomain3D(meanProfilePoints), meanFeatures)
    ASMSample(smean, featureField, featureExtractor)
  }

  /**
   * Returns a random sample mesh from the shape model, along with randomly sampled feature profiles at the profile points
   */
  def sample()(implicit rand: Random): ASMSample = {
    val sampleMesh = statisticalModel.sample()
    val randomProfilePoints = profiles.data.map(p => sampleMesh.pointSet.point(p.pointId))
    val randomFeatures = profiles.data.map(_.distribution.sample)
    val featureField = DiscreteFeatureField[_3D, UnstructuredPointsDomain[_3D]](new UnstructuredPointsDomain3D(randomProfilePoints), randomFeatures)
    ASMSample(sampleMesh, featureField, featureExtractor)
  }

  /**
   * Utility function that allows to randomly sample different feature profiles, while keeping the profile points
   * Meant to allow to easily inspect/debug the feature distribution
   */
  def sampleFeaturesOnly()(implicit rand: Random): ASMSample = {
    val smean = statisticalModel.mean
    val meanProfilePoints = profiles.data.map(p => smean.pointSet.point(p.pointId))
    val randomFeatures = profiles.data.map(_.distribution.sample)
    val featureField = DiscreteFeatureField[_3D, UnstructuredPointsDomain[_3D]](new UnstructuredPointsDomain3D(meanProfilePoints), randomFeatures)
    ASMSample(smean, featureField, featureExtractor)
  }

  /**
   * Returns an Active Shape Model where both the statistical shape Model and the profile points distributions are correctly transformed
   * according to the provided rigid transformation
   *
   */
  def transform(rigidTransformation: RigidTransformation[_3D]): ActiveShapeModelOptimized = {
    val transformedModel = statisticalModel.transform(rigidTransformation)
    this.copy(statisticalModel = transformedModel)
  }

  private def noTransformations = ModelTransformations(statisticalModel.coefficients(statisticalModel.mean), RigidTransformationSpace[_3D]().transformForParameters(RigidTransformationSpace[_3D]().identityTransformParameters))

  /**
   * Perform an ASM fitting for the given target image.
   * This is logically equivalent to calling <code>fitIterator(...).last</code>
   *
   * @param targetImage target image to fit to.
   * @param searchPointSampler sampler that defines the strategy where profiles are to be sampled.
   * @param iterations maximum number of iterations for the fitting.
   * @param config fitting configuration (thresholds). If omitted, uses [[FittingConfiguration.Default]]
   * @param startingTransformations initial transformations to apply to the statistical model. If omitted, no transformations are applied (i.e. the fitting starts from the mean shape, with no rigid transformation)
   * @return fitting result after the given number of iterations
   */
  def fit(targetImage: DiscreteScalarImage[_3D, Float], searchPointSampler: SearchPointSampler, iterations: Int, config: FittingConfiguration = FittingConfiguration.Default, startingTransformations: ModelTransformations = noTransformations): Try[FittingResult] = {

    // we're manually looping the iterator here because we're only interested in the last result -- no need to keep all intermediates.
    val it = fitIterator(targetImage, searchPointSampler, iterations, config, startingTransformations)
    if (!it.hasNext) {
      Failure(new IllegalStateException("iterator was empty"))
    } else {
      var result = it.next()
      while (it.hasNext) {
        result = it.next()
      }
      result
    }
  }

  /**
   * Perform iterative ASM fitting for the given target image. This is essentially the same as the [[fit]] method, except that it returns the full iterator, so every step can be examined.
   * @see [[fit()]] for a description of the parameters.
   *
   */
  def fitIterator(targetImage: DiscreteScalarImage[_3D, Float], searchPointSampler: SearchPointSampler, iterations: Int, config: FittingConfiguration = FittingConfiguration.Default, initialTransform: ModelTransformations = noTransformations): Iterator[Try[FittingResult]] = {
    fitIteratorPreprocessed(preprocessor(targetImage), searchPointSampler, iterations, config, initialTransform)
  }

  /**
   * Perform iterative ASM fitting for the given preprocessed image. This is essentially the same as the [[fitIterator]] method, except that it uses the already preprocessed image.
   * @see [[fit()]] for a description of the parameters.
   *
   */
  def fitIteratorPreprocessed(image: PreprocessedImage, searchPointSampler: SearchPointSampler, iterations: Int, config: FittingConfiguration = FittingConfiguration.Default, initialTransform: ModelTransformations = noTransformations): Iterator[Try[FittingResult]] = {
    require(iterations > 0, "number of iterations must be strictly positive")

    new Iterator[Try[FittingResult]] {
      var lastResult: Option[Try[FittingResult]] = None
      var nextCount = 0

      override def hasNext = nextCount < iterations && (lastResult.isEmpty || lastResult.get.isSuccess)

      override def next() = {
        val mesh = lastResult.map(_.get.mesh).getOrElse(statisticalModel.instance(initialTransform.coefficients).transform(initialTransform.rigidTransform))
        lastResult = Some(fitOnce(image, searchPointSampler, config, mesh, initialTransform.rigidTransform))
        nextCount += 1
        lastResult.get
      }
    }
  }

  private def fitOnce(image: PreprocessedImage, sampler: SearchPointSampler, config: FittingConfiguration, mesh: TriangleMesh[_3D], poseTransform: RigidTransformation[_3D]): Try[FittingResult] = {
    val refPtIdsWithTargetPt = findBestCorrespondingPoints(image, mesh, sampler, config, poseTransform)

    if (refPtIdsWithTargetPt.isEmpty) {
      Failure(new IllegalStateException("No point correspondences found. You may need to relax the configuration thresholds."))
    } else Try {

      val refPtsWithTargetPts = refPtIdsWithTargetPt.map { case (refPtId, tgtPt) => (statisticalModel.referenceMesh.pointSet.point(refPtId), tgtPt) }
      val bestRigidTransform = LandmarkRegistration.rigid3DLandmarkRegistration(refPtsWithTargetPts, poseTransform.rotation.center)

      val refPtIdsWithTargetPtAtModelSpace = refPtIdsWithTargetPt.map { case (refPtId, tgtPt) => (refPtId, bestRigidTransform.inverse(tgtPt)) }


      val coeffs = {
        val trainingDataWithDisplacements = refPtIdsWithTargetPtAtModelSpace.map { case (id, targetPoint) => (id, targetPoint - statisticalModel.referenceMesh.pointSet.point(id)) }
        val cov = MultivariateNormalDistribution(DenseVector.zeros[Double](3), DenseMatrix.eye[Double](3) * 1e-5)
        val trainingData = trainingDataWithDisplacements.map { case (ptId, df) => (ptId, df, cov) }
        val (_Minv, _QtL, yVec, mVec) = genericRegressionComputations[_3D,UnstructuredPointsDomain[_3D],EuclideanVector[_3D]](statisticalModel.gp, trainingData)
        (_Minv * _QtL) * (yVec - mVec)
      }

      val boundedCoeffs = coeffs.map { c => Math.min(config.modelCoefficientBounds, Math.max(-config.modelCoefficientBounds, c)) }
      val resultMesh = statisticalModel.instance(boundedCoeffs).transform(bestRigidTransform)
      val transformations = ModelTransformations(boundedCoeffs, LandmarkRegistration.rigid3DLandmarkRegistration(refPtIdsWithTargetPt.map{case (refPtId, tgtPt) => (poseTransform.inverse(resultMesh.pointSet.point(refPtId)), tgtPt)}, poseTransform.rotation.center))
      FittingResult(transformations, resultMesh)
    }
  }



  private def genericRegressionComputations[D <: Dim: NDSpace, Dom <: DiscreteDomain[D], Value](gp: DiscreteLowRankGaussianProcess[D, Dom, Value], trainingData: IndexedSeq[(PointId, Value, MultivariateNormalDistribution)])(implicit vectorizer: Vectorizer[Value]) = {
    val outputDim = gp.outputDim
    val (ptIds, ys, errorDistributions) = trainingData.unzip3

    val yVec = DiscreteField.vectorize[D, Value](ys)

    val meanValues = DenseVector(ptIds.toArray.flatMap { ptId => gp.meanVector(ptId.id * outputDim until (ptId.id + 1) * outputDim).toArray })

    val Q = DenseMatrix.zeros[Double](trainingData.size * outputDim, gp.rank)
    for ((ptId, i) <- ptIds.zipWithIndex; j <- 0 until gp.rank) {
      val eigenVecAtPoint = gp.basisMatrix((ptId.id * outputDim) until ((ptId.id + 1) * outputDim), j).map(_.toDouble)
      Q(i * outputDim until i * outputDim + outputDim, j) := eigenVecAtPoint * math.sqrt(gp.variance(j))
    }

    // What we are actually computing here is the following:
    // L would be a block diagonal matrix, which contains on the diagonal the blocks that describes the uncertainty
    // for each point (a d x d) block. We then would compute Q.t * L. For efficiency reasons (L could be large but is sparse)
    // we avoid ever constructing the matrix L and do the multiplication by hand.
    val QtL = Q.t.copy
    assert(QtL.cols == errorDistributions.size * outputDim)
    assert(QtL.rows == gp.rank)
    for ((errDist, i) <- errorDistributions.zipWithIndex) {
      QtL(::, i * outputDim until (i + 1) * outputDim) := QtL(::, i * outputDim until (i + 1) * outputDim) * breeze.linalg.inv(errDist.cov)
    }

    val M = QtL * Q + DenseMatrix.eye[Double](gp.rank)
    val Minv = breeze.linalg.pinv(M)

    (Minv, QtL, yVec, meanValues)
  }








  private def refPoint(profileId: ProfileId): Point[_3D] = statisticalModel.referenceMesh.pointSet.point(profiles(profileId).pointId)

  private def findBestCorrespondingPoints(img: PreprocessedImage, mesh: TriangleMesh[_3D], sampler: SearchPointSampler, config: FittingConfiguration, poseTransform: RigidTransformation[_3D]): IndexedSeq[(PointId, Point[_3D])] = {

    val matchingPts = profiles.ids.par.map { index =>
      (profiles(index).pointId, findBestMatchingPointAtPoint(img, mesh, index, sampler, config, profiles(index).pointId, poseTransform))
    }

    val matchingPtsWithinDist = matchingPts.filter(_._2.isDefined).map(p => (p._1, p._2.get))
    matchingPtsWithinDist.toIndexedSeq

  }

  private def findBestMatchingPointAtPoint(image: PreprocessedImage, mesh: TriangleMesh[_3D], profileId: ProfileId, searchPointSampler: SearchPointSampler, config: FittingConfiguration, pointId: PointId, poseTransform: RigidTransformation[_3D]): Option[Point[_3D]] = {
    val sampledPoints = searchPointSampler(mesh, pointId)

    val pointsWithFeatureDistances = (for (point <- sampledPoints) yield {
      val featureVectorOpt = featureExtractor(image, point, mesh, pointId)
      featureVectorOpt.map { fv => (point, featureDistance(profileId, fv)) }
    }).flatten

    if (pointsWithFeatureDistances.isEmpty) {
      // none of the sampled points returned a valid feature vector
      None
    } else {
      val (bestPoint, bestFeatureDistance) = pointsWithFeatureDistances.minBy { case (pt, dist) => dist }

      if (bestFeatureDistance <= config.featureDistanceThreshold) {
        val refPoint = this.refPoint(profileId)
        /** Attention: checking for the deformation vector's pdf needs to be done in the model space !**/
        val inversePoseTransform = poseTransform.inverse
        val bestPointDistance = statisticalModel.gp.marginal(pointId).mahalanobisDistance((inversePoseTransform(bestPoint) - refPoint).toBreezeVector)

        if (bestPointDistance <= config.pointDistanceThreshold) {
          Some(bestPoint)
        } else {

          // point distance above user-set threshold
          None
        }
      } else {
        // feature distance above user-set threshold
        None
      }
    }
  }

  private def featureDistance(pid: ProfileId, features: DenseVector[Double]): Double = {
    val mvdAtPoint = profiles(pid).distribution
    mvdAtPoint.mahalanobisDistance(features)
  }

}