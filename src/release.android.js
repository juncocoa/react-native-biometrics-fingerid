import { DeviceEventEmitter, NativeModules, Platform } from 'react-native';

const { ReactNativeFingerprintScanner } = NativeModules;

export default () => {
  ReactNativeFingerprintScanner.release();
}
