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

import java.io.File
import java.nio.file.Files

import probabilisticFittingASM.data.DataProvider
import probabilisticFittingASM.utils.ExperimentOptions
import scalismo.geometry.{EuclideanVector, _3D}
import scalismo.io.ActiveShapeModelIO
import scalismo.kernels.{DiagonalKernel, GaussianKernel}
import scalismo.numerics.UniformMeshSampler3D
import scalismo.statisticalmodel.{GaussianProcess, LowRankGaussianProcess, StatisticalMeshModel}
import scalismo.utils.Random

object AugmentModels {

  def main(args: Array[String]): Unit = {
    scalismo.initialize()
    implicit val rng = Random(1024l)

    val opts = new ExperimentOptions(args)
    opts.verify()

    val data = DataProvider(opts.root(),opts.leaveOneOut())

    if (opts.leaveOneOut()) {
      for (id <- data.ids) {
        val asmInputFile = data.asmFile(id).getAbsoluteFile
        println(s"... augment model $asmInputFile")

        val inputASM = ActiveShapeModelIO.readActiveShapeModel(asmInputFile).get

        val asm = inputASM.copy(statisticalModel = augmentModel(inputASM.statisticalModel))

        val asmFile = data.asmAugFile(id).getAbsoluteFile
        if ( !asmFile.getParentFile.exists() ) Files.createDirectories(asmFile.getParentFile.toPath)
        ActiveShapeModelIO.writeActiveShapeModel(asm,asmFile).get
      }
    } else {
      val asmInputFile = data.asmFile().getAbsoluteFile
      println(s"... augment model ${asmInputFile}")

      val inputASM = ActiveShapeModelIO.readActiveShapeModel(asmInputFile).get

      val asm = inputASM.copy(statisticalModel = augmentModel(inputASM.statisticalModel))

      val asmFile = data.asmAugFile().getAbsoluteFile
      if ( !asmFile.getParentFile.exists() ) Files.createDirectories(asmFile.getParentFile.toPath)
      ActiveShapeModelIO.writeActiveShapeModel(asm,asmFile).get
    }

  }


  def augmentModel(model: StatisticalMeshModel)(implicit rng: Random): StatisticalMeshModel = {
    val kernel = GaussianKernel[_3D](20) * 16
    val diagKernel = DiagonalKernel[_3D](kernel, 3)
    val sampler = UniformMeshSampler3D(model.referenceMesh, 700)

    val gp = GaussianProcess[_3D,EuclideanVector[_3D]](diagKernel)
    val lrGP = LowRankGaussianProcess.approximateGP(gp, sampler, numBasisFunctions = 80)
    StatisticalMeshModel.augmentModel(model,lrGP)
  }

}
