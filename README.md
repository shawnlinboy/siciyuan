四次元（原微次元） weiciyuan
=========
Sina Weibo Android App, require Android 4.1+, GPL v3 License

Modified by [shawnlinboy](http://mobilelin.me/) for compiling successfully by Android Studio 1.0.3 with gradle 2.2.1.

截图
--------------
<img width="30%" height="30%" src="https://lh5.ggpht.com/liao4yraseucSncbq9ZOAspCb7xZZ-E7iHsSv3OBGbFwLi6pSys8G4jap132pUmuYQ=h900-rw"/>

<img width="30%" height="30%" src="https://lh5.ggpht.com/hlf2Hy7nyvGZ2l6WV3LEd2IiXVp_xYh76_bPUSEaQf0epRwxx3XA-7dAFjQBiZy7Tw=h900-rw"/>

文档
--------------
https://github.com/qii/weiciyuan/wiki

Gradle 构建
--------------
- 版本
    - 最新 Android SDK
    - Gradle
- 环境变量
    - ANDROID_HOME
    - GRADLE_HOME，同时把bin放入path变量
- Android SDK 安装，都更新到最新
    - Android SDK Build-tools
    - Google Repository
    - Android Support Repository
    - Android Support Library
- 移除配置
    - 移除AndroidManifest.xml里面`com.crashlytics.ApiKey`和GlobalContext的`Crashlytics.start(this)`，以免影响四次元的崩溃统计数据
- 编译
    - `./gradlew assembleDebug`，编译好的apk在build/outputs/apk下面，默认用的是 debug.keystore 签名，可与Google Play上的正式版共存