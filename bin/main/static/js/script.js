/* =============================================================
   script.js — 공통 JS: 모달, 탭 전환, 폼 유효성 검사,
               비밀번호 강도, 사업자번호 조회
============================================================= */


/* =============================================================
   Mock 데이터 — 사업자번호 조회용 임시 DB
   실제 구현 시: GET /api/business/verify?number=xxx 로 대체
   응답 형식: { status, last_name, first_name, store_name, address }
============================================================= */
const MOCK_BUSINESS_DB = {
    '1234567890': {
        status: 'ACTIVE',
        last_name: '田中', first_name: '一郎',
        store_name: '田中ベーカリー',
        address: '東京都渋谷区1-1-1'
    },
    '9876543210': {
        status: 'ACTIVE',
        last_name: '山田', first_name: '花子',
        store_name: '山田カフェ',
        address: '大阪府大阪市2-3-4'
    },
    '1111111111': {
        status: 'CLOSED',
        last_name: '鈴木', first_name: '次郎',
        store_name: '鈴木食堂',
        address: '名古屋市3-4-5'
    }
};

/* 사업자번호 인증 완료 여부 */
var bizVerified = false;

/* 현재 모달 모드 ('user' | 'biz') */
var currentMode = 'user';


/* =============================================================
   모달 제어
============================================================= */

/* 모달 열기 — mode: 'user' 또는 'biz' */
function openModal(mode) {
    currentMode = mode || 'user';
    document.body.style.overflow = 'hidden';
    document.getElementById('authOverlay').classList.add('active');
    buildTabs();
    if (currentMode === 'biz') {
        showPanel('panelBizLogin');
    } else {
        showPanel('panelLogin');
    }
}

/* 모달 닫기 */
function closeModal() {
    document.getElementById('authOverlay').classList.remove('active');
    document.body.style.overflow = '';
}

/* 오버레이 클릭 시 닫기 (모달 내부 클릭은 무시) */
function handleOverlayClick(e) {
    if (e.target === document.getElementById('authOverlay')) {
        closeModal();
    }
}

/* ESC 키로 모달 닫기 */
document.addEventListener('keydown', function(e) {
    if (e.key === 'Escape') closeModal();
});


/* =============================================================
   탭 전환
============================================================= */

/* 현재 모드에 따라 탭 버튼 동적 생성 */
function buildTabs() {
    var header = document.getElementById('tabHeader');
    if (currentMode === 'user') {
        header.innerHTML =
            '<button class="tab-btn active" id="tabLogin" onclick="showPanel(\'panelLogin\');setTabActive(\'tabLogin\')">ログイン</button>' +
            '<button class="tab-btn" id="tabRegister" onclick="showPanel(\'panelRegister\');setTabActive(\'tabRegister\')">会員登録</button>';
    } else {
        header.innerHTML =
            '<button class="tab-btn active" id="tabBizLogin" onclick="showPanel(\'panelBizLogin\');setTabActive(\'tabBizLogin\')">ビジネスログイン</button>' +
            '<button class="tab-btn" id="tabBizReg" onclick="showPanel(\'panelBizRegister\');setTabActive(\'tabBizReg\')">ビジネス登録</button>';
    }
}

/* 활성 탭 변경 */
function setTabActive(activeId) {
    document.querySelectorAll('.tab-btn').forEach(function(btn) {
        btn.classList.remove('active');
    });
    document.getElementById(activeId).classList.add('active');
}

/* 선택한 패널만 표시, 나머지 숨김 */
function showPanel(panelId) {
    var allPanels = ['panelLogin', 'panelRegister', 'panelBizLogin', 'panelBizRegister'];
    allPanels.forEach(function(id) {
        var el = document.getElementById(id);
        if (el) el.style.display = 'none';
    });
    var target = document.getElementById(panelId);
    if (target) target.style.display = 'block';
    document.getElementById('authModal').scrollTop = 0;
}

/* 일반 회원 모드로 전환 */
function switchToUser() {
    currentMode = 'user';
    buildTabs();
    showPanel('panelLogin');
}

/* 비즈니스 모드로 전환 */
function switchToBiz() {
    currentMode = 'biz';
    buildTabs();
    showPanel('panelBizLogin');
}

