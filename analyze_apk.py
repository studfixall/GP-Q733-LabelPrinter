import zipfile
import re
import os

apk_path = 'EnjoyMBM.apk'
report = []

with zipfile.ZipFile(apk_path, 'r') as z:
    report.append('=== Deep APK Analysis ===\n')
    
    # Config files
    report.append('=== Config Files ===')
    config_files = [f for f in z.namelist() if any(ext in f.lower() for ext in ['.json', '.xml', '.config', '.properties', '.ini'])]
    for cfg in config_files[:30]:
        report.append(f'  {cfg}')
    report.append('')
    
    # JS files
    report.append('=== JavaScript Files (UniApp) ===')
    js_files = [f for f in z.namelist() if f.endswith('.js')]
    report.append(f'Total JS files: {len(js_files)}')
    for js in js_files[:10]:
        report.append(f'  {js}')
    report.append('')
    
    # Native libraries
    report.append('=== Native Libraries (.so) ===')
    so_files = [f for f in z.namelist() if f.endswith('.so')]
    for so in so_files:
        report.append(f'  {so}')
    report.append('')
    
    # Certificate info
    report.append('=== Certificate ===')
    if 'META-INF/MANIFEST.MF' in z.namelist():
        manifest = z.read('META-INF/MANIFEST.MF').decode('utf-8', errors='ignore')
        report.append('MANIFEST.MF found')
    
    # RSA certificate
    rsa_files = [f for f in z.namelist() if f.endswith('.RSA') or f.endswith('.DSA')]
    for rsa in rsa_files:
        report.append(f'  Certificate: {rsa}')
    report.append('')
    
    # Look for interesting files
    report.append('=== Interesting Files ===')
    interesting = [f for f in z.namelist() if any(k in f.lower() for k in ['key', 'secret', 'token', 'password', 'config', 'setting'])]
    for f in interesting[:20]:
        report.append(f'  {f}')

# Write report
with open('deep_analysis.txt', 'w', encoding='utf-8') as f:
    f.write('\n'.join(report))

print('Deep analysis saved to: deep_analysis.txt')
