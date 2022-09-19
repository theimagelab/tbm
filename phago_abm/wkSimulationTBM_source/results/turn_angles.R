library("dplyr")
angle_between_vectors <- function(a,b) {
    theta <- acos( sum(a*b) / ( sqrt(sum(a * a)) * sqrt(sum(b * b)) ) )
}

stats_dirs <- "../imaging_statistics/Stats/"
zstacks<-list.dirs(stats_dirs, recursive=FALSE)

agg_list <- list()
j=1
for (zstack in zstacks) {
    stats_files <- list.files(zstack, full.names=TRUE)
    zstack_name <- str_extract(zstack, "([^/]+$)")
    delta_file <- stats_files[grepl("Displacement_Delta.csv", stats_files)]
    deltas <- read.csv(delta_file, skip=3, header=TRUE)
    tracks <- deltas$TrackID %>% unique()
    
    timedata <- read.csv(stats_files[grepl("Time.csv", stats_files)],skip=3,header=TRUE)
    endtime<-timedata[nrow(timedata),c(1,4)]
    interval <- endtime[,1] / endtime[,2] # seconds per frame.
    
    for (track in tracks) {
        track_delta <- deltas %>% filter(TrackID == track)
        time_start <- min(track_delta$Time)
        time_end <- max(track_delta$Time)
        
        angles<-list()
        for (i in (time_start+1):(time_end-1)) {
            vec1<-track_delta %>%
                filter(Time == i) %>%
                dplyr::select(Displacement.Delta.X,
                       Displacement.Delta.Y,
                       Displacement.Delta.Z) %>% 
                as.numeric()
            
            vec2 <- track_delta %>%
                filter(Time == i+1) %>%
                dplyr::select(Displacement.Delta.X,
                       Displacement.Delta.Y,
                       Displacement.Delta.Z) %>%
                as.numeric()
            
            angle <- angle_between_vectors(vec1, vec2) #radians
            if (is.na(angle) == TRUE) {
                angle=0
            }
            if (angle > pi) {
                angle <- 2*pi-angle
            }
            turn_speed <- 60*angle / interval  # rads per minute
            
            row<-data.frame("TrackID" = track,
                   "Time" = i ,
                   "File" = zstack,
                   "Turn_Angle" = angle,
                   "Turn_Speed" = turn_speed)
            agg_list[[j]] <- row
            j=j+1
        }
        
    }
}

df_ts<-data.table::rbindlist(agg_list) %>% as.data.frame()
df_ts[is.na(df_ts)] <- 0
x<-df_ts %>% group_by(TrackID) %>% dplyr::summarize(TS_Mean = mean(Turn_Speed, na.rm=TRUE)) 


gts <- ggplot(df_ts, aes(x=1, y=Turn_Speed)) +
    geom_violin(scale="width", aes(fill=1)) +
    geom_boxplot(width=0.1, fill="white", color="black")+
    stat_summary(fun=mean, colour="darkred", geom="point", 
                 shape=18, size=3, show.legend=FALSE) +
    #geom_text(data = gs_means, aes(label = value, y = value + 2)) +
    theme(axis.text.x = element_text(angle=45)) + ylim(0,6.28) 
gts

# beta distribution fits turn angles well, when they are rescaled to (0,1)
library(fitdistrplus)
y=scales::rescale(df_ts$Turn_Speed, to=c(0.0001, 0.9999))
fit_beta <- fitdist(y, "beta")
par(mfrow = c(2, 2))
denscomp(fit_beta)
qqcomp(fit_beta)
cdfcomp(fit_beta)
ppcomp(fit_beta)

fit_beta

# Turn angle ~ Beta(2.02, 1.707)
# scale factor = max(df_ts$Turn_Speed)
