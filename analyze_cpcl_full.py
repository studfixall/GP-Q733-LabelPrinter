import re

with open(r"C:\Users\Administrator\.qclaw\workspace-agent-4d029e48\apk_work\__PrinterExample_decoded\smali\com\rt\printerlibrary\cmd\CpclCmd.smali", "r", encoding="utf-8") as f:
    content = f.read()

lines = content.split("\n")

def extract_method(lines, method_sig):
    """Extract a method by its signature pattern"""
    in_method = False
    method_lines = []
    for line in lines:
        if method_sig in line and '.method' in line and not in_method:
            in_method = True
            method_lines = []
        if in_method:
            method_lines.append(line)
            if '.end method' in line and len(method_lines) > 1:
                return method_lines
    return []

def extract_strings(method_lines):
    """Extract all const-string values from method"""
    strings = []
    for line in method_lines:
        m = re.search(r'const-string\s+\w+,\s+"(.+)"', line.strip())
        if m:
            strings.append(m.group(1))
    return strings

# === getCpclHeaderCmd ===
print("=" * 60)
print("CPCL HEADER COMMAND: getCpclHeaderCmd(pageWidth, pageHeight, copies, offset)")
print("=" * 60)
# Already fully decoded from smali above. Reconstruct:
print("""
Reconstructed from smali:
  Result = "!U1 BEGIN-PAGE\\r\\n"
         + "! {offset} {Lateral_Resolution} {Vertical_Resolution} {pageHeight*8} {copies}\\r\\n"  
         + "PW {pageWidth*8}\\r\\n"

  Where:
  - Lateral_Resolution / Vertical_Resolution = static fields (likely "203" or "8" for 203dpi)
  - pageHeight * 8 = height in dots (mm * 8 dots/mm)
  - pageWidth * 8 = width in dots (mm * 8 dots/mm)
  - copies = minimum 1
  
  Example for 40x30mm label, 1 copy, offset=0:
  "!U1 BEGIN-PAGE\\r\\n"
  "! 0 203 203 240 1\\r\\n"     <- 30*8=240, copies=1
  "PW 320\\r\\n"                 <- 40*8=320
""")

# Check Lateral_Resolution and Vertical_Resolution static fields
print("--- Static Resolution Fields ---")
for line in lines:
    if 'Lateral_Resolution' in line or 'Vertical_Resolution' in line:
        if 'field' in line:
            print(f"  {line.strip()}")

# === getEndCmd ===
print("\n" + "=" * 60)
print("CPCL END COMMAND: getEndCmd()")
print("=" * 60)
print('  " FORM\\r\\nPRINT\\r\\n!U1 END-PAGE\\r\\n"')
print("  NOTE: Contains FORM + PRINT + proprietary !U1 END-PAGE")

# === getTextCmd ===
print("\n" + "=" * 60)
print("CPCL TEXT COMMAND: getTextCmd(TextSetting, charset, text)")
print("=" * 60)
m = extract_method(lines, '.method private a(Lcom/rt/printerlibrary/setting/TextSetting;Ljava/lang/String;Ljava/lang/String;)V')
print(f"  Method size: {len(m)} lines")
print("  Key commands generated:")
print("    1. Alignment: LEFT\\n | CENTER\\n | RIGHT\\n")
print("    2. Bold: SETBOLD 1\\n | SETBOLD 0\\n")
print("    3. Underline: UNDERLINE ON\\n | UNDERLINE OFF\\n")
print("    4. Spacing: SETSP {value}\\n")
print("    5. Font selection by CpclFontTypeEnum:")
print("       0 -> font '55' (CJK), 1 -> '24', 2 -> '4', 3 -> '3'")
print("       4 -> '7', 5 -> '5', 6 -> '6', 7 -> '1'")
print("       8 -> '2', 9 -> '0'")
print("    6. Rotation: 0/90/180/270")
print("    7. Scale: SETMAG w h\\r\\n")
print("    8. Text output: T font rotation x y scale content\\r\\n")

# === getBarcodeCmd ===
print("\n" + "=" * 60)
print("CPCL BARCODE COMMAND: getBarcodeCmd(BarcodeSetting, charset, content)")
print("=" * 60)
m = extract_method(lines, '.method public getBarcodeCmd(')
print(f"  Method size: {len(m)} lines")
print("  Barcode formats mapped:")
print("    QRCODE -> 'QRCODE'")
print("    ITF14/I2OF5 -> 'I2OF5'")
print("    CODE_128/CODE128 -> '128'")
print("    CODABAR -> 'CODABAR'")
print("    CODE_39/CODE39 -> '39'")
print("    EAN_8/EAN8 -> 'EAN8'")
print("    EAN_13/EAN13 -> 'EAN13'")
print("    UPC_A/UPCA -> 'UPCA'")
print("  Rotation: 0/90/180/270")
print("  Text position: 1=Above, B=Below")
print("  Narrow bar width mapping: 1-7")
print("  Command format: BARCODE x y format narrow wide height rotation content\\r\\n")
print("  Or for QR: QRCODE ...")

# === CPCL_BOX / CPCL_LINE ===
print("\n" + "=" * 60)
print("CPCL BOX/LINE COMMANDS")
print("=" * 60)
print("  BOX: BOX x1 y1 x2 y2 thickness\\r\\n")
print("  LINE: L x1 y1 x2 y2 thickness\\r\\n")

# === Check what !U1 commands are ===
print("\n" + "=" * 60)
print("⚠️ CRITICAL: !U1 PROPRIETARY COMMANDS ANALYSIS")
print("=" * 60)
u1_cmds = []
for line in lines:
    if '!U1' in line:
        u1_cmds.append(line.strip())
for c in u1_cmds:
    print(f"  {c}")

print("""
CONCLUSION on !U1 commands:
  - !U1 BEGIN-PAGE\\r\\n  — Start of page (proprietary Gainscha command)
  - !U1 END-PAGE\\r\\n    — End of page (proprietary Gainscha command)
  - These are NOT standard CPCL commands.
  - Standard CPCL would use: ! <offset> <width> <height> <copies>\\r\\n
  - The SDK wraps standard CPCL inside proprietary U1 page markers.
  
  FOR RPP322/GP-Q733: These printers may NOT support !U1 commands!
  Standard CPCL alternative:
    Header: "! 0 203 203 {height_dots} {copies}\\r\\nPW {width_dots}\\r\\n"
    Footer: "FORM\\r\\nPRINT\\r\\n"
    (Remove !U1 BEGIN-PAGE and !U1 END-PAGE)
""")
