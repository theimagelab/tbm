library("spatstat")
library("ggplot2")
library("plotly")


spherical_pack_soln <- 48.8

#### random points (i.e. spheres can overlap)
n_macs<-18 # dummy number. arbitrary.
mean_r_gc=81 # mean gc radius. based on a spherical gc with mean volume 2229154 um^3.
n_runs<-10
mac_radius <- 10

theme_minimal_big_font <- function() {
    theme_minimal() %+replace%
        theme(
            axis.text.x=element_text(size=rel(2),vjust=1),
            axis.text.y=element_text(size=rel(2)),
            axis.title.x=element_text(size=rel(2)),
            axis.title.y=element_text(size=rel(2), angle=90),
            axis.line = element_line(color="black",size=rel(2)),
            panel.grid.major = element_blank(),
            panel.grid.minor = element_blank(),
            axis.ticks = element_line(colour = "black"), 
            axis.ticks.length = unit(0.15, "cm")
        )
}

random_points <- function(n, r_boundary=81){
    result <- matrix(0, nrow=n, ncol=3)
    added_spheres<-0
    while (added_spheres < n) {
        phi <- runif(min = 0, max = pi, n=1)
        theta <- runif(min = 0, max = 2*pi, n=1)
        radius_from_centre <- runif(min = 0, max = r_boundary, n=1)
        
        pos<-sphereplot::sph2car(long=theta, lat=phi, radius = radius_from_centre, deg = FALSE)

        added_spheres<-added_spheres+1
        result[added_spheres,]<-pos
    }
    result # matrix of xyz positions of centres of spheres,
}


