/* =============================================================
   script.js — 공통 JS: 모달, 탭 전환, 폼 유효성 검사,
               비밀번호 강도, 사업자번호 조회
============================================================= */


/* =============================================================
   토스트 알림
============================================================= */

function showToast(message, type) {
    type = type || 'success';
    var wrap = document.getElementById('toastWrap');
    if (!wrap) return;

    var toast = document.createElement('div');
    toast.className = 'toast ' + type;
    toast.innerHTML =
        '<div class="toast-icon"><i class="bi bi-' + (type === 'success' ? 'check-lg' : 'exclamation-lg') + '"></i></div>' +
        '<span class="toast-msg">' + message + '</span>' +
        '<button class="toast-close" onclick="this.parentElement.remove()"><i class="bi bi-x"></i></button>';

    wrap.appendChild(toast);

    setTimeout(function() {
        toast.classList.add('hide');
        setTimeout(function() { toast.remove(); }, 350);
    }, 3000);
}

/* =============================================================
   헤더 로그인 상태 반영
============================================================= */

function updateHeaderAuth() {
    fetch('/api/auth/me')
    .then(function(res) { return res.json(); })
    .then(function(data) {
        var heroCta = document.getElementById('heroCta');
        if (data.success) {
            document.getElementById('btnAuthGuest').style.display = 'none';
            document.getElementById('userMenuWrap').style.display = 'block';
            var name = (data.lastName || '') + (data.firstName || '');
            if (!name) name = data.email;
            document.getElementById('headerUserName').textContent = name;

			var adminLink = document.getElementById('adminMenuLink');
			if (adminLink) {
			    adminLink.style.display = (data.role === 'ROLE_ADMIN') ? 'flex' : 'none';
			}
			var mypageLink = document.getElementById('mypageMenuLink');
			if (mypageLink) {
			    mypageLink.style.display = (data.role === 'ROLE_ADMIN') ? 'none' : 'flex';
			}

            if (heroCta) heroCta.style.display = 'none';
        } else {
            document.getElementById('btnAuthGuest').style.display = '';
            document.getElementById('userMenuWrap').style.display = 'none';
            if (heroCta) heroCta.style.display = '';
        }
    })
    .catch(function() {
        document.getElementById('btnAuthGuest').style.display = '';
        document.getElementById('userMenuWrap').style.display = 'none';
    });
}

function handleLogout() {
    fetch('/api/auth/logout', { method: 'POST' })
    .then(function() {
        showToast('ログアウトしました', 'success');
        setTimeout(function() { location.reload(); }, 1000);
    })
    .catch(function() { location.reload(); });
}

// 페이지 로드 시 로그인 상태 확인
document.addEventListener('DOMContentLoaded', function() {
    updateHeaderAuth();

    // 유저 메뉴 드롭다운 JS 제어 (CSS hover 대신 사용 — 토스트 겹침 문제 방지)
    var menu = document.getElementById('userMenuWrap');
    if (!menu) return;
    var dropdown = document.getElementById('userDropdown');
    var timer;

    function openDropdown() {
        clearTimeout(timer);
        dropdown.style.display = 'block';
    }
    function closeDropdown() {
        timer = setTimeout(function() {
            dropdown.style.display = 'none';
        }, 150);
    }

    menu.addEventListener('mouseenter', openDropdown);
    menu.addEventListener('mouseleave', closeDropdown);
    dropdown.addEventListener('mouseenter', openDropdown);
    dropdown.addEventListener('mouseleave', closeDropdown);
});

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

function closeModal() {
    document.getElementById('authOverlay').classList.remove('active');
    document.body.style.overflow = '';
}

function handleOverlayClick(e) {
    if (e.target === document.getElementById('authOverlay')) {
        closeModal();
    }
}

document.addEventListener('keydown', function(e) {
    if (e.key === 'Escape') closeModal();
});


/* =============================================================
   탭 전환
============================================================= */

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

function setTabActive(activeId) {
    document.querySelectorAll('.tab-btn').forEach(function(btn) {
        btn.classList.remove('active');
    });
    document.getElementById(activeId).classList.add('active');
}

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

