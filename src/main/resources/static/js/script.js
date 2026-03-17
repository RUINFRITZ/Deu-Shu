/* =============================================================
   script.js — 공통 JS: 모달, 탭 전환, 폼 유효성 검사,
               비밀번호 강도, 사업자번호 조회
============================================================= */

/* =============================================================
   Toast
============================================================= */

function showToast(message, type) {
    type = type || 'success';
    var wrap = document.getElementById('toastWrap');
    if (!wrap) return;

    var toast = document.createElement('div');
    toast.className = 'toast ' + type;
	toast.style.pointerEvents = 'auto';
	
    toast.innerHTML =
        '<div class="toast-icon"><i class="bi bi-' + (type === 'success' ? 'check-lg' : 'exclamation-lg') + '"></i></div>' +
        '<span class="toast-msg">' + message + '</span>' +
        '<button class="toast-close" onclick="this.parentElement.remove()"><i class="bi bi-x"></i></button>';

    wrap.appendChild(toast);

    setTimeout(function() {
        toast.classList.add('hide');
        setTimeout(function() { toast.remove(); }, 350);
    }, 2222);
}

/* =============================================================
   헤더 로그인 상태 반영
============================================================= */

function updateHeaderAuth() {
    fetch('/api/auth/me')
    .then(function(res) { return res.json(); })
    .then(function(data) {
		var btnGuest       = document.getElementById('btnAuthGuest');
        var menuWrap       = document.getElementById('userMenuWrap');
        var headerUserName = document.getElementById('headerUserName');
        var heroCta        = document.getElementById('heroCta');
        var mypageLink     = document.getElementById('dropdownMypageLink');
        var ownerLink      = document.getElementById('dropdownOwnerLink');

        if (data.success) {
            if (btnGuest) btnGuest.style.display = 'none';
            if (menuWrap) menuWrap.style.display = 'inline-block';
			
            var name = (data.lastName || '') + (data.firstName || '');
            if (!name) name = data.email;
			if (headerUserName) headerUserName.textContent = name;
			
            if (heroCta) heroCta.style.display = 'none';

            var isOwner = (data.role === 'ROLE_OWNER' || data.role === 'ROLE_ADMIN');
            // マイページ: 일반 회원(ROLE_USER)만 표시
            if (mypageLink) mypageLink.style.display = isOwner ? 'none' : 'flex';
            // オーナーページ: ROLE_OWNER / ROLE_ADMIN 만 표시
            if (ownerLink)  ownerLink.style.display  = isOwner ? 'flex' : 'none';
        } else {
			if (btnGuest) btnGuest.style.display = '';
            if (menuWrap) menuWrap.style.display = 'none';
            if (heroCta)  heroCta.style.display    = '';
            if (mypageLink) mypageLink.style.display = 'flex';
            if (ownerLink)  ownerLink.style.display  = 'none';
        }
    })
	.catch(function() {
			var btnGuest = document.getElementById('btnAuthGuest');
	        var menuWrap = document.getElementById('userMenuWrap');
			if (btnGuest) btnGuest.style.display = '';
		    if (menuWrap) menuWrap.style.display = 'none';
    });
}

function handleLogout() {
    fetch('/api/auth/logout', { method: 'POST' })
    .then(function() {
        showToast('ログアウトしました', 'success');
        setTimeout(function() { window.location.href = '/'; }, 1010);
    })
    .catch(function() { location.reload(); });
}

