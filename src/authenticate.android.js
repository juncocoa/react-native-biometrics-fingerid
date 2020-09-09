import {
  DeviceEventEmitter,
  NativeModules,
  Platform,
} from 'react-native';
import createError from './createError';

const { ReactNativeFingerprintScanner } = NativeModules;

const authCurrent = (title, subTitle, description, cancelButton, resolve, reject) => {
  ReactNativeFingerprintScanner.authenticate(title, subTitle, description, cancelButton)
    .then((result) => {
      resolve(result);
    })
    .catch((error) => {
      // 错误翻译
      reject(createError(error.code, error.message));
    });
}

const authLegacy = (resolve, reject) => {
  ReactNativeFingerprintScanner.authenticate()
    .then((result) => {
      resolve(result);
    })
    .catch((error) => {
      reject(createError(error.code, error.message));
    });
}

export default ({ title, subTitle, description, cancelButton }) => {
  return new Promise((resolve, reject) => {
    if (!title) {
      title = description ? description : "Log In";
      description = ""
    }
    if (!subTitle) {
      subTitle = "";
    }
    if (!description) {
      description = "";
    }
    if (!cancelButton) {
      cancelButton = "Cancel";
    }

    if (Platform.Version < 23) {
      return authLegacy(resolve, reject);
    }

    return authCurrent(title, subTitle, description, cancelButton, resolve, reject);
  });
}
