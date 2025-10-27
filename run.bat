@echo off
mkdir data
call groovy -cp sapjco3.jar readTableDsl.groovy
rscript AssembleOutput.R 
