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

speedm_dirs<-list.dirs(paste(getwd(), sep=""), recursive=FALSE)

data_columns <- c("Population", "SpeedM", "SpeedSd", "TurnM", "TurnSd", "Run", "TrackMI", "TrackSpeed", "TrackDisplacement")
df_all <- data.frame(matrix(nrow=0,ncol=length(data_columns)))
colnames(df_all) <- data_columns

agg_list <- list()
i=1
for (speedm_dir in speedm_dirs) {
    speedm <- str_extract(speedm_dir, "[^/]+$") %>% 
        str_extract(., "(.*?)_") %>%
        str_sub(., end=-2) %>% 
        as.numeric()
    
    speedsd_dirs <-  list.dirs(speedm_dir, recursive=FALSE)
    for (speedsd_dir in speedsd_dirs) {
        
        speedsd <- str_extract(speedsd_dir, "[^/]+$") %>% 
            str_extract(., "(.*?)_") %>%
            str_sub(., end=-2) %>% 
            as.numeric()
            
        turn_m_dirs <- list.dirs(speedsd_dir, recursive=FALSE)
        
        for (turn_m_dir in turn_m_dirs) {
            turn_m <- str_extract(turn_m_dir, "[^/]+$") %>% 
                str_extract(., "(.*?)_") %>%
                str_sub(., end=-2) %>%
                as.numeric()
                
            turn_sd_dirs <- list.dirs(turn_m_dir, recursive=FALSE)
            
            for (turn_sd_dir in turn_sd_dirs) {
                turn_sd <- str_extract(turn_sd_dir, "[^/]+$") %>%
                    str_extract(., "(.*?)_") %>%
                    str_sub(., end=-2) %>%
                    as.numeric()
                
                population_dirs <- list.dirs(turn_sd_dir, recursive=FALSE)

                for (population_dir in population_dirs) {
                    population <- str_extract(population_dir, "[^/]+$") %>%
                        str_extract(., "(.*?)_") %>%
                        str_sub(., end=-2) %>%
                        as.numeric()
                    
                    run_dirs <-  list.dirs(population_dir, recursive=FALSE)
                        for (run_dir in run_dirs) {
                            files = list.files(run_dir, full.names=TRUE)
                            position_file <- files[grepl("_Position.csv", files)]
                            run_number <- str_extract(run_dir, "[^/]+$")
                            
                            positions_all<-read.csv(position_file, skip=3)
                            positions_all$UniqueTrackID <- paste(speedm, speedsd,turn_m,turn_sd,population,run_number, positions_all$ID, positions_all$Parent, sep="_")

                            # Get Displacements for each track
                            tracks <- unique(positions_all$UniqueTrackID)
                            for (track in tracks) {
                                pos <- positions_all %>% filter(UniqueTrackID == track)
                                
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
                                                  Run = run_number,
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
                                                  celltype = celltype,
                                                  meander_count = meander_count,
                                                  SpeedM = as.numeric(speedm),
                                                  SpeedSd = as.numeric(speedsd),
                                                  TurnM = as.numeric(turn_m),
                                                  TurnSd = as.numeric(turn_sd),
                                                  Population = population)
    
                                
                                agg_list[[i]] <- row
                                i=i+1
                            }
                        
                    }
                }
                
            }
        }
    }
}
df_all <- df_all %>% dplyr::filter(celltype != "Macrophage")
df_all <- data.table::rbindlist(agg_list)
interval = 0.25 # 0.75 minutes per frame
df_all$TrackSpeed <- df_all$TrackSpeed / interval #convert um/frame to um/min
df_all <- df_all %>% filter(
        TrackDuration > 1 &
        start_dist_from_center < 81 &
        TrackLength > 0)# ignore cells that started outside the gc.
df_all <- df_all %>% filter(TrackMI <1 )


df_all[is.na(df_all)] <- 0
df_all$MotilityPar <- paste(df_all$SpeedM, "_", df_all$SpeedSd, "_mm", df_all$TurnM, "_ms", df_all$TurnSd, sep="")

df <- df_all %>% dplyr::select(., MotilityPar, Run, TrackMI, TrackSpeed, TrackDisplacement) %>%
    tidyr::gather(key = "variable", value = "value", -MotilityPar, -Run)
