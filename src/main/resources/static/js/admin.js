// ── 전역 상태 ──
let MEMBERS = [], STORES = [], ORDERS = [], REVIEWS = [], COMPLAINTS = [];
let currentMemberId = null, currentOrderId = null, currentComplaintId = null;
let filteredMembers = [], filteredStores = [], filteredOrders = [], filteredReviews = [], filteredComplaints = [];

// ═══════════════════════════════════════════
//  대시보드 함수
// ═══════════════════════════════════════════

// 통계 카드 — API 연동
function loadDashboardStats() {
    fetch('/api/admin/dashboard')
        .then(r => r.ok ? r.json() : null)
        .then(data => {
            if (!data) return;
            const el = id => document.getElementById(id);
            if (data.todayOrderCount != null)  el('dashOrderCount').textContent  = data.todayOrderCount.toLocaleString();
            if (data.todayTotalSales  != null)  el('dashTotalSales').textContent  = '¥' + data.todayTotalSales.toLocaleString();
            if (data.totalMemberCount != null)  el('dashMemberCount').textContent = data.totalMemberCount.toLocaleString();
            if (data.activeStoreCount != null)  el('dashStoreCount').textContent  = data.activeStoreCount.toLocaleString();
            if (el('dashOrderDelta'))  el('dashOrderDelta').textContent  = data.orderDelta  || '';
            if (el('dashSalesDelta'))  el('dashSalesDelta').textContent  = data.salesDelta  || '';
            if (el('dashStoreDelta'))  el('dashStoreDelta').textContent  = data.storeDelta  || '';
        })
        .catch(() => {/* DB 없을 때는 — 그대로 표시 */});
}

// 최근 주문 / 신규 회원
function loadDashboardRecent() {
    // 최근 주문
    fetch('/api/admin/orders?page=0&size=5')
        .then(r => r.ok ? r.json() : null)
        .then(data => {
            const ol = document.getElementById('recentOrdersList');
            if (!ol) return;
            const list = Array.isArray(data) ? data : (data && data.content ? data.content : []);
            if (!list.length) { ol.innerHTML = '<div class="list-empty">注文データがありません</div>'; return; }
            const statusLabel = { PAYMENT_PENDING:'未払い', PAYMENT_COMPLETED:'完了', CANCELLED:'キャンセル' };
            ol.innerHTML = list.slice(0, 5).map(o => `
                <div class="recent-list-item">
                    <div class="recent-icon"><i class="bi bi-bag"></i></div>
                    <div class="recent-info">
                        <div class="recent-name">${o.memberName || '—'} — ${o.storeName || '—'}</div>
                        <div class="recent-meta">${(o.createdAt||'').slice(0,16)} · ${statusLabel[o.status]||o.status||''}</div>
                    </div>
                    <div class="recent-price">¥${(o.totalPrice||0).toLocaleString()}</div>
                </div>`).join('');
        })
        .catch(() => {
            const ol = document.getElementById('recentOrdersList');
            if (ol) ol.innerHTML = '<div class="list-empty">データがありません</div>';
        });

    // 신규 회원
    fetch('/api/admin/members?page=0&size=5')
        .then(r => r.ok ? r.json() : null)
        .then(data => {
            const ml = document.getElementById('recentMembersList');
            if (!ml) return;
            const list = Array.isArray(data) ? data : (data && data.content ? data.content : []);
            if (!list.length) { ml.innerHTML = '<div class="list-empty">会員データがありません</div>'; return; }
            ml.innerHTML = list.slice(0, 5).map(m => {
                const roleClass = m.role === 'ROLE_OWNER' ? 'badge-owner' : 'badge-user';
                const iconStyle = m.role === 'ROLE_OWNER'
                    ? 'background:var(--primary-pale);color:var(--primary);'
                    : 'background:#eff6ff;color:#2563eb;';
                return `
                <div class="recent-list-item">
                    <div class="recent-icon" style="${iconStyle}"><i class="bi bi-person"></i></div>
                    <div class="recent-info">
                        <div class="recent-name">${m.lastName||''} ${m.firstName||''}</div>
                        <div class="recent-meta">${m.email||''}</div>
                    </div>
                    <span class="badge ${roleClass}">${(m.role||'').replace('ROLE_','')}</span>
                </div>`;
            }).join('');
        })
        .catch(() => {
            const ml = document.getElementById('recentMembersList');
            if (ml) ml.innerHTML = '<div class="list-empty">データがありません</div>';
        });
}

