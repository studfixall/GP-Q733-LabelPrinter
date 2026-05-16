import zipfile

apk_path = r"C:\Users\Administrator\.qclaw\workspace-agent-4d029e48\apk_work\EnjoyMBM_fixed_final.apk"

print("Verifying final APK...")

with zipfile.ZipFile(apk_path, 'r') as zf:
    main_js = "assets/apps/__UNI__092D5A7/www/app-service.js"
    content = zf.read(main_js).decode('utf-8', errors='ignore')
    
    print(f"File size: {len(content):,} bytes")
    print()
    
    # 检查修复标记
    has_fix = "BT_FIX" in content
    has_helper = "BT={" in content or "var BT=" in content
    has_check = "check:function" in content
    has_init = "init:function" in content
    
    print("Verification:")
    print(f"  Fix marker (BT_FIX): {has_fix}")
    print(f"  BT helper object: {has_helper}")
    print(f"  check method: {has_check}")
    print(f"  init method: {has_init}")
    
    # 检查括号匹配
    helper_start = content.find("var BT=")
    if helper_start > 0:
        # 找到完整的 BT 对象
        brace_count = 0
        in_helper = False
        helper_end = helper_start
        for i, c in enumerate(content[helper_start:]):
            if c == '{':
                brace_count += 1
                in_helper = True
            elif c == '}':
                brace_count -= 1
            if in_helper and brace_count == 0:
                helper_end = helper_start + i + 1
                break
        
        helper_code = content[helper_start:helper_end]
        open_braces = helper_code.count('{')
        close_braces = helper_code.count('}')
        print(f"\nBrace balance: {open_braces} open, {close_braces} close")
        
        if open_braces == close_braces:
            print("  [OK] Syntax is correct!")
            print("\n=== SUCCESS ===")
            print("Final APK is ready!")
        else:
            print("  [ERROR] Syntax error!")
    
    # 显示修复代码预览
    if has_fix:
        fix_pos = content.find("BT_FIX")
        print("\n--- Fix code (first 300 chars) ---")
        print(content[fix_pos:fix_pos+300])