head(df)

# experiment meandering index mean=0.323

MI_real<-NULL

MI_dir <- "../imaging_statistics/MI/"
movies<-list.dirs(path=MI_dir, recursive=FALSE)
for (movie in movies) {
    stats_files<-list.files(path=movie, full.names=TRUE)
    
    lapply(stats_files, readData) %>%
        purrr::reduce(left_join, by=c("ID", "File")) %>%
        dplyr::select(., "ID","File","Track.Displacement.Length","Track.Length") -> lengths_data_temp
    colnames(lengths_data_temp)<-c("ID", "Run","Track.Displacement.Length","Track.Length")
    MI_real <- rbind(MI_real, lengths_data_temp)
}
MI_real$TrackMI<-MI_real$Track.Displacement.Length/MI_real$Track.Length
colnames(MI_real) <- c("TrackID", "Run", "Track.Displacement.Length","Track.Length",  "value")
MI_real$MotilityPar <- "Observed"
MI_real$variable <- "TrackMI"

speeds_stats <- "../imaging_statistics/Speed/"
movies<-list.dirs(path=speeds_stats, recursive=FALSE)
speed_real<-NULL
for (movie in movies) {
    stats_files<-list.files(path=movie, full.names=TRUE)
    
    lapply(stats_files, readData) %>%
        purrr::reduce(left_join, by=c("ID", "File")) %>%
        dplyr::select(., ID,File,Track.Speed.Mean) -> tmp
    colnames(tmp)<-c("ID", "Run","TrackSpeed")
    speed_real <- rbind(speed_real, tmp)
}
speed_real$MotilityPar <- "Observed"
colnames(speed_real) <- c("ID", "Run", "value", "MotilityPar")
speed_real$variable <- "TrackSpeed"
speed_real$value <- speed_real$value * 60 

df_mi <- df[df$variable == "TrackMI",]
df_mi<-rbind(df_mi, MI_real %>% dplyr::select(-TrackID,-Track.Displacement.Length, -Track.Length))
df_spd <- df[df$variable == "TrackSpeed",]
df_spd<-rbind(df_spd, speed_real %>% dplyr::select(-ID))
df_disp <- df[df$variable == "TrackDisplacement",]

colourCount = length(unique(df_spd$MotilityPar))
getPalette = colorRampPalette(brewer.pal(9, "Spectral"))

mi_means <- aggregate(value ~  MotilityPar, df_mi, mean)
mi_means$value <- round(mi_means$value, 2)
gm <- ggplot(df_mi, aes(x=MotilityPar, y=value)) +
    geom_violin(scale="width", aes(fill=MotilityPar)) +
    geom_boxplot(width=0.1, fill="white", color="black")+
   # geom_jitter() +
    geom_text(data = mi_means, aes(label = value, y = value + 0.1)) +
    theme(axis.text.x = element_text(angle=45)) +
    scale_fill_manual(values=getPalette(colourCount))
gm

gs_means <- aggregate(value ~  MotilityPar, df_spd, mean)
gs_means$value <- round(gs_means$value, 2)
gs <- ggplot(df_spd, aes(x=MotilityPar, y=value)) +
    geom_violin(scale="width", aes(fill=MotilityPar)) +
    geom_boxplot(width=0.1, fill="white", color="black")+
    #geom_jitter() +
    stat_summary(fun=mean, colour="darkred", geom="point", 
                 shape=18, size=3, show.legend=FALSE) +
    #geom_text(data = gs_means, aes(label = value, y = value + 2)) +
    theme(axis.text.x = element_text(angle=45)) + ylim(0,10) +
 scale_fill_manual(values=getPalette(colourCount))
    gs
    
x<-df_spd %>% filter(MotilityPar == "Observed" | MotilityPar == "1.17_0.335_mm-3_ms1.5") 
speed_cdf<-ggplot(x, aes(x=value, color = MotilityPar)) +
        stat_ecdf(geom="step", size=3) + 
        ylab("CDF") + xlab("Speed") +
        theme_minimal_big_font() + scale_colour_brewer(palette="Set1")
