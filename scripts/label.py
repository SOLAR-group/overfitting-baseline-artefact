import os, pickle, sys
from globals import cleanpatch, output_dir

exclusions = sys.argv[1].split(',')
timelimit = float(sys.argv[2])*3600000
bugs_correct={}
bugs_overfitting={}
with open('correct.obj','rb') as f:
  bugs_correct = pickle.load(f)
with open('overfitting.obj','rb') as f:
  bugs_overfitting = pickle.load(f)

bugs_unknown = {}
rootDir = output_dir+'patches_found'
filedepth = 5 + rootDir.count('/')
cbugs = bugs_correct.keys()
obugs = bugs_overfitting.keys()
check = 0

for dirName, subdirList, fileList in os.walk(rootDir):
  path = dirName.split('/')
  nbug = len(path)
  bug=''
  exclude = False
  for tool in exclusions:
    if tool in dirName: exclude = True
  if (nbug>=filedepth) and (not exclude): #('Arja' not in dirName) and ('RSRepair' not in dirName) and ('Cardumen' not in dirName):
    bug = path[3]+'-'+path[4]
    bugc = (bug in cbugs)
    bugo = (bug in obugs)
    for fname in fileList:
      txtend = (('Avatar' in dirName) or ('TBar' in dirName) or ('kPAR' in dirName) or 'FixMiner' in dirName)
      txtend = (txtend and fname.startswith('Patch_'))
      if fname.endswith('diff') or txtend:
        findtime = fname.split('_')
        time = -1
        idx = -1
        for i in findtime:
          j = i.split('.')
          if j[0].isdigit():
            if time<0: time=int(j[0])
            else: idx=int(j[0])
        if (((time>=0) and (idx>=0) and (time<=timelimit)) or (exclusions[0]=='None')): # hack to account for patches without time limit from Avatar, Arja and RSRepair
          check+=1
          print(str(check)+','+dirName+'/'+fname,end=',')
          patch = ''
          with open(dirName+'/'+fname) as f:
            patch = cleanpatch(f.read())
          if bugc:
            if patch in bugs_correct[bug]:
              print('correct',end=',')
              print(bugs_correct[bug].index(patch))
              continue
          if bugo:
            if patch in bugs_overfitting[bug]:
              print('overfitting',end=',')
              print(bugs_overfitting[bug].index(patch))
              continue
          if bug not in bugs_unknown: bugs_unknown[bug]=[]
          if patch not in bugs_unknown[bug]: bugs_unknown[bug].append(patch)
          print('unknown',end=',')
          print(bugs_unknown[bug].index(patch))
