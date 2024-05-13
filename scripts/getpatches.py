import sys, csv
from globals import output_dir

root=output_dir

file = sys.argv[1]

ok = True
count = 0

patches_passed = []
patches_failed = []

with open(file,newline='') as csvfile:
  reader = csv.reader(csvfile, delimiter=',')
  row=next(reader)
  patch = row[0]
  bug = row[1]+'-'+row[2]
  failure = bool(int(row[6]))
  ok = not failure
  count +=1
  for row in reader:
    patch_next = row[0]
    bug_next = row[1]+'-'+row[2]
    failure_next = bool(int(row[6]))
    if (patch==patch_next) and (bug==bug_next):
      if ok: ok = not failure_next
    else:
      if ok: patches_passed.append(patch)
      else: patches_failed.append(patch)
      patch = patch_next
      bug = bug_next
      ok = not failure_next
      count +=1
if ok: patches_passed.append(patch)
else: patches_failed.append(patch)

#print('patches_passed: ',len(patches_passed))
#print('patches_failed: ',len(patches_failed))

with open(root+'patches_passed.csv','w') as fp:
  for p in patches_passed: fp.write(p+'\n') 
with open(root+'patches_failed.csv','w') as ff:
  for p in patches_failed: ff.write(p+'\n') 
