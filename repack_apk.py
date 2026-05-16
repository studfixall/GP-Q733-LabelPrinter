#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
重新打包APK
"""

import zipfile
import os
import shutil

EXTRACTED_DIR = r"C:\Users\Administrator\.qclaw\workspace-agent-4d029e48\apk_work\extracted"
OUTPUT_APK = r"C:\Users\Administrator\.qclaw\workspace-agent-4d029e48\apk_work\EnjoyMBM_fixed_v2.apk"
ORIGINAL_APK = r"C:\Users\Administrator\.qclaw\workspace-agent-4d029e48\apk_work\EnjoyMBM.apk"

def repack_apk():
    """重新打包APK"""
    
    print("开始重新打包APK...")
    print(f"源目录: {EXTRACTED_DIR}")
    print(f"输出文件: {OUTPUT_APK}")
    
    # 删除旧文件
    if os.path.exists(OUTPUT_APK):
        os.remove(OUTPUT_APK)
        print("已删除旧文件")
    
    # 创建新的ZIP文件（APK本质是ZIP）
    with zipfile.ZipFile(OUTPUT_APK, 'w', zipfile.ZIP_DEFLATED) as zipf:
        for root, dirs, files in os.walk(EXTRACTED_DIR):
            for file in files:
                file_path = os.path.join(root, file)
                arcname = os.path.relpath(file_path, EXTRACTED_DIR)
                zipf.write(file_path, arcname)
                
                # 显示进度
                if file.endswith('.js') or file.endswith('.dex'):
                    print(f"  添加: {arcname}")
    
    # 获取文件大小
    file_size = os.path.getsize(OUTPUT_APK)
    print(f"\n打包完成!")
    print(f"文件大小: {file_size} bytes ({file_size/1024/1024:.2f} MB)")
    print(f"保存位置: {OUTPUT_APK}")
    
    return OUTPUT_APK

if __name__ == '__main__':
    repack_apk()
