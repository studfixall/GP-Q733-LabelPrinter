import zipfile

apk_path = r"C:\Users\Administrator\.qclaw\workspace-agent-4d029e48\apk_work\EnjoyMBM_fixed_v3.apk"

print("Verifying v3 APK...")

with zipfile.ZipFile(apk_path, 'r') as zf:
    main_js = "assets/apps/__UNI__092D5A7/www/app-service.js"
    content = zf.read(main_js).decode('utf-8', errors='ignore')
    
    print(f"File size: {len(content):,} bytes")
    print()
    
    # 检查修复标记
    has_fix = "BLUETOOTH_FIX_APPLIED" in content
    has_helper = "BluetoothHelper" in content
    has_check = "checkPermission" in content
    
    print("Checks:")
    print(f"  Fix marker: {has_fix}")
    print(f"  BluetoothHelper object: {has_helper}")
    print(f"  checkPermission method: {has_check}")
    
    # 检查语法 - 括号匹配
    helper_start = content.find("var BluetoothHelper")
    if helper_start > 0:
        helper_code = content[helper_start:helper_start+3000]
        open_braces = helper_code.count('{')
        close_braces = helper_code.count('}')
        print(f"\nBrace balance in helper: {open_braces} open, {close_braces} close")
        
        if open_braces == close_braces:
            print("  [OK] Syntax looks good!")
        else:
            print(f"  [ERROR] Mismatch by {open_braces - close_braces}")
    
    # 显示修复代码的前500字符
    if has_fix:
        fix_pos = content.find("BLUETOOTH_FIX_APPLIED")
        print("\n--- Fix code preview ---")
        print(content[fix_pos:fix_pos+400])
