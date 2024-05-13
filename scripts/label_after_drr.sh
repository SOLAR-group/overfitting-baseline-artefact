rm results/Autotest_check*
cat results/drr_output/*csv > results/Autotest_check.csv
sort -k1 -k2 -k3 results/Autotest_check.csv > results/Autotest_check_sorted.csv
python3 getpatches.py results/Autotest_check_sorted.csv
#python3 matchpatches.py results/drr_input/fordrr_unknown_labells_for_bugs_with_a_correct_patch.csv results/patches_passed.csv > results/test_passing_patches.csv
python3 matchpatches.py results/drr_input/fordrr_unknown_labells_for_bugs_with_a_correct_patch.csv results/patches_failed.csv > results/test_failing_patches.csv 
awk 'NR==FNR{a[$0];next} !($0 in a)' results/test_failing_patches.csv results/drr_input/fordrr_unknown_labells_for_bugs_with_a_correct_patch.csv > results/manual_input/manual.csv
