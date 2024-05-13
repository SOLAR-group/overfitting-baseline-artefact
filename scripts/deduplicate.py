import os, pickle, sys
from globals import cleanpatch, output_dir, patchDir, manualDir

root = output_dir
d4jDir = patchDir+'Defects4J/'
 
addmanual = bool(int(sys.argv[1]))

bugs_correct = {}
bugs_overfitting = {}
check=0

def process_human_fixes(rootDir):
  project = rootDir.split('/')[0]
  for dirName, subdirList, fileList in os.walk(d4jDir+rootDir):
    for fname in fileList:
      if fname.endswith('src.patch'):
        bugnr = fname.split('.')[0]
        bug = project+'-'+bugnr
        if bug not in bugs_correct.keys(): bugs_correct[bug]=[]
        with open(dirName+'/'+fname) as f:
          patch = cleanpatch(f.read())
          #print(bug,len(patch))
          if patch not in bugs_correct[bug]: bugs_correct[bug].append(patch)

def process_correct(dirName,fileList):
  global bugs_correct, check
  for fname in fileList:
    if '.patch' in fname:
      check+=1
      bug = fname.split('-')
      bug = bug[1]+'-'+bug[2]
      if bug not in bugs_correct.keys(): bugs_correct[bug]=[]
      with open(dirName+'/'+fname) as f:
        patch = cleanpatch(f.read())
        if patch not in bugs_correct[bug]: bugs_correct[bug].append(patch)

def process_overfitting(dirName,fileList):
  global bugs_overfitting, check
  for fname in fileList:
    if '.patch' in fname:
      check+=1
      bug = fname.split('-')
      bug = bug[1]+'-'+bug[2]
      if bug not in bugs_overfitting.keys(): bugs_overfitting[bug]=[]
      with open(dirName+'/'+fname) as f:
        patch = cleanpatch(f.read())
        if patch not in bugs_overfitting[bug]: bugs_overfitting[bug].append(patch)

def getpatches(rootDir,filedepth):
  correct = 'Dcorrect'
  overfitting = 'Doverfitting'
  for dirName, subdirList, fileList in os.walk(rootDir):
    bug = dirName.split('/')
    nbug = len(bug)
    if (nbug==filedepth):
      ddrapi = bug[filedepth-3]
      if ddrapi==correct: process_correct(dirName,fileList)
      elif ddrapi==overfitting: process_overfitting(dirName,fileList)
    elif (nbug==filedepth+1):
      icse = bug[filedepth-2]
      if icse==correct: process_correct(dirName,fileList)
      elif icse==overfitting: process_overfitting(dirName,fileList)

process_human_fixes('Closure/patches/')
process_human_fixes('Chart/patches/')
process_human_fixes('Lang/patches/')
process_human_fixes('Mockito/patches/')
process_human_fixes('Math/patches/')
process_human_fixes('Time/patches/')
rootDir = patchDir+'xTestCluster/Patches'
filedepth = 5+rootDir.count('/')
getpatches(rootDir,filedepth)
rootDir = patchDir+'ASE2020/PatchesClean'
filedepth = 5+rootDir.count('/')
getpatches(rootDir,filedepth)
rootDir = patchDir+'Cache'
filedepth = 5+rootDir.count('/')
getpatches(rootDir,filedepth)

if addmanual:
  allpatches=[]
  if os.path.exists(manualDir+'manually_labelled_patches.csv'):
    with open(manualDir+'manually_labelled_patches.csv','r') as f:
      allpatches = f.readlines()
  if os.path.exists(manualDir+'manually_labelled_patches_8h_after_drr.csv'):
    with open(manualDir+'manually_labelled_patches_8h_after_drr.csv','r') as f:
      allpatches = allpatches + f.readlines()
    for fname in allpatches:
      p = fname.strip('\n\r').split(',')
      patch = p[0]
      bug = p[0].split('/')
      bug = bug[3]+'-'+bug[4]
      label=p[1]
      if label.startswith('http'): label = p[3]
      if label=='correct':
        if bug not in bugs_correct.keys(): bugs_correct[bug]=[]
        with open(patch,'r') as fp:
          check += 1
          patch = cleanpatch(fp.read())
          if patch not in bugs_correct[bug]: bugs_correct[bug].append(patch)
      elif label=='overfitting' or label=='Partial fix':
        if bug not in bugs_overfitting.keys(): bugs_overfitting[bug]=[]
        with open(patch,'r') as fp:
          check += 1
          patch = cleanpatch(fp.read())
          if patch not in bugs_overfitting[bug]: bugs_overfitting[bug].append(patch)
      else: print(label)
  
  allpatches=[]
  if os.path.exists(root+'test_failing_patches.csv'):
    with open(root+'test_failing_patches.csv','r') as f:
      allpatches = f.readlines()
    for fname in allpatches:
      p = fname.strip('\n\r').split(',')
      patch = p[0]
      bug = p[0].split('/')
      bug = bug[3]+'-'+bug[4]
      if bug not in bugs_overfitting.keys(): bugs_overfitting[bug]=[]
      with open(patch,'r') as fp:
        check += 1
        patch = cleanpatch(fp.read())
        if patch not in bugs_overfitting[bug]: bugs_overfitting[bug].append(patch)

unique=0
for k,v in bugs_correct.items():
  unique+=len(v)
for k,v in bugs_overfitting.items():
  unique+=len(v)
print('all_patches_with_labels:',check)
print('unique_patches_with_labels:',unique)

with open('correct.obj','wb') as f:
  pickle.dump(bugs_correct,f)
with open('overfitting.obj','wb') as f:
  pickle.dump(bugs_overfitting,f)
