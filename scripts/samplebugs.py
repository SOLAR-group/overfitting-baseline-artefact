import csv
import pandas as pd

root ='results/'

bugs = []
with open(root+'bugs_with_a_correct_patch.csv', newline='') as csvfile:
  reader = csv.reader(csvfile, delimiter=',')
  row = next(reader) # remove header
  for row in reader:
    bug = row[0].strip(' \n\r')
    bugs.append(bug)

preprocessed = []
with open(root+'preprocessed_all.csv', newline='') as csvfile:
  reader = csv.reader(csvfile, delimiter=',')
  header = next(reader)
  preprocessed.append(','.join(header))
  for row in reader:
    found = False
    bug = row[2]
    for b in bugs:
      if b==bug:
        preprocessed.append((','.join(row)).strip('\n'))
        found = True
        break

with open(root+'preprocessed.csv','w') as f:
  for line in preprocessed:
    f.write(line+'\n')


preprocessed_by_bug = []
with open(root+'preprocessed_all_by_bug.csv', newline='') as csvfile:
  reader = csv.reader(csvfile, delimiter=',')
  header = next(reader)
  preprocessed_by_bug.append(','.join(header))
  for row in reader:
    found = False
    bug = row[1]
    for b in bugs:
      if b==bug:
        preprocessed_by_bug.append((','.join(row)).strip('\n'))
        found = True
        break

with open(root+'preprocessed_by_bug.csv','w') as f:
  for line in preprocessed_by_bug:
    f.write(line+'\n')
