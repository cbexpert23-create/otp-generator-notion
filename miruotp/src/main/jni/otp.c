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

int index_password = 0;
char password[11] = {0,};

void encrypt(long seed) {
    if (seed < 26) {
        password[index_password] = (char) seed + 'A';
        index_password++;
    } else {
        long q = seed / 26;
        long r = seed % 26;
        encrypt(q);
        encrypt(r);
    }
}

long decrypt(const char *password) {
    long seed = 0;
    int length = strlen(password);
    for (int i = 0; i < length; ++i) {
        char c = password[i] - 'A';
        seed = seed + c * (long) pow(26, (length - i - 1));
    }
    return seed;
}

int generateChecksum(long seed) {
    long sum = 0;
    long q;
    long r;
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
                                                             jstring jSeed, jint permission) {
    const char *seed = NULL;
    seed = (*env)->GetStringUTFChars(env, jSeed, 0);
    LOGV("Java_com_mirusystems_otp_OneTimePassword_generatePasswordJni: E, seed = %s", seed);

    int pollingStation = strtoi(&seed[6], 2);
    int pollingCenter = strtoi(&seed[0], 6);

    time_t t = time(NULL);
    struct tm tm = *localtime(&t);
    int day = tm.tm_mday;
    int hour = tm.tm_hour;
    int minute = tm.tm_min;

    int seedLength1 = 15; //    PS PC DD HH MM U
//    int seedLength2 = 16; // CS PS PC DD HH MM U
    int seedLength3 = 14; // CS PS PC UMIN
    char num[16] = {0,};
    sprintf(num, "%02d%06d%02d%02d%02d%d", pollingStation, pollingCenter, day, hour, minute,
            permission);
    num[seedLength1] = '\0';
//    LOGV("num = %s", num);

    int checksum = generateChecksum(atol(num));
//    LOGV("checksum = %d", checksum);

    int totalMinutes = getMinutes(day, hour, minute);
    int s1 = totalMinutes / 10000;
    int s2 = totalMinutes % 10000;
    s1 = s1 + permission;
    sprintf(num, "%1d%02d%06d%1d%04d", checksum, pollingStation, pollingCenter, s1, s2);
    num[seedLength3] = '\0';

    long s = atol(num);
    index_password = 0;
    encrypt(s);
    LOGV("password = %s", password);

    (*env)->ReleaseStringUTFChars(env, jSeed, seed);
    return (*env)->NewStringUTF(env, password);
}

JNIEXPORT jboolean JNICALL
Java_com_mirusystems_otp_OneTimePassword_checkPasswordJni(JNIEnv *env, jobject thiz,
                                                          jstring jPassword, jstring jSeed,
                                                          jint permission) {
    const char *password = NULL;
    const char *seed = NULL;

    password = (*env)->GetStringUTFChars(env, jPassword, 0);
    seed = (*env)->GetStringUTFChars(env, jSeed, 0);
    LOGV("Java_com_mirusystems_otp_OneTimePassword_checkPasswordJni: E, password = %s, seed = %s, permission = %d",
         password, seed, permission);

    long s = decrypt(password);
//    LOGV("checkPasswordJni: s = %ld", s);
    char str[256];
    sprintf(str, "%ld", s);
    int length = strlen(str);
//    LOGV("checkPasswordJni: str = %s, length = %d", str, length);
    int pw_checksum = str[0] - '0';
//    LOGV("pw_checksum = %d", pw_checksum);
    int pw_ps = ctoi(str[2]) + ctoi(str[1]) * 10;
    int pw_pc = ctoi(str[8]) + ctoi(str[7]) * 10 + ctoi(str[6]) * 100 + ctoi(str[5]) * 1000 +
                ctoi(str[4]) * 10000 + ctoi(str[3]) * 100000;
    int f1 = ctoi(str[9]);
    int f2 = ctoi(str[13]) + ctoi(str[12]) * 10 + ctoi(str[11]) * 100 + ctoi(str[10]) * 1000;
//    LOGV("ps = %d, pc = %d, f1 = %d, f2 = %d", pw_ps, pw_pc, f1, f2);
    int pw_permission = 0;
    int totalMinute = 0;
    if (f1 >= 5) {
        pw_permission = 5;
        totalMinute = (f1 - 5) * 10000 + f2;
    } else {
        pw_permission = 0;
        totalMinute = f1 * 10000 + f2;
    }
//    LOGV("pw_permission = %d, totalMinute = %d", pw_permission, totalMinute);
    int day = (totalMinute / MINUTES_IN_DAY) + 1;
    totalMinute = totalMinute - ((day - 1) * MINUTES_IN_DAY);
    int hour = totalMinute / MINUTES_IN_HOUR;
    int minute = totalMinute % MINUTES_IN_HOUR;
//    LOGV("day = %02d, hour = %02d, minute = %02d", day, hour, minute);

    char num[16] = {0,};
    sprintf(num, "%02d%06d%02d%02d%02d%d", pw_ps, pw_pc, day, hour, minute, pw_permission);
    num[15] = '\0';
//    LOGV("num = %s", num);

    int checksum = generateChecksum(atol(num));
//    LOGV("checksum = %d", checksum);
    int pollingStation = strtoi(&seed[6], 2);
    int pollingCenter = strtoi(&seed[0], 6);

    (*env)->ReleaseStringUTFChars(env, jPassword, password);
    (*env)->ReleaseStringUTFChars(env, jSeed, seed);

    if (pw_checksum != checksum) {
//        LOGE("pw_checksum = %d, checksum = %d", pw_checksum, checksum);
        return -1;
    }
    if (pw_pc != pollingCenter) {
//        LOGE("pw_pc = %06d, pollingCenter = %06d", pw_pc, pollingCenter);
        return -2;
    }
    if (pw_ps != pollingStation) {
//        LOGE("pw_ps = %06d, pollingStation = %06d", pw_ps, pollingStation);
        return -2;
    }
    if (pw_permission != permission) {
//        LOGE("pw_permission = %d, permission = %d", pw_permission, permission);
        return -3;
    }

    return 0;
}