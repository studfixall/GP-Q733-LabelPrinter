# MEMORY.md
## 项目: GP-Q733 蓝牙标签打印APP
- **仓库**: https://github.com/studfixall/GP-Q733-LabelPrinter
- **技术栈**: Kotlin + MVVM + Jetpack Compose + Hilt + 佳博SDK (rt.printerlibrary)
- **协议**: CPCL(默认)/TSPL/ESC-POS，统一走SDK
- **DPI**: 203 (8 dots/mm)，坐标体系统一为mm
- **CI**: GitHub Actions Build #30+ 成功，JDK 17 temurin

## 关键决策
- 2026-05-17: **统一打印系统** — 删除手写PrintProtocol/PrintService/TestPageGenerator，全部走SDK
- SDK getCpclHeaderCmd(width_mm, height_mm, copies, offset) 内部×8转dots
- SDK getEndCmd() 包含 FORM+PRINT（修复空白出纸）
- 手写CPCL缺少FORM、硬编码font55不支持 — 已通过删除手写路径彻底解决
- 2026-05-17: **签名配置完成** — 新keystore (alias=gp733, password=gp733key)
- GitHub Secrets: SIGNING_KEY, SIGNING_KEY_PASSWORD, SIGNING_STORE_PASSWORD, ALIAS 已配置
- CI Release APK 签名构建成功 ✅ (Run #25979636752)
- 2026-05-17: **修复双连接问题** — commit b2bc207
- 问题：SDK连接和BluetoothRepository连接并存，两套socket互相干扰
- 修复：SDK连接成功后通过 notifyConnectionStateChanged() 通知 repository 更新UI状态
- BluetoothRepositoryImpl 添加 isExternalConnection 标记，disconnect() 时跳过 socket close
- DeviceViewModel.connect(device) 直接传设备参数，不再依赖 selectedDevice StateFlow
- ScanProductViewModel 使用 GpPrinterService.lastConnectedMac 自动重连
- 修复了：需要点两次才连接、断开后打印无反应、扫码打印无反应
- 2026-05-16: 蓝牙短连接模式 — 每次打印前重连、打印后断开

## 签名信息
- Keystore: release-key.jks
- Alias: gp733
- Store Password: gp733key
- Key Password: gp733key

## 工具安装
- gh CLI v2.67.0 安装在 C:\Users\Administrator\bin\gh.exe
- JDK 11 在 C:\Users\Administrator\jdk-11.0.2 (系统默认，不满足项目需求)
- JDK 17 在 C:\Users\Administrator\jdk-17.0.19+10 (项目编译用，JAVA_HOME已持久化注册表)
- JDK 21 在 C:\Users\Administrator\jdk-21.0.11+10
- Git v2.47.0 安装在 C:\Program Files\Git\cmd\git.exe

## 待办 (详见 PROGRESS.md)

### P0 主线跑通
- [ ] 真机蓝牙连接+CPCL打印实测
- [ ] CI Release签名修复 (keystore base64解码)
- [ ] ProductViewModel/ProductManagementScreen/ScanProductViewModel 加mprice
- [ ] sample_products.csv 加mprice列
- [ ] deprecation警告修复

### P1 体验补齐
- [ ] 打印预览精度 (条码真实编码渲染)
- [ ] 模板预览图(base64 PNG)
- [ ] 多列标签/连续打印/打印份数
- [ ] 图片BITMAP打印
- [ ] 打印参数从模板XML读取(density/speed/gap)

### P2 功能增强
- [ ] 标签拖拽编辑器 (大工作量)
- [ ] SQL Server数据源
- [ ] USB/WiFi连接
- [ ] PaperType UI切换入口
## 2026-05-18: 商品管理+反编译Barsoft
- **反编译佳博Barsoft APK** (com.bd.printer.btapp v2.0.2)
- 360加固保护，jadx只能看到壳代码，核心dex被加密
- 获得XML标签模板格式: <Barsoft Version="1.0"> + <items>/<item>
- item属性: viewtype(0=文本,1=条码)、mm坐标、textsize/font/format等
- 80+模板在assets/Label/下
- 字符串资源确认支持ESC/TSC/CPCL三种打印模式
- **Room数据库商品管理**
- ProductEntity + ProductDao + ProductDatabase
- ProductRepositoryImpl从mock改为Room + 首次启动自动预加载示例数据
- ProductViewModel: 搜索/CRUD/CSV导入
- ProductManagementScreen: 列表/搜索/新增编辑删除/CSV文件选择导入
- CsvParser: 支持UTF-8 BOM，期望barcode,name,price,spec,unit,category表头
- Navigation注册ProductManagement路由，HomeScreen加"商品管理"入口

## Barsoft反编译仓库
- **仓库**: https://github.com/studfixall/Barsoft-Reverse (private)
- **本地**: C:\Users\Administrator\Barsoft-Reverse
- **APK来源**: http://www.mayantech.cn/upLoad/ad/Barsoft.apk
- **包名**: com.bd.printer.btapp v2.0.2
- **加固**: 360 jiagu (StubApp + libjiagu so)，核心dex运行时解密
- **关键发现**:
- XML模板格式: <Barsoft Version="1.0"> root, width/height/gap/speed/density属性, <items>/<item>子节点
- item属性: viewtype(0=文本,1=条码,2=线条)、mm坐标(left/top/width/height)、textsize/font/format(CODE_128等)
- 97个XML标签模板，5个分类(normal/price/clothe/jewelry/custome)
- 支持ESC(票据)/TSC(标签)/CPCL(面单)三种打印模式
- 蓝牙(BLE+经典)/USB/WiFi连接
- Activity: MainAct/Splash/BluetoothList/TextEdit/BarCodeEdit/LineEdit/RectEdit/DateEdit/ExcelTable
- Native: libgpequipment.so (arm64/armeabi/x86/x86_64)
- 字体: SourceHanSansCN-Regular.ttf (思源黑体)
- 依赖: Apache POI(Excel)、jTDS(SQL Server)、MaterialDialogs

## 经验与决策
- 用户明确要求升级Windows Server 2016至Windows Server 2022
- JDK 17安装路径是 jdk-17.0.19+10 不是 jdk-17，JAVA_HOME已持久化注册表
- APK 360加固无法直接jadx反编译核心代码，需Frida运行时dump dex
- 用户明确要求在原版Barsoft APK里加入CPCL模式，不是在GP-Q733里另写，脱壳是必须的
- BlackDex脱壳不可用，adb内存dump无root失败，脱壳卡点：手机未连adb

## 用户身份与偏好

- 使用红米K90手机，MIUI，无root

## 当前项目与关注

- 目标是在原版Barsoft APK里加入CPCL打印模式，需脱壳
- 正在补齐GP-Q733模板→数据填充→预览→打印核心链路
