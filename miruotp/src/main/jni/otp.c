//
// Created by cyoh on 2019-10-17.
//
#include <errno.h>
#include <stdint.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <malloc.h>
#include <time.h>

#include <jni.h>
#include <android/log.h>
#include <stdlib.h>
#include <math.h>

#define LOG_TAG "otp"
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG  , LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO   , LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN   , LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR  , LOG_TAG, __VA_ARGS__)

#define MINUTES_IN_DAY 1440
#define MINUTES_IN_HOUR 60

#define BVVD 1
#define PCOS 2
#define RTS 3
#define PCOS_FLAG 40
#define RTS_FLAG 80

int ctoi(char c) {
    return c - '0';
}

int strtoi(const char *str, int length) {
    int i;
    int ret = 0;
    for (i = 0; i < length; ++i) {
        ret = ret * 10 + (str[i] - '0');
    }
    return ret;
}

char password[11] = {0,};

void encrypt(long long seed) {
    for (int i = 0; i < 10; ++i) {
        int r = seed % 26;
        seed = seed / 26;
        password[9-i] = (char) r + 'A';
    }
    password[10] = 0;
}

long long decrypt(const char *password) {
    long long seed = 0;
    int length = strlen(password);
    for (int i = 0; i < length; ++i) {
        char c = password[i] - 'A';
        seed = seed + c * (long long) pow(26, (length - i - 1));
    }
    return seed;
}

int generateChecksum(long long seed) {
    long long sum = 0;
    long long q;
    long long r;
    q = seed;
    while (1) {
        r = q % 10;
        q = q / 10;
        sum = sum + r;
        if (q == 0) {
            break;
        }
    }
    if (sum > 9) {
        sum = generateChecksum(sum);
    }

    return sum;
}

int getMinutes(int day, int hour, int minute) {
    int totalMinutes = (day - 1) * MINUTES_IN_DAY + hour * MINUTES_IN_HOUR + minute;
    return totalMinutes;
}

JNIEXPORT jstring JNICALL
Java_com_mirusystems_otp_OneTimePassword_generatePasswordJni(JNIEnv *env, jobject thiz,
                                                             jstring jSeed, jint deviceId,
                                                             jint permission) {
    memset(password, 0, sizeof(password));
    const char *seed = NULL;
    seed = (*env)->GetStringUTFChars(env, jSeed, 0);
//    LOGV("Java_com_mirusystems_otp_OneTimePassword_generatePasswordJni: E, seed = %s, deviceId = %d, permission = %d",
//         seed, deviceId, permission);

    int pollingCenter = strtoi(&seed[0], 6);
    int pollingStation = strtoi(&seed[6], 2);

    time_t t = time(NULL);
    struct tm tm = *localtime(&t);
    int day = tm.tm_mday;
    int hour = tm.tm_hour;
    int minute = tm.tm_min;

    if (deviceId == RTS) {
        pollingStation = RTS_FLAG;
    } else if (deviceId == PCOS) {
        pollingStation = pollingStation + PCOS_FLAG;
    }

    int seedLength1 = 15; //    PS PC DD HH MM U
//    int seedLength2 = 16; // CS PS PC DD HH MM U
    int seedLength3 = 14; // CS PS PC UMIN
    char num[16] = {0,};
    sprintf(num, "%02d%06d%02d%02d%02d%d", pollingStation, pollingCenter, day, hour, minute,
            permission);
    num[seedLength1] = '\0';
//    LOGV("num1 = %s", num);

    int checksum = generateChecksum(atoll(num));
//    LOGV("checksum = %d", checksum);

    int totalMinutes = getMinutes(day, hour, minute);
    int s1 = totalMinutes / 10000;
    int s2 = totalMinutes % 10000;
    s1 = s1 + permission;
    sprintf(num, "%1d%02d%06d%1d%04d", checksum, pollingStation, pollingCenter, s1, s2);
    num[seedLength3] = '\0';
//    LOGV("num3 = %s", num);
    long long s = atoll(num);
//    LOGV("s = %lld", s);
    encrypt(s);
//    LOGV("password = %s", password);

    (*env)->ReleaseStringUTFChars(env, jSeed, seed);
    return (*env)->NewStringUTF(env, password);
}

