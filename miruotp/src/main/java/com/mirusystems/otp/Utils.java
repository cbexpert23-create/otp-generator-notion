package com.mirusystems.otp;

import android.util.Log;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

public class Utils {
    private static final String TAG = "Utils";

    static String generatePassword(String seed, int deviceId, int permission, String salt) throws OneTimePasswordException {
        String input = String.format(Locale.ENGLISH, "2023OTP%s%02d%02d%sd", seed, deviceId, permission, salt);
        try {
            byte[] hash = sha256(input.getBytes(StandardCharsets.UTF_8));
            BigInteger bigIntegerHash = new BigInteger(1, hash);

            String str = bigIntegerHash.toString(10);
            Log.v(TAG, "generatePasswordJni: str = " + str);
            String[] numbers = new String[3];
            BigInteger sum = BigInteger.ZERO;
            for (int i = 0; i < numbers.length; i++) {
                numbers[i] = str.substring(i * 10, i * 10 + 10);
                Log.v(TAG, "generatePasswordJni: numbers = " + numbers[i]);
                sum = sum.add(new BigInteger(numbers[i]));
            }
            String output = sum.toString(10);
            Log.v(TAG, "generatePasswordJni: output = " + output + ", " + output.length());
            if (output.length() > 10) {
                return output.substring(1);
            } else {
                return output;
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw new OneTimePasswordException(e.getMessage());
        }
    }

    static boolean checkPassword(String password, String seed, int deviceId, int permission, String salt) throws OneTimePasswordException {
        String compare = generatePassword(seed, deviceId, permission, salt);
        return password.equals(compare);
    }

    static byte[] sha256(byte[] input) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return md.digest(input);
    }
}
