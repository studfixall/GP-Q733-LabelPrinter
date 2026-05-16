import zipfile
import json
import re

apk_path = 'EnjoyMBM.apk'
report = []

with zipfile.ZipFile(apk_path, 'r') as z:
    report.append('=== UniApp Configuration Analysis ===\n')
    
    # Read manifest.json
    try:
        manifest = z.read('assets/apps/__UNI__092D5A7/www/manifest.json')
        manifest_data = json.loads(manifest.decode('utf-8'))
        report.append('=== manifest.json ===')
        report.append(f"Name: {manifest_data.get('name', 'N/A')}")
        report.append(f"Version: {manifest_data.get('versionName', 'N/A')} ({manifest_data.get('versionCode', 'N/A')})")
        report.append(f"Description: {manifest_data.get('description', 'N/A')}")
        
        # Permissions
        perms = manifest_data.get('permissions', {})
        if perms:
            report.append('\nPermissions:')
            for p in perms:
                report.append(f'  {p}')
        
        # DCloud configs
        dcloud = manifest_data.get('dcloud', {})
        if dcloud:
            report.append(f'\nDCloud configs: {dcloud}')
        
        report.append('')
    except Exception as e:
        report.append(f'Error reading manifest: {e}\n')
    
    # Read dcloud_url.json
    try:
        url_config = z.read('assets/data/dcloud_url.json')
        url_data = json.loads(url_config.decode('utf-8'))
        report.append('=== dcloud_url.json ===')
        report.append(json.dumps(url_data, indent=2, ensure_ascii=False))
        report.append('')
    except Exception as e:
        report.append(f'Error reading dcloud_url.json: {e}\n')
    
    # Read dcloud_control.xml
    try:
        control = z.read('assets/data/dcloud_control.xml')
        report.append('=== dcloud_control.xml ===')
        report.append(control.decode('utf-8', errors='ignore')[:2000])
        report.append('')
    except Exception as e:
        report.append(f'Error reading dcloud_control.xml: {e}\n')
    
    # Read dcloud_properties.xml
    try:
        props = z.read('assets/data/dcloud_properties.xml')
        report.append('=== dcloud_properties.xml ===')
        report.append(props.decode('utf-8', errors='ignore')[:2000])
        report.append('')
    except Exception as e:
        report.append(f'Error reading dcloud_properties.xml: {e}\n')
    
    # Analyze app-service.js for API endpoints
    try:
        app_service = z.read('assets/apps/__UNI__092D5A7/www/app-service.js')
        app_text = app_service.decode('utf-8', errors='ignore')
        
        report.append('=== API Endpoints from app-service.js ===')
        
        # Find URLs
        urls = re.findall(r'https?://[a-zA-Z0-9.-]+(?:/[a-zA-Z0-9./_-]*)?', app_text)
        unique_urls = sorted(set(urls))
        report.append(f'Found {len(unique_urls)} unique URLs:')
        for url in unique_urls[:30]:
            report.append(f'  {url}')
        
        # Find API patterns
        report.append('\n=== API Patterns ===')
        api_patterns = re.findall(r'["\']/api/[a-zA-Z0-9/_-]+["\']', app_text)
        unique_apis = sorted(set(api_patterns))
        for api in unique_apis[:30]:
            report.append(f'  {api}')
        
        report.append('')
    except Exception as e:
        report.append(f'Error reading app-service.js: {e}\n')

# Write report
with open('config_analysis.txt', 'w', encoding='utf-8') as f:
    f.write('\n'.join(report))

print('Config analysis saved to: config_analysis.txt')
