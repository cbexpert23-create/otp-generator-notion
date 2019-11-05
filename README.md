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