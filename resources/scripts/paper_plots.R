library(dplyr)
library(beanplot)
library(latex2exp)

# output directory
# test in model (TIM) setup
TIMDIR="../../data/experiments/testInModel/"
TIMTITLE="TIM"
matrixTIM <- read.csv(file=paste(TIMDIR,"statistics/fit_measures.csv",sep=""), header=TRUE, sep=",")
dataTIM <- data.frame(matrixTIM)


# leave one out (LOO) setup
LOODIR="../../data/experiments/leaveOneOut/"
LOOTITLE="LOO"
matrixLOO <- read.csv(file=paste(LOODIR,"statistics/fit_measures.csv",sep=""), header=TRUE, sep=",")
dataLOO <- data.frame(matrixLOO)


shape18Plot <- function(data, experiment, expdir, shortname, measurement, limits, tics) {
    
    COLORLIST=list(c("dodgerblue", "navy"), c("cyan","blue"), c("green","darkgreen"))
    
    set <- subset(data, Measure == shortname)
    data <- select(set, standard, sampling, lines)
    pdf(paste(expdir,"plots/",experiment,"-",shortname,".pdf",sep=""),width=4,height=3.25)
    par(mar = c(1.75, 5,1,2))
    beanplot(
        main=paste(experiment," - ",measurement),
        data= data,
        col = COLORLIST,
        ylim = limits,
        beanlines="median",
        ll=0.1,
        axes=F,
        log=""
    )

    XTICS=c("standard","sampling","w. lines")
    XTICSPOS=c(1,2,3)

    axis(side=1, col = "white", at = XTICSPOS, labels = XTICS, cex.axis = 0.8, padj = -0.5)
    axis(side=1, at = XTICSPOS, labels = FALSE)
    axis(2,tics)
    dev.off()
}


# dice specific configuration
DICETITLE="Dice Score"
DICELABEL="Dice Score"
DICELIM=c(0.599,1.01)
DICEYTICS=seq(from=0.6,to=1,by=0.1)

shape18Plot(dataTIM, TIMTITLE, TIMDIR, "dice", DICETITLE, DICELIM, DICEYTICS)
shape18Plot(dataLOO, LOOTITLE, LOODIR, "dice", DICETITLE, DICELIM, DICEYTICS)

# bi avg dist specific configuration
BADTITLE="Bi. Average Distance [mm]"
BADLABEL=TeX('Bi. Average Distance $\\[mm\\]$')
BADLIM=c(-0.1,15.1)
BADYTICS=seq(from=0,to=15,by=5)

shape18Plot(dataTIM, TIMTITLE, TIMDIR, "bia", BADTITLE, BADLIM, BADYTICS)
shape18Plot(dataLOO, LOOTITLE, LOODIR, "bia", BADTITLE, BADLIM, BADYTICS)

# hausdorff specific configuration
HDDTITLE="Hausdorff Distance [mm]"
HDDLABEL=TeX('Hausdorf Distance $\\[mm\\]$')
HDDLIM=c(-0.1,70.1)
HDDYTICS=seq(from=0,to=70,by=10)

shape18Plot(dataTIM, TIMTITLE, TIMDIR, "hdd", HDDTITLE, HDDLIM, HDDYTICS)
shape18Plot(dataLOO, LOOTITLE, LOODIR, "hdd", HDDTITLE, HDDLIM, HDDYTICS)