cd scripts 

./label_and_preprocess.sh $1 # analyse for a given time period (in hours)

# uncomnent to perform extra test case execution to find overfitted patches
# ./drr.sh 

./label_after_drr.sh 

# perform manual analysis

./label_and_analyse_after_manual.sh $1

cd -
