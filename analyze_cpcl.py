import re, sys

with open(r"C:\Users\Administrator\.qclaw\workspace-agent-4d029e48\apk_work\__PrinterExample_decoded\smali\com\rt\printerlibrary\cmd\CpclCmd.smali", "r", encoding="utf-8") as f:
    content = f.read()

# Extract the private method 'a' - text builder
lines = content.split("\n")
in_method = False
method_lines = []
for line in lines:
    if '.method private a(Lcom/rt/printerlibrary/setting/TextSetting;Ljava/lang/String;Ljava/lang/String;)V' in line:
        in_method = True
        method_lines = []
    if in_method:
        method_lines.append(line)
        if '.end method' in line and len(method_lines) > 1:
            break

# Extract key string constants and logic flow
print("=== CPCL Text Command Builder - Key Strings ===")
for line in method_lines:
    line = line.strip()
    if 'const-string' in line:
        # Extract the string value
        m = re.search(r'const-string\s+\w+,\s+"(.+)"', line)
        if m:
            print(f"  STRING: {repr(m.group(1))}")
    if 'if-eq' in line or 'if-ne' in line or 'if-gez' in line or 'if-lez' in line:
        print(f"  BRANCH: {line}")
    if 'goto' in line:
        print(f"  GOTO: {line}")

# Also extract getBarcodeCmd
print("\n=== getBarcodeCmd ===")
in_method = False
method_lines2 = []
for line in lines:
    if '.method public getBarcodeCmd(' in line:
        in_method = True
        method_lines2 = []
    if in_method:
        method_lines2.append(line)
        if '.end method' in line and len(method_lines2) > 1:
            break

for line in method_lines2:
    line = line.strip()
    if 'const-string' in line:
        m = re.search(r'const-string\s+\w+,\s+"(.+)"', line)
        if m:
            print(f"  STRING: {repr(m.group(1))}")
    if 'if-eq' in line or 'if-ne' in line:
        print(f"  BRANCH: {line}")