// 바 차트 — API 우선, 실패 시 플레이스홀더
function loadBarChart() {
    const wrap = document.getElementById('barChart');
    if (!wrap) return;

    fetch('/api/admin/dashboard/weekly-sales')
        .then(r => r.ok ? r.json() : null)
        .then(data => renderBarChart(wrap, data))
        .catch(() => renderBarChart(wrap, null));
}

function renderBarChart(wrap, data) {
    const days = ['月','火','水','木','金','土','日'];
    // data가 없으면 빈 플레이스홀더
    const values = data && data.length
        ? data.map(d => d.totalSales || 0)
        : [0, 0, 0, 0, 0, 0, 0];

    const max = Math.max(...values, 1);
    wrap.innerHTML = '';
    days.forEach((day, i) => {
        const h = Math.round((values[i] / max) * 100);
        const col = document.createElement('div');
        col.className = 'bar-col';
        col.innerHTML = `
            <div class="bar-fill" style="height:${h}%;"
                 title="${day}曜日: ¥${(values[i]).toLocaleString()}"></div>
            <span class="bar-day">${day}</span>`;
        wrap.appendChild(col);
    });
}

// 도넛 차트 — API 우선, 실패 시 플레이스홀더
function loadDonutChart() {
    fetch('/api/admin/dashboard/category-stats')
        .then(r => r.ok ? r.json() : null)
        .then(data => renderDonutChart(data))
        .catch(() => renderDonutChart(null));
}

function renderDonutChart(data) {
    const svg      = document.getElementById('donutSvg');
    const legend   = document.getElementById('donutLegend');
    const totalEl  = document.getElementById('donutTotal');
    if (!svg || !legend) return;

    const COLORS = ['#C14B00','#F28940','#16a34a','#2563eb','#9333ea'];
    const catLabel = { BAKERY:'ベーカリー', SUSHI:'お寿司', LUNCHBOX:'お弁当', CAFE:'カフェ', SIDEDISH:'惣菜' };

    if (!data || !data.length) {
        if (totalEl) totalEl.textContent = '—';
        legend.innerHTML = '<div class="list-empty" style="font-size:0.72rem;padding:0.5rem 0;">データなし</div>';
        return;
    }

    const total = data.reduce((s, d) => s + (d.count || 0), 0);
    if (totalEl) totalEl.textContent = total;

    // 기존 세그먼트 제거
    svg.querySelectorAll('.donut-seg').forEach(e => e.remove());

    const r = 38, cx = 50, cy = 50;
    const circ = 2 * Math.PI * r;
    let offset = 0;

    data.forEach((d, i) => {
        const pct  = total ? d.count / total : 0;
        const dash = pct * circ;
        const seg  = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
        seg.setAttribute('cx', cx);
        seg.setAttribute('cy', cy);
        seg.setAttribute('r', r);
        seg.setAttribute('fill', 'none');
        seg.setAttribute('stroke', COLORS[i % COLORS.length]);
        seg.setAttribute('stroke-width', '16');
        seg.setAttribute('stroke-dasharray', `${dash} ${circ - dash}`);
        seg.setAttribute('stroke-dashoffset', -offset);
        seg.setAttribute('transform', 'rotate(-90 50 50)');
        seg.classList.add('donut-seg');
        svg.appendChild(seg);
        offset += dash;
    });

    legend.innerHTML = data.map((d, i) => `
        <div class="legend-row">
            <span class="legend-dot" style="background:${COLORS[i % COLORS.length]}"></span>
            <span>${catLabel[d.category] || d.category}</span>
            <span class="legend-pct">${total ? Math.round(d.count / total * 100) : 0}%</span>
        </div>`).join('');
}

