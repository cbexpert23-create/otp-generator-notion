

##모듈 추가방법 (프로젝트가 같은 레벨의 디렉토리에 있다고 가정)

###\<project\>/settings.gradle

include ':miruotp'
project(':miruotp').projectDir = new File("../iraq-otp-lib/miruotp")

###\<project\>/app/build.gradle

dependencies {
    implementation project(path: ':miruotp')
}



## 라이브러리 사용방법

### generatePassword

```java
public String generatePassword(String seed, int deviceId, int permission) 
```

#### seed

- BVVD, PCOS: polling center + polling station ex) "105201" + "01" = "10520101"
- RTS: imei 중 가운데 6자리 + "80" ex) "800008" + "80" = "80000880"

#### deviceId

- BVVD: 1
- PCOS: 2
- RTS: 3

#### permission

- ADMIN: 0
- SUPER ADMIN: 5

#### Return Value

영문 대문자 10자

#### exception

- 인자가 null
- 인자의 문자열 조건 맞지 않을 때



### checkPassword

```java
public boolean checkPassword(String password, String seed, int deviceId, int permission)
```

#### password

- 영문 대문자 10자

#### seed

- BVVD, PCOS: polling center + polling station ex) "105201" + "01" = "10520101"
- RTS: imei 중 가운데 6자리 + "80" ex) "800008" + "80" = "80000880"

#### deviceId

- BVVD: 1
- PCOS: 2
- RTS: 3

#### permission

- ADMIN: 0
- SUPER ADMIN: 5

#### Return Value

- true: 패스워드 사용가능
- false: 패스워드 불일치

#### exception

- 인자가 null
- 인자의 문자열 조건 맞지 않을 때
- 이미 사용한 패스워드일 때



## PIN Code 규칙

PIN Code 는 총 16자리의 숫자이다.

Generates a string composed of

DC 1 checksum from the rest of the string

PS 2 polling station

PC 6 polling center

DD 2 day of the month

HH 2 hour

MM 2 minute

AC 1 action (0: admin; 5: superadmin)

The resulting 16-digit string is reduced to 14-digits transforming the six of DDHHMM into five 

calculating the value of the minute within the month and adding CA to the first digit of the calculation result of the minutes. 

The first digit of the minute calculation can not be greater than 4 and AC has a maximum value of 5, then the maximum sum is 9, a single digit.

### checksum

- PS + PC + DD + HH + MM + U
- 각 숫자들의 합을 구하고 합이 1자리 숫자가 될 때까지 합의 숫자들의 합을 구하여 checksum으로 사용

### total minutes

- DD+HH+MM → minuts

- ((DD-1) * (24 * 60)) + (HH * 60) + MM
- MIN: 1일 0시 0분 → 0
- MAX: 31일 23시 59분 → 46079
- SUPER_ADMIN은 50000 추가
  - total minutes가 360 이고 SUPER_ADMIN 권한을 사용한다면 50360
  - total minutes가 360 이고 ADMIN 권한을 사용한다면 00360

### num

- CS + PS + PC + UMIN

### alphabet

