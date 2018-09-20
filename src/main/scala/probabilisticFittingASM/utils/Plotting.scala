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
package probabilisticFittingASM.utils

import java.io.File

import org.nspl._
import org.nspl.awtrenderer._
import org.nspl.data.{DataSource, Row}

/**
  * Simple helpers to make initial plots.
  */
object Plotting extends App {


  val listMaps = Seq(
    Map("d"->1.03,"b"->2.0, "c"-> 5.0),
    Map("d"->1.03,"b"->2.8, "c"-> 5.1),
    Map("d"->1.03,"b"->2.3, "c"-> 5.4),
    Map("d"->1.3,"b"->2.1, "c"-> 5.6),
    Map("d"->1.3,"b"->2.2, "c"-> 5.2),
    Map("d"->1.3,"b"->2.1, "c"-> 5.6),
    Map("d"->1.3,"b"->2.4, "c"-> 5.2),
    Map("d"->1.1,"b"->2.1, "c"-> 5.6),
    Map("d"->1.9,"b"->2.1, "c"-> 5.3)
  )
  plotValues(listMaps.flatMap(_.toList),"test","/tmp/plot.pdf")





  def plotValues(namedList: Seq[(String,Double)], title: String, filename: String): Unit = {

    val bpData = boxplotData(namedList)
    val plot = boxplotFromLabels(
      bxdata = bpData,
      xnames = bpData.iterator.map(_.label).toIndexedSeq,
      main = title,
      ygrid = false
    )

    pdfToFile(new File(filename),plot.build, 300)
  }

  def boxplotFromLabels(  bxdata: DataSource,
                          main: String = "",
                          xlab: String = "",
                          xlim: Option[(Double,Double)] = None,
                          ylab: String = "",
                          ylim: Option[(Double,Double)] = None,
                          xnames: Seq[String] = Nil,
                          fontSize: RelFontSize = 1 fts,
                          xgrid: Boolean = true,
                          ygrid: Boolean = true,
                          xWidth: RelFontSize = 20 fts,
                          yHeight: RelFontSize = 20 fts,
                          boxColor: Colormap = Color.gray4,
                          frame: Boolean = true,
                          xLabelRotation: Double = 0d,
                          yLabelRotation: Double = 0d
                       ) = {

    val min = bxdata.iterator.map(_(4)).min
    val max = bxdata.iterator.map(_(5)).max
    var tickSpaceProposal = Math.max(1.0,Math.floor((max-min)/3))
    while ( (max-min)/tickSpaceProposal < 3 ) tickSpaceProposal /= 2

    xyplotareaBuild(
      List(bxdata -> List(boxwhisker(fill = boxColor))),
      AxisSettings(
        LinearAxisFactory,
        customTicks = xnames.zipWithIndex.map(x => x._2.toDouble + 1 -> x._1),
        numTicks = if (xnames.isEmpty) 5 else 0,
        fontSize = fontSize,
        width = xWidth,
        labelRotation = xLabelRotation
      ),
      AxisSettings(
        LinearAxisFactory,
        fontSize = fontSize,
        width = yHeight,
        labelRotation = yLabelRotation,
        baseTick = Some(0.0),
        tickSpace = Some(tickSpaceProposal)
      ),
      None,
      xlim = if (xlim.isDefined) xlim else Some(0.25d -> (bxdata.iterator.size + 0.75)),
      ylim = if (ylim.isDefined) ylim else Some( 0.0/*(min-(max-min)*0.05) */-> (max+(max-min)*0.05)),
      xgrid = xgrid,
      ygrid = ygrid,
      frame = frame,
      main = main,
      xlab = xlab,
      ylab = ylab,
      xlabFontSize = fontSize,
      ylabFontSize = fontSize,
      mainFontSize = fontSize
    )
  }


  def boxwhisker(
                  xCol: Int = 0,
                  medianCol: Int = 1,
                  q1Col: Int = 2,
                  q3Col: Int = 3,
                  minCol: Int = 4,
                  maxCol: Int = 5,
                  x2Col: Int = 6,
                  fillCol: Int = 7,
                  width: Double = 1,
                  stroke: Stroke = Stroke(1d),
                  strokeColor: Color = Color.black,
                  fill: Colormap = Color.white
                ) = new DataRenderer {
    def asLegend = Some(PointLegend(shapeList(1), fill(0)))

    def render[R <: RenderingContext](
                                       data: Row,
                                       xAxis: Axis,
                                       yAxis: Axis,
                                       ctx: R,
                                       tx: AffineTransform
                                     )(implicit re: Renderer[ShapeElem, R], rt: Renderer[TextBox, R]): Unit = {

      val wX1 = data(xCol)
      val q2 = data(medianCol)
      val q1 = data(q1Col)
      val q3 = data(q3Col)
      val min = data(minCol)
      val max = data(maxCol)
      val color1 =
        if (data.dimension > fillCol) fill(data(fillCol)) else fill(0d)
      val width1 = if (data.dimension > x2Col) data(x2Col) - wX1 else width
      val wX = wX1 + .5 * width1

      if (wX >= xAxis.min && wX <= xAxis.max) {

        val vWidth =
          math.abs(xAxis.worldToView(0.0) - xAxis.worldToView(width1))
        val vX = xAxis.worldToView(wX)
        val vQ1 = yAxis.worldToView(q1)
        val vQ2 = yAxis.worldToView(q2)
        val vQ3 = yAxis.worldToView(q3)
        val vMin = yAxis.worldToView(min)
        val vMax = yAxis.worldToView(max)
        val vHeight = math.abs(vQ1 - vQ3)

        // box outline
        val shape1 = ShapeElem(
          Shape.rectangle(vX - vWidth * 0.5, vQ3, vWidth, vHeight),
          fill = color1,
          stroke = Some(stroke),
          strokeColor = strokeColor
        ).transform(_ => tx)

        re.render(ctx, shape1)

        // median
        val shape2 = ShapeElem(
          Shape.line(Point(vX - vWidth * 0.5, vQ2),
            Point(vX + vWidth * 0.5, vQ2)),
          fill = color1,
          stroke = Some(stroke),
          strokeColor = strokeColor
        ).transform(_ => tx)

        re.render(ctx, shape2)

        // lower spike
        val shape3 = ShapeElem(
          Shape.line(Point(vX, vQ1), Point(vX, vMin)),
          fill = color1,
          stroke = Some(stroke),
          strokeColor = strokeColor
        ).transform(_ => tx)

        re.render(ctx, shape3)

        // upper spike
        val shape4 = ShapeElem(
          Shape.line(Point(vX, vQ3), Point(vX, vMax)),
          fill = color1,
          stroke = Some(stroke),
          strokeColor = strokeColor
        ).transform(_ => tx)

        re.render(ctx, shape4)

      }
    }
  }



}
