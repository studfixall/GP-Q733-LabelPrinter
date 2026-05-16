#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
下载 apktool.jar 和便携 JRE（修复 SSL 错误，用官方源）
"""

import urllib.request
import os
import zipfile
import tempfile
import shutil
import ssl

# 忽略 SSL 证书验证（仅用于下载）
ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

def download_file(url, filename, description):
    print(f"下载 {description}:")
    print(f"  URL: {url}")
    try:
        req = urllib.request.Request(url, headers={
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
        })
        with urllib.request.urlopen(req, context=ctx, timeout=60) as response:
            total_size = int(response.headers.get('Content-Length', 0))
            print(f"  大小: {total_size / 1024 / 1024:.1f} MB")
            
            temp_dir = tempfile.gettempdir()
            filepath = os.path.join(temp_dir, filename)
            with open(filepath, 'wb') as f:
                block_size = 1024 * 1024  # 1MB
                downloaded = 0
                while True:
                    block = response.read(block_size)
                    if not block:
                        break
                    f.write(block)
                    downloaded += len(block)
                    if total_size > 0:
                        percent = downloaded / total_size * 100
                        print(f"\r  进度: {percent:.1f}% ({downloaded/1024/1024:.1f}/{total_size/1024/1024:.1f} MB)", end="", flush=True)
            print(f"\n  [SUCCESS] 已下载: {filepath}")
            return filepath
    except Exception as e:
        print(f"\n  [ERROR] 下载失败: {e}")
        return None

def main():
    import sys
    sys.stdout.reconfigure(encoding='utf-8')
    sys.stderr.reconfigure(encoding='utf-8')
    
    work_dir = r"C:\Users\Administrator\.qclaw\workspace-agent-4d029e48\apktools"
    os.makedirs(work_dir, exist_ok=True)
    
    # 1. 下载 apktool.jar（官方 Bitbucket 源，忽略 SSL）
    apktool_url = "https://bitbucket.org/iBotPeaches/apktool/downloads/apktool_2.9.3.jar"
    apktool_jar = download_file(apktool_url, "apktool.jar", "apktool 2.9.3")
    if apktool_jar:
        dest = os.path.join(work_dir, "apktool.jar")
        shutil.move(apktool_jar, dest)
        print(f"  apktool.jar 已保存到: {dest}")
    else:
        print("\n  尝试备用 apktool 源...")
        apktool_url2 = "https://apktool.org/download/apktool-2.9.3.jar"
        apktool_jar2 = download_file(apktool_url2, "apktool.jar", "apktool 2.9.3 (备用源)")
        if apktool_jar2:
            dest = os.path.join(work_dir, "apktool.jar")
            shutil.move(apktool_jar2, dest)
            print(f"  apktool.jar 已保存到: {dest}")
    
    # 2. 下载便携 JRE（官方 BellSoft 源）
    jre_url = "https://download.bell-sw.com/java/17.0.9+11/bellsoft-jre17.0.9+11-windows-amd64-full.zip"
    jre_zip = download_file(jre_url, "jre17.zip", "BellSoft Liberica JRE 17 便携版")
    if jre_zip:
        dest = os.path.join(work_dir, "jre17.zip")
        shutil.move(jre_zip, dest)
        print(f"  JRE 已保存到: {dest}")
        
        # 解压 JRE
        print("\n  解压 JRE...")
        with zipfile.ZipFile(dest, 'r') as zf:
            zf.extractall(work_dir)
        print("  [SUCCESS] JRE 解压完成")
        
        # 找到 JRE 目录
        jre_dir = None
        for item in os.listdir(work_dir):
            if item.startswith("jre") and os.path.isdir(os.path.join(work_dir, item)):
                jre_dir = os.path.join(work_dir, item)
                break
        
        if jre_dir:
            print(f"  JRE 目录: {jre_dir}")
            
            # 3. 创建 apktool 批处理脚本
            bat_path = os.path.join(work_dir, "apktool.bat")
            with open(bat_path, 'w') as f:
                f.write('@echo off\n')
                f.write(f'set JAVA_HOME={jre_dir}\n')
                f.write(f'"{os.path.join(jre_dir, "bin", "java.exe")}" -jar "{os.path.join(work_dir, "apktool.jar")}" %*\n')
            print(f"\n  apktool 脚本已创建: {bat_path}")
            
            # 4. 创建 jarsigner 签名脚本
            sign_bat = os.path.join(work_dir, "sign_apk.bat")
            with open(sign_bat, 'w') as f:
                f.write('@echo off\n')
                f.write('set JAVA_HOME=%~dp0jre17.0.9+11\n')
                f.write('set PATH=%JAVA_HOME%\\bin;%PATH%\n')
                f.write('jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 -keystore ~/.android/debug.keystore -storepass android -keypass android %1 alias\n')
            print(f"  签名脚本已创建: {sign_bat}")
            
            print("\n" + "=" * 60)
            print("工具已准备完成！")
            print("=" * 60)
            print(f"工作目录: {work_dir}")
            print("\n使用方法:")
            print(f"1. 反编译 APK: {bat_path} d C:\\path\\to\\app.apk")
            print(f"2. 重新编译: {bat_path} b C:\\path\\to\\app_dir")
            print(f"3. 签名 APK: {sign_bat} C:\\path\\to\\app.apk")
            print("\n现在可以处理 APK 了")
        else:
            print("[ERROR] 找不到 JRE 目录")
    else:
        print("\n[ERROR] JRE 下载失败，无法继续")

if __name__ == "__main__":
    main()
