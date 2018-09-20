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

import probabilisticFittingASM.data.{DataProvider, SliverImport}
import probabilisticFittingASM.utils.ExperimentOptions
import scalismo.common.{BoxDomain, Scalar}
import scalismo.geometry.{Point, _3D}
import scalismo.image.{DiscreteImageDomain, DiscreteScalarImage}
import scalismo.io.{ImageIO, LandmarkIO, MeshIO}
import scalismo.registration.{LandmarkRegistration, RigidTransformation}
import scalismo.utils.Random

import scala.reflect.ClassTag
import scala.reflect.runtime.universe.TypeTag

object ImportData {

  def main(args: Array[String]): Unit = {
    scalismo.initialize()
    implicit val rng = Random(1024l)

    val opts = new ExperimentOptions(args)
    opts.verify()

    val data = DataProvider(opts.root(),true)

    val referenceLandmarks = LandmarkIO.readLandmarksCsv[_3D](data.rawLandmarkFile(data.ids.head)).get

    data.ids.foreach{ id =>
      println(s"importing data for $id")
      val convertedId = convertId(id)

      val targetLM = LandmarkIO.readLandmarksJson[_3D](data.rawLandmarkFile(id)).get

      val t = LandmarkRegistration.rigid3DLandmarkRegistration(targetLM,referenceLandmarks,Point(0,0,0))

      { // align landmarks
        val alignedLandmarks = targetLM.map( lm => lm.copy(point = t(lm.point)))
        val outFile = data.landmarksFile(id).getAbsoluteFile()
        if (!outFile.getParentFile.exists()) Files.createDirectories(outFile.getParentFile.toPath)
        LandmarkIO.writeLandmarksJson(alignedLandmarks,outFile).get
      }

      { // align ct-volume
        val volume = data.loadOrigVolume(data.origVolume(id))
        val alignedVolume = alignImage(volume, t)
        val outFile = data.volumeFile(id).getAbsoluteFile()
        if (!outFile.getParentFile.exists()) Files.createDirectories(outFile.getParentFile.toPath)
        ImageIO.writeNifti(alignedVolume.map(_.toFloat),outFile).get
      }

      { // align extracted contour
        val outFile = data.segmentationFile(id).getAbsoluteFile()

        val file = data.origSegmentationFile(convertedId)
        val labelMap = data.origSegmentationVolume(file)

        if (!outFile.getParentFile.exists()) Files.createDirectories(outFile.getParentFile.toPath)

        MeshIO.writeMesh(SliverImport.extractContour(labelMap).transform(t), outFile).get
      }
    }
  }

  def convertId(id: String): String = {
    id.replace("-orig","-seg")
  }


  def alignImage[T: Scalar : TypeTag : ClassTag](originalImage: DiscreteScalarImage[_3D,T], transform: RigidTransformation[_3D]): DiscreteScalarImage[_3D, T] = {
    val contImage = originalImage.interpolate(3)
    val (o1, o2) = (originalImage.domain.boundingBox.origin, originalImage.domain.boundingBox.oppositeCorner)
    val xMin = Math.min(o1.x, o2.x)
    val xMax = Math.max(o1.x, o2.x)
    val yMin = Math.min(o1.y, o2.y)
    val yMax = Math.max(o1.y, o2.y)
    val zMin = Math.min(o1.z, o2.z)
    val zMax = Math.max(o1.z, o2.z)

    val originalPoints = Seq(Point(xMin, yMin, zMin),Point(xMax, yMin, zMin),Point(xMax, yMin, zMax),Point(xMin, yMin, zMax),Point(xMin, yMax, zMax),Point(xMin, yMax, zMin),Point(xMax, yMax, zMin),Point(xMax, yMax, zMax))
    val transformedPoints = originalPoints.map {p => transform(p)}
    val newOrigin = Point(transformedPoints.minBy(_.x).x, transformedPoints.minBy(_.y).y, transformedPoints.minBy(_.z).z)
    val newOpposite = Point(transformedPoints.maxBy(_.x).x, transformedPoints.maxBy(_.y).y, transformedPoints.maxBy(_.z).z)

    val newBoundingBox = BoxDomain(newOrigin, newOpposite)
    val domain = DiscreteImageDomain(newBoundingBox, originalImage.domain.spacing)

    val inverse = transform.inverse
    contImage.compose(inverse).sample[T](domain, outsideValue = 0)
  }
}
