import csv, statistics

def getdata(csvfile, txt):
  totals = []
  mx = 0
  rowmx = []
  
  with open(csvfile, newline='') as csvfile:
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

getdata('../results_3h/preprocessed.csv','Data for bugs for which correct patches exist:')
getdata('../results_3h/preprocessed_all.csv','Data for all bugs for which patches were generated:')