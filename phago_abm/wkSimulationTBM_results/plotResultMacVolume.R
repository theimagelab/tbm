library("spatstat")
library("ggplot2")
library("plotly")
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

diameter_dirs<-list.dirs(paste(getwd(), sep=""), recursive=FALSE)

data_columns <- c("Population", "MacSpd", "FragSpd", "AverageRate", "RatePct", "Run")
df_agg <- data.frame(matrix(nrow=0,ncol=length(data_columns)))
colnames(df_agg) <- data_columns
for (diameter_dir in diameter_dirs) {
    
    diameter <- str_extract(diameter_dir, "[^/]+$") %>% 
        str_extract(., "(.*?)d") %>%
        str_sub(., end=-2) %>% 
        as.numeric()
        
    mac_speeds_dirs <- list.dirs(diameter_dir, recursive=FALSE)
    
    for (mac_speed_dir in mac_speeds_dirs) {
        mac_speed <- str_extract(mac_speed_dir, "[^/]+$") %>% 
            str_extract(., "(.*?)u") %>%
            str_sub(., end=-2) %>%
            as.numeric()
            
        frag_spd_dirs <- list.dirs(mac_speed_dir, recursive=FALSE)
        
        for (frag_spd_dir in frag_spd_dirs) {
            frag_spd <- str_extract(frag_spd_dir, "[^/]+$") %>%
                str_extract(., "(.*?)u") %>%
                str_sub(., end=-2) %>%
                as.numeric()
            
            population_dirs <- list.dirs(frag_spd_dir, recursive=FALSE)
            for (population_dir in population_dirs) {
                population <- str_extract(population_dir, "[^/]+$") %>%
                    str_extract(., "(.*?)_") %>%
                    str_sub(., end=-2) %>%
                    as.numeric()
                
                run_dirs <- list.dirs(population_dir, recursive=FALSE)
                for (run in run_dirs) {
                    files = list.files(run, full.names=TRUE)
                    count_file <- files[grepl("removeCount.csv", files)]
                    run_number <- str_extract(run, "[^/]+$")
                    
                    df<-read.csv(count_file, skip=3)
                    df$Population <- as.numeric(population)
                    df$Run <- run_number
                    df$FragSpd <- frag_spd
                    df$MacSpd <- mac_speed
                    df$Diameter <- diameter
                    
                    
                    df <- df[df$CellType=="Fragment-LogNorm",]
                    df$AverageRate <- (df[nrow(df),"Count"]/df[nrow(df),"Time"]) # last row is the total removed count
                    df$RatePct <- df$AverageRate / df$Population
                    df<-df[nrow(df),c("Population", "Diameter", "MacSpd", "FragSpd", "AverageRate", "RatePct", "Run")]
                    df_agg <- rbind(df_agg, df)
                }
            }
        }
    }
}
df_agg$RatePct<-df_agg$RatePct*100 
df_agg$Volume <- (df_agg$Diameter/2)^3 * (4/3) * pi

#smoothed
# Plot clearance rate as a percentage of population. Much better.
df <- df_agg %>%
    dplyr::select(Run, RatePct, Volume, MacSpd) %>%
    tidyr::gather(key = "variable", value = "value", -Run, -MacSpd, -Volume)
head(df)

df<-df[df$value>0,]

p<-ggplot(NULL)
p<-ggplot(df, aes(y=value, x=Volume, color=factor(MacSpd), fill = factor(MacSpd))) +
    geom_point(size=0.5, alpha=0.8,aes(fill = factor(MacSpd), color=factor(MacSpd))) + 
    #stat_summary(fun=mean, aes(x=Population, y=value), geom="line", size=1.5) +
    #stat_summary(fun.data = mean_se, aes(x=Population, y=value), geom = "errorbar") +
    #stat_summary(geom='ribbon', 
    #             fun.data = mean_cl_normal, 
    #             fun.args=list(conf.int=0.95),
    #             alpha = 0.5, color="black") +  
    geom_smooth(color="black", size=0.5) +
    #xlim(c(7, 21)) + 
    ylab("Clearance Rate (%/min)") + 
    xlab(bquote('Simulated TBM Volume '~(um^3)) ) + #scale_y_continuous(trans = scales::log_trans())+
    #scale_x_continuous(trans = "log")+
    theme_minimal_big_font() + scale_fill_brewer(palette="Set1") + 
    scale_color_brewer(palette="Set1")
ggsave(plot=p, "mac_volume_simulations.svg", height=6, width=6)
#p
#ggplotly(p)

two_way_anova <- aov(value ~ MacSpd + Volume, data = df)
summary(two_way_anova)
sink("mac_volume_pvalues.txt")
print(summary(two_way_anova))
sink() 
