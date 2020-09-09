package com.hieuvp.fingerprint;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.biometrics.BiometricPrompt.AuthenticationCallback;
import android.hardware.fingerprint.Fingerprint;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
//import androidx.biometric.BiometricPrompt;
//import androidx.biometric.BiometricManager;
//import androidx.biometric.BiometricPrompt.PromptInfo;
import android.hardware.biometrics.BiometricPrompt;
import android.hardware.biometrics.BiometricManager;
import android.hardware.biometrics.BiometricPrompt.CryptoObject;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import androidx.annotation.RequiresApi;
import androidx.fragment.app.FragmentActivity;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.UiThreadUtil;

// for Samsung/MeiZu compat, Android v16-23
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule.RCTDeviceEventEmitter;
import android.hardware.fingerprint.FingerprintManager;

import com.wei.android.lib.fingerprintidentify.FingerprintIdentify;
import com.wei.android.lib.fingerprintidentify.aosp.FingerprintManagerCompat;
import com.wei.android.lib.fingerprintidentify.base.BaseFingerprint.ExceptionListener;
import com.wei.android.lib.fingerprintidentify.base.BaseFingerprint.IdentifyListener;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import android.os.Looper;
import com.google.gson.Gson;

//@ReactModule(name="ReactNativeFingerprintScanner")
public class ReactNativeFingerprintScannerModule
        extends ReactContextBaseJavaModule
        implements LifecycleEventListener {
    public static final int MAX_AVAILABLE_TIMES = Integer.MAX_VALUE;
    public static final String TYPE_BIOMETRICS = "Biometrics";
    public static final String TYPE_FINGERPRINT_LEGACY = "Fingerprint";
    private static final String KEY_NAME = "BiometricPromptApi28";

    private final ReactApplicationContext mReactContext;
    ReactContextBaseJavaModule getCurrentActivity;
    private BiometricPrompt biometricPrompt;
    private Signature mSignature;
    // for Samsung/MeiZu compat, Android v16-23
    private FingerprintIdentify mFingerprintIdentify;
    private CancellationSignal mCancellationSignal;


    public ReactNativeFingerprintScannerModule(ReactApplicationContext reactContext) {
        super(reactContext);
        mReactContext = reactContext;

        try {
            mSignature = initSignature(KEY_NAME);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getName() {
        return "ReactNativeFingerprintScanner";
    }

    @Override
    public void onHostResume() {
    }

    @Override
    public void onHostPause() {
    }

    @Override
    public void onHostDestroy() {
        this.release();
    }

    private boolean requiresLegacyAuthentication() {
        return Build.VERSION.SDK_INT <= 23;
    }
    @RequiresApi(api = Build.VERSION_CODES.P)
    public class AuthCallback extends AuthenticationCallback {
        private Promise promise;

        public AuthCallback(final Promise promise) {
            super();
            this.promise = promise;
        }

        @Override
        public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
            super.onAuthenticationError(errorCode, errString);

            if (errorCode == 7) {
                this.promise.reject("DeviceLocked", TYPE_BIOMETRICS);
            } else {
                this.promise.reject("SystemError", (String) errString);
            }
        }

        @Override
        public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
            //result.getId().getBiometricId()
            super.onAuthenticationSucceeded(result);
            String id = getFingerId();

            if(id == null){
                this.promise.resolve(true);
            }else {
                this.promise.resolve(id);
            }
        }


        @Override
        public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
            int code = helpCode;
            CharSequence str = helpString;
        }

        @Override
        public void onAuthenticationFailed() {
            this.promise.reject("AuthenticationNotMatch", TYPE_BIOMETRICS);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private SecretKey createKey() throws NoSuchProviderException, NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        String algorithm = KeyProperties.KEY_ALGORITHM_AES;
        String provider = "AndroidKeyStore";
        KeyGenerator keyGenerator = KeyGenerator.getInstance(algorithm, provider);
        KeyGenParameterSpec keyGenParameterSpec = new KeyGenParameterSpec.Builder("MY_KEY", KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                .setUserAuthenticationRequired(true)
                .build();

        keyGenerator.init(keyGenParameterSpec);
        return keyGenerator.generateKey();
    }

    private Cipher getEncryptCipher(Key key) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        String algorithm = KeyProperties.KEY_ALGORITHM_AES;
        String blockMode = KeyProperties.BLOCK_MODE_CBC;
        String padding = KeyProperties.ENCRYPTION_PADDING_PKCS7;
        Cipher cipher = Cipher.getInstance(algorithm+"/"+blockMode+"/"+padding);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher;
    }
    private void biometricAuthenticate(final String title, final String subtitle, final String description, final String cancelButton, final Promise promise) {
        mReactContext.addLifecycleEventListener(this);
        UiThreadUtil.runOnUiThread(
                new Runnable() {
                    @RequiresApi(api = Build.VERSION_CODES.Q)
                    @Override
                    public void run() {
                        FragmentActivity fragmentActivity = (FragmentActivity) getCurrentActivity();
                        if (fragmentActivity == null) return;
                        BiometricPrompt promptInfo = new BiometricPrompt.Builder(fragmentActivity)
                                .setTitle(title)
                                .setSubtitle(subtitle)
                                .setDescription(description)
                                .setNegativeButton(cancelButton, fragmentActivity.getMainExecutor(), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        promise.reject("UserFallback", "身份验证已取消");
                                        mCancellationSignal.cancel();
                                    }
                                })
                                .setConfirmationRequired(false)
                                .setDeviceCredentialAllowed(false)
                                .build();

                        //promptInfo.setActiveUser(6);
                        mCancellationSignal = new CancellationSignal();
                        mCancellationSignal.setOnCancelListener(new CancellationSignal.OnCancelListener() {
                            @Override
                            public void onCancel() {
                                promise.reject("UserCancel", "身份验证已取消");
                            }
                        });

                        try {

                            CryptoObject cryptoObject = new CryptoObject(getEncryptCipher(createKey()));
                            //fragmentActivity.getMainExecutor()
                            promptInfo.authenticate(cryptoObject,mCancellationSignal,
                                    fragmentActivity.getMainExecutor(), new AuthCallback(promise));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    // 以下常量在 BiometricPrompt 和 BiometricManager 中是一致的
    private String biometricPromptErrName(int errCode) {
        switch (errCode) {
            case BiometricPrompt.BIOMETRIC_ERROR_CANCELED:
                return "SystemCancel";
            case BiometricPrompt.BIOMETRIC_ERROR_HW_NOT_PRESENT:
                return "FingerprintScannerNotSupported";
            case BiometricPrompt.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                return "FingerprintScannerNotAvailable";
            case BiometricPrompt.BIOMETRIC_ERROR_LOCKOUT:
                return "DeviceLocked";
            case BiometricPrompt.BIOMETRIC_ERROR_LOCKOUT_PERMANENT:
                return "DeviceLockedPermanent";
            case BiometricPrompt.BIOMETRIC_ERROR_NO_BIOMETRICS:
                return "FingerprintScannerNotEnrolled";
            case BiometricPrompt.BIOMETRIC_ERROR_NO_DEVICE_CREDENTIAL:
                return "PasscodeNotSet";
            case BiometricPrompt.BIOMETRIC_ERROR_NO_SPACE:
                return "DeviceOutOfMemory";
            case BiometricPrompt.BIOMETRIC_ERROR_TIMEOUT:
                return "AuthenticationTimeout";
            case BiometricPrompt.BIOMETRIC_ERROR_UNABLE_TO_PROCESS:
                return "AuthenticationProcessFailed";
            case BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED:  // 实际上是“用户选择了另一种身份验证方法”
                return "UserCancel";
            case BiometricPrompt.BIOMETRIC_ERROR_VENDOR:
                // 特定于硬件的错误代码
                return "HardwareError";
            default:
                return "FingerprintScannerUnknownError";
        }
    }

    private String getSensorError() {
        BiometricManager biometricManager = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            biometricManager = mReactContext.getSystemService(BiometricManager.class);

            int authResult = biometricManager.canAuthenticate();
            if (authResult == BiometricManager.BIOMETRIC_SUCCESS) {
                return null;
            }
            if (authResult == BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE) {
                return "FingerprintScannerNotSupported";
            } else if (authResult == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {
                return "FingerprintScannerNotEnrolled";
            } else if (authResult == BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE) {
                return "FingerprintScannerNotAvailable";
            }
        }
        return null;
    }

    @ReactMethod
    public void authenticate(String title, String subtitle, String description, String cancelButton, final Promise promise) {
        if (requiresLegacyAuthentication()) {
            legacyAuthenticate(promise);
        } else {
            final String errorName = getSensorError();
            if (errorName != null) {
                promise.reject(errorName, TYPE_BIOMETRICS);
                ReactNativeFingerprintScannerModule.this.release();
                return;
            }

            biometricAuthenticate(title, subtitle, description, cancelButton, promise);
        }
    }

    @ReactMethod
    public void release() {
        if (requiresLegacyAuthentication()) {
            getFingerprintIdentify().cancelIdentify();
            mFingerprintIdentify = null;
        }

        // consistent across legacy and current API
//        if (biometricPrompt != null) {
//            biometricPrompt.cancelAuthentication();  // if release called from eg React
//        }
        biometricPrompt = null;
        mReactContext.removeLifecycleEventListener(this);
    }

    //是否支持指纹登录
    @ReactMethod
    public void isSensorAvailable(final Promise promise) {
        if (requiresLegacyAuthentication()) {
            String errorMessage = legacyGetErrorMessage();
            if (errorMessage != null) {
                promise.reject(errorMessage, TYPE_FINGERPRINT_LEGACY);
            } else {
                promise.resolve(TYPE_FINGERPRINT_LEGACY);
            }
            return;
        }

        // current API
        String errorName = getSensorError();
        if (errorName != null) {
            promise.reject(errorName, TYPE_BIOMETRICS);
        } else {
            promise.resolve(TYPE_BIOMETRICS);
        }
    }

    @SuppressLint("HandlerLeak")
    @ReactMethod
    public void testFingerprints(final Promise promise) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try{
                @SuppressLint("ServiceCast") FingerprintManager fm = (FingerprintManager) mReactContext.getSystemService(Context.FINGERPRINT_SERVICE);
                CancellationSignal mCancellationSignal = new CancellationSignal();
                mCancellationSignal.setOnCancelListener(new CancellationSignal.OnCancelListener() {
                    @Override
                    public void onCancel() {
                        promise.reject("UserCancel", "身份验证已取消");
                    }
                });
                FragmentActivity fragmentActivity = (FragmentActivity) getCurrentActivity();
                CryptoObject cryptoObject = new CryptoObject(getEncryptCipher(createKey()));
                Handler handler = new Handler(){
                    @Override
                    public void handleMessage(Message msg) {
                        switch (msg.what) {
                            case 102:
                                break;
                        }
                    }
                };
                FingerprintManager.AuthenticationCallback mSelfCancelled = new FingerprintManager.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationError(int errorCode, CharSequence errString) {
                        //多次指纹密码验证错误后，进入此方法；并且，不可再验（短时间）
                        //errorCode是失败的次数
                        //ToastUtils.show(mContext, "尝试次数过多，请稍后重试", 3000);
                    }

                    @Override
                    public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
                        //指纹验证失败，可再验，可能手指过脏，或者移动过快等原因。
                    }

                    @Override
                    public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
                        //指纹密码验证成功
                        int i = 0;
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        //指纹验证失败，指纹识别失败，可再验，错误原因为：该指纹不是系统录入的指纹。
                    }
                };
                fm.authenticate(null, mCancellationSignal, 55, mSelfCancelled, handler);
            }catch (Exception e){

            }
        }
    }

    //获取指纹信息列表
    @ReactMethod
    public void getEnrolledFingerprints(final Promise promise) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            @SuppressLint("ServiceCast") FingerprintManager fm = (FingerprintManager) mReactContext.getSystemService(Context.FINGERPRINT_SERVICE);

            List<Fingerprint> li = fm.getEnrolledFingerprints();
            WritableArray arr = Arguments.createArray();

            for (Fingerprint item: li) {
                WritableMap subMap = Arguments.createMap();
                Parcel out = Parcel.obtain();
                item.writeToParcel(out,0);
                out.setDataPosition(0);

                String name = out.readString();
                int mBiometricId = out.readInt();
                long deviceId = out.readLong();
                int mGroupId = out.readInt();

                subMap.putString("name", name);
                subMap.putInt("groupId", mGroupId);
                subMap.putString("deviceId", String.valueOf(deviceId));
                subMap.putInt("biometricId", mBiometricId);

                arr.pushMap(subMap);
            }
            WritableMap map = Arguments.createMap();
            map.putString("tip", "指纹名（name）：有可能 和 系统不一致。");
            map.putArray("result", arr);
            map.putInt("size", li.size());
            promise.resolve(map);
        }else {
            promise.reject("VersionError", TYPE_BIOMETRICS);
        }
    }

    /**
     * 指纹加密（MD5），判断是否发生更改。
     * @param promise
     */
    @ReactMethod
    public void getFingerEncodeByMD5(final Promise promise) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            @SuppressLint("ServiceCast") FingerprintManager fm = (FingerprintManager) mReactContext.getSystemService(Context.FINGERPRINT_SERVICE);

            List<Fingerprint> li = fm.getEnrolledFingerprints();
            WritableArray arr = Arguments.createArray();

            for (Fingerprint item : li) {
                WritableMap subMap = Arguments.createMap();
                Parcel out = Parcel.obtain();
                item.writeToParcel(out, 0);
                out.setDataPosition(0);

                String name = out.readString();
                int mBiometricId = out.readInt();
                long deviceId = out.readLong();
                int mGroupId = out.readInt();

                subMap.putString("name", name);
                subMap.putString("deviceId", String.valueOf(deviceId));
                subMap.putString("biometricId", String.valueOf(mBiometricId));

                arr.pushMap(subMap);
            }
            ArrayList list = arr.toArrayList();
            String content = new Gson().toJson(list);

            String result = EncryptUtil.encode(content);
            promise.resolve(result);
        }else {
            promise.reject("VersionError", TYPE_BIOMETRICS);
        }
    }
    private String getFingerId() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            @SuppressLint("ServiceCast") FingerprintManager fm = (FingerprintManager) mReactContext.getSystemService(Context.FINGERPRINT_SERVICE);

            List<Fingerprint> li = fm.getEnrolledFingerprints();
            if (li.size() == 1){
                for (Fingerprint item : li) {
                    Parcel out = Parcel.obtain();
                    item.writeToParcel(out, 0);
                    out.setDataPosition(0);

                    String name = out.readString();
                    int mBiometricId = out.readInt();
                    long deviceId = out.readLong();
                    int mGroupId = out.readInt();

                    return String.valueOf(mBiometricId);
                }
            }else {
                return null;
            }
        }else {
            return null;
        }
        return null;
    }

    @Nullable
    private Signature initSignature (String keyName) throws Exception {
        KeyPair keyPair = getKeyPair(keyName);

        if (keyPair != null) {
            Signature signature = Signature.getInstance("SHA256withECDSA");
            signature.initSign(keyPair.getPrivate());
            return signature;
        }
        return null;
    }

    @Nullable
    private KeyPair getKeyPair(String keyName) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        if (keyStore.containsAlias(keyName)) {
            // Get public key
            PublicKey publicKey = keyStore.getCertificate(keyName).getPublicKey();
            // Get private key
            PrivateKey privateKey = (PrivateKey) keyStore.getKey(keyName, null);
            // Return a key pair
            return new KeyPair(publicKey, privateKey);
        }
        return null;
    }
    // Samsung/MeiZu 兼容, Android v16-23
    private FingerprintIdentify getFingerprintIdentify() {
        if (mFingerprintIdentify != null) {
            return mFingerprintIdentify;
        }
        mReactContext.addLifecycleEventListener(this);
        mFingerprintIdentify = new FingerprintIdentify(mReactContext);
        mFingerprintIdentify.setSupportAndroidL(true);
        mFingerprintIdentify.setExceptionListener(
            new ExceptionListener() {
                @Override
                public void onCatchException(Throwable exception) {
                    mReactContext.removeLifecycleEventListener(ReactNativeFingerprintScannerModule.this);
                }
            }
        );
        mFingerprintIdentify.init();
        return mFingerprintIdentify;
    }
    //Android v16-23，检测硬件是否支持指纹
    private String legacyGetErrorMessage() {
        if (!getFingerprintIdentify().isHardwareEnable()) {
            return "FingerprintScannerNotSupported";
        } else if (!getFingerprintIdentify().isRegisteredFingerprint()) {
            return "FingerprintScannerNotEnrolled";
        } else if (!getFingerprintIdentify().isFingerprintEnable()) {
            return "FingerprintScannerNotAvailable";
        }

        return null;
    }


    private void legacyAuthenticate(final Promise promise) {
        final String errorMessage = legacyGetErrorMessage();
        if (errorMessage != null) {
            promise.reject(errorMessage, TYPE_FINGERPRINT_LEGACY);
            ReactNativeFingerprintScannerModule.this.release();
            return;
        }

        getFingerprintIdentify().resumeIdentify();
        getFingerprintIdentify().startIdentify(MAX_AVAILABLE_TIMES, new IdentifyListener() {
            @Override
            public void onSucceed() {
                String id = getFingerId();
                if(id == null){
                    promise.resolve(true);
                }else {
                    promise.resolve(id);
                }
            }

            @Override
            public void onNotMatch(int availableTimes) {
                if (availableTimes <= 0) {
                    //mReactContext.getJSModule(RCTDeviceEventEmitter.class).emit("FINGERPRINT_SCANNER_AUTHENTICATION", "DeviceLocked");
                    promise.reject("DeviceLocked", TYPE_FINGERPRINT_LEGACY);
                } else {
                    //mReactContext.getJSModule(RCTDeviceEventEmitter.class).emit("FINGERPRINT_SCANNER_AUTHENTICATION", "AuthenticationNotMatch");
                    promise.reject("AuthenticationNotMatch", TYPE_FINGERPRINT_LEGACY);
                }
            }

            @Override
            public void onFailed(boolean isDeviceLocked) {
                if(isDeviceLocked){
                    promise.reject("DeviceLockedPermanent", TYPE_FINGERPRINT_LEGACY);
                } else {
                    promise.reject("AuthenticationFailed", TYPE_FINGERPRINT_LEGACY);
                }
                ReactNativeFingerprintScannerModule.this.release();
            }

            @Override
            public void onStartFailedByDeviceLocked() {
                // 第一次启动失败，因为该设备被暂时锁定
                promise.reject("DeviceLocked", TYPE_FINGERPRINT_LEGACY);
            }
        });
    }
}