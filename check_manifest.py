import zipfile
import os

apk_paths = [
    r"C:\Users\Administrator\.qclaw\workspace-agent-4d029e48\apk_work\EnjoyMBM_fixed.apk",
    r"C:\Users\Administrator\.qclaw\workspace-agent-4d029e48\apk_work\EnjoyMBM_fixed_final.apk",
]

for apk_path in apk_paths:
    print(f"\n{'='*60}")
    print(f"Checking: {os.path.basename(apk_path)}")
    print('='*60)
    
    if not os.path.exists(apk_path):
        print("File not found!")
        continue
    
    with zipfile.ZipFile(apk_path, 'r') as zf:
        # 检查关键文件
        files_to_check = [
            "AndroidManifest.xml",
            "resources.arsc",
            "classes.dex",
            "META-INF/MANIFEST.MF",
            "META-INF/CERT.RSA",
            "META-INF/CERT.SF",
        ]
        
        print("\nKey files:")
        for f in files_to_check:
            exists = f in zf.namelist()
            status = "OK" if exists else "MISSING"
            print(f"  [{status}] {f}")
        
        # 检查 META-INF 目录
        meta_files = [n for n in zf.namelist() if n.startswith("META-INF/")]
        print(f"\nMETA-INF files ({len(meta_files)}):")
        for f in meta_files[:10]:
            print(f"  - {f}")
        
        # 获取 APK 结构摘要
        print(f"\nTotal entries: {len(zf.namelist())}")
        
        # 检查 AndroidManifest.xml 是否可以读取
        if "AndroidManifest.xml" in zf.namelist():
            try:
                manifest_data = zf.read("AndroidManifest.xml")
                print(f"\nAndroidManifest.xml size: {len(manifest_data)} bytes")
                # 检查是否是二进制 XML
                if manifest_data[:4] == b'\x03\x00\x08\x00':
                    print("  Format: Binary XML (correct)")
                elif b'<?xml' in manifest_data[:20]:
                    print("  Format: Text XML (may cause issues)")
                else:
                    print(f"  Format: Unknown (header: {manifest_data[:4].hex()})")
            except Exception as e:
                print(f"  Error reading manifest: {e}")
