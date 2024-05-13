import csv, sys
from globals import output_dir

root = output_dir

fordrr = sys.argv[1]
#fordrr = 'fordrr.csv'
#alllabels = 'all_labels.csv'

bugs = []
with open(root+'bugs_with_a_correct_patch.csv', newline='') as csvfile:
  reader = csv.reader(csvfile, delimiter=',')
  row = next(reader) # remove header
  for row in reader:
    bug = (row[0].replace('-','/')).strip(' \n')+'/'
    bugs.append(bug)

with open(root+fordrr, newline='') as csvfile:
  reader = csv.reader(csvfile, delimiter=',')
  for row in reader:
    found = False
    patch = row[0]
    for bug in bugs:
      if bug in patch:
        print(patch)
        found = True
        break
