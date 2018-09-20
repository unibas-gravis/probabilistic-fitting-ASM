# Probabilistic Fitting of Active Shape Models
This project makes the code available which is used for active shape model (ASM) fitting in the publication<br>
***Probabilistic Fitting of Active Shape Models*** [1].

# Getting Started

## The Environment
We assume that you have **sbt** already installed. If not, please
follow the instructions given
[here](http://www.scala-sbt.org/release/tutorial/Setup.html).


## Building the Software
We recommend that you build the software as a JAR-file. To create the
JAR-File, issue the command ```sbt assembly``` in the project root
folder where also the ```build.sbt``` file is present. This should
then generate the file ```target/scala-2.12/shape18-asm-sampling.jar```

Alternatively, if you are familiar with **sbt** you can get started by
simply running ```sbt``` in the project directory.
 


## Preparing the Data
First you need to obtain the 20 training items of the
[SLIVER07](http://www.sliver07.org/) dataset [2]. The data set contains
the ct-volumes and the segmentation maps. We provide our landmarks
for those volumes. These are handclicked by a computer scientist,
not by an internist. You should sort the volume data into the folders
```data/sliver/volume-ct``` and ```data/sliver/segmentation``` like
 this:

```
data/sliver/
├── landmarks
│   ├── liver-orig001.json
│   ├── liver-orig002.json
│   └── ...
├── volume-ct
│   ├── liver-orig001.mhd
│   ├── liver-orig001.raw
│   ├── liver-orig002.mhd
│   └── ...
└── volume-labelmap
    ├── liver-seg001.mhd
    ├── liver-seg001.raw
    ├── liver-seg002.mhd
    └── ...
```

From the projects root directory, you can use the JAR-file to prepare
and align the data using following command:

```
java -Xmx8g -cp target/scala-2.12/shape18-asm-sampling.jar probabilisticFittingASM.ImportData
```

The output will be written under the directory ```data/experiments```.

As we are not allowed to distribute derived data from the SLIVER07
data (see the [rules](http://www.sliver07.org/rules.php)),
we can not provide our registered meshes. So you have to use
your registration to register the meshes from the directoy
```data/experiments/segmentation```. The registered meshes should
have the same format and filename and go into the directory
```data/experiments/registered```.

## Building the Models

Based on the registered data we build the ASM with the command:

```
java -Xmx8g -cp target/scala-2.12/shape18-asm-sampling.jar probabilisticFittingASM.BuildModels
```
If you want to run the leave-one-out experiment, issue the command a
second time with additional option ```-l``` standing for
**l**eave-one-out. For the leave one out experiment, we allow the
shape to deform more than the standard PCA-based model. For this we
augment the model using the command:

```
java -Xmx8g -cp target/scala-2.12/shape18-asm-sampling.jar probabilisticFittingASM.AugmentModels
```

## Fitting the Models
Having the models built, we can run the different fittings:

1. The standard fitting can be started with:
    ```
    java -Xmx8g -cp target/scala-2.12/shape18-asm-sampling.jar probabilisticFittingASM.BuildModels
    ```
    *Add the ```-l``` option for using the leave-one-out model.*

1. The fitting using sampling can be started with:
    ```
    java -Xmx8g -cp target/scala-2.12/shape18-asm-sampling.jar probabilisticFittingASM.BuildModels
    ```
    *Add the ```-l``` option for using the leave-one-out model.*

1. When you provide in addition some annotated lines, you can run the
  fitting using sampling incorporating the lines. The lines are
  provided as **vtk** mesh where only the vertex locations are used.
  Place your meshes in the folder ```data/experiments/lines``` having
  the same name as the meshes generated in
  ```data/experiments/segmentation```. The command to fit with lines is:
    ```
    java -Xmx8g -cp target/scala-2.12/shape18-asm-sampling.jar probabilisticFittingASM.BuildModels
    ```
    *Add the ```-l``` option for using the leave-one-out model.*

Each experiments write the final segmentation as mesh into one of
the following folders:
```
data/experiments/testInModel/standard
data/experiments/testInModel/sampling
data/experiments/testInModel/lines

data/experiments/leaveOneOut/standard
data/experiments/leaveOneOut/sampling
data/experiments/leaveOneOut/lines
```

## Evaluating the Results

To evalute the segmentations run the following script:

```
java -Xmx8g -cp target/scala-2.12/shape18-asm-sampling.jar probabilisticFittingASM.BuildModels
```
This command will fill the following two folder with files reporting
the evaluation measures and initial plots:
```
data/experiments/testInModel/statistics

data/experiments/leaveOneOut/statistics
```

## Making the Plots

The plots of the paper can be produced with the help of
[R](https://www.r-project.org/). The script for that is located in ```resources/scripts```.


# Learning more ...

When you are interested to learn more about our software and the
theory behind it, please have a look at our
[GPMM website](https://gravis.dmi.unibas.ch/PMM/) with tutorials and online
courses.

# References

<table>
<tr>
<td>[1]</td>
<td>
<i>A. Morel-Forster, Th. Gerig, M. Lüthi, Th. Vetter:</i>
<b>Probabilistic Fitting of Actiave Shape Models</b>.
<i>ShapeMi Workshop, MICCAI (2018)</i>
</td>
</tr>
<tr>
<td>[2]</td>
<td>
<i>Heimann, T., van Ginneken et al., B.:</i>
<b>Comparison and evaluation of methods for liver segmentation from ct datasets.</b>
<i>IEEE Transactions on Medical Imaging 28(8), 1251–1265 (Aug 2009)</i>
<a href="https://doi.org/10.1109/TMI.2009.2013851">https://doi.org/10.1109/TMI.2009.2013851</a>
</td>
</tr>
</table>
