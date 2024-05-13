import csv, sys
import pandas as pd
from globals import output_dir, submission

root=output_dir
d = []
#{
#    'paper':[],
#    'correctness':[],
#    'bug':[],
#    'patches':[]
#    }
paper = submission
correctness = ''
bug = ''
patches = 1
with open(root+'unique_by_bug.csv', newline='') as csvfile:
  reader = csv.reader(csvfile, delimiter=',')
  row = next(reader)
  correctness = row[1]
  bug = row[0]
  for row in reader:
    correctness_next = row[1]
    bug_next = row[0]
    if (bug_next==bug) and (correctness_next==correctness):
      patches += 1
    else:
      d.append([submission,correctness,bug,patches])
      correctness = correctness_next
      bug = bug_next
      patches = 1
d.append([submission,correctness,bug,patches])

df = pd.DataFrame(d, columns=['paper','correctness','Bug','patches'])
df = df.astype({'patches': 'int64'})
#print(sum(df.patches))

dfc = df.loc[df.correctness=='correct']
dfo = df.loc[df.correctness=='overfitting']
dfu = df.loc[df.correctness=='unknown']

dfc=dfc.assign(incorrect=0)

for idx,row in dfo.iterrows():
  i = dfc.loc[(dfc.Bug==row.Bug)].index
  if len(i)>0:
    dfc.loc[i,'incorrect']=row.patches
  else:
    r = row.copy()
    r['incorrect'] = row.patches
    r.patches = 0
    dfc = pd.concat([dfc, r.to_frame().T], ignore_index=True)

dfc=dfc.assign(unknown=0)
for idx,row in dfu.iterrows():
  i = dfc.loc[(dfc.Bug==row.Bug)].index
  if len(i)>0:
    dfc.loc[i,'unknown']=row.patches
  else:
    r = row.copy()
    r['incorrect'] = 0
    r['unknown'] = row.patches
    r.patches = 0
    dfc = pd.concat([dfc, r.to_frame().T], ignore_index=True)

dfc.rename(columns = {'patches':'correct'}, inplace = True)
dfc.drop('correctness', axis=1, inplace=True)

fn = lambda row: row.correct + row.incorrect + row.unknown
col = dfc.apply(fn, axis=1)
dfc = dfc.assign(total=col.values) 

dfc.to_csv(root+'preprocessed_all_by_bug.csv', index = False, header=True)

#print(sum(dfc.correct)+sum(dfc.incorrect)+sum(dfc.unknown))
#print(sum(dfc.correct))
#print(sum(dfc.incorrect))
#print(sum(dfc.unknown))
