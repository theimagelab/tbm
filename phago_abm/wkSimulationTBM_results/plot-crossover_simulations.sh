#!/bin/bash

cp plotResultCrossover.R ./crossover_results/14diam/

cd ./crossover_results/14diam/
Rscript plotResultCrossover.R
cp crossover.svg ../../
cd ../../