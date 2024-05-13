rm firstpatches/solved*
echo "takes a minute to run; results saved in results/resultsFirstPatches.csv"
./process_first_patches.sh > results/resultsFirstPatches.csv 
echo "bugs solved in the first n runs are saved in firstpatches/solved<n>"
