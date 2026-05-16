#!/usr/bin/env python3
"""
下载并配置 apktool
"""

import os
import urllib.request
import shutil

APKTOOL_DIR = r"C:\Users\Administrator\.qclaw\workspace-agent-4d029e48\tools"
APKTOOL_JAR = os.path.join(APKTOOL_DIR, "apktool.jar")
APKTOOL_BAT = os.path.join(APKTOOL_DIR, "apktool.bat")

def download_apktool():
    """下载 apktool"""
    os.makedirs(APKTOOL_DIR, exist_ok=True)
    
    if os.path.exists(APKTOOL_JAR):
        print("apktool.jar already exists")
        return True
    
    # apktool 下载地址
    url = "https://bitbucket.org/iBotPeaches/apktool/downloads/apktool_2.9.3.jar"
    
    print(f"Downloading apktool from {url}...")
    try:
        urllib.request.urlretrieve(url, APKTOOL_JAR)
        print(f"Downloaded to {APKTOOL_JAR}")
        return True
    except Exception as e:
        print(f"Download failed: {e}")
        # 尝试备用源
        alt_url = "https://github.com/iBotPeaches/Apktool/releases/download/v2.9.3/apktool_2.9.3.jar"
        print(f"Trying alternate source: {alt_url}")
        try:
            urllib.request.urlretrieve(alt_url, APKTOOL_JAR)
            print(f"Downloaded to {APKTOOL_JAR}")
            return True
        except Exception as e2:
            print(f"Alternate download also failed: {e2}")
            return False

def create_bat_file():
    """创建 Windows 批处理文件"""
    if os.path.exists(APKTOOL_BAT):
        return
    
    bat_content = '''@echo off
java -jar "%~dp0apktool.jar" %*
'''
    with open(APKTOOL_BAT, 'w') as f:
        f.write(bat_content)
    print(f"Created {APKTOOL_BAT}")

def main():
    print("Setting up apktool...")
    
    if download_apktool():
        create_bat_file()
        print("\nSetup complete!")
        print(f"apktool location: {APKTOOL_JAR}")
        print(f"Usage: {APKTOOL_BAT} <command>")
    else:
        print("\nSetup failed!")
        print("Please download manually from: https://apktool.org/")

if __name__ == "__main__":
    main()