JNIEXPORT jint JNICALL
Java_com_mirusystems_otp_OneTimePassword_checkPasswordJni(JNIEnv *env, jobject thiz,
                                                          jstring jPassword, jstring jSeed,
                                                          jint deviceId, jint permission) {
    const char *password = NULL;
    const char *seed = NULL;

    password = (*env)->GetStringUTFChars(env, jPassword, 0);
    seed = (*env)->GetStringUTFChars(env, jSeed, 0);
//    LOGV("Java_com_mirusystems_otp_OneTimePassword_checkPasswordJni: E, password = %s, seed = %s, permission = %d",
//         password, seed, permission);

    long long s = decrypt(password);
//    LOGV("checkPasswordJni: s = %lld", s);
    char str[256];
    sprintf(str, "%014lld", s);
    int length = strlen(str);
//    LOGV("checkPasswordJni: str = %s, length = %d", str, length);
    int pw_checksum = str[0] - '0';
//    LOGV("checkPasswordJni: pw_checksum = %d", pw_checksum);
    int pw_ps = ctoi(str[2]) + ctoi(str[1]) * 10;

    int pw_pc = ctoi(str[8]) + ctoi(str[7]) * 10 + ctoi(str[6]) * 100 + ctoi(str[5]) * 1000 +
                ctoi(str[4]) * 10000 + ctoi(str[3]) * 100000;
    int f1 = ctoi(str[9]);
    int f2 = ctoi(str[13]) + ctoi(str[12]) * 10 + ctoi(str[11]) * 100 + ctoi(str[10]) * 1000;
//    LOGV("checkPasswordJni: ps = %d, pc = %d, f1 = %d, f2 = %d", pw_ps, pw_pc, f1, f2);
    int pw_permission = 0;
    int totalMinute = 0;
    if (f1 >= 5) {
        pw_permission = 5;
        totalMinute = (f1 - 5) * 10000 + f2;
    } else {
        pw_permission = 0;
        totalMinute = f1 * 10000 + f2;
    }
//    LOGV("checkPasswordJni: pw_permission = %d, totalMinute = %d", pw_permission, totalMinute);
    int day = (totalMinute / MINUTES_IN_DAY) + 1;
    totalMinute = totalMinute - ((day - 1) * MINUTES_IN_DAY);
    int hour = totalMinute / MINUTES_IN_HOUR;
    int minute = totalMinute % MINUTES_IN_HOUR;
//    LOGV("checkPasswordJni: day = %02d, hour = %02d, minute = %02d", day, hour, minute);

    char num[16] = {0,};
    sprintf(num, "%02d%06d%02d%02d%02d%d", pw_ps, pw_pc, day, hour, minute, pw_permission);
    num[15] = '\0';
//    LOGV("checkPasswordJni: num = %s", num);

    int checksum = generateChecksum(atoll(num));
//    LOGV("checkPasswordJni: checksum = %d", checksum);
    int pw_device_id = 0;
    if (pw_ps == RTS_FLAG) {
        pw_device_id = RTS;
    } else if (pw_ps >= PCOS_FLAG && pw_ps < RTS_FLAG) {
        pw_device_id = PCOS;
        pw_ps -= PCOS_FLAG;
    } else {
        pw_device_id = BVVD;
    }

    int pollingStation = strtoi(&seed[6], 2);
    int pollingCenter = strtoi(&seed[0], 6);

    (*env)->ReleaseStringUTFChars(env, jPassword, password);
    (*env)->ReleaseStringUTFChars(env, jSeed, seed);

    if (pw_checksum != checksum) {
        LOGE("checksum is not matched, checksum = %d", checksum);
        return -1;
    }
    if (pw_pc != pollingCenter) {
        LOGE("pollingCenter is not matched, pollingCenter = %06d", pollingCenter);
        return -2;
    }
    if (pw_ps != pollingStation && deviceId != RTS) {
        LOGE("pollingStation is not matched, pollingStation = %02d", pollingStation);
        return -2;
    }
    if (pw_permission != permission) {
        LOGE("permission is not matched, permission = %d", permission);
        return -3;
    }
    if (pw_device_id != deviceId) {
        LOGE("deviceId is not matched, deviceId = %d", deviceId);
        return -4;
    }

    return 0;
}

