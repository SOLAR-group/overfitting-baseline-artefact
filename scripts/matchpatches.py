import csv, sys

original = sys.argv[1]
manual = sys.argv[2]

patches = {}
with open(original, newline='') as csvfile:
  reader = csv.reader(csvfile, delimiter=',')
  for row in reader:
    patch = row[0].strip(' \n')
    p = patch.split('/')
    bug = p[3]+'-'+p[4]
    tool = p[2]
    idx = p[-1].split('_')[-1].split('.')[0]
    if 'cp' in idx:
      idx = p[-1].split('_')[-2].split('.')[0]
    patches[patch] = (bug,tool,idx)

with open(manual) as csvfile:
  reader = csv.reader(csvfile, delimiter=',')
  for row in reader:
    found = False
    patch = row[0]
    idx = patch.split('.')[0].strip('patch')
    if not idx.isnumeric():
      idx = idx.split('-')[0].strip('patch') 
    p = patch.split('-')
    bug = p[1]+'-'+p[2]
    tool = p[3].split('.')[0]
    check = (bug,tool,idx)
    for candidate in patches.keys():
        #print(check)
        #print(patches[candidate])
        if check == patches[candidate]:
          m = candidate#.split(' ')[1]
          print(m)
          found = True
    #if not found: # likely already labelled, so ignore
    #  print('missing',patch,idx)