/* 비즈니스 회원가입 패널로 이동 */
function switchToBizReg() {
    showPanel('panelBizRegister');
    setTabActive('tabBizReg');
}


/* =============================================================
   비밀번호 기능
============================================================= */

/* 비밀번호 표시/숨김 토글 */
function togglePw(inputId, btn) {
    var input = document.getElementById(inputId);
    var icon  = btn.querySelector('i');
    if (input.type === 'password') {
        input.type = 'text';
        icon.className = 'bi bi-eye-slash';
    } else {
        input.type = 'password';
        icon.className = 'bi bi-eye';
    }
}

/* 비밀번호 강도 계산 (0~4점)
   조건: 8자 이상 / 영문 포함 / 숫자 포함 / 특수문자 포함 */
function calcPwStrength(val) {
    var score = 0;
    if (val.length >= 8)           score++;
    if (/[A-Za-z]/.test(val))      score++;
    if (/[0-9]/.test(val))         score++;
    if (/[^A-Za-z0-9]/.test(val))  score++;
    return score;
}

/* 강도 바 UI 업데이트 */
function applyStrengthBar(fillId, score) {
    var fill = document.getElementById(fillId);
    if (!fill) return;
    var widths = ['0%', '25%', '50%', '75%', '100%'];
    var colors = ['#E5E7EB', '#EF4444', '#F97316', '#EAB308', '#16A34A'];
    fill.style.width      = widths[score];
    fill.style.background = colors[score];
}

/* 일반 회원 비밀번호 강도 체크 */
function checkPwStrength(val) {
    applyStrengthBar('pwStrengthFill', calcPwStrength(val));
}

/* 비즈니스 회원 비밀번호 강도 체크 */
function checkBizPwStrength(val) {
    applyStrengthBar('bizPwStrengthFill', calcPwStrength(val));
}

/* 일반 회원 비밀번호 일치 확인 */
function checkPwMatch() {
    var pw  = document.getElementById('regPw').value;
    var cpw = document.getElementById('regPwConfirm').value;
    var match = pw === cpw && cpw.length > 0;
    showFieldMsg('regPwConfirmMsg', cpw && !match ? 'パスワードが一致しません' : (match ? '✔ 一致しました' : ''), match ? 'ok' : 'error');
    setInputState('regPwConfirm', cpw ? (match ? 'is-ok' : 'is-error') : '');
}

/* 비즈니스 회원 비밀번호 일치 확인 */
function checkBizPwMatch() {
    var pw  = document.getElementById('bizRegPw').value;
    var cpw = document.getElementById('bizRegPwC').value;
    var match = pw === cpw && cpw.length > 0;
    showFieldMsg('bizRegPwCMsg', cpw && !match ? 'パスワードが一致しません' : (match ? '✔ 一致しました' : ''), match ? 'ok' : 'error');
    setInputState('bizRegPwC', cpw ? (match ? 'is-ok' : 'is-error') : '');
}


/* =============================================================
   유효성 검사 공통 헬퍼
============================================================= */

/* 필드 메시지 표시 — type: 'error' | 'ok' */
function showFieldMsg(msgId, text, type) {
    type = type || 'error';
    var el = document.getElementById(msgId);
    if (!el) return;
    el.textContent = text;
    el.className = 'field-msg ' + (text ? type + ' show' : '');
}

/* 입력 필드 상태 설정 — state: 'is-ok' | 'is-error' | '' */
function setInputState(inputId, state) {
    var el = document.getElementById(inputId);
    if (!el) return;
    el.classList.remove('is-ok', 'is-error');
    if (state) el.classList.add(state);
}

/* 필드 오류 상태 초기화 */
function clearFieldError(inputId) {
    setInputState(inputId, '');
    showFieldMsg(inputId + 'Msg', '');
}