// =========================================================================
// [ ドゥーシュー ] 統合された DOMContentLoaded イベントリスナー
// =========================================================================
document.addEventListener('DOMContentLoaded', function() {
    
    // 1. ヘッダーのログイン状態確認
    updateHeaderAuth();
    
    // 2. 事業者登録番号フォーム
    bindBusinessNumberFormatter('bizRegBizNum', clearBizVerify);
    
    // 3. ビジネスログイン Enter キー処理
    var bizEmailInput = document.getElementById('bizLoginEmail');
    var bizPwInput    = document.getElementById('bizLoginPw');

    function handleBizLoginEnter(e) {
        if (e.key === 'Enter') {
            e.preventDefault();
            handleBizLogin();
        }
    }
    if (bizEmailInput) bizEmailInput.addEventListener('keydown', handleBizLoginEnter);
    if (bizPwInput)    bizPwInput.addEventListener('keydown', handleBizLoginEnter);

    // 4. マイページハッシュルーティング (#orders, #favorites)
    if (window.location.pathname.startsWith('/mypage')) {
        var hash = window.location.hash;
        if (hash === '#orders') {
            // data-tab 대신 switchSection 함수 직접 호출
            var btn = document.querySelector('[onclick*="switchSection(\'orders\'"]');
            if (btn) btn.click();
        } else if (hash === '#favorites') {
            var btn = document.querySelector('[onclick*="switchSection(\'favorites\'"]');
            if (btn) btn.click();
        }
    }
	
	var dropdown = document.getElementById('userDropdown');
    if (dropdown) {
        var dropdownItems = dropdown.querySelectorAll('.user-dropdown-item');
        dropdownItems.forEach(function(item) {
            item.addEventListener('click', function(e) {
                // <a>タグの遷移を邪魔しないよう、メニューを非表示にする
                dropdown.style.display = 'none';
                
                // 少し経ったらCSSの :hover に制御を返す
                setTimeout(function() {
                    dropdown.style.display = '';
                }, 300);
            });
        });
    }
});

/* 事業者番号認証の完了フラグ */
var bizVerified = false;

/* 現在のモーダルモード ('user' | 'biz') */
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
    var verifyBtn    = document.querySelector('.btn-verify');

    // 초기화
    bizVerified = false;
    okNote.classList.remove('show');
    clearBizVerify();

    // 입력값 검증
    if (!num) {
        setInputState('bizRegBizNum', 'is-error');
        showFieldMsg('bizRegBizNumMsg', '事業者番号を入力してください');
        return;
    }
    if (!enteredLast || !enteredFirst) {
        setInputState('bizRegBizNum', 'is-error');
        showFieldMsg('bizRegBizNumMsg', '⚠ 先に STEP 1 の代表者名（姓・名）を入力してください');
        return;
    }

    // 버튼 로딩 상태
    if (verifyBtn) {
        verifyBtn.disabled = true;
        verifyBtn.textContent = '確認中...';
    }

    // 실제 API 호출 — GET /api/auth/verify-business/{number}
    // SecurityConfig 에서 /api/auth/** 를 permitAll() 하므로 비로그인도 접근 가능
    fetch('/api/auth/verify-business/' + encodeURIComponent(num))
    .then(function(res) { return res.json(); })
    .then(function(data) {
        if (!data.success) {
            setInputState('bizRegBizNum', 'is-error');
            showFieldMsg('bizRegBizNumMsg', data.message || '⚠ 登録されていない事業者番号です');
            return;
        }

        // 서버에서 반환된 대표자 성명과 입력값 대조
        if (data.lastName !== enteredLast || data.firstName !== enteredFirst) {
            setInputState('bizRegBizNum', 'is-error');
            showFieldMsg('bizRegBizNumMsg',
                '⚠ 代表者名が一致しません（登録名：' + data.lastName + ' ' + data.firstName + '）');
            return;
        }

        // 인증 성공
        bizVerified = true;
        setInputState('bizRegBizNum', 'is-ok');
        showFieldMsg('bizRegBizNumMsg', '');
        okNote.classList.add('show');
        okText.textContent = '✔ 照合完了：' + data.storeName
            + '（代表：' + data.lastName + ' ' + data.firstName + '）';

        // 가게 이름 · 주소 자동입력 (disabled 필드)
        var sn = document.getElementById('bizRegStoreName');
        var sa = document.getElementById('bizRegStoreAddress');
        if (sn) { sn.value = data.storeName  || ''; sn.classList.add('is-ok'); }
        if (sa) { sa.value = data.address    || ''; sa.classList.add('is-ok'); }
    })
    .catch(function() {
        setInputState('bizRegBizNum', 'is-error');
        showFieldMsg('bizRegBizNumMsg', 'サーバーエラーが発生しました。再度お試しください');
    })
    .finally(function() {
        // 버튼 원복
        if (verifyBtn) {
            verifyBtn.disabled = false;
            verifyBtn.textContent = '照合';
        }
    });
}

