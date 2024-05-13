# Artefact for ''The Patch Overfitting Problem in Automated Program Repair: Practical Magnitude and a Baseline for Realistic Benchmarking'' (accepted to [FSE-IVR 2024](https://2024.esec-fse.org/track/fse-2024-ideas--visions-and-reflections))

## Directory structure

The [reproduction.sh](reproduction.sh) file contains commands for analysis of all the patches in the [patches\_found](scripts/results/patches_found) folder. It takes one input - the number of hours over which to do the analysis for.
Note that currently the ./drr.sh script is commented out. 
This should only be used if one wants to re-run automated test check for generated patches. 
Our outputs our currently saved in [drr\_output](scripts/results/drr_output) folder.

The [ground\_truth\_patches](ground_truth_patches) folder contains patches with known labels from previous work:

- Bo Lin, Shangwen Wang, Ming Wen, and Xiaoguang Mao. 2022. Context-Aware Code Change Embedding for Better Patch Correctness Assessment. ACM Trans. Softw. Eng. Methodol. 31, 3 (2022), 51:1–51:29. https://doi.org/10.1145/3505247 
- Shangwen Wang, Ming Wen, Bo Lin, Hongjun Wu, Yihao Qin, Deqing Zou, 1250 Xiaoguang Mao, and Hai Jin. 2020. Automated Patch Correctness Assessment: 1251 How Far Are We?. In Proceedings of the 35th IEEE/ACM International Conference on 1252 Automated Software Engineering (Virtual Event, Australia) (ASE ’20). Association
for Computing Machinery, New York, NY, USA, 968—-980. https://doi.org/10.1 1253 145/3324884.3416590
- He Ye, Matias Martinez, and Martin Monperrus. 2021. Automated patch assessment for program repair at scale. Empir. Softw. Eng. 26, 2 (2021), 20. https://doi.org/10.1007/s10664-020-09920-w
- Maria Kechagia, Xavier Devroey, Annibale Panichella, Georgios Gousios, and Arie van Deursen. 2019. Effective and efficient API misuse detection via exception propagation and search-based testing. In Proceedings of the 28th ACM SIGSOFT International Symposium on Software Testing and Analysis, ISSTA 2019, Beijing, China, July 15-19, 2019, Dongmei Zhang and Anders Møller (Eds.). ACM, 192–203. https://doi.org/10.1145/3293882.3330552

The [scripts](scripts) folder contains scripts we used for patch analysis, e.g., matching patches with known labels, calculating random baselines.


The [saved\_results](saved_results) directory contains various results we saved, which we use in the paper, including results of our automated syntactic ([labels.csv](saved_results/results_8h/labels.csv)), automated test execution ([drr_output](scripts/results/drr_output)), and manual patch assessments ([manual_output](scripts/results/manual_output)).
If reproducing our results, you can check the outputs against results saved in that folder.

## Experimental environment

Dependencies:

For scripts in this repository:

- Unix-based system
- zsh shell
- awk: version 20200816
- Python: version 3.10.9
- Python modules: pandas (1.4.3), scipy (1.9.0), statistics, csv, sys, os, pickle 

Hardware:

- Patch generation was performed on: Dell PowerEdge R630 machine, with Intel Xeon E5-2630 v3 (Haswell, 2.40GHz, 2 CPUs, 8 cores/CPU), and 128 GiB of RAM memory
- Test case generation was performed on: Dell PowerEdge R630 machine, with Intel Xeon E5-2630 v3 (Haswell, 2.40GHz, 2 CPUs, 8 cores/CPU), and 128 GiB of RAM memory
- Data analysis was performed on: MacMini 3.2 GHz 6-Core Intel Core i7 16 GB RAM

## Replication/Reproduction

### Patch Generation

The [tools](tools) folder contains the modified source code of the program repair tools used in this experiment.
We recall that, in this experiment, we have modified those versions in order to register the time each patch is generated.
The file [Readme](tools/README.md) includes the version of each of those tools (commit id from the corresponding GitHub repositories).



### Patch Assessment and Analysis

Once the patches are generated, please save them in the [patches\_found](scripts/results/patches_found) folder.

To reproduce our analysis for a given time limit, simply run ./reproduction.sh with the number of hours. For instance:

```
./reproduction.sh 3
```

For running that script, it's necessary to first download the patch assessment tool [DRR](https://github.com/KTH/drr) and put it inside the folder  [scripts/drr](scripts/drr).
For example,
```
cd ./scripts/drr/
git clone https://github.com/KTH/drr
```



This script will perform three commands:

for Justyna: UPDATE drr_input does not exist

- match labels in [patches\_found](scripts/results/patches_found) folder with ground truth patches; those that could not be matched will be saved in the [drr\_input](scripts/results/drr_input) folder - at this point test-based overfitting assesssment takes place, described in the [drr](scripts/drr) folder

- execute the DRR approach for assessing the patches from [drr\_input](scripts/results/drr_input) folder.
MATIAS: do we want to provide drr scripts? I created a README in scripts/drr for now.

- take output of test case analysis, found in [drr\_output](scripts/results/drr_output), label test failing patches as overfitting, and save the others in the [manual_input](scripts/results/manual_input) folder
- use ground truth patches, output of test case analysis and output of manual analysis (saved in [manual\_output](scripts/results/manual_output)), and label patches up to a given time limit and perform all patch analysis, including calculations of the random baseline

The scripts produce a lot of results, we present a selection in the [paper\_results](saved_results/paper_results) folder.

## Replication with Different Datasets

To replicate our experiment with a different set of patches, please modify your tool so it marks the time at which a patch was generated (see [README](scripts/results/patches_found/README.md)).

Once your patches are generated, please copy them into and follow the directory structure in the [patches\_found](scripts/results/patches_found) folder. 
Double-check if [label.py](scripts/label.py) needs an update (in case of some minor naming discrepancies), and the required dependencies are installed on a Unix-based system.

The [./reproduction.sh](reproduction.sh) shows the 3 required commands.
After running the first command you will get a list of patches which could not be syntactically matched in the [drr\_input](scripts/results/drr_input) folder. 
After running extra test analysis using [drr scripts](scripts/drr) you can save output in the [drr\_output](scripts/results/drr_output) folder.
Next, you can run the second command to receive a set of patches that passed the test suites, thus requiring manual analysis in [manual_input](scripts/results/manual_input) folder.
After manual analysis, save your results in the [manual_outpur](scripts/results/manual_output) folder. 
Please make sure to preserve the output format.
Now you can run the final command.

To generate data and/or point to data which we present in our paper go to scripts/ and run rq1.py, rq2.py, rq3.py. 

Please feel free to contact the authors if you encounter any problems.