// ── 회원 테이블 ──
function renderMembers(data) {
    const t = document.getElementById('memberTbody');
    t.innerHTML = '';
    if (!data.length) { t.innerHTML = '<tr><td colspan="8" style="text-align:center;padding:2rem;color:var(--light-gray);">データがありません</td></tr>'; return; }
    const statusBadgeMap = { ACTIVE:'badge-active', SUSPENDED:'badge-pending', WITHDRAWN:'badge-deleted' };
    const statusLabelMap = { ACTIVE:'有効', SUSPENDED:'停止中', WITHDRAWN:'退会済' };
    data.forEach(m => {
        const rbc = m.role==='ROLE_ADMIN'?'badge-admin':m.role==='ROLE_OWNER'?'badge-owner':'badge-user';
        const isActive    = m.status === 'ACTIVE';
        const isSuspended = m.status === 'SUSPENDED';
        const isWithdrawn = m.status === 'WITHDRAWN';
        t.innerHTML += `<tr style="${!isActive?'opacity:0.55;':''}">
            <td class="text-mono text-sm">#${m.id}</td>
            <td><span class="fw-700">${m.lastName} ${m.firstName}</span><br><span class="text-sm text-muted">${m.lastNameKana} ${m.firstNameKana}</span></td>
            <td class="text-sm">${m.email}</td>
            <td class="text-sm text-mono">${m.phone}</td>
            <td><span class="badge ${rbc}">${m.role.replace('ROLE_','')}</span></td>
            <td class="text-sm text-muted">${m.createdAt}</td>
            <td><span class="badge ${statusBadgeMap[m.status]||'badge-active'}">${statusLabelMap[m.status]||m.status}</span></td>
            <td><div class="action-btns">
                ${isActive?`<button class="btn-admin btn-admin-outline" onclick="openRoleModal(${m.id})" title="役割変更"><i class="bi bi-person-gear"></i></button>`:''}
                ${isActive?`<button class="btn-admin btn-admin-danger" onclick="suspendMember(${m.id})" title="停止"><i class="bi bi-slash-circle"></i></button>`:''}
                ${isSuspended?`<button class="btn-admin btn-admin-success" onclick="restoreMember(${m.id})" title="停止解除"><i class="bi bi-arrow-counterclockwise"></i></button>`:''}
                ${!isWithdrawn?`<button class="btn-admin btn-admin-ghost" onclick="withdrawMember(${m.id})" title="退会処理"><i class="bi bi-person-x"></i></button>`:''}
            </div></td>
        </tr>`;
    });
    document.getElementById('memberCount').textContent = `${data.length}名`;
}

function filterMembers() {
    const kw = document.getElementById('memberSearch').value.toLowerCase();
    const role = document.getElementById('memberRoleFilter').value;
    const st = document.getElementById('memberStatusFilter').value;
    filteredMembers = MEMBERS.filter(m => {
        const mk = !kw || m.email.toLowerCase().includes(kw) || (m.lastName+m.firstName).includes(kw);
        const mr = !role || m.role === role;
        const ms = !st || (st==='active'?!m.deletedAt:!!m.deletedAt);
        return mk && mr && ms;
    });
    renderMembers(filteredMembers);
}

// ── 가게 테이블 ──
const catBadge = {BAKERY:'badge-bakery',SUSHI:'badge-sushi',LUNCHBOX:'badge-lunchbox',CAFE:'badge-cafe',SIDEDISH:'badge-sidedish'};
const catLabel = {BAKERY:'ベーカリー',SUSHI:'お寿司',LUNCHBOX:'お弁当',CAFE:'カフェ',SIDEDISH:'惣菜'};

function renderStores(data) {
    const t = document.getElementById('storeTbody');
    t.innerHTML = '';
    if (!data.length) { t.innerHTML = '<tr><td colspan="9" style="text-align:center;padding:2rem;color:var(--light-gray);">データがありません</td></tr>'; return; }
    data.forEach(s => {
        const del = !!s.deletedAt;
        t.innerHTML += `<tr style="${del?'opacity:0.5;':''}">
            <td class="text-mono text-sm">#${s.id}</td>
            <td class="fw-700">${s.name}</td>
            <td><span class="badge ${catBadge[s.category]}">${catLabel[s.category]}</span></td>
            <td class="text-sm">${s.ownerName}<br><span class="text-muted">${s.ownerEmail}</span></td>
            <td class="text-sm text-muted">${s.address}</td>
            <td class="text-sm text-mono">${s.openTime}〜${s.closeTime}</td>
            <td class="text-sm text-muted">${s.createdAt}</td>
            <td><span class="badge ${del?'badge-deleted':'badge-active'}">${del?'閉店':'営業中'}</span></td>
            <td><div class="action-btns">
                ${!del
                    ?`<button class="btn-admin btn-admin-danger" onclick="deactivateStore(${s.id})"><i class="bi bi-slash-circle"></i> 停止</button>`
                    :`<button class="btn-admin btn-admin-success" onclick="restoreStore(${s.id})"><i class="bi bi-arrow-counterclockwise"></i> 復旧</button>`}
            </div></td>
        </tr>`;
    });
    document.getElementById('storeCount').textContent = `${data.length}件`;
}

