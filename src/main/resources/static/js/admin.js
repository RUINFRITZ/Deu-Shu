/* =============================================
   관리자 공통 JS
   모든 admin 페이지에서 공유하는 함수
============================================= */

// ── 모달 ──
function openModal(id)  { document.getElementById(id).classList.add('active'); }
function closeModal(id) { document.getElementById(id).classList.remove('active'); }

// ── 캐시 초기화 ──
function evictCache() {
    showToast('キャッシュ初期化リクエスト中...', 'info');
    fetch('/api/stores/admin/cache/evict', { method: 'POST' })
        .then(() => showToast('キャッシュを初期化しました', 'success'))
        .catch(() => showToast('キャッシュ初期化に失敗しました', 'error'));
}

// ── 토스트 ──
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
document.addEventListener('DOMContentLoaded', function() {
    document.querySelectorAll('.admin-overlay').forEach(el => {
        el.addEventListener('click', e => { if (e.target===el) el.classList.remove('active'); });
    });
});