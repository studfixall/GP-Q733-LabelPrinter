import zipfile

apk_path = r"C:\Users\Administrator\.qclaw\workspace-agent-4d029e48\apk_work\EnjoyMBM_fixed_v2.apk"

with zipfile.ZipFile(apk_path, 'r') as zf:
    main_js = "assets/apps/__UNI__092D5A7/www/app-service.js"
    content = zf.read(main_js).decode('utf-8', errors='ignore')
    
    # 查找修复代码的位置
    fix_pos = content.find("BLUETOOTH_FIX_APPLIED")
    if fix_pos > 0:
        print("Found BLUETOOTH_FIX_APPLIED at position:", fix_pos)
        print("\n--- First 500 chars after fix marker ---")
        print(content[fix_pos:fix_pos+500])
    
    # 检查是否有语法问题 - 查找未闭合的括号等
    print("\n\n--- Checking for syntax issues ---")
    
    # 检查修复代码是否完整
    if "function checkBluetoothPermission()" in content:
        print("[OK] checkBluetoothPermission function found")
        
        # 提取函数看是否完整
        func_start = content.find("function checkBluetoothPermission()")
        func_part = content[func_start:func_start+2000]
        
        # 检查括号匹配
        open_braces = func_part.count('{')
        close_braces = func_part.count('}')
        print(f"  Braces: open={open_braces}, close={close_braces}")
        
        if open_braces != close_braces:
            print("  [WARNING] Braces mismatch - syntax error!")
        else:
            print("  [OK] Braces balanced")
    
    # 检查是否在正确的位置插入（不应该在字符串中间）
    print("\n--- Context around fix ---")
    ctx_start = max(0, fix_pos - 100)
    print(repr(content[ctx_start:ctx_start+200]))