function filterStores() {
    const kw = document.getElementById('storeSearch').value.toLowerCase();
    const cat = document.getElementById('storeCatFilter').value;
    const st = document.getElementById('storeStatusFilter').value;
    filteredStores = STORES.filter(s => {
        const mk = !kw || s.name.toLowerCase().includes(kw);
        const mc = !cat || s.category === cat;
        const ms = !st || (st==='active'?!s.deletedAt:!!s.deletedAt);
        return mk && mc && ms;
    });
    renderStores(filteredStores);
}

// ── 주문 테이블 ──
const statusBadge = {PAYMENT_PENDING:'badge-pending',PAYMENT_COMPLETED:'badge-completed',CANCELLED:'badge-cancelled'};
const statusLabel = {PAYMENT_PENDING:'決済待ち',PAYMENT_COMPLETED:'決済完了',CANCELLED:'キャンセル'};

function renderOrders(data) {
    const t = document.getElementById('orderTbody');
    t.innerHTML = '';
    if (!data.length) { t.innerHTML = '<tr><td colspan="9" style="text-align:center;padding:2rem;color:var(--light-gray);">データがありません</td></tr>'; return; }
    data.forEach(o => {
        t.innerHTML += `<tr>
            <td class="text-mono text-sm fw-700">#${o.id}</td>
            <td class="text-sm">${o.memberName}<br><span class="text-muted">${o.email}</span></td>
            <td class="fw-700 text-sm">${o.storeName}</td>
            <td class="text-mono fw-700 text-primary">¥${o.totalPrice.toLocaleString()}</td>
            <td class="text-sm text-muted">${o.itemCount}個</td>
            <td class="text-mono text-sm">${o.pickupCode}</td>
            <td><span class="badge ${statusBadge[o.status]}">${statusLabel[o.status]}</span></td>
            <td class="text-sm text-muted">${o.createdAt}</td>
            <td><button class="btn-admin btn-admin-outline" onclick="openOrderModal(${o.id})"><i class="bi bi-pencil"></i> 変更</button></td>
        </tr>`;
    });
    document.getElementById('orderCount').textContent = `${data.length}件`;
}

function filterOrders() {
    const kw = document.getElementById('orderSearch').value.toLowerCase();
    const st = document.getElementById('orderStatusFilter').value;
    filteredOrders = ORDERS.filter(o => {
        const mk = !kw || o.email.toLowerCase().includes(kw) || o.storeName.toLowerCase().includes(kw);
        const ms = !st || o.status === st;
        return mk && ms;
    });
    renderOrders(filteredOrders);
}

// ── 리뷰 테이블 ──
function renderReviews(data) {
    const t = document.getElementById('reviewTbody');
    t.innerHTML = '';
    if (!data.length) { t.innerHTML = '<tr><td colspan="8" style="text-align:center;padding:2rem;color:var(--light-gray);">データがありません</td></tr>'; return; }
    data.forEach(r => {
        const stars = '★'.repeat(r.rating) + '☆'.repeat(5-r.rating);
        const sc = r.rating<=2?'var(--error)':r.rating<=3?'var(--warning, #d97706)':'#f59e0b';
        t.innerHTML += `<tr>
            <td class="text-mono text-sm">#${r.id}</td>
            <td class="fw-700 text-sm">${r.storeName}</td>
            <td class="text-sm">${r.memberName}<br><span class="text-muted">${r.email}</span></td>
            <td class="fw-700 text-sm">${r.title}</td>
            <td class="text-sm text-muted" style="max-width:160px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">${r.content}</td>
            <td style="color:${sc};">${stars}</td>
            <td class="text-sm text-muted">${r.createdAt}</td>
            <td><button class="btn-admin btn-admin-danger" onclick="deleteReview(${r.id})"><i class="bi bi-trash"></i> 削除</button></td>
        </tr>`;
    });
    document.getElementById('reviewCount').textContent = `${data.length}件`;
}

function filterReviews() {
    const kw = document.getElementById('reviewSearch').value.toLowerCase();
    const mr = parseInt(document.getElementById('reviewRatingFilter').value) || 99;
    filteredReviews = REVIEWS.filter(r => {
        const mk = !kw || r.storeName.toLowerCase().includes(kw) || r.content.toLowerCase().includes(kw);
        return mk && r.rating <= mr;
    });
    renderReviews(filteredReviews);
}

