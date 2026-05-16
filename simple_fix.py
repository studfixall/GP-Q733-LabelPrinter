#!/usr/bin/env python3
"""
最简单的修复方案：
直接修改原始 APK 中的 JS 文件，保留所有原始签名数据
"""

import zipfile
import os
import shutil
import io

WORK_DIR = r"C:\Users\Administrator\.qclaw\workspace-agent-4d029e48\apk_work"
# 使用原始未修改的 APK 作为基础
ORIGINAL_APK = os.path.join(WORK_DIR, "EnjoyMBM.apk")
OUTPUT_APK = os.path.join(WORK_DIR, "EnjoyMBM_quick_fix.apk")

def quick_fix():
    """快速修复：直接修改 ZIP 中的 JS 文件"""
    print("=" * 60)
    print("Quick APK Fix (Preserving Original Structure)")
    print("=" * 60)
    
    # 检查原始 APK 是否存在
    if not os.path.exists(ORIGINAL_APK):
        # 尝试其他文件名
        for name in ["EnjoyMBM_fixed.apk", "EnjoyMBM_fixed_final.apk"]:
            path = os.path.join(WORK_DIR, name)
            if os.path.exists(path):
                ORIGINAL_APK = path
                break
        else:
            print("Error: No source APK found!")
            return
    
    print(f"Source: {ORIGINAL_APK}")
    
    # 读取原始 APK
    print("\n1. Reading original APK...")
    with zipfile.ZipFile(ORIGINAL_APK, 'r') as zf_in:
        # 找到 JS 文件
        js_name = "assets/apps/__UNI__092D5A7/www/app-service.js"
        
        try:
            js_content = zf_in.read(js_name).decode('utf-8', errors='ignore')
        except KeyError:
            print(f"   Error: {js_name} not found!")
            # 列出所有文件
            print("   Available files:")
            for name in zf_in.namelist():
                if 'app' in name.lower() and name.endswith('.js'):
                    print(f"     {name}")
            return
        
        print(f"   JS size: {len(js_content):,} bytes")
        
        # 检查是否已修复
        if "BT_FIX" in js_content:
            print("   Already fixed!")
            return
        
        # 创建修复代码
        fix_code = '''/* BT_FIX */var BT={check:function(cb){var i=uni.getSystemInfoSync();if(i.platform==="ios"){cb&&cb(true);return;}var v=parseInt((i.system||"").split(".")[0]||"0");if(v>=12){uni.authorize({scope:"scope.bluetooth",success:function(){cb&&cb(true);},fail:function(){uni.showModal({title:"需要蓝牙权限",content:"请允许使用蓝牙",confirmText:"去设置",success:function(r){r.confirm&&uni.openSetting();}});cb&&cb(false);}});}else if(v>=6){uni.authorize({scope:"scope.userLocation",success:function(){cb&&cb(true);},fail:function(){uni.showToast({title:"需要位置权限",icon:"none"});cb&&cb(false);}});}else{cb&&cb(true);}},init:function(o){this.check(function(h){if(!h){o&&o.fail&&o.fail({errCode:-1,errMsg:"no permission"});return;}var s=o.success,f=o.fail;o.success=function(r){console.log("BT ok");s&&s(r);};o.fail=function(e){console.error("BT fail:",e);var m="蓝牙初始化失败";if(e.errCode===10001)m="请开启手机蓝牙";else if(e.errCode===10002)m="蓝牙未授权";uni.showToast({title:m,icon:"none"});f&&f(e);};uni.openBluetoothAdapter(o);});}};
'''
        
        # 找到插入位置
        insert_pos = js_content.find("var ")
        if insert_pos < 0:
            insert_pos = js_content.find("function ")
        if insert_pos < 0:
            insert_pos = 0
        
        new_js = js_content[:insert_pos] + fix_code + js_content[insert_pos:]
        new_js_bytes = new_js.encode('utf-8')
        
        print(f"   New JS size: {len(new_js_bytes):,} bytes")
        
        # 创建新的 APK
        print("\n2. Creating modified APK...")
        with zipfile.ZipFile(OUTPUT_APK, 'w', zipfile.ZIP_DEFLATED) as zf_out:
            for item in zf_in.infolist():
                if item.filename == js_name:
                    # 写入修改后的 JS
                    zf_out.writestr(item, new_js_bytes)
                else:
                    # 复制其他文件不变
                    data = zf_in.read(item.filename)
                    zf_out.writestr(item, data)
        
        print("   Done!")
    
    file_size = os.path.getsize(OUTPUT_APK)
    print(f"\n3. Output: {OUTPUT_APK}")
    print(f"   Size: {file_size / 1024 / 1024:.2f} MB")
    
    print("\n" + "=" * 60)
    print("IMPORTANT NOTES:")
    print("=" * 60)
    print("This APK has modified content but original signatures.")
    print("Android will reject it because the content doesn't match the signature.")
    print("")
    print("To install this app, you need to:")
    print("1. Uninstall the original app completely")
    print("2. Sign this APK with your own key using:")
    print("   - Android Studio")
    print("   - apksigner (from Android SDK)")
    print("   - jarsigner (from JDK)")
    print("")
    print("OR use a tool like:")
    print("- https://www.apkeditor.app/ (online)")
    print("- APK Signer app (on Android)")

if __name__ == "__main__":
    quick_fix()
