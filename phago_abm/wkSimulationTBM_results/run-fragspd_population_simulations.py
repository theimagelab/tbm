# # -*- coding: utf-8 -*-
# """
# Wunna Kyaw 19/Dec/22
# Pyhton script to run many agent-based simulations at once, while varying parameters of the ABM in a ./xml file.
# Running an extra few simulations at >3000 fragment count to make the vertical line more obvious on the plot.
# """

import xml.etree.ElementTree as ET
import subprocess

jar_name = "wkSimulationTBM.jar"
result_dir_name = "fragspd_population_simulations"

diameter_values = [27] 
mac_motility_values = [0,5]
frag_motility_values =  [-2,-1,0.00, 0.50, 0.90, 1.17, 1.50]
meander_chance_mean = -3 
meander_chance_std = 1.5
n_frag_values = [5, 10,20, 30,40, 60, 80, 100, 160, 200, 320, 640, 1000, 2500, 3000, 5000,7000]
simulation_runs = ["run1" "run2", "run3", "run4", "run5"]
sim_count = 0
simulation_end_time_minutes = 75

total_sims = len(simulation_runs) * len(n_frag_values) * len(frag_motility_values) *len(mac_motility_values) * len(diameter_values)

for diameter in diameter_values:
    for mac_mot in mac_motility_values:
        for frag_mot in frag_motility_values:
            for num_frag in n_frag_values:
                for run in simulation_runs:
                    mot_dir = str(frag_mot) + 'ummin-1frags'
                    macmot_dir = str(mac_mot) + 'ummin-1macs'
                    diameter_dir = str(diameter) + 'diam'

                    numfrags_dir = str(num_frag) + "_frags"
                    output_dir = "./" + result_dir_name + "/"+diameter_dir+"/"+macmot_dir + "/" + mot_dir + "/" + numfrags_dir + "/" + run
                
                    parameters_template_file = "./parameters/calibration-parameters.xml"
    
                    pars = ET.parse(parameters_template_file)
                
                    end_time = pars.find("./Simulation/endTime")
                    end_time.text = str(simulation_end_time_minutes)
        
                    # edit xml macrophage speedMMean to equal mot
                    mac_speed = pars.find("./Simulation/Macrophage/speedM_Mean")
                    mac_speed.text = str(mac_mot)

                    meander_chance = pars.find("./Simulation/Fragment/meanderMean")
                    meander_chance.text = str(meander_chance_mean)
                    meander_std = pars.find("./Simulation/Fragment/meanderStD")
                    meander_std.text = str(meander_chance_std)


                    # edit xml numfrags to equal num_frag
                    num_frags = pars.find("./Simulation/Fragment/numFrags")
                    num_frags.text = str(num_frag)
                
                    # edit xml numfrags to equal num_frag
                    frag_spd = pars.find("./Simulation/Fragment/speedM_Mean")
                    frag_spd.text = str(frag_mot)
                
            
                # edit xml diameter to equal diameter_values
                    mac_dia = pars.find("./Simulation/Macrophage/diameter")
                    mac_dia.text = str(diameter)

                    #save edited parameters
                    parameters_file = "./parameters/abm_dia-" + str(diameter) + "-macmot-" + str(mac_mot) + "-fragmot-" + str(frag_mot) + "-nfrag-" + str(num_frag) + ".xml"
                    pars.write(parameters_file)

                    subprocess.call(['java', '-jar', jar_name, '-p', parameters_file, "-o", output_dir])
                    sim_count = sim_count + 1
                    print(str(sim_count) + "out of " + str(total_sims))



