import { ToastAndroid } from 'react-native';

const ERRORS = {
  // 传感器可用性
  FingerprintScannerNotSupported: '设备不支持指纹扫描仪。',
  FingerprintScannerNotEnrolled: '由于指纹扫描仪没有注册手指，因此无法启动身份验证。',
  FingerprintScannerNotAvailable: '由于指纹扫描仪在设备上不可用，因此无法启动身份验证。',

  // 身份验证失败
  AuthenticationNotMatch: '没有匹配成功。',
  AuthenticationFailed: '身份验证失败，用户无法提供有效的凭据。',
  AuthenticationTimeout: '验证失败，因为操作超时。',
  AuthenticationProcessFailed: '传感器无法处理图像。 请再试一遍。',
  UserCancel: '身份验证已取消。', //返回键，取消
  UserFallback: '身份验证已取消（取消按键）。', //点击 取消按键
  SystemCancel: '验证已被系统取消-例如 如果在身份验证对话框打开时另一个应用程序出现在前台。',
  PasscodeNotSet: '由于未在设备上设置密码，因此无法启动身份验证。',
  FingerprintScannerUnknownError: '由于未知原因，无法进行身份验证。',
  DeviceLocked: '身份验证失败，该设备当前处于 30秒 的锁定状态。',
  DeviceLockedPermanent: '认证失败，必须通过密码解锁设备。',
  DeviceOutOfMemory: '无法进行身份验证，因为设备上没有足够的可用内存。',
  HardwareError: '发生硬件错误。',
  VersionError: '当前版本不支持此功能。'
};

class FingerprintScannerError extends Error {

  constructor({ name, message, biometric }) {
    super(message);
    this.name = name || this.constructor.name;
    this.biometric = biometric;
    if (typeof Error.captureStackTrace === 'function') {
      Error.captureStackTrace(this, this.constructor);
    } else {
      this.stack = (new Error(message)).stack;
    }
  }
}

export default (name, biometric) => {
  if(name === "SystemError"){
    //这是个系统错误，将返回系统错误
    return new FingerprintScannerError({name, message: biometric, biometric: "Biometrics" });
  }else if(name === "AuthenticationNotMatch" && biometric === "Biometrics"){
    ToastAndroid.showWithGravity(ERRORS["AuthenticationNotMatch"], ToastAndroid.SHORT, ToastAndroid.CENTER);
  }else {
    return new FingerprintScannerError({ name, message: ERRORS[name], biometric });
  }
}
