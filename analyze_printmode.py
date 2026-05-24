import re

# Read arrays.xml to get the printMode array items
with open(r'C:\Users\Administrator\Barsoft-Reverse\resources\res\values\arrays.xml', 'r', encoding='utf-8') as f:
    content = f.read()

# Extract printMode array
match = re.search(r'<array name="printMode">(.*?)</array>', content, re.DOTALL)
if match:
    items = re.findall(r'<item>(.*?)</item>', match.group(1))
    print(f'printMode array has {len(items)} items:')
    for i, item in enumerate(items):
        print(f'  index {i}: {item}')
    
    print()
    print('Mapping to SDK BaseEnum.CmdType:')
    print('  CMD_ESC  = 1 (0x1)')
    print('  CMD_TSPL = 2 (0x2)')  
    print('  CMD_CPCL = 3 (0x3)')
    print('  CMD_ZPL  = 4 (0x4)')
    print('  CMD_PIN  = 5 (0x5)')
    print()
    print('Probable mapping:')
    for i, item in enumerate(items):
        if i == 0:
            print(f'  index 0: "{item}" -> CMD_TSPL (2) [标签模式=TSC/TSPL]')
        elif i == 1:
            print(f'  index 1: "{item}" -> CMD_ESC (1) [小票模式=ESC]')
        else:
            print(f'  index {i}: "{item}" -> ??? (CPCL would be index 2 -> CMD_CPCL=3)')
    print()
    print('Key question: If we add "面单模式" at index 2,')
    print('  will the app code map index 2 -> CMD_CPCL (3)?')
    print()
    print('Evidence FOR CPCL support in original code:')
    print('  1. strings.xml has: str_cpclmode, str_cpcl_print, str_printer_printmode_cpcl')
    print('  2. SDK has CpclFactory, CpclCmd with full CPCL command set')
    print('  3. Original printMode array only has 2 items (TSC + ESC)')
    print('  4. But the code references suggest CPCL mode was planned/available')
    print()
    print('Evidence AGAINST simple patch working:')
    print('  1. The original array only has 2 items - code may use simple 0/1 toggle')
    print('  2. If code uses: if(index==0)->TSC, else->ESC, adding index 2 would still go to ESC')
    print('  3. Code inside encrypted dex - we cannot verify the mapping logic')
    print('  4. CountView2 click handler is in encrypted code')

# Also check strings.xml for all print-mode related strings
strings_path = r'C:\Users\Administrator\Barsoft-Reverse\resources\res\values\strings.xml'
with open(strings_path, 'r', encoding='utf-8') as f:
    strings = f.read()

print()
print('=== All print mode related strings ===')
for m in re.finditer(r'<string name="(.*(?:print|mode|cpcl|tsc|esc|label|receipt).*)">(.*?)</string>', strings, re.IGNORECASE):
    print(f'  {m.group(1)} = {m.group(2)}')
