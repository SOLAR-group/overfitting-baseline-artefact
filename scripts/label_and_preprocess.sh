echo "Gathering data about existing labels from previous work.. "
python3 deduplicate.py 0 # ignore manual and drr labels
./label.sh $1 # label patches
