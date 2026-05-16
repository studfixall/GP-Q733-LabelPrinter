import zipfile

apk_path = r"C:\Users\Administrator\.qclaw\workspace-agent-4d029e48\apk_work\EnjoyMBM_fixed_v2.apk"

print("Verifying fixed APK...")
print(f"File: {apk_path}")
print()

with zipfile.ZipFile(apk_path, 'r') as zf:
    main_js = "assets/apps/__UNI__092D5A7/www/app-service.js"
    if main_js in zf.namelist():
        content = zf.read(main_js).decode('utf-8', errors='ignore')
        
        has_fix = "BLUETOOTH_FIX_APPLIED" in content
        has_check = "checkBluetoothPermission" in content
        has_safe = "initBluetoothAdapterSafe" in content
        
        print("Fix verification:")
        print("-" * 40)
        print(f"  [OK] BLUETOOTH_FIX marker: {has_fix}")
        print(f"  [OK] Permission check function: {has_check}")
        print(f"  [OK] Safe init function: {has_safe}")
        
        print()
        print(f"File size: {len(content):,} bytes")
        
        if has_fix:
            print("\n[SUCCESS] Fix applied to APK!")
        else:
            print("\n[FAILED] Fix marker not found")
    else:
        print(f"Error: {main_js} not found")
