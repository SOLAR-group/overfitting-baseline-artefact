echo $1
./firstpatches/label_first_patches.sh $1 TBar
tail -n +2 results/bugs_with_a_correct_patch.csv >> solved$1
./firstpatches/label_first_patches.sh $1 SimFix
tail -n +2 results/bugs_with_a_correct_patch.csv >> solved$1
./firstpatches/label_first_patches.sh $1 jMutRepair
tail -n +2 results/bugs_with_a_correct_patch.csv >> solved$1
./firstpatches/label_first_patches.sh $1 Avatar
tail -n +2 results/bugs_with_a_correct_patch.csv >> solved$1
./firstpatches/label_first_patches.sh $1 kPAR
tail -n +2 results/bugs_with_a_correct_patch.csv >> solved$1
./firstpatches/label_first_patches.sh $1 FixMiner
tail -n +2 results/bugs_with_a_correct_patch.csv >> solved$1
./firstpatches/label_first_patches.sh $1 jGenProg
tail -n +2 results/bugs_with_a_correct_patch.csv >> solved$1
./firstpatches/label_first_patches.sh $1 dynamoth
tail -n +2 results/bugs_with_a_correct_patch.csv >> solved$1
./firstpatches/label_first_patches.sh $1 jKali
tail -n +2 results/bugs_with_a_correct_patch.csv >> solved$1
sort -u solved$1 > tmpout
mv tmpout solved$1
mv solved$1 firstpatches/
