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
import scalismo.mesh.TriangleMesh
import scalismo.utils.{ImageConversion, MeshConversion}

object SliverImport {

  def readVolumeCT(file : File) : DiscreteScalarImage[_3D, Short] = {
    val reader = new vtk.vtkMetaImageReader
    reader.SetFileName(file.getAbsolutePath)
    reader.SetFileDimensionality(3)
    reader.Update()
    ImageConversion.vtkStructuredPointsToScalarImage[_3D, Short](reader.GetOutput()).get
  }

  def readLabelmap(file : File) : DiscreteScalarImage[_3D, Byte] = {
    val reader = new vtk.vtkMetaImageReader
    reader.SetFileName(file.getAbsolutePath)
    reader.Update()
    ImageConversion.vtkStructuredPointsToScalarImage[_3D,Byte](reader.GetOutput()).get
  }

  def extractContour(img : DiscreteScalarImage[_3D, Byte]): TriangleMesh[_3D] = {
    import vtk._

    val imgvtk = ImageConversion.imageToVtkStructuredPoints(img)

    val mc = new vtkContourFilter()
    mc.SetValue(0, 1)
    mc.ComputeNormalsOn()
    mc.ComputeGradientsOn()
    mc.SetNumberOfContours(1)

    mc.SetInputData(imgvtk)

    val spd = new vtkPolyDataConnectivityFilter()
    spd.SetExtractionModeToLargestRegion()
    spd.SetInputConnection(mc.GetOutputPort())
    spd.SetExtractionModeToLargestRegion()

    val smoother = new vtkWindowedSincPolyDataFilter
    smoother.SetInputConnection(spd.GetOutputPort());
    smoother.SetNumberOfIterations(30);
    smoother.BoundarySmoothingOff();
    smoother.FeatureEdgeSmoothingOff();
    smoother.SetFeatureAngle(120);
    smoother.SetPassBand(0.1);
    smoother.NonManifoldSmoothingOn();
    smoother.NormalizeCoordinatesOn();
    smoother.Update();


    val decimate = new vtkQuadricClustering()
    decimate.SetNumberOfDivisions(Array(100,100,100))
    decimate.SetInputConnection(smoother.GetOutputPort)
    decimate.Update()

    val meshVTK = decimate.GetOutput()
    val surface = MeshConversion.vtkPolyDataToCorrectedTriangleMesh(meshVTK).get

    surface
  }

}