function switchToUser() {
    currentMode = 'user';
    buildTabs();
    showPanel('panelLogin');
}

function switchToBiz() {
    currentMode = 'biz';
    buildTabs();
    showPanel('panelBizLogin');
}

function switchToBizReg() {
    showPanel('panelBizRegister');
    setTabActive('tabBizReg');
}


/* =============================================================
   비밀번호 기능
============================================================= */

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

/* 비밀번호 강도 계산 (0~4점) */
function calcPwStrength(val) {
    var score = 0;
    if (val.length >= 8)           score++;
    if (/[A-Za-z]/.test(val))      score++;
    if (/[0-9]/.test(val))         score++;
    if (/[^A-Za-z0-9]/.test(val))  score++;
    return score;
}

function applyStrengthBar(fillId, score) {
    var fill = document.getElementById(fillId);
    if (!fill) return;
    var widths = ['0%', '25%', '50%', '75%', '100%'];
    var colors = ['#E5E7EB', '#EF4444', '#F97316', '#EAB308', '#16A34A'];
    fill.style.width      = widths[score];
    fill.style.background = colors[score];
}

function checkPwStrength(val) {
    applyStrengthBar('pwStrengthFill', calcPwStrength(val));
}

function checkBizPwStrength(val) {
    applyStrengthBar('bizPwStrengthFill', calcPwStrength(val));
}

function checkPwMatch() {
    var pw  = document.getElementById('regPw').value;
    var cpw = document.getElementById('regPwConfirm').value;
    var match = pw === cpw && cpw.length > 0;
    showFieldMsg('regPwConfirmMsg', cpw && !match ? 'パスワードが一致しません' : (match ? '✔ 一致しました' : ''), match ? 'ok' : 'error');
    setInputState('regPwConfirm', cpw ? (match ? 'is-ok' : 'is-error') : '');
}

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

function showFieldMsg(msgId, text, type) {
    type = type || 'error';
    var el = document.getElementById(msgId);
    if (!el) return;
    el.textContent = text;
    el.className = 'field-msg ' + (text ? type + ' show' : '');
}

function setInputState(inputId, state) {
    var el = document.getElementById(inputId);
    if (!el) return;
    el.classList.remove('is-ok', 'is-error');
    if (state) el.classList.add(state);
}

function clearFieldError(inputId) {
    setInputState(inputId, '');
    showFieldMsg(inputId + 'Msg', '');
}

function isValidEmail(email) {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

function isValidPhone(phone) {
    return /^[\d\-+]{10,15}$/.test(phone.replace(/\s/g, ''));
}


/* =============================================================
   사업자번호 조회 — 실제 구현 시: GET /api/business/verify?number=xxx
============================================================= */

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

    fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: email, password: pw })
    })
    .then(function(res) { return res.json(); })
    .then(function(data) {
        if (data.success) {
            closeModal();
            showToast('ログインしました', 'success');
            updateHeaderAuth();
        } else {
            setInputState('loginPw', 'is-error');
            showFieldMsg('loginPwMsg', data.message || 'ログインに失敗しました');
        }
    })
    .catch(function() {
        showFieldMsg('loginPwMsg', 'サーバーエラーが発生しました');
    });
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

    if (!email || !isValidEmail(email))  { setInputState('regEmail', 'is-error');       showFieldMsg('regEmailMsg', '正しいメールアドレスを入力してください'); ok = false; }
    if (calcPwStrength(pw) < 2)          { setInputState('regPw', 'is-error');           ok = false; }
    if (pw !== pwC || !pwC)              { setInputState('regPwConfirm', 'is-error');    showFieldMsg('regPwConfirmMsg', 'パスワードが一致しません'); ok = false; }
    if (!lastName || !firstName)         { setInputState('regLastName', 'is-error');     showFieldMsg('regLastNameMsg', '姓・名をご入力ください'); ok = false; }
    if (!lastKana || !firstKana)         { setInputState('regLastNameKana', 'is-error'); showFieldMsg('regLastNameKanaMsg', 'フリガナをご入力ください'); ok = false; }
    if (!phone || !isValidPhone(phone))  { setInputState('regPhone', 'is-error');        showFieldMsg('regPhoneMsg', '正しい電話番号を入力してください'); ok = false; }
    if (!ok) return;

    fetch('/api/auth/signup', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            email:         email,
            password:      pw,
            lastName:      lastName,
            firstName:     firstName,
            lastNameKana:  lastKana,
            firstNameKana: firstKana,
            phone:         phone
        })
    })
    .then(function(res) {
        var status = res.status;
        return res.text().then(function(text) {
            try {
                return { status: status, data: JSON.parse(text) };
            } catch(e) {
                return { status: status, data: null, raw: text };
            }
        });
    })
    .then(function(result) {
        if (result.data && result.data.success) {
            showToast('会員登録が完了しました', 'success');
            showPanel('panelLogin');
            setTabActive('tabLogin');
        } else if (result.data && result.data.message) {
            showFieldMsg('regEmailMsg', result.data.message);
        } else {
            showFieldMsg('regEmailMsg', 'エラー [HTTP ' + result.status + '] サーバーに接続できません');
        }
    })
    .catch(function(err) {
        showFieldMsg('regEmailMsg', 'ネットワークエラー: ' + err.message);
    });
}

