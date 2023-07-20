package com.mirusystems.otp;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class OneTimePassword {
    private static final String TAG = "OneTimePassword";

    public static final int SUCCESS = 0;
    public static final int ERR_CHECKSUM = -1;
    public static final int ERR_SEED = -2;
    public static final int ERR_PERMISSION = -3;
    public static final int ERR_DEVICE = -4;

    public static final int VVD = 1;
    public static final int PCOS = 2;
    public static final int RTS = 3;

    public static final int ADMIN = 0;
    public static final int SUPER_ADMIN = 5;

    public static final String ERROR_SEED_NULL = "Seed must not be null.";
    public static final String ERROR_SEED_LENGTH = "The length of the seed must be 10.";
    public static final String ERROR_SEED_NOT_DIGIT = "The seed must consist only of numbers.";
    public static final String ERROR_UNKNOWN_DEVICE_ID = "The deviceId is not supported";
    public static final String ERROR_UNKNOWN_PERMISSION = "The permission is not supported";
    public static final String ERROR_PASSWORD_NULL = "Password must not be null.";
    public static final String ERROR_PASSWORD_LENGTH = "The length of the password must be 10.";
    public static final String ERROR_USED_PASSWORD = "The password has already been used.";
    //    public static final String ERROR_PASSWORD_NOT_UPPERCASE = "Passwords should only use uppercase letters.";
    public static final String ERROR_PASSWORD_NOT_DIGIT = "The password must consist only of numbers.";
    public static final String ERROR_CHECKSUM_NOT_MATCH = "Checksum is not matched.";

    private static final String KEY_USED_PASSWORDS = "used_passwords";
    private static final String KEY_RANDOM_NUMBERS = "random_numbers";
    private static final int RANDOM_NUMBER_SIZE = 100;


    private Context context;
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;
    private Random random = new Random();

    public OneTimePassword(Context context) {
        this.context = context;
        preferences = context.getSharedPreferences("otp", Context.MODE_PRIVATE);
        editor = preferences.edit();

        Log.v(TAG, "OneTimePassword: KEY_RANDOM_NUMBERS = " + preferences.contains(KEY_RANDOM_NUMBERS));
        if (!preferences.contains(KEY_RANDOM_NUMBERS)) {
            resetPassword();
        }
    }

    public String generateRandomNumber() {
        String s = preferences.getString(KEY_RANDOM_NUMBERS, null);
        if (s != null) {
            List<String> numberList = new ArrayList<>(Arrays.asList(s.split(",")));
            if (!numberList.isEmpty()) {
                int index = random.nextInt(numberList.size());
                Log.v(TAG, "generateRandomNumber: index = " + index);
                String number = numberList.get(index);
                numberList.remove(index);
                setRandomNumbers(numberList);
                return number;
            }
        }
        return null;
    }

    private void setRandomNumbers(List<String> randomList) {
        editor.putString(KEY_RANDOM_NUMBERS, String.join(",", randomList));
        editor.apply();
    }

    public String generatePassword(String seed, int deviceId, int permission) throws OneTimePasswordException {
        if (seed == null) {
            throw new OneTimePasswordException(ERROR_SEED_NULL);
        }
        if (seed.length() != 10) { // polling_station_id (8) + random_number (2)
            throw new OneTimePasswordException(ERROR_SEED_LENGTH);
        }
        try {
            long s = Long.parseLong(seed);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            throw new OneTimePasswordException(ERROR_SEED_NOT_DIGIT);
        }
        if (!isSupportedDevice(deviceId)) {
            throw new OneTimePasswordException(ERROR_UNKNOWN_DEVICE_ID);
        }
        if (!isSupportedPermission(permission)) {
            throw new OneTimePasswordException(ERROR_UNKNOWN_PERMISSION);
        }

        return generatePasswordJni(seed, deviceId, permission);
    }

    public boolean checkPassword(String password, String seed, int deviceId, int permission) throws OneTimePasswordException {
        if (password == null) {
            throw new OneTimePasswordException(ERROR_PASSWORD_NULL);
        }
        if (password.length() != 10) {
            throw new OneTimePasswordException(ERROR_PASSWORD_LENGTH);
        }
        if (isUsedPassword(password)) {
            throw new OneTimePasswordException(ERROR_USED_PASSWORD);
        }
        try {
            long s = Long.parseLong(seed);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            throw new OneTimePasswordException(ERROR_PASSWORD_NOT_DIGIT);
        }
        if (seed == null) {
            throw new OneTimePasswordException(ERROR_SEED_NULL);
        }
        if (seed.length() != 10) {
            throw new OneTimePasswordException(ERROR_SEED_LENGTH);
        }
        try {
            long s = Long.parseLong(seed);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            throw new OneTimePasswordException(ERROR_SEED_NOT_DIGIT);
        }
        if (!isSupportedDevice(deviceId)) {
            throw new OneTimePasswordException(ERROR_UNKNOWN_DEVICE_ID);
        }
        if (!isSupportedPermission(permission)) {
            throw new OneTimePasswordException(ERROR_UNKNOWN_PERMISSION);
        }

        int ret = checkPasswordJni(password, seed, deviceId, permission);
        if (ret == SUCCESS) {
            addUsedPassword(password);
            return true;
        } else {
            return false;
        }
    }

    public void resetPassword() {
        Log.v(TAG, "resetPassword: E");
        Map<String, ?> map = preferences.getAll();
        Set<String> keys = map.keySet();
        if (keys != null) {
            for (String key : keys) {
                editor.remove(key);
            }
        }
        editor.apply();
        List<String> randomList = new ArrayList<>();
        for (int i = 0; i < RANDOM_NUMBER_SIZE; i++) {
            randomList.add(String.valueOf(i));
        }
        setRandomNumbers(randomList);
        Log.v(TAG, "resetPassword: X");
    }

    private boolean isUsedPassword(String password) {
        String s = preferences.getString(KEY_USED_PASSWORDS, null);
        if (s != null) {
            List<String> passwords = Arrays.asList(s.split(","));
            return passwords.contains(password);
        }
        return false;
    }

    private List<String> getUsedPasswords() {
        String s = preferences.getString(KEY_USED_PASSWORDS, null);
        if (s != null) {
            return new ArrayList<>(Arrays.asList(s.split(",")));
        }
        return new ArrayList<>();
    }

    private void addUsedPassword(String password) {
        List<String> passwordList = getUsedPasswords();
        if (!passwordList.contains(password)) {
            passwordList.add(password);
        }
        editor.putString(KEY_USED_PASSWORDS, String.join(",", passwordList));
        editor.apply();
    }

    private boolean isSupportedDevice(int deviceId) {
        return deviceId == VVD || deviceId == PCOS || deviceId == RTS;
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

    private static String generatePasswordJni(String seed, int deviceId, int permission) throws OneTimePasswordException {
        String input = String.format(Locale.ENGLISH, "%s%02d%02d", seed, deviceId, permission);
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

    private static int checkPasswordJni(String password, String seed, int deviceId, int permission) throws OneTimePasswordException {
        String compare = generatePasswordJni(seed, deviceId, permission);
        if (password.equals(compare)) {
            return SUCCESS;
        }

        return ERR_CHECKSUM;
    }

    private static byte[] sha256(byte[] input) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return md.digest(input);
    }

//    static {
//        System.loadLibrary("miruotp");
//    }

//    private native String generatePasswordJni(String seed, int deviceId, int permission);
//
//    private native int checkPasswordJni(String password, String seed, int deviceId, int permission);
//
//    private native String getSeedJni(String password);
}
