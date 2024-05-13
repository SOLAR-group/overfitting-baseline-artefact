mkdir $1
mkdir $1/drr_input
mkdir $1/manual_input
mkdir $1/firstpatches
cp results/*csv $1/
cp results/drr_input/*csv $1/drr_input/
cp results/manual_input/*csv $1/manual_input/
echo "results copied to "$1
