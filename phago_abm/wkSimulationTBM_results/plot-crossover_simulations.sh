#!/bin/bash

cp plotResultCrossover.R ./crossover_results/27diam/

cd ./crossover_results/27diam/
Rscript plotResultCrossover.R
cp crossover.svg ../../
cd ../../