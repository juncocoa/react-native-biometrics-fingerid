# React Native Biometrics FingerId

[![React Native Version](https://img.shields.io/badge/react--native-latest-blue.svg?style=flat-square)](http://facebook.github.io/react-native/releases)
[![Version](https://img.shields.io/npm/v/react-native-biometrics-fingerid.svg)](https://www.npmjs.com/package/react-native-biometrics-fingerid)
[![NPM](https://img.shields.io/npm/dm/react-native-biometrics-fingerid.svg)](https://www.npmjs.com/package/react-native-biometrics-fingerid)

React Native 指纹扫描仪是一个 [React Native](http://facebook.github.io/react-native/) 库，用于使用指纹（TouchID）对用户进行身份验证。
![waPiu9](https://s1.ax1x.com/2020/09/12/waPiu9.jpg)
![waPFBR](https://s1.ax1x.com/2020/09/12/waPFBR.jpg)

### iOS 版本
Touch ID 的使用基于名为 **本地身份验证** 的框架。

它提供了 **默认视图**，提示用户将手指放在 iPhone 的按钮上进行扫描。

### Android 版本
在 4.0.0 版本上，Android >= v23（M）上首选原生 Android BiometricPrompt 库，作为指纹识别。

在 4.0.0 版本后，还弃用了对 Samsung 和 MeiZu 手机的支持的旧版库

3.0.2 以下:
使用可扩展的 Android Fingerprint API 库，该库结合了 [Samsung](http://developer.samsung.com/galaxy/pass#) 和 [MeiZu](http://open-wiki.flyme.cn/index.php?title=%E6%8C%87%E7%BA%B9%E8%AF%86%E5%88%ABAPI) 的官方指纹 API。

Samsung 和 MeiZu 的 Fingerprint SDK 支持大多数系统版本低于 Android 6.0 的设备。
<div>
  <img src="https://github.com/hieuvp/react-native-biometrics-fingerid/raw/master/screenshots/android-availability.png" height="600">
</div>

## 安装

```sh
$ npm install react-native-biometrics-fingerid --save
```

### 自动配置
RN > 0.60 以上
```sh
$ cd ios && pod install
```

RN <= 0.60, 使用 react-native link 添加到您的项目中：
```sh
$ react-native link react-native-biometrics-fingerid
```

### 手动配置

#### iOS

1. 在 XCode 中的项目导航器中，右键单击 `Libraries` ➜ `Add Files to [your project's name]`
2. 找到 `node_modules` ➜ `react-native-biometrics-fingerid` 并且添加 `ReactNativeFingerprintScanner.xcodeproj`
3. 在 XCode 中的项目导航器中，选择您的项目，添加 `libReactNativeFingerprintScanner.a` 到你的工程文件 `Build Phases` ➜ `Link Binary With Libraries`
4. 运行你的项目 (`Cmd+R`)

#### Android

1. 打开 `android/app/src/main/java/[...]/MainApplication.java`
  - 添加 `import com.hieuvp.fingerprint.ReactNativeFingerprintScannerPackage;` 到文件顶部导入包
  - 添加 `new ReactNativeFingerprintScannerPackage()` to the list returned by the `getPackages()` 方法
2. 将以下行附加到 `android/settings.gradle`:
  	```
  	include ':react-native-biometrics-fingerid'
  	project(':react-native-biometrics-fingerid').projectDir = new File(rootProject.projectDir, '../node_modules/react-native-biometrics-fingerid/android')
  	```
3. 将以下行插入到 dependencies 块中 `android/app/build.gradle`:
  	```
    implementation project(':react-native-biometrics-fingerid')
  	```

### App 权限

将以下权限添加到它们各自的文件中：

#### Android
在你的 `AndroidManifest.xml`:

API level 28+ (使用 Android 自带的 BiometricPrompt) ([Reference](https://developer.android.com/reference/android/Manifest.permission#USE_BIOMETRIC))
```xml
<uses-permission android:name="android.permission.USE_BIOMETRIC" />
```

API level 23-28 (使用 Android 自带的 FingerprintCompat) [Reference](https://developer.android.com/reference/android/Manifest.permission#USE_FINGERPRINT))
```xml
<uses-permission android:name="android.permission.USE_FINGERPRINT" />
```

// 弃用 4.0.0
API level <23 (如果需要，Samsung & MeiZu 使用设备特定的本机指纹) [Reference](https://developer.android.com/reference/android/Manifest.permission#USE_FINGERPRINT))
```xml
<uses-permission android:name="android.permission.USE_FINGERPRINT" />
```
#### iOS
在你的 `Info.plist`:

```xml
<key>NSFaceIDUsageDescription</key>
<string>$(PRODUCT_NAME) requires FaceID access to allows you quick and secure access.</string>
```

### 额外配置

1. 确保以下版本均正确无误 `android/app/build.gradle`
    ```
    // API v29 enables FaceId
    android {
        compileSdkVersion 29
        buildToolsVersion "29.0.2"
    ...
        defaultConfig {
          targetSdkVersion 29
    ```

2. 将必要的规则添加到 `android/app/proguard-rules.pro` 如果您使用的是 proguard:
    ```
    # MeiZu Fingerprint
    # DEPRECATED in 4.0.0

    -keep class com.fingerprints.service.** { *; }
    -dontwarn com.fingerprints.service.**

    # Samsung Fingerprint
    # DEPRECATED in 4.0.0

    -keep class com.samsung.android.sdk.** { *; }
    -dontwarn com.samsung.android.sdk.**
    ```

**iOS 展示**
```javascript
import React, { Component } from 'react';
import PropTypes from 'prop-types';
import { AlertIOS } from 'react-native';
import FingerprintScanner from 'react-native-biometrics-fingerid';

class FingerprintPopup extends Component {

  componentDidMount() {
    FingerprintScanner
      .authenticate({ description: 'Scan your fingerprint on the device scanner to continue' })
      .then(() => {
        this.props.handlePopupDismissed();
        AlertIOS.alert('Authenticated successfully');
      })
      .catch((error) => {
        this.props.handlePopupDismissed();
        AlertIOS.alert(error.message);
      });
  }

  render() {
    return false;
  }
}

FingerprintPopup.propTypes = {
  handlePopupDismissed: PropTypes.func.isRequired,
};

export default FingerprintPopup;
```

**Android 展示**
```javascript

import React, { Component } from 'react';
import PropTypes from 'prop-types';
import {
  Alert,
  Image,
  Text,
  TouchableOpacity,
  View,
  ViewPropTypes,
  Platform,
} from 'react-native';

import FingerprintScanner from 'react-native-biometrics-fingerid';
import styles from './FingerprintPopup.component.styles';
import ShakingText from './ShakingText.component';


// - this example component supports both the
//   legacy device-specific (Android < v23) and
//   current (Android >= 23) biometric APIs
// - your lib and implementation may not need both
class BiometricPopup extends Component {
  constructor(props) {
    super(props);
    this.state = {
      errorMessageLegacy: undefined,
      biometricLegacy: undefined
    };

    this.description = null;
  }

  componentDidMount() {
    if (this.requiresLegacyAuthentication()) {
      this.authLegacy();
    } else {
      this.authCurrent();
    }
  }

  componentWillUnmount = () => {
    FingerprintScanner.release();
  }

  requiresLegacyAuthentication() {
    return Platform.Version < 23;
  }

  authCurrent() {
    FingerprintScanner
      .authenticate({ title: 'Log in with Biometrics' })
      .then(() => {
        this.props.onAuthenticate();
      });
  }

  authLegacy() {
    FingerprintScanner
      .authenticate({ onAttempt: this.handleAuthenticationAttemptedLegacy })
      .then(() => {
        this.props.handlePopupDismissedLegacy();
        Alert.alert('Fingerprint Authentication', 'Authenticated successfully');
      })
      .catch((error) => {
        this.setState({ errorMessageLegacy: error.message, biometricLegacy: error.biometric });
        this.description.shake();
      });
  }

  handleAuthenticationAttemptedLegacy = (error) => {
    this.setState({ errorMessageLegacy: error.message });
    this.description.shake();
  };

  renderLegacy() {
    const { errorMessageLegacy, biometricLegacy } = this.state;
    const { style, handlePopupDismissedLegacy } = this.props;

    return (
      <View style={styles.container}>
        <View style={[styles.contentContainer, style]}>

          <Image
            style={styles.logo}
            source={require('./assets/finger_print.png')}
          />

          <Text style={styles.heading}>
            Biometric{'\n'}Authentication
          </Text>
          <ShakingText
            ref={(instance) => { this.description = instance; }}
            style={styles.description(!!errorMessageLegacy)}>
            {errorMessageLegacy || `Scan your ${biometricLegacy} on the\ndevice scanner to continue`}
          </ShakingText>

          <TouchableOpacity
            style={styles.buttonContainer}
            onPress={handlePopupDismissedLegacy}
          >
            <Text style={styles.buttonText}>
              BACK TO MAIN
            </Text>
          </TouchableOpacity>

        </View>
      </View>
    );
  }


  render = () => {
    if (this.requiresLegacyAuthentication()) {
      return this.renderLegacy();
    }

    // current API UI provided by native BiometricPrompt
    return null;
  }
}

BiometricPopup.propTypes = {
  onAuthenticate: PropTypes.func.isRequired,
  handlePopupDismissedLegacy: PropTypes.func,
  style: ViewPropTypes.style,
};

export default BiometricPopup;
```

## API

### `isSensorAvailable()`: (Android, iOS)
检查现在是否可以使用指纹扫描仪。

- 返回一个 `Promise<string>`
- `biometryType: String` - 设备支持的生物特征认证类型。
  - iOS: biometryType = 'Touch ID', 'Face ID'
  - Android: biometryType = 'Biometrics'
- `error: FingerprintScannerError { name, message, biometric }`
  - 失败的 名称 和 消息 以及所使用的生物识别类型。

```javascript
componentDidMount() {
  FingerprintScanner
    .isSensorAvailable()
    .then(biometryType => this.setState({ biometryType }))
    .catch(error => this.setState({ errorMessage: error.message }));
}
```

### `authenticate({ description, fallbackEnabled })`: (iOS)
在 iOS 上启动指纹认证。

- 返回一个 `Promise`
- `description: String` - 解释用户身份验证请求的字符串。
- `fallbackEnabled: Boolean` - 默认 `true`, 是否显示备用按钮(e.g. 输入密码).

```javascript
componentDidMount() {
  FingerprintScanner
    .authenticate({ description: 'Scan your fingerprint on the device scanner to continue' })
    .then(() => {
      this.props.handlePopupDismissed();
      AlertIOS.alert('Authenticated successfully');
    })
    .catch((error) => {
      this.props.handlePopupDismissed();
      AlertIOS.alert(error.message);
    });
}
```

### `authenticate({ title="Log In", subTitle, description, cancelButton="Cancel"`: (Android)
在 Android 上启动指纹认证（如果本机只有一个指纹，则返回指纹ID，否者返回 true）。

- 返回一个 `Promise`
- `title: String` 弹出窗口中的 `标题` 文本
- `subTitle: String` 弹出窗口中的 `副标题` 文本
- `description: String` 弹出窗口中的 `描述` 文本
- `cancelButton: String` 弹出窗口中 `取消按钮` 的文本

```javascript
componentDidMount() {
  if (requiresLegacyAuthentication()) {
    authLegacy();
  } else {
    authCurrent();
  }
}

componentWillUnmount = () => {
  FingerprintScanner.release();
}

requiresLegacyAuthentication() {
  return Platform.Version < 23;
}

authCurrent() {
  FingerprintScanner
    .authenticate({ title: '生物识别 - 登录' })
    .then(() => {
      this.props.onAuthenticate();
    });
}

authLegacy() {
  FingerprintScanner.isSensorAvailable().then((result)=>{
    FingerprintScanner.authenticate({
      title: '验证指纹',
      description: '触摸传感器以验证指纹',
      cancelButton: '使用账号密码登陆'
    }).then((result) => {
      if(result === true){
        alert('指纹验证成功')
      }else {
        alert('指纹 ID：' + result)
      }
    }).catch(err => {
      alert(err.message);
    });
  }).catch(err => {
    alert(err.message)
  })
}
```

### `release()`: (Android)
停止指纹扫描器，释放本机代码中的内存和缓存，并取消本机指纹 UI（如果可见）。

- 返回一个 `Void`

```javascript
componentWillUnmount() {
  FingerprintScanner.release();
}
```

### `getEnrolledFingerprints()`: (Android)
获取系统内部指纹信息，size, result -> (deviceId，biometricId 和 name) 以列表形式返回。

- 返回一个 `Json`

```javascript
FingerprintScanner.getEnrolledFingerprints().then((result)=>{
  //本机所有指纹信息，可调用 getFingerEncodeByMD5 判断指纹发生改变
  alert(JSON.stringify(result));
})
```

### `getFingerEncodeByMD5()`: (Android)
根据本机所有指纹信息，加密生成 MD5，用于判断是否发生改变。

- 返回一个 `Json`

```javascript
FingerprintScanner.getFingerEncodeByMD5().then((result)=>{
  //result：MD5 值，只要添加 或 删除指纹，MD5 值就会发生改变。
  if(result === oldResult){
    //无修改
  }else{
    //已经改变
  }
})
```
### `Biometrics 类型`

| Value | OS | Description|
|---|---|---|
| Touch ID | iOS | |
| Face ID | iOS | |
| Biometrics | Android | 指设备上首选的生物识别类型 |

### `错误描述`

| Name | Message |
|---|---|
| AuthenticationNotMatch | 没有匹配成功。 |
| AuthenticationFailed | 身份验证失败，用户无法提供有效的凭据。 |
| AuthenticationTimeout | 验证失败，因为操作超时。 |
| AuthenticationProcessFailed | 传感器无法处理图像。 请再试一遍。 |
| UserCancel | 身份验证已取消。 |
| UserFallback | 身份验证已取消（点击按钮）。 |
| SystemCancel | 验证已被系统取消-例如 如果在身份验证对话框打开时另一个应用程序出现在前台。 |
| PasscodeNotSet | 由于未在设备上设置密码，因此无法启动身份验证。 |
| DeviceLocked | 身份验证失败，该设备当前处于 30秒 的锁定状态。 |
| DeviceLockedPermanent | 认证失败，必须通过密码解锁设备。 |
| DeviceOutOfMemory | 无法进行身份验证，因为设备上没有足够的可用内存。 |
| HardwareError | 发生硬件错误。 |
| FingerprintScannerUnknownError | 由于未知原因，无法进行身份验证。 |
| FingerprintScannerNotSupported | 设备不支持指纹扫描仪。 |
| FingerprintScannerNotEnrolled  | 由于指纹扫描仪没有注册手指，因此无法启动身份验证。 |
| FingerprintScannerNotAvailable | 由于指纹扫描仪在设备上不可用，因此无法启动身份验证。 |

### 协议（License）

MIT
