import csv,sys
#from globals import output_dir as root
root = 'results/'

patches = {}
x = int(sys.argv[1]) # first_x_patches 
y = sys.argv[2] # tool_name

with open(root+'labels.csv',newline='') as csvfile:
  reader = csv.reader(csvfile, delimiter=',')
  for row in reader:
    patch =  row[1].split('/')
    tool = patch[2]
    bug = patch[3]+'-'+patch[4]
    if (tool,bug) not in patches.keys(): patches[(tool,bug)]=[]
    idx = int(patch[-1].split('_')[1])
    patches[(tool,bug)].append((idx,','.join(row))) 

for k,v in patches.items():
  if y==k[0]:
    v.sort(key=lambda tup: tup[0])
    for patch in v[:min(x,len(v))]:
      print(patch[1])
