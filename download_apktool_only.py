#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
下载 apktool.jar（备用源）
"""

import urllib.request
import os
import shutil
import ssl

# 忽略 SSL 证书验证
ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

def download_file(url, filepath, description):
    print(f"下载 {description}:")
    print(f"  URL: {url}")
    try:
        req = urllib.request.Request(url, headers={
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
        })
        with urllib.request.urlopen(req, context=ctx, timeout=60) as response:
            total_size = int(response.headers.get('Content-Length', 0))
            print(f"  大小: {total_size / 1024:.1f} KB")
            
            with open(filepath, 'wb') as f:
                f.write(response.read())
            print(f"  [SUCCESS] 已保存: {filepath}")
            return True
    except Exception as e:
        print(f"  [ERROR] 下载失败: {e}")
        return False

def main():
    import sys
    sys.stdout.reconfigure(encoding='utf-8')
    sys.stderr.reconfigure(encoding='utf-8')
    
    work_dir = r"C:\Users\Administrator\.qclaw\workspace-agent-4d029e48\apktools"
    apktool_jar = os.path.join(work_dir, "apktool.jar")
    
    # 尝试多个 apktool 下载源
    urls = [
        ("apktool.org 官方", "https://apktool.org/download/apktool-2.9.3.jar"),
        ("GitHub 备份", "https://github.com/TechRabbitDev/apktool/releases/download/v2.9.3/apktool_2.9.3.jar"),
        ("SourceForge", "https://sourceforge.net/projects/apktool/files/apktool-2.9.3.jar/download"),
        ("国内镜像", "https://ghproxy.net/https://github.com/TechRabbitDev/apktool/releases/download/v2.9.3/apktool_2.9.3.jar"),
    ]
    
    success = False
    for name, url in urls:
        print(f"\n尝试: {name}")
        if download_file(url, apktool_jar, f"apktool 2.9.3 ({name})"):
            success = True
            break
    
    if success:
        print("\n" + "=" * 60)
        print("[SUCCESS] apktool.jar 下载完成！")
        print("=" * 60)
        
        # 更新 apktool.bat
        jre_dir = os.path.join(work_dir, "jre-17.0.9-full")
        bat_path = os.path.join(work_dir, "apktool.bat")
        with open(bat_path, 'w') as f:
            f.write('@echo off\n')
            f.write(f'set "JAVA_HOME={jre_dir}"\n')
            f.write(f'"{os.path.join(jre_dir, "bin", "java.exe")}" -version >nul 2>&1\n')
            f.write(f'"{os.path.join(jre_dir, "bin", "java.exe")}" -jar "{apktool_jar}" %*\n')
        print(f"apktool.bat 已更新: {bat_path}")
        
        # 测试 apktool
        print("\n测试 apktool...")
        import subprocess
        result = subprocess.run([bat_path, "--version"], capture_output=True, text=True)
        print(result.stdout)
        if result.returncode != 0:
            print(result.stderr)
    else:
        print("\n[ERROR] 所有 apktool 下载源均失败")
        print("请手动下载: https://apktool.org/docs/install/")

if __name__ == "__main__":
    main()
