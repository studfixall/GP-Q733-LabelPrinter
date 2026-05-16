#!/usr/bin/env python3
"""
下载 Dodo APKSign 工具
"""

import urllib.request
import os

WORK_DIR = r"C:\Users\Administrator\.qclaw\workspace-agent-4d029e48"
OUTPUT_FILE = os.path.join(WORK_DIR, "Dodo_APKSign.zip")

def download():
    urls = [
        "http://www.ddooo.com/softdown/93664.htm",
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
        print("\nDownload failed.")
