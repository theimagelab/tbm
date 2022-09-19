library("ggplot2")
library("plotly"); library("dplyr")
library("lhs"); library("stringr"); library("RColorBrewer");

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

mac_dia_dirs<-list.dirs(paste(getwd(), sep=""), recursive=FALSE)

data_columns <- c("Population", "Diameter", "MacSpd", "FragSpd", "AverageRate", "RatePct", "Run")
df_agg <- data.frame(matrix(nrow=0,ncol=length(data_columns)))
colnames(df_agg) <- data_columns
mac_dia_dirs <- mac_dia_dirs[1]
for (diameters_dirs in mac_dia_dirs) {
    diameter <- str_extract(diameters_dirs, "[^/]+$") %>% 
        str_extract(., "(.*?)d") %>%
        str_sub(., end=-2) %>% 
        as.numeric()
    
    mac_speeds_dirs <-  list.dirs(diameters_dirs, recursive=FALSE)
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
                    df$Diameter <- as.numeric(diameter)
                    
                    df$Run <- run_number
                    df$FragSpd <- frag_speed
                    df$MacSpd <- mac_speed
                    
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

#smoothed

library(mgcv)
df_mean<-df_agg %>%
    group_by(Diameter, MacSpd, FragSpd, Population) %>% 
    dplyr::summarise(meanRate =  mean(RatePct), nrun=n())

#https://stackoverflow.com/questions/65918325/how-to-plot-surface-fit-through-3d-data-in-r

predfun <- function(x,y){
    newdat <- data.frame(Population = x, FragSpd=y)
    predict(mod, newdata=newdat)
}

fig <- plot_ly()

df_mean$FragSpd = exp(df_mean$FragSpd)
Population.seq <- seq(min(df_mean$Population, na.rm=TRUE), max(df_mean$Population, na.rm=TRUE), length=25)
FragSpd.seq <- seq(min(df_mean$FragSpd, na.rm=TRUE), max(df_mean$FragSpd, na.rm=TRUE), length=25)

surf_dia <- list()
for (diameter in unique(df_agg$Diameter)) {
    surf0_df <- df_mean %>% filter(Diameter == diameter, MacSpd == 0)
    mod <- gam(meanRate ~ te(Population) + te(FragSpd) + ti(Population, FragSpd), data=surf0_df)
    surf0_matrix <- outer(Population.seq, FragSpd.seq, Vectorize(predfun)) %>% t()
    
    surf5_df <- df_mean %>% filter(MacSpd == 5)
    mod <- gam(meanRate ~ te(Population) + te(FragSpd) + ti(Population, FragSpd), data=surf5_df)
    surf5_matrix <- outer(Population.seq, FragSpd.seq, Vectorize(predfun)) %>% t()
    
    surf_diff <- surf5_matrix - surf0_matrix
    surf_dia <- list(surf_dia, surf_diff)
}

# Asymmetric diverging color scale represents the data better
## Make vector of colors for values smaller than 0 (20 colors)
rc1 <- colorRampPalette(c(brewer.pal(10, "RdBu")[1:5],"#FFFFFF"))(abs(min(surf_diff))*100)

## Make vector of colors for values larger than 0 (180 colors)
rc2 <- colorRampPalette(c("#FFFFFF",brewer.pal(10, "RdBu")[7:10]))(abs(max(surf_diff))*100)

## Combine the two color palettes
rampcols_asymm <- c(rc1, rc2)


fig_diff <- plot_ly()
fig_diff <- fig_diff %>% add_trace(x = ~Population.seq,
                           y = ~FragSpd.seq,
                           z = surf_diff, type = "contour", zmid=0,  zmax=0.06, zmin=-0.29,
                           colors=rampcols_asymm,
                           contours = list(
                               coloring = 'heatmap',
                               showlabels=TRUE,
                               labelfont=list(size=20),
                               start = 0,
                               end = 0,
                               size = 4
                               ),
                           line = list(width = 3, color = "black")
                           )

n_sims <- df_mean %>% filter(MacSpd == 0 | MacSpd == 5) %>% select(nrun) %>% pull(nrun) %>% sum()

hline <- function(y = 0, color = "black") {
    list(
        type = "line",
        x0 = 0,
        x1 = 1,
        xref = "paper",
        y0 = y,
        y1 = y,
        line = list(color = color, dash="dot")
    )
}

fig_diff<- fig_diff %>%
    layout(
         
            xaxis = list(
                title = "Fragment Count",
                type = "linear",
                tickfont = list(size = 20)
            ),
            yaxis = list(
                title = "Fragment Speed (um/min)",
                tickfont = list(size = 20)
            ),
            shapes = list(
                list(
                    type = "line",
                    x0 = 0,
                    x1 = 1,
                    xref = "paper",
                    y0 = 3.4,
                    y1 = 3.4,
                    line = list(color = "black", dash="dot")
                    )
                ,
                list(
                    type = "line",
                    x0 = 2665,
                    x1 = 2665,
                    y0 = 0,
                    y1 = 1,
                    yref= "paper",
                    line = list(color = "black", dash="dot")
                )
                # , list(type = "rect",
                #      fillcolor = "grey", line = list(color = "grey"), opacity = 0.4,
                #      y0 = 1, y1 = 7.389, x0 = 1105, x1 = 4000)
                    
                )
            )
                
fig_diff
message(paste(n_sims, "simulations plotted"))

fig_diff_surf <- plot_ly() %>% add_surface(x = ~Population.seq,
                           y = ~FragSpd.seq,
                           z = surf_diff)
#fig_diff_surf

surf0_df <- df_agg %>% filter(Diameter == diameter, MacSpd == 0)
mod <- gam(AverageRate ~ te(Population) + te(FragSpd) + ti(Population, FragSpd), data=surf0_df)
surf0_matrix <- outer(Population.seq, FragSpd.seq, Vectorize(predfun)) %>% t()

three_way_anova <- aov(RatePct ~ Population + MacSpd + FragSpd, data = df_agg)
summary(three_way_anova)


###### # Plot clearance rate at fragspd = 3.4 um.

# vol_gc = 2226095
# 
# df_agg <- df_agg %>% filter(FragSpd == 1.17) %>%
#     filter(MacSpd == 0 | MacSpd == 5)
# 
# df_agg$Density_per_um3 <- df_agg$Population / vol_gc
# df_agg$Density_per_mm3 <- df_agg$Population*1e9 / vol_gc

# df <- df_agg %>%
#     dplyr::select(Run, Population, RatePct, MacSpd) %>%
#     tidyr::gather(key = "variable", value = "value", -Population, -Run, -MacSpd)
# head(df)
# 
# df<-df[df$value>0,]
# 
# p<-ggplot(NULL)
# p<-ggplot(df, aes(y=value, x=Population, color=factor(MacSpd), fill = factor(MacSpd))) +
#     geom_point(size=0.5, alpha=0.8,aes(fill = factor(MacSpd), color=factor(MacSpd))) + 
#     #stat_summary(fun=mean, aes(x=Population, y=value), geom="line", size=1.5) +
#     #stat_summary(fun.data = mean_se, aes(x=Population, y=value), geom = "errorbar") +
#     #stat_summary(geom='ribbon', 
#     #             fun.data = mean_cl_normal, 
#     #             fun.args=list(conf.int=0.95),
#     #             alpha = 0.5, color="black") +  
#     geom_smooth(color="black", size=0.5) +
#     ylab("Clearance Rate (%/min)") + 
#     xlab("Simulated Fragments per GC") + 
#     #scale_y_continuous(trans = scales::log_trans())+
#     #scale_x_continuous(trans = "log")+
#     theme_minimal_big_font() + scale_fill_brewer(palette="Set1") + 
#     scale_color_brewer(palette="Set1")
# #ggsave(plot=p, "crossover.svg", height=6, width=6)
# #ggplotly(p)


