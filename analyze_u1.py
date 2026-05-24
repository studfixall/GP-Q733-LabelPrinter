import re
with open(r"C:\Users\Administrator\.qclaw\workspace-agent-4d029e48\apk_work\__PrinterExample_decoded\smali\com\rt\printerlibrary\cmd\CpclCmd.smali", "r", encoding="utf-8") as f:
    content = f.read()
lines = content.split("\n")

print("=== !U1 PROPRIETARY COMMANDS ===")
for line in lines:
    if '!U1' in line:
        print(f"  {line.strip()}")

# Also check the CpclFontTypeEnum for font mapping
print("\n=== CpclFontTypeEnum ===")
enum_path = r"C:\Users\Administrator\.qclaw\workspace-agent-4d029e48\apk_work\__PrinterExample_decoded\smali\com\rt\printerlibrary\enumerate\CpclFontTypeEnum.smali"
if True:
    with open(enum_path, "r", encoding="utf-8") as f:
        enum_content = f.read()
    for line in enum_content.split("\n"):
        line = line.strip()
        if 'enum' in line.lower() or 'field' in line or 'const' in line:
            print(f"  {line}")

# Check CpclFactory for how it creates the command object
print("\n=== CpclFactory ===")
with open(r"C:\Users\Administrator\.qclaw\workspace-agent-4d029e48\apk_work\__PrinterExample_decoded\smali\com\rt\printerlibrary\cmd\CpclFactory.smali", "r", encoding="utf-8") as f:
    factory = f.read()
for line in factory.split("\n"):
    line = line.strip()
    if line and not line.startswith('#') and not line.startswith('.source') and not line.startswith('.implements'):
        print(f"  {line}")
