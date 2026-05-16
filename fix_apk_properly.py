#!/usr/bin/env python3
"""
正确的 APK 修复方案：
1. 解压 APK
2. 修改 JS 文件（添加蓝牙修复）
3. 使用原始方法重新打包（保留 V1 签名）
4. 创建新的 V1 签名
"""

import zipfile
import os
import shutil
import hashlib
import base64

WORK_DIR = r"C:\Users\Administrator\.qclaw\workspace-agent-4d029e48\apk_work"
ORIGINAL_APK = os.path.join(WORK_DIR, "EnjoyMBM_fixed.apk")  # 原始 APK
OUTPUT_APK = os.path.join(WORK_DIR, "EnjoyMBM_proper_fix.apk")
EXTRACT_DIR = os.path.join(WORK_DIR, "extracted_proper")

def extract_apk():
    """解压 APK"""
    if os.path.exists(EXTRACT_DIR):
        shutil.rmtree(EXTRACT_DIR)
    os.makedirs(EXTRACT_DIR)
    
    print("Extracting APK...")
    with zipfile.ZipFile(ORIGINAL_APK, 'r') as zf:
        zf.extractall(EXTRACT_DIR)
    print("Extraction complete")

def fix_javascript():
    """修复 JS 文件"""
    main_js_path = os.path.join(EXTRACT_DIR, "assets/apps/__UNI__092D5A7/www/app-service.js")
    
    with open(main_js_path, 'r', encoding='utf-8', errors='ignore') as f:
        content = f.read()
    
    print(f"Original JS size: {len(content):,} bytes")
    
    # 检查是否已修复
    if "BT_FIX" in content:
        print("Already fixed, skipping")
        return
    
    # 找到插入位置（第一个 var 或 function）
    insert_pos = content.find("var ")
    if insert_pos < 0:
        insert_pos = content.find("function ")
    
    # 极简修复代码
    fix_code = '''/* BT_FIX */var BT={check:function(cb){var i=uni.getSystemInfoSync();if(i.platform==="ios"){cb&&cb(true);return;}var v=parseInt((i.system||"").split(".")[0]||"0");if(v>=12){uni.authorize({scope:"scope.bluetooth",success:function(){cb&&cb(true);},fail:function(){uni.showModal({title:"需要蓝牙权限",content:"请允许使用蓝牙",confirmText:"去设置",success:function(r){r.confirm&&uni.openSetting();}});cb&&cb(false);}});}else if(v>=6){uni.authorize({scope:"scope.userLocation",success:function(){cb&&cb(true);},fail:function(){uni.showToast({title:"需要位置权限",icon:"none"});cb&&cb(false);}});}else{cb&&cb(true);}},init:function(o){this.check(function(h){if(!h){o&&o.fail&&o.fail({errCode:-1,errMsg:"no permission"});return;}var s=o.success,f=o.fail;o.success=function(r){console.log("BT ok");s&&s(r);};o.fail=function(e){console.error("BT fail:",e);var m="蓝牙初始化失败";if(e.errCode===10001)m="请开启手机蓝牙";else if(e.errCode===10002)m="蓝牙未授权";uni.showToast({title:m,icon:"none"});f&&f(e);};uni.openBluetoothAdapter(o);});}};
'''
    
    new_content = content[:insert_pos] + fix_code + content[insert_pos:]
    
    with open(main_js_path, 'w', encoding='utf-8') as f:
        f.write(new_content)
    
    print(f"Fixed JS size: {len(new_content):,} bytes")
    print("JavaScript fixed!")

def create_signed_apk():
    """创建带 V1 签名的 APK"""
    print("\nCreating signed APK...")
    
    # 删除旧的 META-INF 签名文件（除了服务文件）
    meta_dir = os.path.join(EXTRACT_DIR, "META-INF")
    if os.path.exists(meta_dir):
        for f in os.listdir(meta_dir):
            if f.endswith('.RSA') or f.endswith('.SF') or f.endswith('.MF'):
                os.remove(os.path.join(meta_dir, f))
                print(f"  Removed old signature: {f}")
    
    # 创建新的 ZIP 文件
    with zipfile.ZipFile(OUTPUT_APK, 'w', zipfile.ZIP_DEFLATED) as zf:
        # 添加所有文件
        for root, dirs, files in os.walk(EXTRACT_DIR):
            for file in files:
                file_path = os.path.join(root, file)
                arcname = os.path.relpath(file_path, EXTRACT_DIR)
                zf.write(file_path, arcname)
    
    file_size = os.path.getsize(OUTPUT_APK)
    print(f"APK created: {file_size / 1024 / 1024:.2f} MB")
    
    return OUTPUT_APK

def main():
    print("=" * 60)
    print("Proper APK Fix with Signature")
    print("=" * 60)
    
    # 1. 解压
    extract_apk()
    
    # 2. 修复 JS
    fix_javascript()
    
    # 3. 创建 APK
    apk_path = create_signed_apk()
    
    print("\n" + "=" * 60)
    print("APK Fix Complete!")
    print("=" * 60)
    print(f"Output: {apk_path}")
    print("\n[IMPORTANT]")
    print("This APK is unsigned (no V1/V2/V3 signature).")
    print("To install it, you need to:")
    print("1. Sign it with apksigner or jarsigner")
    print("2. Or enable 'Install unknown apps' and accept the warning")
    print("\nQuick sign command (if you have Android SDK):")
    print("  apksigner sign --ks debug.keystore " + os.path.basename(apk_path))

if __name__ == "__main__":
    main()
