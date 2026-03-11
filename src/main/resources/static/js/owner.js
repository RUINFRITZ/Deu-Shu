/**
 * owner.js — 사업자 페이지 전용 스크립트 (v2)
 *
 * 변경 사항:
 *  - 사업자등록번호 입력 → refreshAddress 연동
 *  - 주소 readonly, btnRefreshAddress 클릭 시 lat/lng 까지 hidden 갱신 + upsert
 *  - lat/lng 입력 필드 제거 → hidden input 으로 관리
 *  - 영업시간 커스텀 선택기 (午前/午後) ↔ HH:mm 변환
 *  - storeThumbnail / storePhoto / itemThumbnail: 파일 선택 → S3 업로드
 *  - itemExpireAt: flatpickr 일본어 달력
 */

'use strict';

// =====================================================================
// 공통 유틸
// =====================================================================

async function apiCall(url, options = {}) {
    const res = await fetch(url, {
        headers: { 'Content-Type': 'application/json' },
        ...options,
    });
    return res.json();
}

function showToast(message, type = 'success') {
    const wrap = document.getElementById('toastWrap');
    const toast = document.createElement('div');
    toast.className = 'owner-toast' + (type === 'error' ? ' error' : '');
    toast.textContent = message;
    wrap.appendChild(toast);
    requestAnimationFrame(() => requestAnimationFrame(() => toast.classList.add('show')));
    setTimeout(() => {
        toast.classList.remove('show');
        setTimeout(() => toast.remove(), 350);
    }, 3000);
}

function formatPrice(n) { return '¥' + Number(n || 0).toLocaleString('ja-JP'); }
function formatDateTime(str) { return str ? str.replace('T', ' ').slice(0, 16) : '—'; }

// =====================================================================
// ② 커스텀 시간 선택기 유틸 (午前/午後)
//    prefix: 'storeOpen' | 'storeClose'
// =====================================================================

/** "HH:mm" 문자열 → 커스텀 셀렉트에 바인딩 */
function setTimePicker(prefix, timeStr) {
    if (!timeStr) return;
    const [hStr, mStr] = timeStr.split(':');
    const h = parseInt(hStr, 10);
    const m = String(parseInt(mStr, 10)).padStart(2, '0');

    const ampm = h < 12 ? 'AM' : 'PM';
    const hour  = h === 0 ? 12 : h > 12 ? h - 12 : h;

    const ampmEl = document.getElementById(prefix + 'AmPm');
    const hourEl = document.getElementById(prefix + 'Hour');
    const minEl  = document.getElementById(prefix + 'Min');

    if (ampmEl) ampmEl.value = ampm;
    if (hourEl) hourEl.value = String(hour);
    // 분은 가장 가까운 옵션으로 (정확히 없으면 00)
    if (minEl) {
        const opts = Array.from(minEl.options).map(o => o.value);
        minEl.value = opts.includes(m) ? m : '00';
    }
}

/** 커스텀 셀렉트 → "HH:mm" 문자열 반환 */
function getTimePicker(prefix) {
    const ampm = document.getElementById(prefix + 'AmPm').value;
    const hour = parseInt(document.getElementById(prefix + 'Hour').value, 10);
    const min  = document.getElementById(prefix + 'Min').value;

    let h = hour;
    if (ampm === 'AM' && hour === 12) h = 0;
    if (ampm === 'PM' && hour !== 12) h = hour + 12;

    return String(h).padStart(2, '0') + ':' + min;
}

// =====================================================================
// ③⑦ S3 업로드 공통 함수
//    POST /api/owner/upload (multipart/form-data)
//    백엔드가 S3에 저장 후 { success: true, url: "https://..." } 반환
// =====================================================================

async function uploadFile(file) {
    const formData = new FormData();
    formData.append('file', file);

    const res = await fetch('/api/owner/upload', {
        method: 'POST',
        body: formData,   // Content-Type은 브라우저가 자동 설정 (multipart boundary 포함)
    });
    return res.json();
}

/** 미리보기 이미지 렌더링 헬퍼 */
function renderPreview(containerId, url) {
    const el = document.getElementById(containerId);
    if (!el) return;
    if (url) {
        el.innerHTML = `<img src="${url}" class="thumbnail-preview"
            onerror="this.src='/images/no-image.png'" alt="プレビュー">`;
    } else {
        el.innerHTML = '';
    }
}