// ── 초기 렌더 ──
renderMembers(MEMBERS); renderStores(STORES); renderOrders(ORDERS); renderReviews(REVIEWS);

// ── 모달 ──
function openModal(id)  { document.getElementById(id).classList.add('active'); }
function closeModal(id) { document.getElementById(id).classList.remove('active'); }

function openRoleModal(id) {
    currentMemberId = id;
    const m = MEMBERS.find(x=>x.id===id);
    document.getElementById('roleModalName').textContent = m.lastName+' '+m.firstName;
    document.getElementById('roleModalSelect').value = m.role;
    openModal('roleModal');
}

function confirmRoleChange() {
    const r = document.getElementById('roleModalSelect').value;
    fetch('/api/admin/members/role', {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ memberId: currentMemberId, role: r })
    })
    .then(() => {
        loadMembers();
        closeModal('roleModal');
        showToast('役割を変更しました', 'success');
    })
    .catch(() => showToast('エラーが発生しました', 'error'));
}


function openOrderModal(id) {
    currentOrderId = id;
    const o = ORDERS.find(x=>x.id===id);
    document.getElementById('orderModalId').textContent = '#'+id;
    document.getElementById('orderModalSelect').value = o.status;
    openModal('orderModal');
}

function confirmOrderChange() {
    const s = document.getElementById('orderModalSelect').value;
    fetch('/api/admin/orders/status', {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ orderId: currentOrderId, orderStatus: s })
    })
    .then(() => {
        loadOrders();
        closeModal('orderModal');
        showToast(`注文 #${currentOrderId} のステータスを変更しました`, 'success');
    })
    .catch(() => showToast('エラーが発生しました', 'error'));
}

function suspendMember(id) {
    if (!confirm('このメンバーを停止しますか？')) return;
    fetch(`/api/admin/members/${id}/suspend`, { method: 'PATCH' })
        .then(() => { loadMembers(); showToast('停止しました', 'success'); })
        .catch(() => showToast('エラーが発生しました', 'error'));
}
function restoreMember(id) {
    fetch(`/api/admin/members/${id}/restore`, { method: 'PATCH' })
        .then(() => { loadMembers(); showToast('停止を解除しました', 'success'); })
        .catch(() => showToast('エラーが発生しました', 'error'));
}
function withdrawMember(id) {
    if (!confirm('このメンバーを退会処理しますか？この操作は取り消せません。')) return;
    fetch(`/api/admin/members/${id}/withdraw`, { method: 'PATCH' })
        .then(() => { loadMembers(); showToast('退会処理しました', 'success'); })
        .catch(() => showToast('エラーが発生しました', 'error'));
}
function deactivateStore(id) {
    const reason = prompt('停止理由を入力してください（任意）');
    if (reason === null) return;
    fetch(`/api/admin/stores/${id}/deactivate`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ stopReason: reason || null })
    })
    .then(() => { loadStores(); showToast('店舗を停止しました', 'success'); })
    .catch(() => showToast('エラーが発生しました', 'error'));
}
function restoreStore(id) {
    fetch(`/api/admin/stores/${id}/restore`, { method: 'PATCH' })
        .then(() => { loadStores(); showToast('店舗を復旧しました', 'success'); })
        .catch(() => showToast('エラーが発生しました', 'error'));
}

function deleteReview(id) {
    if (!confirm('このレビューを削除しますか？')) return;
    fetch(`/api/admin/reviews/${id}`, { method: 'DELETE' })
        .then(() => { loadReviews(); showToast('レビューを削除しました', 'success'); })
        .catch(() => showToast('エラーが発生しました', 'error'));
}

// ── 클레임 API 로드 ──
// COMPLAINTS 상태는 파일 상단에서 선언됨

function loadComplaints() {
    fetch('/api/admin/complaints?page=0&size=100')
        .then(r => r.json())
        .then(res => {
            COMPLAINTS = (res.data && res.data.data) || [];
            filteredComplaints = [...COMPLAINTS];
            renderComplaints(filteredComplaints);
        });
}

// ── 클레임 테이블 렌더 ──
const complaintTypeBadge = {
    USER_TO_STORE:  'badge-owner',
    USER_TO_ADMIN:  'badge-user',
    OWNER_TO_ADMIN: 'badge-admin'
};
const complaintTypeLabel = {
    USER_TO_STORE:  'ユーザー→店舗',
    USER_TO_ADMIN:  'ユーザー→運営',
    OWNER_TO_ADMIN: 'オーナー→運営'
};
const complaintStatusBadge = {
    PENDING:     'badge-pending',
    IN_PROGRESS: 'badge-owner',
    RESOLVED:    'badge-completed'
};
const complaintStatusLabel = {
    PENDING:     '未対応',
    IN_PROGRESS: '対応中',
    RESOLVED:    '解決済'
};

