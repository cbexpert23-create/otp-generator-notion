#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "sha256.h"

#define BVVD 1
#define PCOS 2
#define RTS 3
#define PCOS_FLAG 40
#define RTS_FLAG 80

// SHA256 해시 함수 (문자열 입력용)
void sha256_hash_string(const char* input, unsigned char* output) {
    SHA256_CTX ctx;
    sha256_init(&ctx);
    sha256_update(&ctx, (const unsigned char*)input, strlen(input));
    sha256_final(&ctx, output);
}

// 원본 GeneratePassword 알고리즘 사용
void generate_otp(const char* seed, int deviceId, const char* salt, char* output) {
    char num[21] = {0,};
    char processed_seed[9] = {0,};
    
    // RTS의 경우 6자리 시드에 80을 붙여서 8자리로 만듦
    if (deviceId == RTS) {
        sprintf(processed_seed, "%s80", seed);
    } else {
        strcpy(processed_seed, seed);
    }
    
    sprintf(num, "2023OTP%s%02d%s", processed_seed, deviceId, salt);

    unsigned char hash[SHA256_BLOCK_SIZE];
    sha256_hash_string(num, hash);

    for (int i = 0; i < 10; ++i) {
        unsigned char c = hash[i] + hash[i + 10] + hash[i + 20];
        c = c % 10;
        output[i] = c + '0';
    }
    output[10] = '\0';
}