/* 이메일 형식 검사 */
function isValidEmail(email) {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

/* 전화번호 형식 검사 (10~15자리, 숫자/하이픈 허용) */
function isValidPhone(phone) {
    return /^[\d\-+]{10,15}$/.test(phone.replace(/\s/g, ''));
}


/* =============================================================
   사업자번호 조회
   실제 구현 시: GET /api/business/verify?number=xxx
============================================================= */

/* 사업자번호 조회 및 대표자명 대조 */
function verifyBusinessNumber() {
    var num          = document.getElementById('bizRegBizNum').value.trim();
    var okNote       = document.getElementById('bizVerifyOk');
    var okText       = document.getElementById('bizVerifyOkText');
    var enteredLast  = document.getElementById('bizRegLastName').value.trim();
    var enteredFirst = document.getElementById('bizRegFirstName').value.trim();

    bizVerified = false;
    okNote.classList.remove('show');

    var sn = document.getElementById('bizRegStoreName');
    var sa = document.getElementById('bizRegStoreAddress');
    if (sn) { sn.value = ''; sn.classList.remove('is-ok'); }
    if (sa) { sa.value = ''; sa.classList.remove('is-ok'); }

    if (!num) {
        setInputState('bizRegBizNum', 'is-error');
        showFieldMsg('bizRegBizNumMsg', '事業者番号を入力してください');
        return;
    }
    if (num.length < 10) {
        setInputState('bizRegBizNum', 'is-error');
        showFieldMsg('bizRegBizNumMsg', '10桁以上の数字を入力してください');
        return;
    }

    var record = MOCK_BUSINESS_DB[num];
    if (!record) {
        setInputState('bizRegBizNum', 'is-error');
        showFieldMsg('bizRegBizNumMsg', '⚠ 登録されていない事業者番号です');
        return;
    }
    if (record.status === 'CLOSED') {
        setInputState('bizRegBizNum', 'is-error');
        showFieldMsg('bizRegBizNumMsg', '⚠ この事業者は廃業済みです');
        return;
    }
    if (record.status === 'SUSPENDED') {
        setInputState('bizRegBizNum', 'is-error');
        showFieldMsg('bizRegBizNumMsg', '⚠ この事業者は現在停止中です');
        return;
    }
    if (!enteredLast || !enteredFirst) {
        setInputState('bizRegBizNum', 'is-error');
        showFieldMsg('bizRegBizNumMsg', '⚠ 先に STEP 1 の代表者名（姓・名）を入力してください');
        return;
    }
    if (enteredLast !== record.last_name || enteredFirst !== record.first_name) {
        setInputState('bizRegBizNum', 'is-error');
        showFieldMsg('bizRegBizNumMsg', '⚠ 代表者名が一致しません（登録名：' + record.last_name + ' ' + record.first_name + '）');
        return;
    }

    /* 조회 성공 — 점포명/주소 자동 입력 */
    bizVerified = true;
    setInputState('bizRegBizNum', 'is-ok');
    showFieldMsg('bizRegBizNumMsg', '');
    okNote.classList.add('show');
    okText.textContent = '✔ 照合完了：' + record.store_name + '（代表：' + record.last_name + ' ' + record.first_name + '）';
    sn.value = record.store_name;
    sa.value = record.address;
    sn.classList.add('is-ok');
    sa.classList.add('is-ok');
}

/* 사업자번호 조회 결과 초기화 */
function clearBizVerify() {
    bizVerified = false;
    document.getElementById('bizVerifyOk').classList.remove('show');
    clearFieldError('bizRegBizNum');
    var sn = document.getElementById('bizRegStoreName');
    var sa = document.getElementById('bizRegStoreAddress');
    if (sn) { sn.value = ''; sn.classList.remove('is-ok'); }
    if (sa) { sa.value = ''; sa.classList.remove('is-ok'); }
}


/* =============================================================
   폼 제출 핸들러
   실제 구현 시 fetch() 또는 axios로 API 호출
============================================================= */

/* 일반 회원 로그인 — POST /api/auth/login */
function handleUserLogin() {
    var email = document.getElementById('loginEmail').value.trim();
    var pw    = document.getElementById('loginPw').value;
    var ok = true;
    if (!email || !isValidEmail(email)) {
        setInputState('loginEmail', 'is-error');
        showFieldMsg('loginEmailMsg', '正しいメールアドレスを入力してください');
        ok = false;
    }
    if (!pw) {
        setInputState('loginPw', 'is-error');
        showFieldMsg('loginPwMsg', 'パスワードを入力してください');
        ok = false;
    }
    if (!ok) return;
    alert('[デモ送信]\nPOST /api/auth/login\n{ email: "' + email + '", password: "***" }');
    closeModal();
}

/* 일반 회원 가입 — POST /api/auth/signup */
function handleUserRegister() {
    var email     = document.getElementById('regEmail').value.trim();
    var pw        = document.getElementById('regPw').value;
    var pwC       = document.getElementById('regPwConfirm').value;
    var lastName  = document.getElementById('regLastName').value.trim();
    var firstName = document.getElementById('regFirstName').value.trim();
    var lastKana  = document.getElementById('regLastNameKana').value.trim();
    var firstKana = document.getElementById('regFirstNameKana').value.trim();
    var phone     = document.getElementById('regPhone').value.trim();
    var ok = true;
    if (!email || !isValidEmail(email))  { setInputState('regEmail', 'is-error');        showFieldMsg('regEmailMsg', '正しいメールアドレスを入力してください'); ok = false; }
    if (calcPwStrength(pw) < 2)          { setInputState('regPw', 'is-error');            ok = false; }
    if (pw !== pwC || !pwC)              { setInputState('regPwConfirm', 'is-error');     showFieldMsg('regPwConfirmMsg', 'パスワードが一致しません'); ok = false; }
    if (!lastName || !firstName)         { setInputState('regLastName', 'is-error');      showFieldMsg('regLastNameMsg', '姓・名をご入力ください'); ok = false; }
    if (!lastKana || !firstKana)         { setInputState('regLastNameKana', 'is-error');  showFieldMsg('regLastNameKanaMsg', 'フリガナをご入力ください'); ok = false; }
    if (!phone || !isValidPhone(phone))  { setInputState('regPhone', 'is-error');         showFieldMsg('regPhoneMsg', '正しい電話番号を入力してください'); ok = false; }
    if (!ok) return;
    alert('[デモ送信]\nPOST /api/auth/signup\n{ email: "' + email + '", role: "ROLE_USER" }');
    closeModal();
}

/* 비즈니스 로그인 — POST /api/auth/login (ROLE_OWNER 확인) */
function handleBizLogin() {
    var email = document.getElementById('bizLoginEmail').value.trim();
    var pw    = document.getElementById('bizLoginPw').value;
    var ok = true;
    if (!email || !isValidEmail(email)) {
        setInputState('bizLoginEmail', 'is-error');
        showFieldMsg('bizLoginEmailMsg', '正しいメールアドレスを入力してください');
        ok = false;
    }
    if (!pw) { ok = false; }
    if (!ok) return;
    alert('[デモ送信]\nPOST /api/auth/login\n{ email: "' + email + '", role: "ROLE_OWNER" }');
    closeModal();
}

/* 비즈니스 회원 가입 — POST /api/auth/signup/owner */
function handleBizRegister() {
    if (!bizVerified) {
        setInputState('bizRegBizNum', 'is-error');
        showFieldMsg('bizRegBizNumMsg', '先に事業者番号の認証を完了してください');
        document.getElementById('authModal').scrollTop = 0;
        return;
    }
    var email     = document.getElementById('bizRegEmail').value.trim();
    var pw        = document.getElementById('bizRegPw').value;
    var pwC       = document.getElementById('bizRegPwC').value;
    var lastName  = document.getElementById('bizRegLastName').value.trim();
    var firstName = document.getElementById('bizRegFirstName').value.trim();
    var bizNum    = document.getElementById('bizRegBizNum').value.trim();
    var ok = true;
    if (!email || !isValidEmail(email)) {
        setInputState('bizRegEmail', 'is-error');
        showFieldMsg('bizRegEmailMsg', '正しいメールアドレスを入力してください');
        ok = false;
    }
    if (pw !== pwC) {
        setInputState('bizRegPwC', 'is-error');
        showFieldMsg('bizRegPwCMsg', 'パスワードが一致しません');
        ok = false;
    }
    if (!ok) return;
    alert('[デモ送信]\nPOST /api/auth/signup/owner\n{ email: "' + email + '", business_number: "' + bizNum + '", role: "ROLE_OWNER" }');
    closeModal();
}