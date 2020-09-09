import { NativeModules } from 'react-native';

import authenticate from './authenticate';
import isSensorAvailable from './isSensorAvailable';
import release from './release';
import createError from './createError';
const { ReactNativeFingerprintScanner } = NativeModules;

const getEnrolledFingerprints = () => {
  return new Promise((resolve, reject) => {
    ReactNativeFingerprintScanner.getEnrolledFingerprints()
      .then((data) => resolve(data))
      .catch(error => reject(createError(error.code, error.message)));
  })
}
const getFingerEncodeByMD5 = () => {
  return new Promise((resolve, reject) => {
    ReactNativeFingerprintScanner.getFingerEncodeByMD5()
      .then((data) => resolve(data))
      .catch(error => reject(createError(error.code, error.message)));
  })
}
const testFingerprints = () => {
  return new Promise((resolve, reject) => {
    ReactNativeFingerprintScanner.testFingerprints()
      .then((data) => resolve(data))
      .catch(error => reject(createError(error.code, error.message)));
  })
}
export default {
  authenticate,
  release,
  isSensorAvailable,
  getEnrolledFingerprints,
  getFingerEncodeByMD5,
  testFingerprints
};
