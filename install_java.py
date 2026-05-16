#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
用腾讯云国内镜像安装 BellSoft Liberica JDK 17
"""

import urllib.request
import os
import zipfile
import tempfile
import subprocess
import sys

# 设置标准输出编码为 UTF-8
sys.stdout.reconfigure(encoding='utf-8')
sys.stderr.reconfigure(encoding='utf-8')

# BellSoft Liberica JDK 17（腾讯云镜像）
JDK_VERSION = "17.0.9"
JDK_BUILD = "11"
JDK_URL = f"https://mirrors.cloud.tencent.com/bell-sw/liberica/releases/{JDK_VERSION}+{JDK_BUILD}/bellsoft-jdk{JDK_VERSION}+{JDK_BUILD}-windows-amd64-full.zip"
INSTALL_DIR = r"C:\Program Files\BellSoft\LibericaJDK"

def download_jdk():
    print(f"下载 BellSoft Liberica JDK {JDK_VERSION} (腾讯云镜像)...")
    print(f"URL: {JDK_URL}")
    
    try:
        req = urllib.request.Request(JDK_URL, headers={
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
        })
        with urllib.request.urlopen(req, timeout=120) as response:
            total_size = int(response.headers.get('Content-Length', 0))
            print(f"文件大小: {total_size / 1024 / 1024:.1f} MB")
            
            temp_zip = os.path.join(tempfile.gettempdir(), "liberica17.zip")
            with open(temp_zip, 'wb') as f:
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
                        print(f"\r下载进度: {percent:.1f}% ({downloaded/1024/1024:.1f}/{total_size/1024/1024:.1f} MB)", end="", flush=True)
            print(f"\n[SUCCESS] 下载完成: {temp_zip}")
            return temp_zip
    except Exception as e:
        print(f"\n[ERROR] 下载失败: {e}")
        print("\n备用手动下载链接:")
        print("1. 腾讯云镜像: https://mirrors.cloud.tencent.com/bell-sw/liberica/")
        print("2. 官方下载: https://bell-sw.com/pages/downloads/")
        return None

def install_jdk(zip_path):
    print(f"\n解压到 {INSTALL_DIR}...")
    os.makedirs(INSTALL_DIR, exist_ok=True)
    
    with zipfile.ZipFile(zip_path, 'r') as zf:
        zf.extractall(INSTALL_DIR)
    
    # 找 JDK 目录
    jdk_home = None
    for item in os.listdir(INSTALL_DIR):
        item_path = os.path.join(INSTALL_DIR, item)
        if os.path.isdir(item_path) and "jdk" in item.lower():
            jdk_home = item_path
            break
    
    if not jdk_home:
        print("[ERROR] 无法定位 JDK 目录")
        return False
    
    print(f"[SUCCESS] JDK 目录: {jdk_home}")
    
    # 设置环境变量
    java_bin = os.path.join(jdk_home, "bin")
    
    # 临时环境变量（当前进程）
    os.environ["JAVA_HOME"] = jdk_home
    os.environ["PATH"] = java_bin + ";" + os.environ["PATH"]
    
    # 永久环境变量（系统级）
    try:
        print("\n设置永久环境变量...")
        os.system(f'setx JAVA_HOME "{jdk_home}" /M')
        
        # 更新 PATH
        result = subprocess.run(['powershell', '-Command', 
                               '[Environment]::GetEnvironmentVariable("PATH", "Machine")'], 
                              capture_output=True, text=True)
        current_path = result.stdout.strip()
        if java_bin not in current_path:
            new_path = current_path + ";" + java_bin
            os.system(f'powershell -Command [Environment]::SetEnvironmentVariable("PATH", "{new_path}", "Machine")')
            print("[SUCCESS] 永久环境变量已设置")
    except Exception as e:
        print(f"[ERROR] 设置永久环境变量失败: {e}")
        print("请手动设置:")
        print(f"JAVA_HOME={jdk_home}")
        print(f"PATH 追加: {java_bin}")
    
    return True

def test_java():
    print("\n测试 Java 安装...")
    try:
        result = subprocess.run(["java", "-version"], capture_output=True, text=True)
        print("[SUCCESS] Java 版本信息:")
        print(result.stderr)
        return True
    except Exception as e:
        print(f"[ERROR] 测试失败: {e}")
        return False

if __name__ == "__main__":
    print("=" * 60)
    print("BellSoft Liberica JDK 17 安装工具（腾讯云国内镜像）")
    print("=" * 60)
    
    zip_path = download_jdk()
    if not zip_path:
        exit(1)
    
    if install_jdk(zip_path):
        if test_java():
            print("\n" + "=" * 60)
            print("[SUCCESS] Java 安装完成！")
            print("=" * 60)
            print(f"JAVA_HOME: {os.environ.get('JAVA_HOME')}")
            print(f"Java bin: {os.path.join(os.environ.get('JAVA_HOME'), 'bin')}")
            print("\n现在可以安装 apktool 和 apksigner 了")
        else:
            print("\n[ERROR] Java 测试失败")
    else:
        print("\n[ERROR] 安装失败")
