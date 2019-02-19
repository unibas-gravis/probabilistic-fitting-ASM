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
package probabilisticFittingASM

import java.nio.file.Files

import breeze.linalg.DenseVector
import probabilisticFittingASM.data.DataProvider
import probabilisticFittingASM.sampling.parameters.{ModelFittingParameters, PoseParameters, ShapeParameters}
import probabilisticFittingASM.utils.ExperimentOptions
import scalismo.geometry.{Landmark, Point, EuclideanVector, _3D}
import scalismo.image.DiscreteScalarImage
import scalismo.io.{ActiveShapeModelIO, ImageIO, MeshIO}
import scalismo.mesh.TriangleMesh
import scalismo.registration.RigidTransformation
import scalismo.statisticalmodel.asm._
import scalismo.utils.Random

object FitStandardASM {

  final val fittingMethod = DataProvider.fittingUsingStandardASM

  def main(args: Array[String]): Unit = {
    scalismo.initialize()
    implicit val rng = Random(1024l)

    val opts = new ExperimentOptions(args)
    opts.verify()

    val data = DataProvider(opts.root(),opts.leaveOneOut())

    if (opts.leaveOneOut()) {
      for (testDatum <- data.ids ) {
        println(s"$testDatum - $fittingMethod, leaveOneOut:${opts.leaveOneOut()}")
        val asmFile = data.asmAugFile(testDatum)
        val asm = ActiveShapeModelIO.readActiveShapeModel(asmFile).get
        fitModel(data, testDatum, asm, opts.debug())
      }
    } else {
      val asmFile = data.asmFile()
      val asm = ActiveShapeModelIO.readActiveShapeModel(asmFile).get
      data.ids.foreach { testDatum =>
        println(s"$testDatum - $fittingMethod, leaveOneOut:${opts.leaveOneOut()}")
        fitModel(data, testDatum, asm, opts.debug())
      }
    }
  }


  def fitModel(data: DataProvider, testDatum: String, asm: ActiveShapeModel, debug: Boolean = false)(implicit rng: Random): Unit = {
    val targetFile = data.volumeFile(testDatum)
    val targetImage = ImageIO.read3DScalarImage[Float](targetFile).get

    val standardMesh = fit(targetImage,asm,debug = debug)
    val outFile = data.fitFile(testDatum,fittingMethod)
    if (!outFile.getParentFile.exists) Files.createDirectories(outFile.getParentFile.toPath)
    MeshIO.writeMesh(standardMesh,outFile).get
  }

  def fit(target: DiscreteScalarImage[_3D,Float], asm: ActiveShapeModel, iterations: Int = 1000, debug: Boolean = false): TriangleMesh[_3D] = {


    val fitConfig = FittingConfiguration(6.0,3.0,3.0)
    val nSearchPoints = 61
    val searchDistance = 60f

    val initParams = ModelFittingParameters(PoseParameters(EuclideanVector(0,0,0),(0,0,0),Point(0,0,0)),ShapeParameters(DenseVector.zeros[Double](asm.statisticalModel.rank)))

    val fits = fittingIterations(fitConfig,nSearchPoints,f => searchDistance/nSearchPoints/f,asm, Seq(), target, Seq(), initParams.shapeParameters.parameters,PoseParameters.toTransform(initParams.poseParameters),iterations).zipWithIndex
    val result = fits.drop(iterations-1).next
    result._1.mesh
  }

  def fittingIterations(config: FittingConfiguration, samplerNumberOfPoints: Int, samplerSpacing: Float => Float, asm: ActiveShapeModel, referenceLandmarks : Seq[Landmark[_3D]], img: DiscreteScalarImage[_3D,Float], targetLandmarks : Seq[Landmark[_3D]], initialParameters: DenseVector[Double], initialTransformation: RigidTransformation[_3D], numberOfIterations: Int): Iterator[FittingResult] = {
    val prepImg = asm.preprocessor(img)

    val spacing = asm.featureExtractor match {
      case fe: NormalDirectionFeatureExtractor => fe.spacing
      case _ => ???
    }

    val spsampler = NormalDirectionSearchPointSampler(samplerNumberOfPoints, samplerSpacing(spacing.toFloat))
    val regIt = for (it <- asm.fitIteratorPreprocessed(prepImg, spsampler, Math.max(numberOfIterations, 1), config, ModelTransformations(initialParameters, initialTransformation))) yield it.get
    regIt.take(numberOfIterations)
  }

}