#n=number of spheres, r= radius of small spheres, r_boundary=radius of the sphere in which all small spheres are placed.
random_nonoverlapping_spheres <- function(n_sph, r_sph, r_boundary=81){
    result <- matrix(0, nrow=n_sph, ncol=3)
    R <- r_boundary - r_sph # avoid spheres intersecting with border.
    added_spheres<-0
    while (added_spheres < n_sph) {
        phi <- runif(min = 0, max = pi, n=1)
        theta <- runif(min = 0, max = 2*pi, n=1)
        radius_from_centre <- runif(min = 0, max = R, n=1)
        
        pos<-sphereplot::sph2car(long=theta, lat=phi, radius = radius_from_centre, deg = FALSE)
        
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

#### plot random spheres,
sph_pos<-random_nonoverlapping_spheres(n_sph=18, r_sph=mac_radius, r_boundary=81) %>% as.data.frame()


#gc as sphere with radius 
theta_gc = seq(from=0,to=2*pi,length.out=100)
phi_gc = seq(from=0,to=pi,length.out=100)
x_gc<-mean_r_gc * cos(theta_gc)*sin(phi_gc)
y_gc<-mean_r_gc*sin(theta_gc)*sin(phi_gc)
z_gc<-mean_r_gc*cos(phi_gc)
gc_sphere_coords<-cbind(x_gc,y_gc,z_gc) %>% as.data.frame()

fig<-plot_ly(sph_pos,x=~V1, y=~V2, z=~V3 ) %>% add_markers(marker=list(size=25, opacity=0.8, line=list(color='black', width=1))) %>%
    #add_markers(x=0,y=0,z=30, marker=list(size=250, opacity=0.1)) %>% 
    layout(scene=list(aspectmode='cube'))
    
fig


df1<-read.csv("imaging_statistics/TBM_positions/M450 WK27 nuctg GC2 Zstack_hqmacs_Position.csv", header=TRUE, skip=3, stringsAsFactors = FALSE)
df2<-read.csv("imaging_statistics/TBM_positions/M370 TOM LN2 GC2 Zstack quite steady_hqmacs_Position.csv", header=TRUE,  skip=3,stringsAsFactors=FALSE)
df3<-read.csv("imaging_statistics/TBM_positions/M71 TOM GC1 Zstack_hqmacs_Position.csv", header=TRUE, skip=3,stringsAsFactors=F)
df<-rbind(df1,df2,df3)
box3<-box3(xrange=c(-1000, 1000), yrange=c(-1000, 1000), zrange=c(-1000, 1000))
d1<-nndist.pp3(pp3(df1[,1], df1[,2], df1[,3], box3))
d2<-nndist.pp3(pp3(df2[,1], df2[,2], df2[,3], box3))
d3<-nndist.pp3(pp3(df3[,1], df3[,2], df3[,3], box3))

d<-c(d1,d2,d3)

sd(d)
mean(d)
quantile(d,0.1)
quantile(d,0.9)


nn_rand_df<-data.frame(matrix(ncol=1, nrow=0))
points<-list()
range=c(0,81)
for (i in 1:n_runs) {
    #random uniform distribution of macs.
    sph_pos<-random_nonoverlapping_spheres(n_sph=n_macs, r_sph=mac_radius, r_boundary=mean_r_gc) %>% as.data.frame()
    #sph_pos<-random_points(n=18, r_boundary=81) %>% as.data.frame()
    sph_pos<-pp3(sph_pos$V1, sph_pos$V2, sph_pos$V3, xrange = range, yrange = range, zrange = range)
    rand_nn_dists<-nndist.pp3(sph_pos)
    mean_1nn<-mean(rand_nn_dists)
    
    points[i]<-sph_pos
    nn_rand_df<-rbind(nn_rand_df, mean_1nn)
}

nn_rand_df<-as.data.frame(nn_rand_df)
colnames(nn_rand_df)<-c("nn1_dists")
mean_nn_random<-mean(nn_rand_df$nn1_dists)

rand_1nn_sphere<-as.data.frame(nn_rand_df)
rand_1nn_sphere$variable <- "random_1nn_sphere"
data_df_1nn<-as.data.frame(d)
data_df_1nn$variable <- "observed_1nn"
colnames(rand_1nn_sphere) <- c("value", "variable")
colnames(data_df_1nn) <- c("value", "variable")

rand_df_means<-data.frame(matrix(ncol=2, nrow=n_runs))
colnames(rand_df_means)<-c("mean_1nn_dist", "mean_4nn_dist")

plot_df<-rbind(rand_1nn_sphere, data_df_1nn)
plot<-ggplot(data=plot_df, aes(x=variable, y=value, fill=variable)) +
    geom_violin( color="black", scale="width", size=1) + 
    geom_boxplot(width=0.1, fill="white", color="black") +
    scale_fill_manual(values=c( "#E41A1C","grey60", "grey60")) + 
    geom_hline(yintercept=spherical_pack_soln, linetype='dashed') + ylim(c(0,80)) + 
    ylab( bquote('Nearest Neighbour Distance '~(um)) ) + theme_minimal_big_font() 
plot
#ggsave(plot, filename="nn_vs_random_violin_n=55,900,wilcoxpval=6.3e-14.svg")

nni<-mean(data_df_1nn$value)/mean(rand_1nn_sphere$value)

nn1_pvalue<-wilcox.test(data_df_1nn$value,rand_1nn_sphere$value)


# average nn dists for a random sample.
null_1nn_dist<-rand_1nn_sphere[['mean_1nn_dist']] %>% mean()
null_4nn_dist<-rand_1nn_sphere[['mean_4nn_dist']] %>% mean()


# ##plotting the nearest neighbour lines for M71 on a transparent png to overlay on the GC image.
nn<-read.csv("imaging_statistics/TBM_positions/M71 TOM GC1 Zstack_hqmacs_Position.csv", header=TRUE, skip=3, stringsAsFactors=F)
nn<-nn[,1:3]
nn$neighbour_X<-nn[nnwhich(nn, k=3), c("Position.X")]
nn$neighbour_Y<-nn[nnwhich(nn, k=3), c("Position.Y")]

p4= plot_ly()
line <- list(
    type = "line",
    line = list(color = "lightgrey", width=10),
    xref = "x",
    yref = "y"
)

lines <- list()
for (i in 1:nrow(nn)) {
    line[["x0"]] <- nn[i, "Position.X"]
    line[["x1"]] <-  nn[i, "neighbour_X"]
    line[["y0"]] <- nn[i, "Position.Y"]
    line[["y1"]] <-  nn[i, "neighbour_Y"]
    lines <- c(lines, list(line))
}
p4<- layout(p4, shapes = lines,
            xaxis = list(
                range=c(0,400), showgrid=F
            ),
            yaxis = list(
                range=c(0,400), showgrid=F,scaleanchor = "x",
                scaleratio = 1
            ),
            paper_bgcolor='transparent',
            plot_bgcolor  = 'transparent'
            )

p4

nn1_lines<-read.csv("imaging_statistics/TBM_positions/M71 TOM GC1 Zstack_hqmacs_Position.csv", header=TRUE, skip=3, stringsAsFactors=F)
nn1_lines<-nn1_lines[,1:3]
nn1_lines$neighbour_X<-nn1_lines[nnwhich(nn1_lines, k=1), c("Position.X")]
nn1_lines$neighbour_Y<-nn1_lines[nnwhich(nn1_lines, k=1), c("Position.Y")]

p1= plot_ly()
line<-NULL
line <- list(
    type = "line",
    line = list(color = "purple", width=10),
    xref = "x",
    yref = "y"
)

lines_1 <- list()
for (i in 1:nrow(nn1_lines)) {
    line[["x0"]] <- nn1_lines[i, "Position.X"]
    line[["x1"]] <-  nn1_lines[i, "neighbour_X"]
    line[["y0"]] <- nn1_lines[i, "Position.Y"]
    line[["y1"]] <-  nn1_lines[i, "neighbour_Y"]
    lines_1 <- c(lines_1, list(line))
}
p1<- layout(p1, shapes = lines_1,
            xaxis = list(
                range=c(0,400), showgrid=F
            ),
            yaxis = list(
                range=c(0,400), showgrid=F,scaleanchor = "x",
                scaleratio = 1
            ),
            paper_bgcolor='transparent',
            plot_bgcolor  = 'transparent'
)


simulated<-readRDS("../../simulatedNNdistPoints.Rds")
simulated$variable <- "simulation_points"
colnames(simulated) <- c("value", "variable")

sim_df<-rbind(plot_df, simulated)
sim_plot<-ggplot(data=sim_df, aes(y=value, x=variable, fill=variable)) +
    geom_violin( color="black", scale="width", size=1) + 
    geom_boxplot(width=0.1, fill="white", color="black") +
    scale_fill_manual(values=c( "#E41A1C","grey60", "darkgreen")) + ylim(c(0,80)) +
    ylab( bquote('Nearest Neighbour Distance '~(um)) ) + theme_minimal_big_font() 
#sim_plot

observed_mean_nn = sim_df %>% filter(variable == "observed_1nn") %>% pull(value) %>% mean()
simulated_mean_nn = sim_df %>% filter(variable == "simulation_points") %>% pull(value) %>% mean()
