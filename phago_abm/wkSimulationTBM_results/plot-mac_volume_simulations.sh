#!/bin/bash

cp plotResultMacVolume.R ./mac_volume_simulations/

cd ./mac_volume_simulations/
Rscript plotResultMacVolume.R
cp mac_volume_simulations.svg ../../
cd ../../