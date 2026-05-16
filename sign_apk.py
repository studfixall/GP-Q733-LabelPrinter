#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
使用 debug 密钥对 APK 进行签名
"""

import os
import subprocess
import shutil

# 路径配置
WORK_DIR = r"C:\Users\Administrator\.qclaw\workspace-agent-4d029e48\apk_work"
APK_INPUT = os.path.join(WORK_DIR, "EnjoyMBM_fixed_final.apk")
APK_OUTPUT = os.path.join(WORK_DIR, "EnjoyMBM_debug.apk")
KEYSTORE = os.path.join(WORK_DIR, "debug.keystore")

# 检查 Java 是否可用
def check_java():
    try:
        result = subprocess.run(["java", "-version"], capture_output=True, text=True)
        print("Java found:")
        print(result.stderr.split('\n')[0])
        return True
    except FileNotFoundError:
        print("Java not found!")
        return False

# 创建 debug 密钥库
def create_keystore():
    if os.path.exists(KEYSTORE):
        print(f"Using existing keystore: {KEYSTORE}")
        return True
    
    print("Creating debug keystore...")
    cmd = [
        "keytool",
        "-genkey", "-v",
        "-keystore", KEYSTORE,
        "-alias", "androiddebugkey",
        "-keyalg", "RSA",
        "-keysize", "2048",
        "-validity", "10000",
        "-dname", "CN=Android Debug,O=Android,C=US",
        "-storepass", "android",
        "-keypass", "android"
    ]
    
    try:
        result = subprocess.run(cmd, capture_output=True, text=True)
        if result.returncode == 0:
            print("Keystore created successfully!")
            return True
        else:
            print(f"Failed to create keystore: {result.stderr}")
            return False
    except FileNotFoundError:
        print("keytool not found! Need JDK installed.")
        return False

# 签名 APK（使用 jarsigner，V1 签名）
def sign_apk_v1():
    print("\nSigning APK with V1 scheme (jarsigner)...")
    
    # 复制 APK
    shutil.copy(APK_INPUT, APK_OUTPUT)
    
    cmd = [
        "jarsigner",
        "-verbose",
        "-sigalg", "SHA1withRSA",
        "-digestalg", "SHA1",
        "-keystore", KEYSTORE,
        "-storepass", "android",
        "-keypass", "android",
        APK_OUTPUT,
        "androiddebugkey"
    ]
    
    try:
        result = subprocess.run(cmd, capture_output=True, text=True)
        if result.returncode == 0:
            print("APK signed successfully!")
            return True
        else:
            print(f"Signing failed: {result.stderr}")
            return False
    except FileNotFoundError:
        print("jarsigner not found! Need JDK installed.")
        return False

# 验证签名
def verify_apk():
    print("\nVerifying APK signature...")
    cmd = ["jarsigner", "-verify", "-verbose", "-certs", APK_OUTPUT]
    
    try:
        result = subprocess.run(cmd, capture_output=True, text=True)
        if "jar verified" in result.stdout.lower():
            print("[OK] APK signature verified!")
            return True
        else:
            print("Signature verification output:")
            print(result.stdout[-500:])  # 最后 500 字符
            return False
    except Exception as e:
        print(f"Verification error: {e}")
        return False

def main():
    print("=" * 60)
    print("APK Debug Signing Tool")
    print("=" * 60)
    
    # 检查输入文件
    if not os.path.exists(APK_INPUT):
        print(f"Input APK not found: {APK_INPUT}")
        return
    
    # 检查 Java
    if not check_java():
        print("\n[ERROR] Java JDK is required but not found.")
        print("Please install JDK from: https://adoptium.net/")
        return
    
    # 创建密钥库
    if not create_keystore():
        print("\n[ERROR] Failed to create keystore.")
        return
    
    # 签名 APK
    if not sign_apk_v1():
        print("\n[ERROR] Failed to sign APK.")
        return
    
    # 验证
    verify_apk()
    
    # 输出信息
    print("\n" + "=" * 60)
    print("Signing complete!")
    print("=" * 60)
    print(f"Output APK: {APK_OUTPUT}")
    print(f"File size: {os.path.getsize(APK_OUTPUT) / 1024 / 1024:.2f} MB")
    print("\n[IMPORTANT] This APK uses debug signature.")
    print("You need to uninstall the original app before installing this one.")

if __name__ == "__main__":
    main()
