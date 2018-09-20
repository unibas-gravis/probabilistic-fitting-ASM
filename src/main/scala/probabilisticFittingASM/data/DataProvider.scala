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

import java.io.File

import scalismo.geometry._3D
import scalismo.image.DiscreteScalarImage

import scala.reflect.io.Path

case class DataProvider(root: Path, leaveOneOut: Boolean) {
  import DataProvider._

  val dataDirectory = root / "experiments"
  val experimentDirectory = dataDirectory / (if (leaveOneOut) leaveOneOutExperiment else testInModelExperiment)
  val sliverDirectory = root / "sliver"

  val ids: Seq[String] = ( sliverDirectory / "volume-ct" ).toDirectory.files.filter(_.toFile.name.endsWith(".mhd")).map(f => f.jfile.getName.replace(".mhd","")).toIndexedSeq.sorted

  // sliver data
  def origVolume(id: String): File = (sliverDirectory / "volume-ct" / s"$id.mhd").jfile
  def loadOrigVolume(file: File): DiscreteScalarImage[_3D, Short] = SliverImport.readVolumeCT( file )

  def origSegmentationFile(id: String): File = (sliverDirectory / "volume-labelmap" / s"$id.mhd").jfile
  def origSegmentationVolume(file: File): DiscreteScalarImage[_3D, Byte] = SliverImport.readLabelmap( file )

  // reference
  def referenceMesh(): File = (sliverDirectory / "reference" / "handcrafted.stl").jfile
  def referenceLandmarks(): File = (sliverDirectory / "reference" / "handcrafted.csv").jfile
  def initialModelFile(): File = (sliverDirectory / "initial-model" / "handcrafted.h5").jfile

  // supplied data
  def rawLandmarkFile(id: String): File = (sliverDirectory / "landmarks" / s"$id.json").jfile
  def rawLineFile(id: String): File = throw new Exception("The lines were marked on aligned data. You should not ask for this file.")

  // aligned data
  def volumeFile(id: String): File = (dataDirectory / "ct-volume" / s"$id.nii").jfile
  def segmentationFile(id: String): File = (dataDirectory / "segmentation" / s"$id.vtk").jfile
  def landmarksFile(id: String): File = (dataDirectory / "landmarks" / s"$id.json").jfile

  def registrationFile(id: String): File = (dataDirectory / "registered" / s"$id.vtk").jfile
  def linesFile(id: String): File = (dataDirectory / "lines" / s"$id.vtk").jfile


  // experimental data
  def ssmFile(id: String): File = (experimentDirectory / "ssm" / s"$id.h5").jfile
  def ssmFile(): File = ssmFile(testInModelExperiment)
  def asmFile(id: String): File = (experimentDirectory / "asm" / s"$id.h5").jfile
  def asmFile(): File = asmFile(testInModelExperiment)
  def asmAugFile(id: String): File = (experimentDirectory / "asm-aug" / s"$id.h5").jfile
  def asmAugFile(): File = asmAugFile(testInModelExperiment)

  def fitFile(id: String, fittingMethod: String): File = (experimentDirectory / fittingMethod / s"$id.vtk").jfile

  def statisticsFile(filename: String): File = (experimentDirectory / "statistics" / filename).jfile

}


object DataProvider {
  final val fittingUsingStandardASM = "standard"
  final val fittingUsingSampling = "sampling"
  final val fittingUsingSamplingWithLines = "lines"
  val fittingMethods: Seq[String] = Seq(fittingUsingStandardASM,fittingUsingSampling,fittingUsingSamplingWithLines)

  final val leaveOneOutExperiment = "leaveOneOut"
  final val testInModelExperiment = "testInModel"
  val experimentalSetups: Seq[String] = Seq(leaveOneOutExperiment,testInModelExperiment)
}