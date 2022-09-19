library("ggplot2")
library("plotly"); library("dplyr")
library("lhs"); library("stringr"); library("RColorBrewer")
rm(list=ls())
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

readData<-function(filename) {
    read.csv(filename, skip=3, header=TRUE) %>%
        mutate(File = substr(str_extract(filename,"[^/]+$"), start=1, stop=15)) # add filename as column
}


data_columns <- c("Population", "SpeedM", "SpeedSd", "TurnM", "TurnSd", "Run", "TrackMI", "TrackSpeed", "TrackDisplacement")
df_all <- data.frame(matrix(nrow=0,ncol=length(data_columns)))
colnames(df_all) <- data_columns
dir<-list.dirs(getwd(), recursive=FALSE)
files = list.files(dir, full.names=TRUE)
position_file <- files[grepl("_Position.csv", files)]

positions_all<-read.csv(position_file, skip=3)

# Get Displacements for each track
agg_list <- list()
i<-1
tracks <- unique(positions_all$Parent)
for (track in tracks) {
    pos <- positions_all %>% filter(Parent == track)
    
    celltype <- pos$ID %>% head(1)
    end_frame <- max(pos$Time) 
    start_frame <- min(pos$Time)
    end <- pos[pos$Time == end_frame,]
    start <- pos[pos$Time == start_frame,]
    
    meander_count <- end$meanderCount
    
    start_x <- start$Position.X
    start_y <- start$Position.Y
    start_z <- start$Position.Z
    
    end_x <- end$Position.X
    end_y <- end$Position.Y
    end_z <- end$Position.Z
    
    disp <- sqrt( (end_x-start_x)^2 + (end_y-start_y)^2 + (end_z-start_z)^2 )
    
    #Get track length...sum up all positions in each timestep.
    x_length <- diff(pos$Position.X)
    y_length <- diff(pos$Position.Y)
    z_length <- diff(pos$Position.Z)
    d<-data.frame(x_length, y_length,z_length)
    d$delta<- sqrt( d$x_length^2 + d$y_length^2 + d$z_length^2 )
    tracklength<-d$delta %>% sum()
    
    # x_length <- sum(abs(diff(pos$Position.X)))
    # y_length <- sum(abs(diff(pos$Position.Y)))
    # z_length <- sum(abs(diff(pos$Position.Z)))
    # 
    # tracklength <- sqrt( x_length^2 + y_length^2 + z_length^2 )
    # 
    duration <- (end_frame - start_frame)
    track_mi <- disp / tracklength
    track_speed <- tracklength / duration
    
    start_dist_from_center = sqrt(
        (start_x - 81)^2 +
            (start_y - 81)^2 +
            (start_z - 81)^2)
    end_dist_from_center = sqrt(
        (end_x - 81)^2 +
            (end_y - 81)^2 +
            (end_z - 81)^2)
    
    row <- data.frame(
        TrackID = track,
        TrackMI = track_mi,
        TrackSpeed = track_speed,
        TrackDisplacement = disp,
        TrackLength = tracklength,
        TrackDuration = duration,
        start_time  = start_frame,
        end_time = end_frame,
        startx= start_x,
        starty=start_y,
        startz= start_z,
        endx=end_x,
        endy=end_y,
        endz=end_z,
        start_dist_from_center = start_dist_from_center,
        end_dist_from_center = end_dist_from_center,
        celltype = celltype)
    
    
    agg_list[[i]] <- row
    i=i+1
}
    
df_all <- data.table::rbindlist(agg_list)
interval = 0.25 # 0.75 minutes per frame
df_all$TrackSpeed <- df_all$TrackSpeed / interval #convert um/frame to um/min
df_all <- df_all %>% filter(
        TrackDuration > 1 &
        start_dist_from_center < 81 &
        TrackLength > 0)# ignore cells that started outside the gc.
df_all <- df_all %>% filter(TrackMI <1 )

df <- df_all %>% dplyr::select(., TrackMI, TrackSpeed, TrackDisplacement) %>%
    tidyr::gather(key = "variable", value = "value")
head(df)

df_mi <- df %>% filter(variable == "TrackMI")
gm <- ggplot(df_mi, aes(x=variable, y=value)) +
    geom_violin(scale="width")+
    geom_boxplot(width=0.1, fill="white", color="black")+
   # geom_jitter() +
    theme(axis.text.x = element_text(angle=45)) 
gm



library(fitdistrplus)
fit_lnorm <- fitdist(speed_real$value, "lnorm")
par(mfrow = c(2, 2))
denscomp(fit_lnorm)
qqcomp(fit_lnorm)
cdfcomp(fit_lnorm)
ppcomp(fit_lnorm)

library(fitdistrplus)
y=MI_real$value
fit_mi <- fitdist(y, "lnorm")
par(mfrow = c(2, 2))
denscomp(fit_mi)
qqcomp(fit_mi)
cdfcomp(fit_mi)
ppcomp(fit_mi)

gm
ggsave(plot=gm, "meandering_index.svg", height=5, width=15)