JNIEXPORT jstring JNICALL
Java_com_mirusystems_otp_OneTimePassword_getSeedJni(JNIEnv *env, jobject thiz,
                                                    jstring jPassword) {
    jstring result;
    const char *password = NULL;

    password = (*env)->GetStringUTFChars(env, jPassword, 0);
//    LOGV("Java_com_mirusystems_otp_OneTimePassword_getSeedJni: E, password = %s",
//         password);

    long long s = decrypt(password);
//    LOGV("getSeedJni: s = %014lld", s);
    char str[256];
    sprintf(str, "%014lld", s);
    int length = strlen(str);
//    LOGV("getSeedJni: str = %s, length = %d", str, length);
    int pw_checksum = str[0] - '0';
//    LOGV("getSeedJni: pw_checksum = %d", pw_checksum);
    int pw_ps = ctoi(str[2]) + ctoi(str[1]) * 10;

    int pw_pc = ctoi(str[8]) + ctoi(str[7]) * 10 + ctoi(str[6]) * 100 + ctoi(str[5]) * 1000 +
                ctoi(str[4]) * 10000 + ctoi(str[3]) * 100000;
    int f1 = ctoi(str[9]);
    int f2 = ctoi(str[13]) + ctoi(str[12]) * 10 + ctoi(str[11]) * 100 + ctoi(str[10]) * 1000;
//    LOGV("getSeedJni: ps = %d, pc = %d, f1 = %d, f2 = %d", pw_ps, pw_pc, f1, f2);
    int pw_permission = 0;
    int totalMinute = 0;
    if (f1 >= 5) {
        pw_permission = 5;
        totalMinute = (f1 - 5) * 10000 + f2;
    } else {
        pw_permission = 0;
        totalMinute = f1 * 10000 + f2;
    }
//    LOGV("getSeedJni: pw_permission = %d, totalMinute = %d", pw_permission, totalMinute);
    int day = (totalMinute / MINUTES_IN_DAY) + 1;
    totalMinute = totalMinute - ((day - 1) * MINUTES_IN_DAY);
    int hour = totalMinute / MINUTES_IN_HOUR;
    int minute = totalMinute % MINUTES_IN_HOUR;
//    LOGV("getSeedJni: day = %02d, hour = %02d, minute = %02d", day, hour, minute);

    char num[16] = {0,};
    sprintf(num, "%02d%06d%02d%02d%02d%d", pw_ps, pw_pc, day, hour, minute, pw_permission);
    num[15] = '\0';
//    LOGV("getSeedJni: num = %s", num);

    int checksum = generateChecksum(atoll(num));
//    LOGV("getSeedJni: checksum = %d", checksum);
    int pw_device_id = 0;
    if (pw_ps == RTS_FLAG) {
        pw_device_id = RTS;
    } else if (pw_ps >= PCOS_FLAG && pw_ps < RTS_FLAG) {
        pw_device_id = PCOS;
        pw_ps -= PCOS_FLAG;
    } else {
        pw_device_id = BVVD;
    }

    (*env)->ReleaseStringUTFChars(env, jPassword, password);

    if (pw_checksum != checksum) {
        LOGE("checksum is not matched, checksum = %d", checksum);
        return NULL;
    }

    char output[128] = {0};
    sprintf(&output, "%06d%02d", pw_pc, pw_ps);
    result = (*env)->NewStringUTF(env, output);
    return result;
}