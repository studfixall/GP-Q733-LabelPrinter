#!/usr/bin/env python3
"""
下载 360apk签名工具
"""

import urllib.request
import os

WORK_DIR = r"C:\Users\Administrator\.qclaw\workspace-agent-4d029e48"
OUTPUT_FILE = os.path.join(WORK_DIR, "360Signer.zip")

def download():
    # 尝试多个可能的下载链接
    urls = [
        "http://www.kkx.net/soft/down/5666.html",
        "http://www.kkx.net/uploadfile/2020/0929/360Signer_v1.3.zip",
    ]
    
    for url in urls:
        print(f"Trying: {url}")
        try:
            req = urllib.request.Request(url, headers={
                'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
            })
            with urllib.request.urlopen(req, timeout=30) as response:
                data = response.read()
                with open(OUTPUT_FILE, 'wb') as f:
                    f.write(data)
                print(f"Downloaded: {len(data):,} bytes")
                print(f"Saved to: {OUTPUT_FILE}")
                return True
        except Exception as e:
            print(f"  Failed: {e}")
    
    return False

if __name__ == "__main__":
    if download():
        print("\nDownload successful!")
    else:
        print("\nAll download attempts failed.")
        print("Please download manually from: http://www.kkx.net/soft/5666.html")
