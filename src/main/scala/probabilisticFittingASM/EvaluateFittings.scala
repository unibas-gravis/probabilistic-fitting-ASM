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

import com.github.tototoshi.csv.CSVWriter
import probabilisticFittingASM.data.DataProvider
import probabilisticFittingASM.utils._
import scalismo.geometry._3D
import scalismo.io.MeshIO
import scalismo.mesh.{MeshMetrics, TriangleMesh}
import scalismo.utils.Random

import scala.collection.immutable

object EvaluateFittings {

  def main(args: Array[String]): Unit = {
    scalismo.initialize()
    implicit val rng = Random(1024l)

    val opts = new ExperimentOptions(args)
    opts.verify()

    val dataProvider = DataProvider(opts.root(),opts.leaveOneOut())

    val errors = for (i <- (0 until dataProvider.ids.size) ) yield {
      val testDatum = dataProvider.ids(i)
      println(s"evaluating - $testDatum")
      val fits = loadFits(dataProvider, testDatum)
      evaluateFit(dataProvider, testDatum, fits)
    }

    plot(dataProvider,errors)
    writeCSV(dataProvider,errors)
  }

  def loadFits(data: DataProvider, testDatum: String)(implicit rng: Random): Map[String,TriangleMesh[_3D]] = {
    val registeredMeshFile = data.registrationFile(testDatum)
    val registeredMesh = MeshIO.readMesh(registeredMeshFile).get

    val expNameWithFit = DataProvider.fittingMethods.map{fittingMethod =>
      val fitResultMeshFile = data.fitFile(testDatum,fittingMethod)
      fittingMethod -> MeshIO.readMesh(fitResultMeshFile).get
    }.toMap

    expNameWithFit ++ Map(("registration"->registeredMesh))
  }


  def evaluateFit(data: DataProvider, testDatum: String, fits: Map[String,TriangleMesh[_3D]])(implicit rng: Random): Map[String,Map[String,Double]] =  {
    val gtMesh = MeshIO.readMesh(data.segmentationFile(testDatum)).get

    def bidirectionalDist(m1: TriangleMesh[_3D], m2: TriangleMesh[_3D]): Double = {
      MeshMetrics.avgDistance(m1, m2) * 0.5 +
        MeshMetrics.avgDistance(m2, m1) * 0.5
    }

    fits.map { case (method, fit) =>
      method -> Map(
        "avg" -> MeshMetrics.avgDistance(fit, gtMesh),
        "bia" -> bidirectionalDist(fit, gtMesh),
        "hdd" -> MeshMetrics.hausdorffDistance(fit, gtMesh),
        "dice" -> MeshMetrics.diceCoefficient(fit, gtMesh)
      )
    }
  }

  def plot(data: DataProvider, errors: immutable.IndexedSeq[Map[String, Map[String, Double]]]): Unit = {
    errors.head.keys.foreach { case key =>
      val outFile = data.statisticsFile(s"fit_${key}.pdf").getAbsoluteFile
      if ( !outFile.getParentFile.exists() ) Files.createDirectories(outFile.getParentFile.toPath)
      Plotting.plotValues(errors.flatMap(_ (key).toSeq) , key, outFile.toString)
    }

    errors.head.values.head.keys.foreach { case key =>
      val outFile = data.statisticsFile(s"fit_${key}.pdf").getAbsoluteFile
      if ( !outFile.getParentFile.exists() ) Files.createDirectories(outFile.getParentFile.toPath)
      Plotting.plotValues(errors.flatMap(_.map(p => p._1 -> p._2(key)).toSeq) , key, outFile.toString)
    }
  }


  def writeCSV(data: DataProvider, errors: IndexedSeq[Map[String, Map[String, Double]]]): Unit = {


    val repetitions = errors.size
    val fittings = errors.head.keys.toIndexedSeq
    val measures = errors.head.head._2.keys.toIndexedSeq

    {
      val file = data.statisticsFile("fittings.csv")
      println(s"... writing file $file")
      val writer = CSVWriter.open(file)
      val header = "Fitting" +: measures
      writer.writeRow(header)
      fittings.foreach { fitting =>
        val data = (0 until repetitions).map { i =>
          measures.map(measure => errors(i)(fitting)(measure))
        }
        val lines = data.map { fitting +: _ }
        writer.writeAll(lines)
      }
      writer.flush()
      writer.close()
    }

    {
      val file = data.statisticsFile("fit_measures.csv")
      println(s"... writing file $file")
      val writer = CSVWriter.open(file)
      val header = "Measure" +: fittings
      writer.writeRow(header)
      measures.foreach { measure =>
        val data = (0 until repetitions).map { i =>
          fittings.map(fitting => errors(i)(fitting)(measure))
        }
        val lines = data.map {  measure +: _ }
        writer.writeAll(lines)
      }
      writer.flush()
      writer.close()
    }

  }


}
