output_dir = 'results/'
manualDir = output_dir+'manual_output/'
patchDir = '../ground_truth_patches/'
submission = 'submission'

def cleanpatch(patch):
  p = patch.splitlines()
  patch = ''
  for line in p:
    l = line.lstrip(' \t\n\r')
    if not (l.startswith('+') or l.startswith('-')): continue # or l.startswith('@')): continue
    #elif l.startswith('diff --git'): continue
    #elif l.startswith('index '): continue
    elif l.startswith('+++'): continue
    elif l.startswith('---'): continue
    elif l.lstrip('+-').startswith('//'): continue
    elif 'No newline at end of file' in l: continue
    elif len(l.strip('+- \t\n\r'))==0: continue
    else:
      l = l.replace('\t','')
      l = l.replace('\n','')
      l = l.replace('\r','')
      l = l.replace(' ','')
      #l = l.strip(' \t\n\r')
      patch += l+'\n'
  return patch
