cd scripts 

./label_and_preprocess.sh $1 # analyse for a given time period (in hours)

# add command to run DRR scripts to produce an output file in scripts/results/drr_output
./drr.sh

./label_after_drr.sh 

# perform manual analysis

./label_and_analyse_after_manual.sh $1

cd -