/* 비즈니스 로그인 — POST /api/auth/login (백엔드 미구현, 프론트만 연결) */
function handleBizLogin() {
    var email = document.getElementById('bizLoginEmail').value.trim();
    var pw    = document.getElementById('bizLoginPw').value;
    var ok = true;

    if (!email || !isValidEmail(email)) {
        setInputState('bizLoginEmail', 'is-error');
        showFieldMsg('bizLoginEmailMsg', '正しいメールアドレスを入力してください');
        ok = false;
    }
    if (!pw) {
        setInputState('bizLoginPw', 'is-error');
        showFieldMsg('bizLoginEmailMsg', 'パスワードを入力してください');
        ok = false;
    }
    if (!ok) return;

    fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: email, password: pw })
    })
    .then(function(res) { return res.json(); })
    .then(function(data) {
        if (data.success) {
            closeModal();
            location.reload();
        } else {
            setInputState('bizLoginEmail', 'is-error');
            showFieldMsg('bizLoginEmailMsg', data.message || 'ログインに失敗しました');
        }
    })
    .catch(function() {
        showFieldMsg('bizLoginEmailMsg', 'サーバーエラーが発生しました');
    });
}

/* 비즈니스 회원 가입 — POST /api/auth/signup/owner (백엔드 미구현, 프론트만 연결) */
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
    var lastKana  = document.getElementById('bizRegLastNameKana').value.trim();
    var firstKana = document.getElementById('bizRegFirstNameKana').value.trim();
    var phone     = document.getElementById('bizRegPhone').value.trim();
    var bizNum    = document.getElementById('bizRegBizNum').value.trim();
    var storeName = document.getElementById('bizRegStoreName').value.trim();
    var storeAddr = document.getElementById('bizRegStoreAddress').value.trim();
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

    fetch('/api/auth/signup/owner', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            email:          email,
            password:       pw,
            lastName:       lastName,
            firstName:      firstName,
            lastNameKana:   lastKana,
            firstNameKana:  firstKana,
            phone:          phone,
            businessNumber: bizNum,
            storeName:      storeName,
            storeAddress:   storeAddr
        })
    })
    .then(function(res) { return res.json(); })
    .then(function(data) {
        if (data.success) {
            alert('ビジネス登録が完了しました。ログインしてください。');
            showPanel('panelBizLogin');
            setTabActive('tabBizLogin');
        } else {
            showFieldMsg('bizRegEmailMsg', data.message || 'ビジネス登録に失敗しました');
        }
    })
    .catch(function() {
        showFieldMsg('bizRegEmailMsg', 'サーバーエラーが発生しました');
    });
}