awk -F',' '{split($2, array, "/"); print array[3]","array[4]"-"array[5]","$3","$4 }' results/labels_first.csv | sort -u > results/unique_by_tool_and_bug.csv
awk -F',' '{print $2","$3","$4}' results/unique_by_tool_and_bug.csv | sort -u > results/unique_by_bug.csv

python3 preprocess_bugs_only.py
awk -F',' '($3>0){print $2;}' results/preprocessed_all_by_bug.csv > results/bugs_with_a_correct_patch.csv
awk 'END{print FNR-1;}' results/bugs_with_a_correct_patch.csv 
