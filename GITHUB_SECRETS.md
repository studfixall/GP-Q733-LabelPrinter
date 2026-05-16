# GitHub Secrets 配置指南

## 已生成的密钥库信息

- **Keystore 文件**: `keystore.jks`
- **Alias**: `gpq733`
- **Store Password**: `gpq733key`
- **Key Password**: `gpq733key`

## 需要配置的 Secrets

在 GitHub 仓库中设置以下 4 个 Secrets：

### 1. SIGNING_KEY
- **值**: 将 `keystore-base64.txt` 文件内容完整复制粘贴
- **说明**: Keystore 文件的 Base64 编码内容

### 2. ALIAS
- **值**: `gpq733`
- **说明**: 密钥别名

### 3. KEY_STORE_PASSWORD
- **值**: `gpq733key`
- **说明**: Keystore 密码

### 4. KEY_PASSWORD
- **值**: `gpq733key`
- **说明**: 密钥密码

## 配置步骤

1. 打开 https://github.com/studfixall/GP-Q733-LabelPrinter/settings/secrets/actions
2. 点击 "New repository secret"
3. 依次添加上述 4 个 Secret
4. 保存后，下次 push 到 main 分支会自动生成签名的 Release APK

## 注意事项

- `keystore-base64.txt` 文件内容约 3446 字符，需完整复制
- 密钥信息已保存在本地，请勿泄露
- 建议将 `keystore.jks` 备份到安全位置
