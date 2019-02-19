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

import probabilisticFittingASM.data.DataProvider
import probabilisticFittingASM.sampling.evaluators._
import probabilisticFittingASM.sampling.parameters.{ModelFittingParameters, PoseParameters, ScaleParameter, ShapeParameters}
import probabilisticFittingASM.sampling.proposals.{PoseProposal, ShapeProposal}
import probabilisticFittingASM.utils.ExperimentOptions
import scalismo.geometry.{Point, EuclideanVector, _3D}
import scalismo.image.DiscreteScalarImage
import scalismo.io.{ActiveShapeModelIO, ImageIO, MeshIO}
import scalismo.mesh.TriangleMesh
import scalismo.sampling.algorithms.MetropolisHastings
import scalismo.sampling.evaluators.ProductEvaluator
import scalismo.sampling.loggers.BestSampleLogger
import scalismo.sampling.loggers.ChainStateLogger.implicits._
import scalismo.sampling.proposals.{MetropolisFilterProposal, MixtureProposal}
import scalismo.statisticalmodel.asm.ActiveShapeModel
import scalismo.utils.Random

object FitSamplingWithLines {

  final val fittingMethod = DataProvider.fittingUsingSamplingWithLines

  def main(args: Array[String]): Unit = {
    scalismo.initialize()
    implicit val rng = Random(1024l)

    val opts = new ExperimentOptions(args)
    opts.verify()

    val dataProvider = DataProvider(opts.root(),opts.leaveOneOut())

    if (opts.leaveOneOut()) {
      for (i <- (0 until dataProvider.ids.size) ) {
        val trainingData = dataProvider.ids.patch(i, Nil, 1)
        val testDatum = dataProvider.ids(i)
        println(s"$testDatum - $fittingMethod, leaveOneOut:${opts.leaveOneOut()}")


        val asmFile = dataProvider.asmAugFile(testDatum)
        val asm = ActiveShapeModelIO.readActiveShapeModel(asmFile).get

        fitModel(dataProvider, testDatum, asm, opts.debug())
      }
    } else {
      val trainingData = dataProvider.ids

      val asmFile = dataProvider.asmFile()
      val asm = ActiveShapeModelIO.readActiveShapeModel(asmFile).get

      dataProvider.ids.foreach { testDatum =>
        println(s"$testDatum - $fittingMethod, leaveOneOut:${opts.leaveOneOut()}")
        fitModel(dataProvider, testDatum, asm, opts.debug())
      }
    }
  }

  def fitModel(data: DataProvider, testDatum: String, asm: ActiveShapeModel, debug: Boolean = false)(implicit rng: Random): Unit = {
    val targetFile = data.volumeFile(testDatum)
    val targetImage = ImageIO.read3DScalarImage[Float](targetFile).get
    val outlineFile = data.linesFile(testDatum)
    val outline = MeshIO.readMesh(outlineFile).map(_.pointSet.points.toIndexedSeq).get

    val samplingMesh = fit(targetImage,outline,asm,debug = debug)
    val outFile = data.fitFile(testDatum,fittingMethod)
    if  (!outFile.getParentFile.exists) Files.createDirectories(outFile.getParentFile.toPath)
    MeshIO.writeMesh(samplingMesh,outFile).get
  }





  def fit(target: DiscreteScalarImage[_3D,Float], outline: IndexedSeq[Point[_3D]], asm: ActiveShapeModel, iterations: Int = 10000, debug: Boolean = false)(implicit rng: Random): TriangleMesh[_3D] = {

    val processedImg = asm.preprocessor(target)

    val generator = {
      val poseProposal = PoseProposal(1.0f)
      val shapeRoughProposal = ShapeProposal(asm.statisticalModel,0.1f)
      val shapeProposal = ShapeProposal(asm.statisticalModel,0.05f)
      val shapeFineProposal = ShapeProposal(asm.statisticalModel,0.01f)
      MixtureProposal.fromProposalsWithTransition(
        (0.3, shapeFineProposal),
        (0.3, shapeProposal),
        (0.3, shapeRoughProposal),
        (0.3, poseProposal)
      )
    }

    val appearanceEval = IndependentASMEvaluator(asm,processedImg,breeze.stats.distributions.Gaussian(0.0,1.0))

    val priorFiltered = MetropolisFilterProposal(generator,ProductEvaluator(ModelPriorEvaluator(asm.statisticalModel),VolumePrior(target,asm)))


    val lineEvaluator = LineDistanceIPEvaluator(asm.statisticalModel, Seq(outline), breeze.stats.distributions.Gaussian(0.0, 1.0))
    val lineFiltered = MetropolisFilterProposal(priorFiltered,lineEvaluator)
    val chain = MetropolisHastings(lineFiltered, appearanceEval)

    val initialTranslation = EuclideanVector(0,0,0)
    val initialRotation = (0.0,0.0,0.0)
    val initialRotationCenter = Point(0,0,0)
    val initialPoseParameters = PoseParameters(initialTranslation,initialRotation,initialRotationCenter)
    val initialShapeParameters = ShapeParameters(asm.statisticalModel.coefficients(asm.statisticalModel.mean))
    val initialParameters = ModelFittingParameters(ScaleParameter(1), initialPoseParameters, initialShapeParameters)


    val bestLogger = BestSampleLogger(ProductEvaluator(appearanceEval,ModelPriorEvaluator(asm.statisticalModel),VolumePrior(target,asm),lineEvaluator))
    val mhIt = chain.iterator(initialParameters) loggedWith bestLogger

    val last = mhIt.drop(iterations).next()

    val bestSample = bestLogger.currentBestSample().get
    asm.statisticalModel.instance(bestSample.shapeParameters.parameters).transform(PoseParameters.toTransform(bestSample.poseParameters))
  }

}
