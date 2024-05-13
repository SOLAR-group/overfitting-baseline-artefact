import csv, statistics
from globals import output_dir as root

def getdata(csvfile, txt):
  totals = []
  mx = 0
  rowmx = []
  
  with open(root+csvfile, newline='') as csvfile:
    reader = csv.reader(csvfile, delimiter=',')
    row = next(reader)
    for row in reader:
      t = int(row[-1])
      totals.append(t)
      if t > mx:
        mx = t
        rowmx = row 
  
  med = statistics.median(totals)
  men = statistics.mean(totals)
  mx = max(totals)
  print(txt)
  print('Median,Mean,Max')
  print(med,men,mx)
  print(rowmx)

getdata('preprocessed.csv','Data for bugs for which correct patches exist:')
getdata('preprocessed_all.csv','Data for all bugs for which patches were generated:')