// ── 가게 대표이미지 업로드 ───────────────────────────────────────────
async function uploadStoreThumbnail(input) {
    const file = input.files[0];
    if (!file) return;

    document.getElementById('storeThumbnailFileName').textContent = file.name;
    const uploading = document.getElementById('storeThumbnailUploading');
    uploading.classList.remove('d-none');

    try {
        const data = await uploadFile(file);
        if (data.success) {
            document.getElementById('storeThumbnailUrl').value = data.url;
            renderPreview('storeThumbnailPreview', data.url);
            showToast('代表画像をアップロードしました');
        } else {
            showToast(data.message || 'アップロードに失敗しました', 'error');
        }
    } catch {
        showToast('サーバーエラーが発生しました', 'error');
    } finally {
        uploading.classList.add('d-none');
        input.value = '';   // 같은 파일 재선택 가능하도록 초기화
    }
}

// ── 가게 추가 사진 업로드 ────────────────────────────────────────────
function triggerPhotoUpload() {
    if (!currentStoreId) {
        showToast('先に店舗情報を登録してください', 'error');
        return;
    }
    document.getElementById('storePhotoFile').click();
}

async function uploadStorePhoto(input) {
    const file = input.files[0];
    if (!file) return;

    const currentCount = document.querySelectorAll('#photoGrid .photo-item').length;

    try {
        // 1) S3 업로드
        const uploadData = await uploadFile(file);
        if (!uploadData.success) {
            showToast(uploadData.message || 'アップロードに失敗しました', 'error');
            return;
        }
        // 2) DB 등록
        const data = await apiCall('/api/owner/store/images', {
            method: 'POST',
            body: JSON.stringify({ imageUrl: uploadData.url, sortOrder: currentCount }),
        });
        if (data.success) {
            showToast('写真を追加しました');
            const storeData = await apiCall('/api/owner/store');
            renderStoreImages(storeData.images || []);
        } else {
            showToast(data.message || '追加に失敗しました', 'error');
        }
    } catch {
        showToast('サーバーエラーが発生しました', 'error');
    } finally {
        input.value = '';
    }
}

// ── 상품 이미지 업로드 ───────────────────────────────────────────────
async function uploadItemThumbnail(input) {
    const file = input.files[0];
    if (!file) return;

    document.getElementById('itemThumbnailFileName').textContent = file.name;
    const uploading = document.getElementById('itemThumbnailUploading');
    uploading.classList.remove('d-none');

    try {
        const data = await uploadFile(file);
        if (data.success) {
            document.getElementById('itemThumbnailUrl').value = data.url;
            renderPreview('itemThumbnailPreview', data.url);
            showToast('商品画像をアップロードしました');
        } else {
            showToast(data.message || 'アップロードに失敗しました', 'error');
        }
    } catch {
        showToast('サーバーエラーが発生しました', 'error');
    } finally {
        uploading.classList.add('d-none');
        input.value = '';
    }
}

// =====================================================================
// 섹션 전환
// =====================================================================

function switchSection(sectionId, clickedBtn) {
    document.querySelectorAll('.owner-section').forEach(s => s.classList.remove('active'));
    document.getElementById('section-' + sectionId).classList.add('active');

    document.querySelectorAll('.sidebar-nav-item').forEach(b => b.classList.remove('active'));
    if (clickedBtn) clickedBtn.classList.add('active');

    if (sectionId === 'sales') loadSales();
}

// =====================================================================
// 페이지 초기 로드
// =====================================================================

document.addEventListener('DOMContentLoaded', async () => {

    // ⑥ flatpickr 일본어 달력 초기화
    if (window.flatpickr) {
        flatpickr('#itemExpireAt', {
            locale: 'ja',
            enableTime: true,
            dateFormat: 'Y-m-d\\TH:i',
            time_24hr: true,
            minDate: 'today',
        });
    }

    // 로그인 / 권한 확인
    const me = await apiCall('/api/auth/me');
    if (!me.success || (me.role !== 'ROLE_OWNER' && me.role !== 'ROLE_ADMIN')) {
        alert('オーナーアカウントでログインしてください');
        location.href = '/';
        return;
    }

    // 사이드바 프로필 바인딩 (PC + Offcanvas 양쪽)
    const name = (me.lastName || '') + (me.firstName || '') || me.email;
    ['sidebarName', 'sidebarNameCanvas'].forEach(id => {
        const el = document.getElementById(id);
        if (el) el.textContent = name + ' 様';
    });
    ['sidebarEmail', 'sidebarEmailCanvas'].forEach(id => {
        const el = document.getElementById(id);
        if (el) el.textContent = me.email || '';
    });

    // 모달 오버레이 클릭 닫기
    document.getElementById('itemModalOverlay').addEventListener('click', e => {
        if (e.target === document.getElementById('itemModalOverlay')) closeItemModal();
    });
	bindBusinessNumberFormatter('bizNumberInput', clearAddressOnBizChange);
    // 가게 정보 로드
    loadStore();
});

