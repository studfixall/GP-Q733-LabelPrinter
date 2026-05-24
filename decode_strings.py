import re

with open(r'C:\Users\Administrator\Barsoft-Reverse\resources\res\values\strings.xml', 'rb') as f:
    raw = f.read()

for enc in ['utf-8', 'gbk', 'gb18030']:
    try:
        content = raw.decode(enc)
        print(f'Decoded with: {enc}')
        break
    except:
        continue

keywords = ['mode', 'cpcl', 'tsc', 'esc', 'label', 'receipt', 'print']
for m in re.finditer(r'<string name="([^"]+)">([^<]+)</string>', content):
    name = m.group(1)
    value = m.group(2)
    if any(k in name.lower() for k in keywords):
        print(f'  {name} = {value}')

print()
print('=== Gap type array ===')
with open(r'C:\Users\Administrator\Barsoft-Reverse\resources\res\values\arrays.xml', 'rb') as f:
    raw2 = f.read()
for enc in ['utf-8', 'gbk', 'gb18030']:
    try:
        content2 = raw2.decode(enc)
        break
    except:
        continue

for m in re.finditer(r'<array name="([^"]+)">(.*?)</array>', content2, re.DOTALL):
    arr_name = m.group(1)
    arr_content = m.group(2)
    items = re.findall(r'<item>(.*?)</item>', arr_content)
    print(f'{arr_name}: {items}')