// 하이픈 자동삽입
function formatBusinessNumber(value) {
    var digits = String(value || '').replace(/\D/g, '').slice(0, 10);

    if (digits.length > 5) {
        return digits.replace(/(\d{3})(\d{2})(\d{1,5})/, '$1-$2-$3');
    }
    if (digits.length > 3) {
        return digits.replace(/(\d{3})(\d{1,2})/, '$1-$2');
    }
    return digits;
}

function bindBusinessNumberFormatter(inputId, onFormatted) {
    var input = document.getElementById(inputId);
    if (!input) return;

    input.addEventListener('input', function(e) {
        var formatted = formatBusinessNumber(e.target.value);
        e.target.value = formatted;

        if (typeof onFormatted === 'function') {
            onFormatted();
        }
    });
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
            window.location.reload();
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

/* 비즈니스 로그인 — POST /api/auth/owner-login
   ROLE_USER 전용인 /api/auth/login 대신 오너 전용 엔드포인트 호출
   응답에 role 포함 → /api/auth/me 이중 호출 없이 즉시 리다이렉트 */
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

    // 버튼 로딩 상태
    var loginBtn = document.querySelector('#panelBizLogin .btn-orange-full');
    if (loginBtn) { loginBtn.disabled = true; loginBtn.textContent = 'ログイン中...'; }

    fetch('/api/auth/owner-login', {
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
            // 응답의 role 로 즉시 리다이렉트 결정 (/api/auth/me 이중 호출 없음)
            setTimeout(function() { location.href = '/owner'; }, 800);
        } else {
            setInputState('bizLoginEmail', 'is-error');
            showFieldMsg('bizLoginEmailMsg', data.message || 'ログインに失敗しました');
        }
    })
    .catch(function() {
        showFieldMsg('bizLoginEmailMsg', 'サーバーエラーが発生しました');
    })
    .finally(function() {
        if (loginBtn) { loginBtn.disabled = false; loginBtn.textContent = 'ビジネスログイン'; }
    });
}

/* 사업자 회원가입 — POST /api/auth/owner-signup */
function handleBizRegister() {
    // 사업자번호 인증 완료 여부 확인
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
    if (calcPwStrength(pw) < 2) {
        setInputState('bizRegPw', 'is-error');
        showFieldMsg('bizRegPwCMsg', 'パスワードは8文字以上で、英字・数字を含めてください');
        ok = false;
    }
    if (pw !== pwC || !pwC) {
        setInputState('bizRegPwC', 'is-error');
        showFieldMsg('bizRegPwCMsg', 'パスワードが一致しません');
        ok = false;
    }
    if (!lastName || !firstName) {
        showFieldMsg('bizRegEmailMsg', '代表者名を入力してください');
        ok = false;
    }
    if (!lastKana || !firstKana) {
        showFieldMsg('bizRegEmailMsg', 'フリガナを入力してください');
        ok = false;
    }
    if (!phone || !isValidPhone(phone)) {
        showFieldMsg('bizRegEmailMsg', '正しい電話番号を入力してください');
        ok = false;
    }
    if (!ok) return;

    fetch('/api/auth/owner-signup', {
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
            showToast('ビジネス登録が完了しました。ログインしてください', 'success');
            // 사업자 로그인 패널로 이동
            setTimeout(function() {
                showPanel('panelBizLogin');
                setTabActive('tabBizLogin');
                // 이메일 자동 입력 (편의성)
                var loginEmail = document.getElementById('bizLoginEmail');
                if (loginEmail) loginEmail.value = email;
            }, 1200);
        } else {
            showFieldMsg('bizRegEmailMsg', data.message || 'ビジネス登録に失敗しました');
        }
    })
    .catch(function() {
        showFieldMsg('bizRegEmailMsg', 'サーバーエラーが発生しました');
    });
}