// =====================================================================
// 전역 상태
// =====================================================================
let currentStoreId       = null;
let currentBusinessNumber = null;

// =====================================================================
// 가게 정보 로드 + 폼 바인딩
// =====================================================================

async function loadStore() {
    const data = await apiCall('/api/owner/store');
    if (!data.success) return;

    const store = data.store;
    if (!store) return;

    currentStoreId        = store.id;
    currentBusinessNumber = store.businessNumber;

    // 기본 필드
    document.getElementById('storeName').value     = store.name     || '';
    document.getElementById('storeCategory').value = store.category || '';
    document.getElementById('storeAddress').value  = store.address  || '';
    document.getElementById('storeInfo').value     = store.info     || '';

    // ① 사업자등록번호
    document.getElementById('bizNumberInput').value = store.businessNumber || '';

    // ④ lat/lng → hidden
    document.getElementById('storeLat').value = store.lat || '';
    document.getElementById('storeLng').value = store.lng || '';

    // ② 커스텀 시간 선택기 바인딩
    if (store.openTime)  setTimePicker('storeOpen',  store.openTime.slice(0, 5));
    if (store.closeTime) setTimePicker('storeClose', store.closeTime.slice(0, 5));

    // ③ 대표이미지 URL → hidden + 미리보기
    const thumbUrl = store.thumbnailUrl || '';
    document.getElementById('storeThumbnailUrl').value = thumbUrl;
    if (thumbUrl) {
        document.getElementById('storeThumbnailFileName').textContent = '登録済み';
        renderPreview('storeThumbnailPreview', thumbUrl);
    }

    document.getElementById('storeSubmitBtn').textContent = '✏️ 店舗情報を更新する';

    renderStoreImages(data.images || []);
    loadItems();
}

// =====================================================================
// ①⑤ 주소 자동입력 — 사업자번호로 mock_business_registry 조회
//     lat/lng 까지 stores 테이블에 upsert
// =====================================================================
function formatBusinessNumber(value) {
    let digits = String(value || '').replace(/\D/g, '').slice(0, 10);

    if (digits.length > 5) {
        return digits.replace(/(\d{3})(\d{2})(\d{1,5})/, '$1-$2-$3');
    }
    if (digits.length > 3) {
        return digits.replace(/(\d{3})(\d{1,2})/, '$1-$2');
    }
    return digits;
}

function bindBusinessNumberFormatter(inputId, onFormatted) {
    const input = document.getElementById(inputId);
    if (!input) return;

    input.addEventListener('input', function (e) {
        const formatted = formatBusinessNumber(e.target.value);
        e.target.value = formatted;

        if (typeof onFormatted === 'function') {
            onFormatted();
        }
    });
}

function clearAddressOnBizChange() {
    // 사업자번호 변경 시 주소/위경도 초기화 (사용자에게 재조회 안내)
    document.getElementById('storeAddress').value = '';
    document.getElementById('storeLat').value     = '';
    document.getElementById('storeLng').value     = '';
}

