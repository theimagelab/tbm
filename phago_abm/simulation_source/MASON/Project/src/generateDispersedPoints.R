library("spatstat")
library("ggplot2")
library("plotly")
library("lhs")
rm(list=ls())
## R script to generate a .csv of xyz positions that fulfils these criteria 
# 1. Represents the centers of non-overlapping spheres
# 2. The nearest neighbour index of the points is between nni_min and nni_max.
## For use in ABM simulation.
n_macs<-18 # dummy number. arbitrary.
mean_r_gc=81 # mean gc radius. based on a spherical gc with mean volume 2229154 um^3.
n_runs<-100

random_nonoverlapping_spheres <- function(n_sph, r_sph, r_boundary=81){
    result <- matrix(0, nrow=n_sph, ncol=3)
    R <- r_boundary 
    added_spheres<-0
    while (added_spheres < n_sph) {
        phi <- runif(min = 0, max = pi, n=1)
        theta <- runif(min = 0, max = 2*pi, n=1)
        r <- runif(min = 0, max = R, n=1)
        
        x<- r * cos(theta) * sin(phi)
        y<-r*sin(theta)*sin(phi)
        z<-r*cos(phi)
        
        pos=t(as.matrix(c(x, y, z)))
        #pos<-sphereplot::sph2car(long=theta, lat=phi, radius = r, deg = FALSE)
        
        if (added_spheres<=1) {
            dists_xyz_sq<-(result[1:added_spheres,]-pos)^2
        } else {
            dists_xyz_sq <- sweep(result[1:added_spheres,], 2, pos)^2
        }
        dists<-sqrt(rowSums(dists_xyz_sq))
        if (  any(dists < 2*r_sph) == FALSE) {
            # success, new point doesn't touch any existing spheres
            added_spheres<-added_spheres+1
            result[added_spheres,]<-pos
        } 
    }
    result # matrix of xyz positions of centres of spheres,
}


nn_rand_df<-data.frame(matrix(ncol=1, nrow=0))
points<-list()
range=c(0,81)
for (i in 1:n_runs) {
    #random uniform distribution of macs.
    sph_pos<-random_nonoverlapping_spheres(n_sph=n_macs, r_sph=14.7, r_boundary=mean_r_gc) %>% as.data.frame()
    sph_pos_pp3<-pp3(sph_pos$V1, sph_pos$V2, sph_pos$V3, xrange = range, yrange = range, zrange = range)
    rand_nn_dists<-nndist.pp3(sph_pos_pp3)
    mean_1nn<-mean(rand_nn_dists)
    
    points[[i]]<-sph_pos
    nn_rand_df<-rbind(nn_rand_df, mean_1nn)
}

nn_rand_df<-as.data.frame(nn_rand_df)
colnames(nn_rand_df)<-c("nn1_dists")
mean_nn_random<-mean(nn_rand_df$nn1_dists)
mean_nn_random
#saveRDS(nn_rand_df, file ="initMacPositionsDataFrame.rds")
# for (i in 1:length(points)) {
#     dfOut <- points[[i]] %>% as.data.frame()
#     write.csv(dfOut, paste("initialPositions_seed",i,".csv", sep=""),  row.names=FALSE)
# }

## read the csvs again. to plot their nndist.
range=c(0,81)
nn_rand_df<-data.frame(matrix(ncol=1, nrow=0))
files<-list.files(paste(getwd(), "/macInitPos_meanNNdist=36.8/", sep=""), recursive=FALSE, full.names=TRUE)
for (file in files) {
    df<-read.csv(file)
    sph_pos_pp3<-pp3(df$V1, df$V2, df$V3, xrange = range, yrange = range, zrange = range)
    rand_nn_dists<-nndist.pp3(sph_pos_pp3)
    mean_1nn<-mean(rand_nn_dists)
    nn_rand_df<-rbind(nn_rand_df, mean_1nn)
}
colnames(nn_rand_df)<-c("nn1_dists")

    
plot<-ggplot(data=nn_rand_df, aes(x="nn1_dists", y=nn1_dists)) +
    geom_violin( color="black", scale="width", size=1) + 
    geom_boxplot(width=0.1, fill="white", color="black") +
    scale_fill_manual(values=c( "#E41A1C","grey60", "grey60")) + 
    ylab( bquote('Nearest Neighbour Distance '~(um)) ) #+ theme_minimal_big_font() 
plot

# save the nndist data so we can plot alongside the experimental nndists in it's own R script.
saveRDS(nn_rand_df, file="simulatedNNdistPoints.Rds")
