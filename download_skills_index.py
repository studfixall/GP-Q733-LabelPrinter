import urllib.request
import ssl
import json

# 禁用SSL验证
ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

url = "https://skillhub-1388575217.cos.ap-guangzhou.myqcloud.com/skills.json"
print(f"Downloading skills index from {url}...")

try:
    with urllib.request.urlopen(url, context=ctx, timeout=30) as response:
        data = json.loads(response.read().decode('utf-8'))
        print(f"Found {len(data.get('skills', []))} skills")
        print("\nSearching for wecom related skills...")
        for skill in data.get('skills', []):
            name = skill.get('name', '').lower()
            slug = skill.get('slug', '').lower()
            desc = skill.get('description', '').lower()
            if 'wecom' in name or 'wecom' in slug or 'wecom' in desc or '微信' in name or '企业' in name:
                print(f"\n  - {skill.get('slug')}")
                print(f"    Name: {skill.get('name')}")
                print(f"    Desc: {skill.get('description', 'N/A')[:80]}")
                print(f"    Version: {skill.get('version', 'N/A')}")
except Exception as e:
    print(f"Error: {e}")