async function refreshAddress() {
    const bizNum = document.getElementById('bizNumberInput').value.trim()
                || currentBusinessNumber;

    if (!bizNum) {
        showToast('事業者登録番号を入力してください', 'error');
        document.getElementById('bizNumberInput').focus();
        return;
    }

    const btn = document.getElementById('btnRefreshAddress');
    btn.disabled = true;
    btn.innerHTML = '<span class="spin d-inline-block">⟳</span> 照会中...';

    try {
        const data = await apiCall(`/api/owner/business/${encodeURIComponent(bizNum)}`);

        if (!data.success) {
            showToast(data.message || '事業者情報が取得できませんでした', 'error');
            return;
        }

        // 가게이름 자동입력 (기존에 직접 입력한 값이 있어도 덮어씀)
        if (data.storeName) {
            document.getElementById('storeName').value = data.storeName;
        }

        // 주소 자동입력 (readonly)
        document.getElementById('storeAddress').value = data.address || '';

        // lat/lng hidden 저장
        document.getElementById('storeLat').value = data.lat || '';
        document.getElementById('storeLng').value = data.lng || '';

        currentBusinessNumber = bizNum;

        // 이미 가게가 등록된 경우 → 주소/위경도 즉시 stores 테이블에 upsert
        if (currentStoreId) {
            await apiCall('/api/owner/store/address', {
                method: 'PATCH',
                body: JSON.stringify({
                    address: data.address,
                    lat:     data.lat,
                    lng:     data.lng,
                }),
            });
        }

        if (data.geocodeWarning) {
            showToast(data.geocodeWarning, 'error');
        } else {
            showToast('店舗名・住所を自動入力しました');
        }

    } catch {
        showToast('サーバーエラーが発生しました', 'error');
    } finally {
        btn.disabled = false;
        btn.innerHTML = '<i class="bi bi-search"></i> 照会';
    }
}

// =====================================================================
// 가게 기본 정보 등록 / 수정
// =====================================================================

async function saveStore() {
    const name    = document.getElementById('storeName').value.trim();
    const category= document.getElementById('storeCategory').value;
    const address = document.getElementById('storeAddress').value.trim();
    const info    = document.getElementById('storeInfo').value.trim();
    const lat     = parseFloat(document.getElementById('storeLat').value)  || 0;
    const lng     = parseFloat(document.getElementById('storeLng').value)  || 0;
    const bizNum  = document.getElementById('bizNumberInput').value.trim() || currentBusinessNumber || '';

    // ② 커스텀 시간 선택기에서 HH:mm 추출
    const openTime  = getTimePicker('storeOpen');
    const closeTime = getTimePicker('storeClose');

    // ③ hidden에 저장된 S3 URL 사용
    const thumbnailUrl = document.getElementById('storeThumbnailUrl').value.trim();

    if (!name)      { showToast('店舗名を入力してください', 'error');    return; }
    if (!category)  { showToast('業種を選択してください',   'error');    return; }
    if (!address)   { showToast('住所を取得してください（「住所を更新」ボタンをご利用ください）', 'error'); return; }
    if (!bizNum)    { showToast('事業者登録番号を入力してください', 'error'); return; }

    const payload = {
        name, category, address, lat, lng,
        openTime, closeTime, thumbnailUrl, info,
        businessNumber: bizNum,   // ① 사업자번호 백엔드에 전달
    };

	const res = await apiCall('/api/owner/store', {
	    method: 'POST',
	    body: JSON.stringify(payload)
	});

    if (res.success) {
        showToast(res.message || '保存しました');
        if (!currentStoreId && res.storeId) {
            currentStoreId = res.storeId;
            document.getElementById('storeSubmitBtn').textContent = '✏️ 店舗情報を更新する';
        }
    } else {
        showToast(res.message || '保存に失敗しました', 'error');
    }
}

// =====================================================================
// 가게 추가 사진 관리
// =====================================================================

function renderStoreImages(images) {
    const grid = document.getElementById('photoGrid');
    grid.querySelectorAll('.photo-item').forEach(el => el.remove());

    images.forEach(img => {
        const el = document.createElement('div');
        el.className = 'photo-item';
        el.dataset.imageId = img.id;
        el.innerHTML = `
            <img src="${img.imageUrl}" alt="店舗写真"
                 onerror="this.src='/images/no-image.png'">
            <button class="photo-delete" onclick="deleteStoreImage(${img.id})"
                    title="削除"><i class="bi bi-x"></i></button>`;
        grid.insertBefore(el, grid.querySelector('.photo-add-box'));
    });
}

async function deleteStoreImage(imageId) {
    if (!confirm('この写真を削除しますか？')) return;
    const data = await apiCall(`/api/owner/store/images/${imageId}`, { method: 'DELETE' });
    if (data.success) {
        showToast('写真を削除しました');
        document.querySelector(`.photo-item[data-image-id="${imageId}"]`)?.remove();
    } else {
        showToast(data.message || '削除に失敗しました', 'error');
    }
}

// =====================================================================
// 상품 목록 로드 + 렌더링
// =====================================================================

async function loadItems() {
    const data = await apiCall('/api/owner/items');
    if (!data.success) return;
    renderItems(data.items || []);
}

