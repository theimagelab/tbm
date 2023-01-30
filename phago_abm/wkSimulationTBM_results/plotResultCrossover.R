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
vol_gc = 2226095

mac_speeds_dirs<-list.dirs(paste(getwd(), sep=""), recursive=FALSE)

data_columns <- c("Population", "MacSpd", "FragSpd", "AverageRate", "RatePct", "Run")
df_agg <- data.frame(matrix(nrow=0,ncol=length(data_columns)))
colnames(df_agg) <- data_columns
for (mac_speed_dir in mac_speeds_dirs) {
    
    mac_speed <- str_extract(mac_speed_dir, "[^/]+$") %>% 
        str_extract(., "(.*?)u") %>%
        str_sub(., end=-2) %>% 
        as.numeric()
        
    frag_speeds_dirs <- list.dirs(mac_speed_dir, recursive=FALSE)
    
    for (frag_speed_dir in frag_speeds_dirs) {
        frag_speed <- str_extract(frag_speed_dir, "[^/]+$") %>% 
            str_extract(., "(.*?)u") %>%
            str_sub(., end=-2) %>%
            as.numeric()
            
        population_dirs <- list.dirs(frag_speed_dir, recursive=FALSE)
        
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
                df$FragSpd <- frag_speed
                df$MacSpd <- mac_speed
                
                df <- df[df$CellType=="Fragment-LogNorm",]
                df$AverageRate <- (df[nrow(df),"Count"]/df[nrow(df),"Time"]) # last row is the total removed count
                df$RatePct <- df$AverageRate / df$Population
                df<-df[nrow(df),c("Population", "MacSpd", "FragSpd", "AverageRate", "RatePct", "Run")]
                df_agg <- rbind(df_agg, df)
            }
        }
    }
}
df_agg$RatePct<-df_agg$RatePct*100 

df_agg$Density_per_um3 <- df_agg$Population / vol_gc
df_agg$Density_per_mm3 <- df_agg$Population*1e9 / vol_gc

#smoothed
# Plot clearance rate as a percentage of population. Much better.
df <- df_agg %>%
    dplyr::select(Run, Population, RatePct, MacSpd) %>%
    tidyr::gather(key = "variable", value = "value", -Population, -Run, -MacSpd)
head(df)

df<-df[df$value>0,]

p<-ggplot(NULL)
p<-ggplot(df, aes(y=value, x=Population, color=factor(MacSpd), fill = factor(MacSpd))) +
    geom_point(size=0.5, alpha=0.8,aes(fill = factor(MacSpd), color=factor(MacSpd))) + 
    #stat_summary(fun=mean, aes(x=Population, y=value), geom="line", size=1.5) +
    #stat_summary(fun.data = mean_se, aes(x=Population, y=value), geom = "errorbar") +
    #stat_summary(geom='ribbon', 
    #             fun.data = mean_cl_normal, 
    #             fun.args=list(conf.int=0.95),
    #             alpha = 0.5, color="black") +  
    geom_smooth(color="black", size=0.5) +
    ylab("Clearance Rate (%/min)") + ylim(c(0,0.4))+
    xlab("Simulated Fragments per GC") + 
    #scale_y_continuous(trans = scales::log_trans())+
    #scale_x_continuous(trans = "log")+
    theme_minimal_big_font() + scale_fill_brewer(palette="Set1") + 
    scale_color_brewer(palette="Set1")
ggsave(plot=p, "crossover.svg", height=6, width=6)
#ggplotly(p)

# two way anova for time-series p value
df
two_way_anova <- aov(value ~ MacSpd + Population, data = df)
summary(two_way_anova)


# pvals at every time point
# pvals <- data.frame()
# for (pop in unique(df$Population) %>% sort()) {
#     stationary <- df %>% filter(Population == pop, MacSpd == 0) %>% pull(value)
#     motile <- df %>% filter(Population == pop, MacSpd == 5) %>% pull(value)
#     pval = wilcox.test(stationary, motile)$p.value
#     pvals <- rbind(pvals, c(pop, pval))
# }
# colnames(pvals) <- c("Population", "pvalue")
# write.csv(file="pvalues.csv", pvals)

