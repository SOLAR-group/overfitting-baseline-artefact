import csv, sys


filename=sys.argv[1]

print(filename)

with open(filename) as csvfile:
  reader = csv.reader(csvfile, delimiter=' ')
  for row in reader:
    patch = row[0]
    p = patch.split('/')
    project = p[3]
    tool = p[2]
    bug = p[4]
    pdir = 'Patches/Dunassessed/'+tool+'/'
    patchid = p[-1].split('_')
    if len(patchid)>2: patchid = patchid[2]
    else: patchid = patchid[1]
    src = 'cp '+'./scripts/'+row[0]+' '
    dest = pdir+project+'/'
    print("mkdir -p "+dest)
    patch = 'patch'+patchid+'-'+project+'-'+bug+'-'+tool+'.patch'
    print(src+dest+patch)
    #print('sed -i "" "s/--- a/--- /g" '+dest+patch)
    #print('sed -i "" "s/+++ b/+++ /g" '+dest+patch)
    print('./run.py patch_assessment '+patch+' Dunassessed 2019_Evosuite')