speed_cdf
ggsave(plot=speed_cdf, "Speed_cdf_pval=0.805_1wayanova.svg", height=5, width=5)
a= x %>% filter(MotilityPar == "Observed") %>% pull(value)
b= x %>% filter(MotilityPar == "1.17_0.335_mm-3_ms1.5") %>% pull(value)
summary(aov(data=x, value ~ MotilityPar))



## Displacement not an accurate calibration metric 
# because imaris tracking is not perfect, the real data underestimates displacements.
gd <- ggplot(df_disp, aes(x=MotilityPar, y=value)) +
    geom_violin() +
    geom_boxplot(width=0.1, fill="white", color="black") +
    theme(axis.text.x = list(angle=45))

fit_lognorm<-MASS::fitdistr(speed_real$value,"log-normal")$estimate
fit_gaus<-MASS::fitdistr(speed_real$value,"normal")$estimate
#dat <- data.frame(x="lognorm_model", y=rlnorm(nrow(speed_real), meanlog = fit[[1]], fit[[2]] ))
dat<-data.frame(x="Fragments", y=speed_real$value)
lognorm_generated <- exp(rnorm(nrow(speed_real), mean = fit_lognorm[[1]], fit_lognorm[[2]] ))
lognorm_generated <- exp(rnorm(900)*fit_lognorm[[2]] + fit_lognorm[[1]])
gaus_generated <- rnorm(nrow(speed_real), mean = fit_gaus[[1]], sd = fit_gaus[[2]])
dat<-rbind(dat,
           data.frame(x="Lognorm fit", y=lognorm_generated),
           data.frame(x="Normal fit", y=gaus_generated)
)

library(fitdistrplus)
fit_lnorm <- fitdist(speed_real$value, "lnorm")
library(svglite)
svglite("speed_calibration_4panel.svg", width=8, height=6)
par(mfrow = c(2, 2))
denscomp(fit_lnorm)
qqcomp(fit_lnorm)
cdfcomp(fit_lnorm)
ppcomp(fit_lnorm)
dev.off()

library(fitdistrplus)
y=MI_real$value
fit_mi <- fitdist(y, "lnorm")
svglite("MI_calibration_4panel.svg", width=8, height=6)
par(mfrow = c(2, 2))
denscomp(fit_mi)
qqcomp(fit_mi)
cdfcomp(fit_mi)
ppcomp(fit_mi)
dev.off()

gs_means <- aggregate(y ~ x, dat, mean)
gs_means$y <- round(gs_means$y, 2)
gs_meds <- aggregate(y ~ x, dat, median)
gs_meds$y <- round(gs_meds$y, 2)
gsim<-ggplot(dat, aes(x=x, y=y, fill=x)) + geom_violin(scale="width") + geom_boxplot(fill="white", width=0.2)+
    geom_text(data = gs_means, aes(label = y, y = y + 2))  +
    #geom_text(data = gs_meds, aes(label = y, y = y -2)) +
    scale_fill_manual(values=c("Fragments" = "Purple",
                                "Lognorm fit" = brewer.pal(name="Set1",n=5)[1],
                                "Normal fit" = brewer.pal(name="Set1",n=5)[2])) +
    ylab("Mean Speed") +
    theme_minimal_big_font()
#gsim
pal=brewer.pal(6, "Set1")
x<-df_mi %>% filter(MotilityPar == "Observed" | MotilityPar == "1.17_0.335_mm-3_ms1.5") 
cdf<-ggplot(x, aes(x=value, color = MotilityPar)) +
    stat_ecdf(geom="step", size=3) + 
    ylab("CDF") + xlab("Meandering Index") +
    theme_minimal_big_font() + scale_colour_brewer(palette="Set1")
cdf
ggsave(plot=cdf, "MI_cdf_pval=0.561_1wayanova.svg", height=5, width=5)

a= x %>% filter(MotilityPar == "Observed") %>% pull(value)
b= x %>% filter(MotilityPar == "1.17_0.335_mm-3_ms1.5") %>% pull(value)
summary(aov(data=x, value ~ MotilityPar))

gm
ggsave(plot=gm, "meandering_index_calibrated.svg", height=5, width=15)
