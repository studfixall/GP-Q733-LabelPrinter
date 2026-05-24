import re

# Compare TsplCmd vs CpclCmd string constants
for cmd_name, path in [
    ('TsplCmd', r'C:\Users\Administrator\.qclaw\workspace-agent-4d029e48\apk_work\__PrinterExample_decoded\smali\com\rt\printerlibrary\cmd\TsplCmd.smali'),
    ('EscCmd', r'C:\Users\Administrator\.qclaw\workspace-agent-4d029e48\apk_work\__PrinterExample_decoded\smali\com\rt\printerlibrary\cmd\EscCmd.smali'),
]:
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    strings = re.findall(r'const-string\s+\w+,\s+"([^"]+)"', content)
    print(f'=== {cmd_name} String Constants ===')
    for s in strings:
        if len(s) > 2:
            print(repr(s))
    print()

# Also check CpclCmd internal method 'a' which builds text commands
with open(r'C:\Users\Administrator\.qclaw\workspace-agent-4d029e48\apk_work\__PrinterExample_decoded\smali\com\rt\printerlibrary\cmd\CpclCmd.smali', 'r', encoding='utf-8') as f:
    content = f.read()

# Find the private 'a' method that builds text (TextSetting, String, String)
for m in re.finditer(r'\.method private a\(Lcom/rt/printerlibrary/setting/TextSetting;Ljava/lang/String;Ljava/lang/String;\)V.*?\.end method', content, re.DOTALL):
    text = m.group()
    print('=== CpclCmd.a(TextSetting, String, String) - text builder ===')
    if len(text) > 4000:
        print(text[:4000] + '...')
    else:
        print(text)
