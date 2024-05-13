echo "Gathering data about existing labels from previous work.. and from manual and test execution analysis if files exits.."
python3 deduplicate.py 1 # add manual and drr labels

./label.sh $1

python3 samplebugs.py # sample data for bugs with at least one correct patch for further analysis
## do not sample
#cp results/preprocessed_all.csv results/preprocessed.csv
#cp results/preprocessed_all_by_bug.csv results/preprocessed_by_bug.csv

echo "Analysing data to calculate probability of finding a correct patch at random.."
python3 processdata.py 'Tool' 'Bug' 1 # data aggregated per tool per bug
python3 processdata.py 'Bug' 'Tool' 1 # data aggregated per bug per tool
python3 processbugs.py # data per bug
./format_bug_table.sh > results/result_by_Bug_only1.csv
