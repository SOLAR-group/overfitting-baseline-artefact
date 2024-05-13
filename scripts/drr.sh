
## generate scripts to run drr
python generateDRRscripts.py results/drr_input/fordrr_unknown_labells_for_bugs_with_a_correct_patch.csv > ./drr/runDrrForUn.sh
chmod +x ./drr/runDrrForUn.sh

# Run drr for Unknowns
./drr/runDrrForUn.sh