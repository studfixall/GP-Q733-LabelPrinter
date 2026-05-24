import os, re

fp = r'C:\Users\Administrator\Barsoft-Reverse\resources\assets\Label\Templet\normal\4030.xml'
with open(fp, 'rb') as f:
    raw = f.read()

# Find text attribute values
texts = re.findall(b'text="([^"]+)"', raw)
textnames = re.findall(b'textName="([^"]+)"', raw)

print("=== text attributes ===")
for t in texts:
    print(f"Raw hex: {t.hex()}")
    try:
        print(f"  GBK:     {t.decode('gbk')}")
    except Exception as e:
        print(f"  GBK:     FAIL {e}")
    try:
        print(f"  UTF-8:   {t.decode('utf-8', errors='replace')}")
    except:
        print(f"  UTF-8:   FAIL")
    print()

print("=== textName attributes ===")
for t in textnames:
    print(f"Raw hex: {t.hex()}")
    try:
        print(f"  GBK:     {t.decode('gbk')}")
    except Exception as e:
        print(f"  GBK:     FAIL {e}")
    try:
        print(f"  UTF-8:   {t.decode('utf-8', errors='replace')}")
    except:
        print(f"  UTF-8:   FAIL")
    print()
