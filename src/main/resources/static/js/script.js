/* =============================================================
   script.js — 공통 JS: 모달, 탭 전환, 폼 유효성 검사,
               비밀번호 강도, 사업자번호 조회
============================================================= */


/* =============================================================
   토스트 알림
============================================================= */

function showToast(message, type) {
    type = type || 'success';

    // wrap을 body 직계 자식으로 — 인라인 스타일로 완전 자립, 외부 CSS 영향 없음
    var wrap = document.getElementById('toastWrap');
    if (!wrap) {
        wrap = document.createElement('div');
        wrap.id = 'toastWrap';
        document.body.appendChild(wrap);
    } else if (wrap.parentNode !== document.body) {
        document.body.appendChild(wrap);
    }
    wrap.style.cssText =
        'position:fixed;top:80px;right:20px;left:auto;z-index:99999;' +
        'display:flex;flex-direction:column;gap:10px;pointer-events:none;width:300px;';

    var colorMap = {
        success: { bg:'#dcfce7', color:'#16a34a', border:'#16a34a', icon:'✓' },
        error:   { bg:'#fee2e2', color:'#dc2626', border:'#dc2626', icon:'✕' },
        info:    { bg:'#dbeafe', color:'#2563eb', border:'#2563eb', icon:'ℹ' }
    };
    var c     = colorMap[type] || colorMap.success;
    var title = { success:'完了', error:'エラー', info:'お知らせ' }[type] || '完了';

    var toast = document.createElement('div');
    toast.style.cssText =
        'display:flex;align-items:flex-start;gap:12px;background:#fff;' +
        'border:1px solid #e5e7eb;border-left:4px solid ' + c.border + ';' +
        'border-radius:8px;box-shadow:0 4px 20px rgba(0,0,0,0.15);' +
        'padding:14px 44px 14px 16px;pointer-events:auto;position:relative;' +
        'font-family:"Noto Sans JP",sans-serif;' +
        'opacity:0;transform:translateX(20px);transition:opacity 0.25s,transform 0.25s;';

    var iconEl = document.createElement('div');
    iconEl.style.cssText =
        'width:28px;height:28px;border-radius:50%;background:' + c.bg + ';' +
        'color:' + c.color + ';display:flex;align-items:center;justify-content:center;' +
        'font-size:14px;font-weight:700;flex-shrink:0;margin-top:1px;';
    iconEl.textContent = c.icon;

    var body = document.createElement('div');
    body.style.cssText = 'flex:1;min-width:0;';

    var titleEl = document.createElement('div');
    titleEl.style.cssText = 'font-size:13px;font-weight:700;color:#111827;margin-bottom:3px;';
    titleEl.textContent = title;

    var msgEl = document.createElement('div');
    msgEl.style.cssText = 'font-size:13px;color:#6b7280;line-height:1.5;';
    msgEl.textContent = message;

    var closeBtn = document.createElement('button');
    closeBtn.style.cssText =
        'position:absolute;top:8px;right:10px;background:none;border:none;' +
        'color:#9ca3af;cursor:pointer;font-size:18px;line-height:1;padding:0;';
    closeBtn.textContent = '×';
    closeBtn.onclick = function() { toast.remove(); };

    body.appendChild(titleEl);
    body.appendChild(msgEl);
    toast.appendChild(iconEl);
    toast.appendChild(body);
    toast.appendChild(closeBtn);
    wrap.appendChild(toast);

    // 슬라이드인 애니메이션
    requestAnimationFrame(function() {
        toast.style.opacity = '1';
        toast.style.transform = 'translateX(0)';
    });

    setTimeout(function() {
        toast.style.opacity = '0';
        toast.style.transform = 'translateX(20px)';
        setTimeout(function() { if (toast.parentNode) toast.remove(); }, 280);
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
           var name = String(data.memberId);
            document.getElementById('headerUserName').textContent = name;
            // 메인페이지 히어로 버튼 숨기기
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
        // 로그아웃 성공 → 현재 페이지의 기존 파라미터를 유지하고 toast 파라미터 추가
        var params = new URLSearchParams(window.location.search);
        params.set('toast', 'logout');
        location.href = location.pathname + '?' + params.toString();
    })
    .catch(function() { location.reload(); });
}

// 페이지 로드 시 로그인 상태 확인
document.addEventListener('DOMContentLoaded', function() {
    updateHeaderAuth();

    // URL 파라미터 ?toast=login|logout|register 를 읽어서 토스트 표시
    // 새로고침 후에도 토스트가 보이도록 하기 위한 방식
    var params = new URLSearchParams(window.location.search);
    var toast  = params.get('toast');
    if (toast === 'login')    showToast('ログインしました', 'success');
    if (toast === 'logout')   showToast('ログアウトしました', 'success');
    if (toast === 'register') showToast('会員登録が完了しました', 'success');
    if (toast === 'withdraw') showToast('退会が完了しました', 'info');

    // 토스트 표시 후 URL에서 파라미터 제거 (주소창 깔끔하게)
    if (toast) {
        var cleanUrl = window.location.pathname;
        window.history.replaceState(null, '', cleanUrl);
    }

    // 유저 메뉴 드롭다운 JS 제어 (CSS hover 대신 사용 — 토스트 겹침 문제 방지)
    var menu = document.getElementById('userMenuWrap');
    if (!menu) return;
    var dropdown = document.getElementById('userDropdown');
    var timer;

	function openDropdown() {
	    clearTimeout(timer);
	    dropdown.style.visibility = 'visible';
	    dropdown.style.maxHeight = dropdown.scrollHeight + 'px';
	}
	function closeDropdown() {
	    timer = setTimeout(function() {
	        dropdown.style.maxHeight = '0';
	        dropdown.style.visibility = 'hidden';
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
            // 로그인 성공 → 현재 페이지의 기존 파라미터를 유지하고 toast 파라미터 추가
            var params = new URLSearchParams(window.location.search);
            params.set('toast', 'login');
            location.href = location.pathname + '?' + params.toString();
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
