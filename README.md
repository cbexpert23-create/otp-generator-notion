#모듈 추가방법 (프로젝트가 같은 레벨의 디렉토리에 있다고 가정)

##\<project\>/settings.gradle

include ':miruotp'
project(':miruotp').projectDir = new File("../iraq-otp-lib/miruotp")

##\<project\>/app/build.gradle

dependencies {
    implementation project(path: ':miruotp')
}