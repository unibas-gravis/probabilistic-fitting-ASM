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

import probabilisticFittingASM.data.{ASMBuilder, DataProvider, SSMBuilder}
import probabilisticFittingASM.utils.ExperimentOptions
import scalismo.io.{ActiveShapeModelIO, StatismoIO}
import scalismo.utils.Random

object BuildModels {


  def main(args: Array[String]): Unit = {
    scalismo.initialize()
    implicit val rng = Random(1024l)

    val opts = new ExperimentOptions(args)
    opts.verify()

    val dataProvider = DataProvider(opts.root(),opts.leaveOneOut())

    if (opts.leaveOneOut()) {
      for (i <- (0 until dataProvider.ids.size) ) {
        val testDatum = dataProvider.ids(i)
        println(s"leave one out - $testDatum")

        val trainingData = dataProvider.ids.patch(i, Nil, 1)

        val ssm = SSMBuilder.buildModel(dataProvider, trainingData)
        val shapeModelFile = dataProvider.ssmFile(testDatum).getAbsoluteFile()
        Files.createDirectories(shapeModelFile.getParentFile.toPath)
        StatismoIO.writeStatismoMeshModel(ssm, shapeModelFile)

        val asm = ASMBuilder.buildModel(dataProvider, trainingData, ssm)
        val asmFile = dataProvider.asmFile(testDatum).getAbsoluteFile()
        Files.createDirectories(asmFile.getParentFile.toPath)
        ActiveShapeModelIO.writeActiveShapeModel(asm,asmFile).get
      }
    } else {
      val trainingData = dataProvider.ids
      println(s"build model using all training data")

      val ssm = SSMBuilder.buildModel(dataProvider, trainingData)
      val shapeModelFile = dataProvider.ssmFile().getAbsoluteFile()
      Files.createDirectories(shapeModelFile.getParentFile.toPath)
      StatismoIO.writeStatismoMeshModel(ssm, shapeModelFile).get

      val asm = ASMBuilder.buildModel(dataProvider, trainingData, ssm)
      val asmFile = dataProvider.asmFile().getAbsoluteFile()
      Files.createDirectories(asmFile.getParentFile.toPath)
      ActiveShapeModelIO.writeActiveShapeModel(asm,asmFile).get
    }
  }

}
