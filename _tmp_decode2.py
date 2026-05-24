import os, re, codecs

fp = r'C:\Users\Administrator\Barsoft-Reverse\resources\assets\Label\Templet\normal\4030.xml'
with open(fp, 'rb') as f:
    raw = f.read()

# The raw bytes for text content like e59381e5908defbc9a...
# Let's try: these might be valid UTF-8 but our console can't display them
# Let's just decode as UTF-8 and check codepoints

texts = re.findall(b'text="([^"]+)"', raw)
textnames = re.findall(b'textName="([^"]+)"', raw)

print("=== text attributes (UTF-8 decode + codepoint analysis) ===")
for t in texts:
    try:
        s = t.decode('utf-8')
        # Show each char with its unicode name
        chars = []
        for c in s:
            try:
                import unicodedata
                name = unicodedata.name(c, f'U+{ord(c):04X}')
                chars.append(f'{c}({name})')
            except:
                chars.append(f'{c}(U+{ord(c):04X})')
        print(' '.join(chars))
    except Exception as e:
        print(f"UTF-8 decode failed: {e}")

print()
print("=== textName attributes ===")
for t in textnames:
    try:
        s = t.decode('utf-8')
        chars = []
        for c in s:
            try:
                import unicodedata
                name = unicodedata.name(c, f'U+{ord(c):04X}')
                chars.append(f'{c}({name})')
            except:
                chars.append(f'{c}(U+{ord(c):04X})')
        print(' '.join(chars))
    except Exception as e:
        print(f"UTF-8 decode failed: {e}")