function renderItems(items) {
    const grid = document.getElementById('itemGrid');
    grid.innerHTML = '';

    if (items.length === 0) {
        grid.innerHTML = `<div class="text-center py-5 text-muted" style="grid-column:1/-1;">
            <i class="bi bi-box-seam fs-1 d-block mb-2"></i>登録された商品はありません</div>`;
        return;
    }

    items.forEach(item => {
        const card = document.createElement('div');
        card.className = 'item-card';
        card.innerHTML = `
            <div class="item-card-img">
                ${item.thumbnailUrl
                    ? `<img src="${item.thumbnailUrl}" alt="${item.name}"
                           onerror="this.parentNode.innerHTML='<i class=\\'bi bi-image\\'></i>'">`
                    : '<i class="bi bi-image"></i>'}
            </div>
            <div class="item-card-body">
                <div class="item-card-name">${item.name}</div>
                <div>
                    <span class="price-original">${formatPrice(item.originalPrice)}</span>
                    <span class="price-discount">${formatPrice(item.discountPrice)}</span>
                    <span class="item-badge">${item.discountRate}%OFF</span>
                </div>
                <div class="item-meta"><i class="bi bi-box"></i> 在庫: <strong>${item.stock}個</strong></div>
                <div class="item-meta"><i class="bi bi-clock"></i> 販売期限: ${formatDateTime(item.expireAt)}</div>
            </div>
            <div class="item-card-actions">
                <button class="btn btn-outline-secondary btn-sm flex-fill"
                        onclick="openEditItemModal(${item.id})">
                    <i class="bi bi-pencil"></i> 修正
                </button>
                <button class="btn btn-outline-danger btn-sm" onclick="deleteItem(${item.id})">
                    <i class="bi bi-trash"></i>
                </button>
            </div>`;
        grid.appendChild(card);
    });
}

// =====================================================================
// 상품 등록 모달
// =====================================================================

let editingItemId = null;

function openItemModal() {
    editingItemId = null;
    document.getElementById('itemModalTitle').textContent = '商品を登録する';
    document.getElementById('itemForm').reset();

    // flatpickr 초기화
    const fp = document.getElementById('itemExpireAt')._flatpickr;
    if (fp) fp.clear();

    // 이미지 미리보기 초기화
    document.getElementById('itemThumbnailUrl').value       = '';
    document.getElementById('itemThumbnailFileName').textContent = '未選択';
    document.getElementById('itemThumbnailPreview').innerHTML   = '';
    document.getElementById('discountRatePreview').textContent  = '';

    document.getElementById('itemModalOverlay').classList.add('open');
    document.body.style.overflow = 'hidden';
}

function openEditItemModal(itemId) {
    apiCall('/api/owner/items').then(data => {
        const item = (data.items || []).find(i => i.id === itemId);
        if (!item) return;

        editingItemId = itemId;
        document.getElementById('itemModalTitle').textContent     = '商品情報を修正する';
        document.getElementById('itemName').value                 = item.name          || '';
        document.getElementById('itemOriginalPrice').value        = item.originalPrice || '';
        document.getElementById('itemDiscountPrice').value        = item.discountPrice || '';
        document.getElementById('itemDiscountRate').value         = item.discountRate  || '';
        document.getElementById('itemStock').value                = item.stock         || '';

        // ⑥ flatpickr에 값 세팅
        const fp = document.getElementById('itemExpireAt')._flatpickr;
        if (fp && item.expireAt) {
            fp.setDate(item.expireAt.replace(' ', 'T').slice(0, 16), false, 'Y-m-d\\TH:i');
        }

        // ⑦ 기존 이미지 URL → hidden + 미리보기
        const url = item.thumbnailUrl || '';
        document.getElementById('itemThumbnailUrl').value = url;
        document.getElementById('itemThumbnailFileName').textContent = url ? '登録済み' : '未選択';
        renderPreview('itemThumbnailPreview', url);

        updateDiscountRatePreview();

        document.getElementById('itemModalOverlay').classList.add('open');
        document.body.style.overflow = 'hidden';
    });
}

function closeItemModal() {
    document.getElementById('itemModalOverlay').classList.remove('open');
    document.body.style.overflow = '';
}

