package com.mirusystems.otp;

public class Utils {
    private static final String TAG = "Utils";

    static String generatePassword(String seed, int deviceId, String salt) throws OneTimePasswordException {
        return GeneratePassword(seed, deviceId, salt);
    }

    static boolean checkPassword(String password, String seed, int deviceId, String salt) throws OneTimePasswordException {
        String compare = generatePassword(seed, deviceId, salt);
        return password.equals(compare);
    }

    static {
        System.loadLibrary("miruotp");
    }

    private static native String GeneratePassword(String seed, int deviceId, String salt);
}