function renderComplaints(data) {
    const t = document.getElementById('complaintTbody');
    t.innerHTML = '';
    if (!data.length) {
        t.innerHTML = '<tr><td colspan="8" style="text-align:center;padding:2rem;color:var(--light-gray);">データがありません</td></tr>';
        return;
    }
    data.forEach(c => {
        t.innerHTML += `<tr>
            <td class="text-mono text-sm">#${c.id}</td>
            <td><span class="badge ${complaintTypeBadge[c.complaintType]||'badge-user'}">${complaintTypeLabel[c.complaintType]||c.complaintType}</span></td>
            <td class="text-sm">${c.reporterName}<br><span class="text-muted">${c.reporterEmail}</span></td>
            <td class="text-sm text-muted">${c.targetStoreName || '-'}</td>
            <td class="fw-700 text-sm">${c.title}</td>
            <td><span class="badge ${complaintStatusBadge[c.status]||'badge-pending'}">${complaintStatusLabel[c.status]||c.status}</span></td>
            <td class="text-sm text-muted">${c.createdAt}</td>
            <td><button class="btn-admin btn-admin-outline" onclick="openComplaintModal(${c.id})"><i class="bi bi-reply"></i> 対応</button></td>
        </tr>`;
    });
    document.getElementById('complaintCount').textContent = `${data.length}件`;
}

function filterComplaints() {
    const type = document.getElementById('complaintTypeFilter').value;
    const st   = document.getElementById('complaintStatusFilter').value;
    filteredComplaints = COMPLAINTS.filter(c => {
        const mt = !type || c.complaintType === type;
        const ms = !st   || c.status === st;
        return mt && ms;
    });
    renderComplaints(filteredComplaints);
}

function openComplaintModal(id) {
    currentComplaintId = id;
    const c = COMPLAINTS.find(x => x.id === id);
    document.getElementById('complaintContent').value = c.content;
    document.getElementById('complaintModalStatus').value = c.status;
    document.getElementById('complaintReply').value = c.adminReply || '';
    openModal('complaintModal');
}

function confirmComplaintReply() {
    const reply  = document.getElementById('complaintReply').value.trim();
    const status = document.getElementById('complaintModalStatus').value;
    if (!reply) { showToast('返答内容を入力してください', 'error'); return; }
    fetch('/api/admin/complaints/reply', {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ complaintId: currentComplaintId, adminReply: reply, status: status })
    })
    .then(r => r.json())
    .then(() => {
        const c = COMPLAINTS.find(x => x.id === currentComplaintId);
        c.adminReply = reply;
        c.status = status;
        renderComplaints(filteredComplaints);
        closeModal('complaintModal');
        showToast('返答を送信しました', 'success');
    })
    .catch(() => showToast('エラーが発生しました', 'error'));
}

function evictCache() {
    showToast('キャッシュ初期化リクエスト中...', 'info');
    fetch('/api/stores/admin/cache/evict', { method: 'POST' })
        .then(() => showToast('キャッシュを初期化しました', 'success'))
        .catch(() => showToast('エラーが発生しました', 'error'));
}

// ── 토스트 — 기존 style.css의 .toast 구조에 맞춤 ──
function showToast(msg, type='success') {
    const wrap = document.getElementById('toastWrap');
    const iconMap = { success:'bi-check-circle-fill', error:'bi-x-circle-fill', info:'bi-info-circle-fill' };
    const t = document.createElement('div');
    t.className = `toast ${type}`;
    t.innerHTML = `
        <div class="toast-icon"><i class="bi ${iconMap[type]||iconMap.info}"></i></div>
        <span class="toast-msg">${msg}</span>
        <button class="toast-close" onclick="this.parentElement.remove()"><i class="bi bi-x"></i></button>`;
    wrap.appendChild(t);
    setTimeout(() => { t.classList.add('hide'); setTimeout(()=>t.remove(), 350); }, 3500);
}

// ── 오버레이 클릭 닫기 ──
document.querySelectorAll('.admin-overlay').forEach(el => {
    el.addEventListener('click', e => { if (e.target===el) el.classList.remove('active'); });
});