function updateDiscountRatePreview() {
    const original = parseFloat(document.getElementById('itemOriginalPrice').value);
    const discount = parseFloat(document.getElementById('itemDiscountPrice').value);
    const preview  = document.getElementById('discountRatePreview');

    if (original > 0 && discount >= 0 && discount <= original) {
        const rate = Math.round((1 - discount / original) * 100);
        document.getElementById('itemDiscountRate').value = rate;
        preview.textContent = `→ ${rate}% OFF`;
    } else {
        preview.textContent = '';
    }
}

async function saveItem() {
    const name          = document.getElementById('itemName').value.trim();
    const originalPrice = parseInt(document.getElementById('itemOriginalPrice').value);
    const discountPrice = parseInt(document.getElementById('itemDiscountPrice').value);
    const discountRate  = parseInt(document.getElementById('itemDiscountRate').value)  || 0;
    const stock         = parseInt(document.getElementById('itemStock').value);
    const expireAt      = document.getElementById('itemExpireAt').value;                // flatpickr 출력값
    const thumbnailUrl  = document.getElementById('itemThumbnailUrl').value.trim();     // ⑦ hidden

    if (!name)                         { showToast('商品名を入力してください',   'error'); return; }
    if (isNaN(originalPrice))          { showToast('定価を入力してください',     'error'); return; }
    if (isNaN(discountPrice))          { showToast('割引価格を入力してください', 'error'); return; }
    if (discountPrice > originalPrice) { showToast('割引価格は定価以下にしてください', 'error'); return; }
    if (isNaN(stock) || stock < 0)     { showToast('在庫数を入力してください',   'error'); return; }
    if (!expireAt)                     { showToast('販売期限を入力してください', 'error'); return; }

    const payload = { name, originalPrice, discountPrice, discountRate,
                      stock, expireAt, thumbnailUrl };

    const data = editingItemId
        ? await apiCall(`/api/owner/items/${editingItemId}`, { method: 'PUT',  body: JSON.stringify(payload) })
        : await apiCall('/api/owner/items',                   { method: 'POST', body: JSON.stringify(payload) });

    if (data.success) {
        showToast(data.message || '保存しました');
        closeItemModal();
        loadItems();
    } else {
        showToast(data.message || '保存に失敗しました', 'error');
    }
}

async function deleteItem(itemId) {
    if (!confirm('この商品を削除しますか？')) return;
    const data = await apiCall(`/api/owner/items/${itemId}`, { method: 'DELETE' });
    if (data.success) {
        showToast('商品を削除しました');
        loadItems();
    } else {
        showToast(data.message || '削除に失敗しました', 'error');
    }
}

// =====================================================================
// 매출 정산
// =====================================================================

async function loadSales(isManual = false) {
    const btn = document.getElementById('btnRefreshSales');
    btn.disabled = true;
    btn.innerHTML = '<span class="spin d-inline-block">⟳</span> 更新中...';

    try {
        const endpoint = isManual ? '/api/owner/sales/refresh' : '/api/owner/sales';
        const method   = isManual ? 'POST' : 'GET';
        const data     = await apiCall(endpoint, { method });

        if (!data.success) return;

        const s = data.summary || {};
        document.getElementById('salesRevenue').textContent  = formatPrice(s.totalRevenue);
        document.getElementById('salesOrders').textContent   = s.totalOrders     || 0;
        document.getElementById('salesPickup').textContent   = s.pickupCompleted || 0;
        document.getElementById('salesPending').textContent  = s.paymentPending  || 0;
        document.getElementById('salesCanceled').textContent = s.canceled        || 0;
        document.getElementById('salesRefreshedAt').textContent =
            data.refreshedAt ? formatDateTime(data.refreshedAt) : '—';

        const tbody = document.getElementById('salesDailyBody');
        tbody.innerHTML = '';
        const daily = data.daily || [];

        if (daily.length === 0) {
            tbody.innerHTML = `<tr><td colspan="3" class="text-center text-muted py-4">
                データがありません</td></tr>`;
        } else {
            [...daily].reverse().forEach(d => {
                const tr = document.createElement('tr');
                tr.innerHTML = `
                    <td>${d.orderDate || '—'}</td>
                    <td>${d.dailyCount || 0} 件</td>
                    <td class="text-end">${formatPrice(d.dailyRevenue)}</td>`;
                tbody.appendChild(tr);
            });
        }

        if (isManual) showToast('売上データを更新しました');
    } catch {
        showToast('データ取得に失敗しました', 'error');
    } finally {
        btn.disabled = false;
        btn.innerHTML = '<i class="bi bi-arrow-clockwise"></i> 今すぐ更新';
    }
}