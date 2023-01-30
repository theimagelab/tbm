# -*- coding: utf-8 -*-
"""
Wunna Kyaw 05/Aug/22
This script runs Phago_ABM_BrownianMeander.jar, which outputs position data for each agent.
The goal is to find simulated motility parameters for fragments such that the behaviour
of fragments matches what we see in vivo. 
Particularly in terms of speed, and meandering index (confinement)
"""

import xml.etree.ElementTree as ET
import subprocess

jar_name = "Phago_ABM.jar"
result_dir_name = "calibration_MI_statmacs"

frag_speed_means = [1.17]
frag_speed_stds = [0.335]
meander_means = [-1,-2, -3, -4]
meander_stds = [1.5]
n_frag_values = [500]
simulation_runs = ["run1", "run2"]
sim_count = 0

for frag_mot in frag_speed_means:
    for frag_speed_std in frag_speed_stds:
        for meander_mean in meander_means:
            for meander_std in meander_stds:
                for num_frag in n_frag_values:
                    for run in simulation_runs:
                        mot_dir = str(frag_mot) + '_ummin-1frags'
                        mot_std_dir = str(frag_speed_std) + "_spdstd"
                        turn_dir = str(meander_mean) + "_meander"
                        turn_std_dir = str(meander_std) + "_meanderstd"
                        numfrags_dir = str(num_frag) + "_frags"
                        output_dir = f"./{result_dir_name}/{mot_dir}/{mot_std_dir}/{turn_dir}/{turn_std_dir}/{numfrags_dir}/{run}"
                        
                        parameters_template_file = "./parameters/calibration-parameters.xml"

                        pars = ET.parse(parameters_template_file)
                        output_tracks = pars.find("./Simulation/trackCells")


                        # edit xml numfrags to equal num_frag
                        num = pars.find("./Simulation/Fragment/numFrags")
                        num.text = str(num_frag)
                        
                        # edit xml numfrags to equal num_frag
                        frag_spd = pars.find("./Simulation/Fragment/speedM_Mean")
                        frag_spd.text = str(frag_mot)
                        frag_spd_std_par = pars.find("./Simulation/Fragment/speedS_Mean")
                        frag_spd_std_par.text = str(frag_speed_std)

                        #frag_pitch_mean_par = pars.find("./Simulation/Fragment/pitchM_Mean")
                        #frag_pitch_mean_par.text = str(frag_roll_mean)
                        meander_mean_par = pars.find("./Simulation/Fragment/meanderMean")
                        meander_mean_par.text = str(meander_mean)

                        meander_std_par = pars.find("./Simulation/Fragment/meanderStD")
                        meander_std_par.text = str(meander_std)
                        #frag_pitch_std_par = pars.find("./Simulation/Fragment/pitchS_Mean")
                        #frag_pitch_std_par.text = str(frag_roll_mean)
                        #save edited parameters
                        parameters_file = "./parameters/cal_" + str(frag_mot) + "std" + str(frag_speed_std) +"turn_" + str(turn_dir) + "std_" +str(turn_std_dir) +"-b-" + str(num_frag) + ".xml"
                        pars.write(parameters_file)

                        subprocess.call(['java', '-jar', jar_name, '-p', parameters_file, "-o", output_dir])
                        sim_count = sim_count + 1

print(str(sim_count))


