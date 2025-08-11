# OTP Generator for Notion

Notion에서 사용할 수 있는 OTP(One-Time Password) 생성기입니다.

## 기능

- VVD, PCOS, RTS 디바이스 지원
- WebAssembly 기반 빠른 OTP 생성
- Notion Embed 호환 디자인
- 반응형 UI

## 사용 방법

### 1. GitHub Pages 배포

1. 이 저장소를 GitHub에 업로드
2. Settings > Pages에서 Source를 "Deploy from a branch"로 설정
3. Branch를 "main"으로, folder를 "/ (root)"로 설정
4. 배포 완료 후 제공되는 URL 복사

### 2. Notion에서 사용

1. Notion 페이지에서 `/embed` 입력
2. GitHub Pages URL 입력: `https://[username].github.io/[repository-name]/otp-notion.html`
3. Enter 키로 삽입

### 3. 로컬 테스트

```bash
python3 -m http.server 8000
```

그 후 브라우저에서 `http://localhost:8000/otp-notion.html` 접속

## 파일 구조

- `otp-notion.html` - Notion용 최적화된 OTP 생성기
- `sha256_wasm.js` - WebAssembly 모듈
- `sha256_wasm.wasm` - WebAssembly 바이너리
- `otp_wasm.c` - C 소스 코드

## 예시

- `99010101 / 105` → `239-912-0676`
- `12200102 / 100` → `030-231-2630`
- `10552001 / 225` → `688-222-3794`

## 기술 스택

- HTML5, CSS3, JavaScript
- WebAssembly (Emscripten)
- SHA256 해시 알고리즘

