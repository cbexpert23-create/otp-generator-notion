package com.mirusystems.otp;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class OneTimePassword {
    private static final String TAG = "OneTimePassword";

    public static final int SUCCESS = 0;
    public static final int ERR_CHECKSUM = -1;
    public static final int ERR_SEED = -2;
    public static final int ERR_PERMISSION = -3;
    public static final int ERR_DEVICE = -4;

    public static final int BVVD = 1;
    public static final int PCOS = 2;
    public static final int RTS = 3;

    public static final int ADMIN = 0;
    public static final int SUPER_ADMIN = 5;

    private Context context;
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;

    public OneTimePassword(Context context) {
        this.context = context;
        preferences = context.getSharedPreferences("otp", Context.MODE_PRIVATE);
        editor = preferences.edit();
    }

    public String generatePassword(String seed, int deviceId, int permission) throws OneTimePasswordException {
        if (seed == null) {
            throw new OneTimePasswordException("Seed must not be null.");
        }
        if (seed.length() != 8) {
            throw new OneTimePasswordException("The length of the seed must be 8.");
        }
        try {
            long s = Long.parseLong(seed);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            throw new OneTimePasswordException("The seed must consist only of numbers.");
        }
        if (!isSupportedDevice(deviceId)) {
            throw new OneTimePasswordException("The deviceId is not supported");
        }
        if (!isSupportedPermission(permission)) {
            throw new OneTimePasswordException("The permission is not supported");
        }

        return generatePasswordJni(seed, deviceId, permission);
    }

    public boolean checkPassword(String password, String seed, int deviceId, int permission) throws OneTimePasswordException {
        if (password == null) {
            throw new OneTimePasswordException("Password must not be null.");
        }
        if (password.length() != 10) {
            throw new OneTimePasswordException("The length of the password must be 10.");
        }
        if (isUsedPassword(password)) {
            throw new OneTimePasswordException("The password has already been used.");
        }
        if (!isUpperCase(password)) {
            throw new OneTimePasswordException("Passwords should only use uppercase letters.");
        }
        if (seed == null) {
            throw new OneTimePasswordException("Seed must not be null.");
        }
        if (seed.length() != 8) {
            throw new OneTimePasswordException("The length of the seed must be 8.");
        }
        try {
            long s = Long.parseLong(seed);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            throw new OneTimePasswordException("The seed must consist only of numbers.");
        }
        if (!isSupportedDevice(deviceId)) {
            throw new OneTimePasswordException("The deviceId is not supported");
        }
        if (!isSupportedPermission(permission)) {
            throw new OneTimePasswordException("The permission is not supported");
        }

        int ret = checkPasswordJni(password, seed, deviceId, permission);
        if (ret == SUCCESS) {
            addUsedPassword(password);
            return true;
        } else {
            return false;
        }
    }

    public String getSeed(String password) throws OneTimePasswordException {
        if (password == null) {
            throw new OneTimePasswordException("Password must not be null.");
        }
        if (password.length() != 10) {
            throw new OneTimePasswordException("The length of the password must be 10.");
        }
        if (!isUpperCase(password)) {
            throw new OneTimePasswordException("Passwords should only use uppercase letters.");
        }

        String ret = getSeedJni(password);
        if (ret == null) {
            throw new OneTimePasswordException("Checksum is not matched.");
        }
        Log.v(TAG, "getSeed: ret = " + ret);
        return ret;
    }

    private boolean isUsedPassword(String password) {
        return preferences.contains(password);
    }

    private void addUsedPassword(String password) {
        editor.putBoolean(password, true);
        editor.apply();
    }

    private boolean isSupportedDevice(int deviceId) {
        return deviceId == BVVD || deviceId == PCOS || deviceId == RTS;
    }

    private boolean isSupportedPermission(int permission) {
        return permission == ADMIN || permission == SUPER_ADMIN;
    }

    private static boolean isUpperCase(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isUpperCase(s.charAt(i))) {
                Log.e(TAG, "isUpperCase: " + s.charAt(i));
                return false;
            }
        }
        return true;
    }

    static {
        System.loadLibrary("miruotp");
    }

    private native String generatePasswordJni(String seed, int deviceId, int permission);

    private native int checkPasswordJni(String password, String seed, int deviceId, int permission);

    private native String getSeedJni(String password);
}
