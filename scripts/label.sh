echo "Labelling patches from the results/patches_found folder.."
## parameters: tools_excluded time_limit_in_hours
python3 label.py Arja,Cardumen,RSRepair $1 > results/labels.csv 
#python3 label.py 'None' 8 > results/labels.csv

echo "Extracting unique patches.."
awk -F',' '{split($2, array, "/"); print array[3]","array[4]"-"array[5]","$3","$4 }' results/labels.csv | sort -u > results/unique_by_tool_and_bug.csv
echo "Unique patches by tool and bug:"
wc -l  results/unique_by_tool_and_bug.csv
awk -F',' '{print $2","$3","$4}' results/unique_by_tool_and_bug.csv | sort -u > results/unique_by_bug.csv
echo "Unique patches by bug only:"
wc -l  results/unique_by_bug.csv
echo "Unique unlabelled patches:"
awk -F',' '($3=="unknown"){split($2, array, "/"); print $2" "array[4]"-"array[5]" "$4;}' results/labels.csv | sort -k2 -k3 -u  > out
awk '{print $1};' out > results/fordrr.csv
wc -l  results/fordrr.csv
awk -F',' '{split($2, array, "/"); print $2" "array[4]"-"array[5]" "$4;}' results/labels.csv | sort -k2 -k3 > out
awk '{print $1};' out > results/all_labels.csv
rm out

# returns all correct labels
awk -F',' '($3=="correct"){split($2, array, "/"); print $2" "array[4]"-"array[5]" "$4;}' results/labels.csv | sort -k2 -k3 -u  > out
awk '{print $1};' out > results/correct_labels_from_previous_work.csv
rm out

echo "Preprocessing data.."
python3 preprocess.py
python3 preprocess_bugs_only.py
awk 'END{print "bugs for which a patch was generated: "FNR-1;}' results/preprocessed_all_by_bug.csv 
awk -F',' '($3>0){print $2;}' results/preprocessed_all_by_bug.csv > results/bugs_with_a_correct_patch.csv
awk 'END{print "bugs with a correct patch: "FNR-1;}' results/bugs_with_a_correct_patch.csv 
python3 fordrr.py 'fordrr.csv' > results/drr_input/fordrr_unknown_labells_for_bugs_with_a_correct_patch.csv
python3 fordrr.py 'all_labels.csv' > results/labels_for_bugs_with_a_correct_patch.csv
awk 'END{print "patches for bugs with a correct patch: "FNR;}' results/labels_for_bugs_with_a_correct_patch.csv
awk -F',' 'BEGIN{s=0;}($3>0){s=s+$3+$4;}END{print "unique labelled for bugs with a correct patch: " s;}' results/preprocessed_all_by_bug.csv
awk 'END{print "unique unlabelled patches: "FNR;}' results/drr_input/fordrr_unknown_labells_for_bugs_with_a_correct_patch.csv
