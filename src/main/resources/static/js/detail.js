// ============================================
// XSS防止 (最上部 - すべての関数より先に)
// ============================================
function escapeHtml(str) {
    return String(str).replace(/[&<>"']/g, m => ({
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        '"': "&quot;",
        "'": "&#39;"
    }[m]));
}

// ============================================
// レビューモーダル — 2段階: 注文選択 → レビュー作成
// ============================================
let _selectedOrderId = null;

async function openReviewModal() {
    _selectedOrderId = null;
    showReviewStep(1);
    document.getElementById('reviewModal').classList.add('active');

    try {
        const res = await fetch(`/api/v1/orders/my?storeId=${storeId}`);
        if (res.status === 401) {
            Swal.fire({ title: 'ログインが必要です', text: 'レビューを作成するにはログインしてください。', icon: 'warning', iconColor: '#F28940', confirmButtonColor: '#065f46', confirmButtonText: '確認' });
            document.getElementById('reviewModal').classList.remove('active');
            return;
        }
        const data   = await res.json();
        const orders = data.result ?? data.data ?? data ?? [];
        renderOrderList(Array.isArray(orders) ? orders : []);
    } catch (err) {
        console.error('[注文リスト エラー]', err);
        document.getElementById('orderListBody').innerHTML =
            '<p style="color:var(--gray-500);padding:1rem;">注文リストの読み込みに失敗しました。</p>';
    }
}

function closeReviewModal() {
    document.getElementById('reviewModal').classList.remove('active');
	ratingLocked = false;
}
function handleModalClick(event) {
    if (event.target.id === 'reviewModal') closeReviewModal();
}

function showReviewStep(step) {
    document.getElementById('reviewStep1').style.display = step === 1 ? '' : 'none';
    document.getElementById('reviewStep2').style.display = step === 2 ? '' : 'none';
}

/* =========================================================================
   [ ドゥーシュー ] レビュー作成のための注文リストレンダリング
   修正: 受取完了 (PICKUP_COMPLETED) 状態の注文のみをフィルタリングして表示します。
========================================================================= */
function renderOrderList(orders) {
    const body = document.getElementById('orderListBody');
    
    // 1. ステータスが 'PICKUP_COMPLETED' (受取完了) の注文のみを抽出
    // ※ バックエンドの DTO フィールド名が 'status' または 'orderStatus' の場合を両方考慮
    const pickupCompletedOrders = orders.filter(order => {
        const currentStatus = order.status || order.orderStatus;
        return currentStatus === 'PICKUP_COMPLETED';
    });

    // 2. 受取完了の注文が一つもない場合のメッセージ処理
    if (!pickupCompletedOrders.length) {
        body.innerHTML = `
            <div style="text-align:center; padding:2rem 1rem;">
                <i class="bi bi-bag-x" style="font-size: 2rem; color: #d1d5db;"></i>
                <p style="color:var(--gray-500); margin-top: 0.5rem;">
                    レビューを作成できる注文がありません。<br>
                    <span style="font-size: 0.8rem;">(※ レビューは商品の「受取完了」後に作成可能です)</span>
                </p>
            </div>`;
        return;
    }

    body.innerHTML = '';
    
    // 3. フィルタリングされた注文のみを画面に描画
    pickupCompletedOrders.forEach(order => {
        const itemNames = (order.items || []).map(i => i.itemName).join(', ');
        const date      = order.createdAt
            ? new Date(order.createdAt).toLocaleDateString('ja-JP') : '';
            
        const div = document.createElement('div');
        div.className = 'review-order-item' + (order.reviewed ? ' reviewed' : '');
        div.innerHTML = `
            <div class="roi-info">
                <span class="roi-date">${date}</span>
                <span class="roi-items">${escapeHtml(itemNames)}</span>
                <span class="roi-price">${(order.totalPrice || 0).toLocaleString()}円</span>
            </div>
            ${order.reviewed
                ? '<span class="roi-badge done">レビュー済み</span>'
                : `<button class="roi-btn" onclick="selectOrderForReview(${order.orderId})">選択</button>`
            }
        `;
        body.appendChild(div);
    });
}

function selectOrderForReview(orderId) {
    _selectedOrderId = orderId;
    _lastOrderId     = orderId;
    showReviewStep(2);
}

function backToOrderList() {
    _selectedOrderId = null;
    showReviewStep(1);
}

// ============================================
// 評価選択
// ============================================
const starButtons = document.querySelectorAll('.star-btn');
let currentRating = 5.0;
let ratingLocked  = false;

starButtons.forEach((star, index) => {
    star.addEventListener("mousemove", (e) => {
		if (ratingLocked) return;
        const rect = star.getBoundingClientRect();
        const x = e.clientX - rect.left;
        currentRating = x < rect.width / 2 ? index + 0.5 : index + 1;
        updateStars();
        const ratingValue = document.querySelector('.rating-value');
        if (ratingValue) ratingValue.textContent = currentRating.toFixed(1);
    });
    star.addEventListener("click", (e) => {
		const rect = star.getBoundingClientRect();
		        const x = e.clientX - rect.left;
		        currentRating = x < rect.width / 2 ? index + 0.5 : index + 1;  // ← 클릭 시 정확한 값 저장
		        ratingLocked = true;  // ← 고정
		        updateStars();
		        const ratingValue = document.querySelector('.rating-value');
		        if (ratingValue) ratingValue.textContent = currentRating.toFixed(1);
    });
});

function updateStars() {
    starButtons.forEach((star, index) => {
        const starValue = index + 1;
        if (currentRating >= starValue)
            star.className = "bi bi-star-fill star-btn";
        else if (currentRating >= starValue - 0.5)
            star.className = "bi bi-star-half star-btn";
        else
            star.className = "bi bi-star star-btn";
    });
}

// ============================================
// 文字数カウンター
// ============================================
const textarea     = document.getElementById('reviewContent');
const currentCount = document.getElementById('currentCount');
textarea?.addEventListener('input', () => {
    currentCount.textContent = textarea.value.length;
});

// ============================================
// 画像アップロード (レビューモーダル)
// ============================================
const imageInput   = document.getElementById('imageInput');
const imagePreview = document.getElementById('imagePreview');
const previewImage = document.getElementById('previewImage');

imageInput?.addEventListener('change', (e) => {
    const file = e.target.files[0];
    if (file) {
        const reader = new FileReader();
        reader.onload = (event) => {
            previewImage.src = event.target.result;
            imagePreview.style.display = 'block';
        };
        reader.readAsDataURL(file);
    }
});

function removeImage() {
    imageInput.value = '';
    previewImage.src = '';
    imagePreview.style.display = 'none';
}

// ============================================
// レビュー もっと見る/折りたたむ
// ============================================
function toggleReviewText(button) {
    const reviewItem = button.closest('.review-item');
    const reviewText = reviewItem.querySelector('.review-text');
    if (button.classList.contains('expanded')) {
        reviewText.textContent = reviewText.dataset.short;
        button.innerHTML = 'もっと見る <i class="bi bi-chevron-down"></i>';
        button.classList.remove('expanded');
    } else {
        reviewText.dataset.short = reviewText.textContent;
        reviewText.textContent   = reviewText.dataset.full || reviewText.textContent;
        button.innerHTML = '折りたたむ <i class="bi bi-chevron-up"></i>';
        button.classList.add('expanded');
    }
}

// ============================================
// 詳細データ ロード
// ============================================
document.addEventListener("DOMContentLoaded", () => {
    if (!storeId) return;

    fetch(`/api/stores/${storeId}`)
        .then(res => {
            if (!res.ok) throw new Error("詳細取得失敗: " + res.status);
            return res.json();
        })
        .then(data => renderStore(data))
        .catch(err => console.error("詳細データエラー:", err));

    if (starButtons.length) updateStars();
});

// ============================================
// renderStore
// ============================================
function renderStore(data) {
    const store = {
        name:      data.name      ?? '-',
        category:  data.category  ?? '-',
        address:   data.address   ?? '-',
        lat:       data.lat,
        lng:       data.lng,
        openTime:  data.openTime  || '',
        closeTime: data.closeTime || '',
        phone:     data.phone     ?? '-',
    };
    const images = (data.imageUrls ?? []).filter(Boolean);
    const rating = { avg: data.avgRating ?? 0, count: data.reviewCount ?? 0 };
    const items  = data.items ?? [];

    document.getElementById('storeName').textContent      = store.name;
    document.getElementById('storeCategory').textContent  = store.category;
    document.getElementById('storeAddress').textContent   = store.address;
    document.getElementById('storeHours').textContent     =
        store.openTime && store.closeTime ? `${store.openTime} ~ ${store.closeTime}` : '-';
    document.getElementById('ratingAvg').textContent      = Number(rating.avg).toFixed(1);
    document.getElementById('reviewCount').textContent    = `レビュー ${rating.count}件`;
    document.getElementById('reviewsTitle').textContent   = `レビュー (${rating.count})`;

    const infoEl = document.getElementById('storeInfo');
    if (infoEl) infoEl.textContent = data.info || '案内文は登録されていません。';

    initSlider(images);

    const minDiscountPrice = items.length > 0
        ? Math.min(...items.map(i => i.discountPrice)) : null;
    const totalStock = items.reduce((s, i) => s + i.stock, 0);

    initMap(store.lat, store.lng, store.name, store.category,
            store.closeTime, store.address, minDiscountPrice, totalStock);

    renderItems(items);
    loadReviews(storeId);
    initFavBtn(data.favorited === true);

    // AI レビュー要約 非同期ロード (renderStore 最後に呼び出し)
    loadAiSummary(storeId);
}

// ============================================
// AI レビュー要約 ロード及びレンダリング
// ============================================

/**
 * Gemini Map-Reduce 要約 API 呼び出し
 * GET /api/stores/{storeId}/review-summary
 * 非同期呼び出し → ローディングスピナー → 結果レンダリング
 */
async function loadAiSummary(storeId) {
    const loadingEl = document.getElementById('aiSummaryLoading');
    const emptyEl   = document.getElementById('aiSummaryEmpty');
    const resultEl  = document.getElementById('aiSummaryResult');

    // 初期状態: スピナー表示
    if (loadingEl) loadingEl.style.display = '';
    if (emptyEl)   emptyEl.style.display   = 'none';
    if (resultEl)  resultEl.style.display  = 'none';

    try {
        const res = await fetch(`/api/stores/${storeId}/review-summary`);
        if (!res.ok) throw new Error('AI要約 API エラー: ' + res.status);

        const data = await res.json();

        // スピナー非表示
        if (loadingEl) loadingEl.style.display = 'none';

        // レビューがない場合
        if (!data.hasReviews) {
            if (emptyEl) emptyEl.style.display = '';
            return;
        }

        // AI要約結果 レンダリング
        renderAiSummary(data);
        if (resultEl) resultEl.style.display = '';

    } catch (err) {
        console.error('[AI要約] ロード失敗:', err);
        // エラー時はセクション自体を非表示 (UX妨害防止)
        const block = document.getElementById('aiSummaryBlock');
        if (block) block.style.display = 'none';
    }
}

/**
 * AI要約データをDOMにレンダリング
 * ReviewSummaryResponseDto: { hasReviews, summary, keywords }
 * @param {Object} data - ReviewSummaryResponseDto
 */
function renderAiSummary(data) {
    // 統合要約文
    const summaryEl = document.getElementById('aiSummarySummary');
    if (summaryEl) summaryEl.textContent = data.summary || '';

    // 主要キーワードタグ レンダリング
    const keywordsEl = document.getElementById('aiKeywords');
    if (keywordsEl) {
        keywordsEl.innerHTML = '';
        const kws = data.keywords || [];
        kws.forEach(kw => {
            const span = document.createElement('span');
            span.className   = 'ai-keyword-tag';
            span.textContent = kw;
            keywordsEl.appendChild(span);
        });
    }
}

// ============================================
// レビュー API 呼び出し
// ============================================
function loadReviews(storeId) {
    fetch(`/api/reviews?storeId=${storeId}`)
        .then(res => {
            if (!res.ok) throw new Error("レビュー取得失敗");
            return res.json();
        })
        .then(data => {
            const list = data.items || data || [];
            renderReviews(Array.isArray(list) ? list : []);
        })
        .catch(err => console.error("レビューデータエラー:", err));
}

// ============================================
// レビュー レンダリング
// ============================================
function renderReviews(reviews) {
    const reviewsList = document.getElementById("reviewsList");
    reviewsList.innerHTML = "";
    if (!reviews.length) {
        reviewsList.innerHTML = `<div class="empty">レビューがありません。</div>`;
        return;
    }

    // 現在のログインセッション memberId (ヘッダーから取得)
    const sessionMemberId = window._sessionMemberId ?? null;

    reviews.forEach(r => {
        const stars    = renderStars(r.rating ?? 0);
        const date     = r.createdAt
            ? new Date(r.createdAt).toLocaleDateString('ja-JP') : '';
        const orderDate = r.orderDate
            ? new Date(r.orderDate).toLocaleDateString('ja-JP') : null;
        const isOwner  = sessionMemberId && sessionMemberId === r.memberId;

        const div = document.createElement("div");
        div.className = "review-item";
        div.dataset.reviewId = r.id;
        div.innerHTML = `
            <!-- 左: 作成者情報 -->
            <div class="ri-author">
                <div class="ri-avatar">${escapeHtml((r.firstName || '?')[0])}</div>
                <div class="ri-author-info">
                    <span class="ri-firstname">${escapeHtml(r.firstName || '匿名')}</span>
                    ${orderDate ? `<span class="ri-orderdate">注文日 ${orderDate}</span>` : ''}
                </div>
            </div>

            <!-- 右: レビュー内容 -->
            <div class="ri-body">
                <div class="ri-body-top">
                    <div class="ri-stars">${stars}</div>
                    <span class="ri-date">${date}</span>
                    ${isOwner ? `<button class="ri-delete-btn" onclick="deleteReview(${r.id})">
                        <i class="bi bi-trash3"></i>
                    </button>` : ''}
                </div>
                ${r.orderedItems ? `<div class="ri-ordered-items">
                    <i class="bi bi-bag"></i> ${escapeHtml(r.orderedItems)}
                </div>` : ''}
                <div class="ri-title">${escapeHtml(r.title || '')}</div>
                <div class="ri-content">${escapeHtml(r.content || '')}</div>
                ${r.photoUrl ? `<img class="ri-photo" src="${escapeHtml(r.photoUrl)}" alt="レビュー写真"
                    onclick="openPhotoOverlay('${escapeHtml(r.photoUrl)}')">` : ''}
            </div>
        `;
        reviewsList.appendChild(div);
    });
}

function renderStars(rating) {
    let html = '';
    for (let i = 1; i <= 5; i++) {
        if (rating >= i)
            html += '<i class="bi bi-star-fill ri-star"></i>';
        else if (rating >= i - 0.5)
            html += '<i class="bi bi-star-half ri-star"></i>';
        else
            html += '<i class="bi bi-star ri-star"></i>';
    }
    html += `<span class="ri-rating-num">${Number(rating).toFixed(1)}</span>`;
    return html;
}

async function deleteReview(reviewId) {

	const swalResult = await Swal.fire({
        title: 'レビューを削除しますか？',
        text: '削除したレビューは復元できません。',
        icon: 'warning',
        iconColor: '#dc2626',
        showCancelButton: true,
        confirmButtonColor: '#dc2626', 
        cancelButtonColor: '#9ca3af',  
        confirmButtonText: '削除する',
        cancelButtonText: 'キャンセル',
        reverseButtons: true 
    });

    if (!swalResult.isConfirmed) return;	
	
    try {
        const res  = await fetch(`/api/reviews/${reviewId}`, { method: 'DELETE' });
        const json = await res.json();
        if (json.success) {
            const el = document.querySelector(`.review-item[data-review-id="${reviewId}"]`);
            if (el) el.remove();
            // レビュー件数 更新
            const countEl = document.getElementById('reviewCount');
            if (countEl) {
                const cur = parseInt(countEl.textContent.replace(/[^0-9]/g, '')) || 0;
                countEl.textContent = `レビュー ${Math.max(0, cur - 1)}件`;
            }
			
			Swal.fire({
                title: '削除完了',
                text: 'レビューを削除しました。',
                icon: 'success',
                iconColor: '#065f46',
                confirmButtonColor: '#065f46',
                confirmButtonText: '確認'
            });
        } else {
            Swal.fire({ title: 'エラー', text: json.message || '削除に失敗しました。', icon: 'error', iconColor: '#dc2626', confirmButtonColor: '#065f46', confirmButtonText: '確認' });
        }
    } catch (err) {
        console.error('[レビュー削除] エラー:', err);
        Swal.fire({ title: 'エラー', text: 'エラーが発生しました。', icon: 'error', iconColor: '#dc2626', confirmButtonColor: '#065f46', confirmButtonText: '確認' });
    }
}

// ============================================
// レビュー作成 送信
// ============================================
async function submitReview() {
    const title   = document.getElementById('reviewTitle')?.value.trim();
    const content = document.getElementById('reviewContent')?.value.trim();
    const fileInput = document.getElementById('imageInput');
	
	const showWarning = (text) => Swal.fire({ title: '確認', text: text, icon: 'info', iconColor: '#F28940', confirmButtonColor: '#065f46', confirmButtonText: '確認' });

	if (!title)   { showWarning('タイトルを入力してください。'); return; }
    if (!content) { showWarning('内容を入力してください。'); return; }
    if (currentRating < 1) { showWarning('評価を選択してください。'); return; }

    // 画像がある場合、先にS3へアップロード
    let photoUrl = null;
    if (fileInput && fileInput.files.length > 0) {
        const formData = new FormData();
        formData.append('file', fileInput.files[0]);
        try {
            const uploadRes  = await fetch('/api/reviews/image', { method: 'POST', body: formData });
            const uploadJson = await uploadRes.json();
            if (!uploadJson.success) {
                Swal.fire({ title: 'エラー', text: uploadJson.message || '画像のアップロードに失敗しました。', icon: 'error', iconColor: '#dc2626', confirmButtonColor: '#065f46', confirmButtonText: '確認' });
                return;
            }
            photoUrl = uploadJson.url;
        } catch (err) {
            console.error('[画像アップロード] エラー:', err);
            Swal.fire({ title: 'エラー', text: '画像アップロード中にエラーが発生しました。', icon: 'error', iconColor: '#dc2626', confirmButtonColor: '#065f46', confirmButtonText: '確認' });
            return;
        }
    }

    const body = {
        storeId:  storeId,
        title:    title,
        content:  content,
        rating:   currentRating,
        photoUrl: photoUrl,
        orderId:  _lastOrderId
    };

    try {
        const res = await fetch('/api/reviews', {
            method:  'POST',
            headers: { 'Content-Type': 'application/json' },
            body:    JSON.stringify(body)
        });
        const json = await res.json();

        if (res.status === 401) {
            await Swal.fire({ title: 'ログインが必要です', text: 'レビュー作成はログイン後にご利用いただけます。', icon: 'warning', iconColor: '#F28940', confirmButtonColor: '#065f46', confirmButtonText: '確認' });
            window.location.href = '/';
            return;
        }
        if (!json.success) {
            Swal.fire({ title: 'エラー', text: json.message || 'レビューの登録に失敗しました。', icon: 'error', iconColor: '#dc2626', confirmButtonColor: '#065f46', confirmButtonText: '確認' });
            return;
        }

		await Swal.fire({
            title: '登録完了',
            text: 'レビューが正常に登録されました。',
            icon: 'success',
            iconColor: '#065f46',
            confirmButtonColor: '#065f46',
            confirmButtonText: '確認'
        });
		
        closeReviewModal();
		
        document.getElementById('reviewTitle').value   = '';
        document.getElementById('reviewContent').value = '';
        currentRating = 5.0;
		ratingLocked  = false;
        updateStars();
        removeImage();
        loadReviews(storeId);

    } catch (err) {
        console.error('[レビュー登録] エラー:', err);
        Swal.fire({ title: 'エラー', text: 'エラーが発生しました。もう一度お試しください。', icon: 'error', iconColor: '#dc2626', confirmButtonColor: '#065f46', confirmButtonText: '確認' });
    }
}

// ============================================
// 地図 (Leaflet)
// ============================================
let _map    = null;
let _marker = null;

function _createDetailMarkerIcon(stock) {
    const color = stock === 0  ? '#D32F2F'
                : stock <= 5  ? '#FF9800'
                :               '#4F7F68';
    return L.divIcon({
        className: '',
        html: `<div style="
                   width:32px;height:32px;
                   border-radius:50% 50% 50% 0;
                   background:${color};
                   border:2px solid #fff;
                   transform:rotate(-45deg);
                   box-shadow:0 2px 8px rgba(0,0,0,0.25);
               "></div>`,
        iconSize:    [32, 32],
        iconAnchor:  [16, 32],
        popupAnchor: [0, -36]
    });
}

function _buildDetailPopup(name, category, closeTime, address, minDiscountPrice, totalStock, lat, lng) {
    const stockBadge = totalStock === 0
        ? '<span style="color:#D32F2F;font-weight:700;">売り切れ</span>'
        : totalStock <= 5
            ? `<span style="color:#FF9800;font-weight:700;">在庫 ${totalStock}個</span>`
            : `<span style="color:#4F7F68;font-weight:700;">在庫余裕 ${totalStock}+</span>`;

    const priceHtml = (minDiscountPrice != null)
        ? `<div class="map-popup-price">¥${Number(minDiscountPrice).toLocaleString()} &nbsp;${stockBadge}</div>`
        : `<div class="map-popup-price">${stockBadge}</div>`;

    return `
        <div style="min-width:180px;">
            <div class="map-popup-title">${escapeHtml(name)}</div>
            <div class="map-popup-sub">${escapeHtml(category)} · 閉店 ${escapeHtml(closeTime)}</div>
            <div class="map-popup-sub" style="margin-top:2px;">${escapeHtml(address)}</div>
            ${priceHtml}
            <a href="https://www.openstreetmap.org/?mlat=${lat}&mlon=${lng}#map=16/${lat}/${lng}"
               target="_blank" rel="noopener"
               style="display:block;margin-top:8px;text-align:center;
                      background:#4F7F68;color:#fff;border-radius:6px;
                      padding:4px 0;font-size:0.8rem;text-decoration:none;">
                大きな地図で見る ↗
            </a>
        </div>`;
}

function initMap(lat, lng, name, category, closeTime, address, minDiscountPrice, totalStock) {
    if (!lat || !lng) {
        console.warn("[initMap] lat/lng なし - 地図スキップ");
        return;
    }
    const section     = document.querySelector('.map-section');
    const placeholder = section.querySelector('.map-placeholder');

    if (_map) {
        _map.setView([lat, lng], 16);
        if (_marker) _marker.remove();
        const stock2 = totalStock ?? 99;
        _marker = L.marker([lat, lng], { icon: _createDetailMarkerIcon(stock2) }).addTo(_map);
        _marker.bindPopup(
            _buildDetailPopup(name, category, closeTime, address, minDiscountPrice, stock2, lat, lng),
            { maxWidth: 240 }
        );
        _marker.openPopup();
        return;
    }

    placeholder.style.display = 'none';
    const mapDiv = document.createElement('div');
    mapDiv.id = 'leafletMap';
    mapDiv.style.cssText = 'width:100%;height:24rem;border-radius:0.75rem;z-index:0;';
    section.appendChild(mapDiv);

    _map = L.map('leafletMap', { center: [lat, lng], zoom: 16, zoomControl: true, scrollWheelZoom: false });
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
        maxZoom: 19,
    }).addTo(_map);

    const stock = totalStock ?? 99;
    _marker = L.marker([lat, lng], { icon: _createDetailMarkerIcon(stock) }).addTo(_map);
    _marker.bindPopup(
        _buildDetailPopup(name, category, closeTime, address, minDiscountPrice, stock, lat, lng),
        { maxWidth: 240 }
    );
    _marker.openPopup();
    _map.on('click', () => { if (!_map.scrollWheelZoom.enabled()) _map.scrollWheelZoom.enable(); });
}

// ============================================
// カート
// ============================================
const CART_KEY   = `deushu_cart_${window._sessionMemberId ?? 'guest'}_${storeId}`;
let _cart        = [];
let _lastOrderId = null;  // 決済完了後 orderId 保管 → レビュー作成に連携

function cartLoad() {
    try { _cart = JSON.parse(localStorage.getItem(CART_KEY)) || []; }
    catch { _cart = []; }
}
function cartSave() { localStorage.setItem(CART_KEY, JSON.stringify(_cart)); }

function cartSet(item, qty) {
    const idx = _cart.findIndex(c => c.id === item.id);
    if (qty <= 0) {
        if (idx !== -1) _cart.splice(idx, 1);
    } else {
        if (idx !== -1) _cart[idx].quantity = qty;
        else _cart.push({ ...item, quantity: qty });
    }
    cartSave();
    updateCartBadge();
}
function cartRemove(itemId) {
    _cart = _cart.filter(c => c.id !== itemId);
    cartSave();
    updateCartBadge();
    renderCartPanel();
    const card = document.querySelector(`.product-card[data-item-id="${itemId}"]`);
    if (card) resetCardQty(card);
}
function cartChangeQty(itemId, delta) {
    const found = _cart.find(c => c.id === itemId);
    if (!found) return;
    found.quantity = Math.min(Math.max(1, found.quantity + delta), found.stock);
    cartSave();
    renderCartPanel();
}

// ── パネル DOM 挿入 (IIFE) ──
(function injectPanel() {
    cartLoad();

    const overlay = document.createElement('div');
    overlay.className = 'reservation-overlay';
    overlay.id = 'reservationOverlay';
    overlay.addEventListener('click', closeCart);

    const floatTab = document.createElement('div');
    floatTab.className = 'cart-float-tab';
    floatTab.id = 'cartFloatTab';
    floatTab.onclick = openCart;
    floatTab.innerHTML = `
        <div class="cft-icon">
            <i class="bi bi-bag"></i>
            <span class="cft-badge" id="cftBadge">0</span>
        </div>
        <span class="cft-label">カート</span>
    `;

    const panel = document.createElement('div');
    panel.className = 'reservation-panel';
    panel.id = 'reservationPanel';
    panel.innerHTML = `
        <div class="rp-header">
            <h3>カート <span class="rp-cart-badge" id="cartBadge" style="display:none;">0</span></h3>
            <button class="rp-close" onclick="closeCart()"><i class="bi bi-x-lg"></i></button>
        </div>
        <div class="rp-body" id="cartBody"></div>
        <div class="rp-footer" id="cartFooter" style="display:none;">
            <div class="rp-price-section">
                <div class="rp-price-row">
                    <span class="label">定価</span>
                    <span class="value strike" id="summaryOriginal">0円</span>
                </div>
                <div class="rp-price-row">
                    <span class="label">割引金額</span>
                    <span class="value save" id="summarySave">-0円</span>
                </div>
                <div class="rp-price-total">
                    <span class="label">お支払い金額</span>
                    <span class="total-value" id="summaryFinal">0円</span>
                </div>
            </div>
            <button class="btn-pay" onclick="onClickPay()">
                <i class="bi bi-credit-card"></i> 決済する
            </button>
            <p class="rp-footer-note"><i class="bi bi-shield-check"></i> 安全に決済されます</p>
        </div>
    `;

    document.body.appendChild(floatTab);
    document.body.appendChild(overlay);
    document.body.appendChild(panel);
    updateCartBadge();
})();

function openCart() {
    renderCartPanel();
    document.getElementById('reservationOverlay').classList.add('active');
    document.getElementById('reservationPanel').classList.add('open');
    document.getElementById('cartFloatTab').classList.add('hidden');
    document.body.style.overflow = 'hidden';
}
function closeCart() {
    document.getElementById('reservationOverlay').classList.remove('active');
    document.getElementById('reservationPanel').classList.remove('open');
    document.getElementById('cartFloatTab').classList.remove('hidden');
    document.body.style.overflow = '';
}

function renderCartPanel() {
    const body   = document.getElementById('cartBody');
    const footer = document.getElementById('cartFooter');
    body.innerHTML = '';
    if (!_cart.length) {
        footer.style.display = 'none';
        body.innerHTML = `
            <div class="rp-cart-empty">
                <i class="bi bi-bag-x"></i>
                <p>カートが空です</p>
            </div>`;
        return;
    }
    footer.style.display = 'flex';
    let totalOrig = 0, totalFinal = 0;
    _cart.forEach(c => {
        const lineOrig  = c.originalPrice * c.quantity;
        const lineFinal = c.discountPrice  * c.quantity;
        totalOrig  += lineOrig;
        totalFinal += lineFinal;
        const div = document.createElement('div');
        div.className = 'rp-cart-item';
        div.innerHTML = `
            <img src="${c.thumbnailUrl || ''}" alt="${c.name}" class="rp-cart-item-img">
            <div class="rp-cart-item-info">
                <p class="rp-cart-item-name">${c.name}</p>
                <p class="rp-cart-item-price">${c.discountPrice.toLocaleString()}円</p>
                <div class="rp-qty-control rp-qty-sm">
                    <div class="rp-qty-btn ${c.quantity <= 1 ? 'disabled' : ''}" onclick="cartChangeQty(${c.id}, -1)">
                        <i class="bi bi-dash"></i>
                    </div>
                    <div class="rp-qty-num">${c.quantity}</div>
                    <div class="rp-qty-btn ${c.quantity >= c.stock ? 'disabled' : ''}" onclick="cartChangeQty(${c.id}, 1)">
                        <i class="bi bi-plus"></i>
                    </div>
                </div>
            </div>
            <div class="rp-cart-item-right">
                <button class="rp-cart-remove" onclick="cartRemove(${c.id})"><i class="bi bi-trash3"></i></button>
                <span class="rp-cart-line-total">${lineFinal.toLocaleString()}円</span>
            </div>
        `;
        body.appendChild(div);
    });
    const saved = totalOrig - totalFinal;
    document.getElementById('summaryOriginal').textContent = `${totalOrig.toLocaleString()}円`;
    document.getElementById('summarySave').textContent     = `-${saved.toLocaleString()}円`;
    document.getElementById('summaryFinal').textContent    = `${totalFinal.toLocaleString()}円`;
}

function updateCartBadge() {
    const count      = _cart.reduce((s, c) => s + c.quantity, 0);
    const totalPrice = _cart.reduce((s, c) => s + c.discountPrice * c.quantity, 0);
    const badge = document.getElementById('cartBadge');
    if (badge) { badge.textContent = count; badge.style.display = count > 0 ? 'inline-flex' : 'none'; }
    const cftBadge = document.getElementById('cftBadge');
    if (cftBadge) { cftBadge.textContent = count; cftBadge.classList.toggle('show', count > 0); }
    const cftPrice = document.getElementById('cftPrice');
    if (cftPrice) { cftPrice.textContent = `${totalPrice.toLocaleString()}円`; cftPrice.classList.toggle('show', count > 0); }
}

function onClickPay() {
    if (!_cart.length) return;
    const cartItems  = _cart.map(c => ({ itemId: c.id, quantity: c.quantity }));
    const totalPrice = _cart.reduce((s, c) => s + c.discountPrice * c.quantity, 0);
    console.log('[決済リクエスト準備完了]', { storeId, cartItems, totalPrice });
    processCheckout(cartItems, totalPrice, storeId);
}

// ============================================
// 商品カード レンダリング
// ============================================
function renderItems(items) {
    const itemsGrid = document.getElementById('itemsGrid');
    itemsGrid.innerHTML = '';
    if (!items.length) {
        itemsGrid.innerHTML = `<p style="color:var(--gray-500);padding:1rem;">商品が登録されていません。</p>`;
        return;
    }
    items.forEach(item => {
        const saved   = _cart.find(c => c.id === item.id);
        const initQty = saved ? saved.quantity : 0;
        const card = document.createElement('div');
        card.className = 'product-card';
        card.dataset.itemId = item.id;
        card.innerHTML = `
            <div class="product-image">
                <img src="${item.thumbnailUrl || ''}" alt="${item.name}">
                <span class="discount-badge">${item.discountRate}%</span>
            </div>
            <div class="product-info">
                <h3>${item.name}</h3>
                <div class="product-price">
                    <span class="discount-price">${item.discountPrice.toLocaleString()}円</span>
                    <span class="original-price">${item.originalPrice.toLocaleString()}円</span>
                </div>
                <div class="product-meta">
                    <div class="meta-item"><i class="bi bi-box"></i><span>在庫 ${item.stock}個</span></div>
                </div>
                <div class="card-add-row">
                    <div class="rp-qty-control rp-qty-sm">
                        <div class="rp-qty-btn ${initQty <= 1 ? 'disabled' : ''}" onclick="cardChangeQty(this, ${item.id}, -1)">
                            <i class="bi bi-dash"></i>
                        </div>
                        <div class="rp-qty-num card-qty-num">${initQty || 1}</div>
                        <div class="rp-qty-btn ${initQty >= item.stock ? 'disabled' : ''}" onclick="cardChangeQty(this, ${item.id}, 1)">
                            <i class="bi bi-plus"></i>
                        </div>
                    </div>
                    <button class="btn-add-cart-card ${initQty > 0 ? 'in-cart' : ''}"
                            onclick="cardAddToCart(this, ${JSON.stringify(item).replace(/"/g, '&quot;')})">
                        ${initQty > 0
                            ? `<i class="bi bi-bag-check-fill"></i> ${initQty}個 追加済み`
                            : `<i class="bi bi-bag-plus"></i> カートに追加`}
                    </button>
                </div>
            </div>
        `;
        itemsGrid.appendChild(card);
    });
}

function cardChangeQty(btn, itemId, delta) {
    const card     = btn.closest('.product-card');
    const qtyEl    = card.querySelector('.card-qty-num');
    const minusBtn = card.querySelector('.rp-qty-btn:first-child');
    const plusBtn  = card.querySelector('.rp-qty-btn:last-child');
    const stockEl  = card.querySelector('.meta-item span');
    const stock    = parseInt(stockEl?.textContent.replace(/[^0-9]/g, '')) || 99;
    let qty = parseInt(qtyEl.textContent) || 1;
    qty = Math.min(Math.max(1, qty + delta), stock);
    qtyEl.textContent = qty;
    minusBtn.classList.toggle('disabled', qty <= 1);
    plusBtn.classList.toggle('disabled',  qty >= stock);
}

function cardAddToCart(btn, item) {
    const card  = btn.closest('.product-card');
    const qtyEl = card.querySelector('.card-qty-num');
    const qty   = parseInt(qtyEl.textContent) || 1;
    cartSet(item, qty);
    btn.classList.add('in-cart');
    btn.innerHTML = `<i class="bi bi-bag-check-fill"></i> ${qty}個 追加済み`;
    setTimeout(() => openCart(), 300);
}

function resetCardQty(card) {
    const qtyEl = card.querySelector('.card-qty-num');
    const btn   = card.querySelector('.btn-add-cart-card');
    if (qtyEl) qtyEl.textContent = '1';
    if (btn) { btn.classList.remove('in-cart'); btn.innerHTML = '<i class="bi bi-bag-plus"></i> カートに追加'; }
}

// ============================================
// PortOne 決済
// ============================================

/*
 * 決済実行のメイン関数
 * 1. バックエンドに注文を生成(排他制御による在庫確保)
 * 2. 成功時、PortOneの決済窓口を呼び出し
 */
async function processCheckout(cartItems, totalPrice, storeId) {
    // 0. 未ログイン防御
    const userNameElement = document.getElementById('headerUserName');
    if (!userNameElement || !userNameElement.innerText) {
        showToast(' * 決済を行うにはログインが必要です。', 'error');
        openModal('user');
        return;
    }

    try {
        // STEP 1: 注文生成 (悲観的ロックで在庫確保)
        const orderResponse = await fetch('/api/v1/orders', {
            method:  'POST',
            headers: { 'Content-Type': 'application/json' },
            body:    JSON.stringify({ storeId, cartItems, totalPrice })
        });

        // HTTP 4xx, 5xx エラー (在庫不足、販売終了など) のハンドリング
        if (!orderResponse.ok) {
            const errorData = await orderResponse.json();
            const errorMessage = errorData.message || '既に他のお客様が購入したか、在庫が不足しています。';

            // 1. ユーザーに在庫変動を警告(Alert)
			await Swal.fire({
	            title: '在庫不足',
	            text: errorMessage,
	            icon: 'warning',
	            iconColor: '#F28940',
	            confirmButtonColor: '#065f46',
	            confirmButtonText: '確認'
	        });

            // 2. 最新の在庫状態を画面に反映させるためにページを強制リロード
            window.location.reload();
            return;
        }

        const orderData = await orderResponse.json();

        // ApiResponse の isSuccess が false の場合 (カスタム例外処理のフォールバック)
        if (!orderData.isSuccess) {
			await Swal.fire({
                title: '注文失敗',
                text: orderData.message || '注文の生成に失敗しました。',
                icon: 'error',
                iconColor: '#dc2626',
                confirmButtonColor: '#065f46',
                confirmButtonText: '確認'
            });
            window.location.reload();
            return;
        }

        const pendingOrderId = orderData.data;

        // STEP 2: PortOne 決済画面呼び出し
        IMP.init("imp41118237");
        const paymentData = {
            pg:           "tosspayments",
            pay_method:   "card",
            merchant_uid: `ORDER_${pendingOrderId}_${new Date().getTime()}`,
            name:         "ドゥーシュー マルトク割引商品",
            amount:       totalPrice,
            buyer_email:  document.getElementById('headerUserName').innerText + "@test.com",
            buyer_name:   document.getElementById('headerUserName').innerText,
        };

        IMP.request_pay(paymentData, async function(rsp) {
            console.log("========== [PortOne 決済応答データ] ==========");
            console.log(JSON.stringify(rsp, null, 2));
            console.log("==============================================");

            // Toss Payments edge case対応: successフィールド欠如時はimp_uidで判断
            const isPaymentSuccessful = rsp.success || (rsp.imp_uid && !rsp.error_msg && !rsp.error_code);

            if (isPaymentSuccessful) {
                console.log(`✅ 決済成功！ imp_uid: ${rsp.imp_uid}, orderId: ${pendingOrderId}`);
                verifyPayment(rsp.imp_uid, pendingOrderId);
            } else {
                console.error(`❌ 決済失敗: ${rsp.error_msg || '理由不明'}`);
				
				// 決済キャンセル時も Swal を適用してUXを統一
                await Swal.fire({
                    title: '決済キャンセル',
                    text: rsp.error_msg || '決済がキャンセルされました。',
                    icon: 'info',
                    iconColor: '#9ca3af',
                    confirmButtonColor: '#065f46',
                    confirmButtonText: '確認'
                });

                // 決済がキャンセルされた場合、すでにサーバー側で確保(減少)された
                // 在庫状態を画面に正しく反映させるため、ページをリロードします。
                window.location.reload();
            }
        });

    } catch (error) {
        console.error("決済プロセス中にエラーが発生しました:", error);
		await Swal.fire({
            title: 'システムエラー',
            text: 'サーバーとの通信に失敗しました。',
            icon: 'error',
            iconColor: '#dc2626',
            confirmButtonColor: '#065f46',
            confirmButtonText: '確認'
        });
    }
}

/*
 * PortOne 決済完了後 バックエンド検証
 */
async function verifyPayment(impUid, orderId) {
    try {
        const verifyRes = await fetch(`/api/v1/orders/${orderId}/payment`, {
            method:  'POST',
            headers: { 'Content-Type': 'application/json' },
            body:    JSON.stringify({ impUid })
        });
        const verifyData = await verifyRes.json();

        if (verifyData.isSuccess) {
            // _lastOrderId 保管 (レビュー作成時の連携用)
            _lastOrderId = orderId;
            // カート 初期化
            localStorage.removeItem(CART_KEY);
            _cart = [];
            updateCartBadge();
            closeCart();
            showToast('決済が正常に完了しました！マイページへ移動します。', 'success');
            setTimeout(() => { window.location.href = '/mypage'; }, 1500);
        } else {
            showToast(verifyData.message || '決済の検証に失敗しました。', 'error');
        }
    } catch (error) {
        console.error("検証プロセス中にエラーが発生:", error);
        showToast('サーバー通信エラーが発生しました。', 'error');
    }
}

// ============================================
// お気に入り
// ============================================
function initFavBtn(favorited) {
    const btn = document.getElementById('favBtn');
    if (!btn) return;
    _setFavState(btn, favorited);
    const newBtn = btn.cloneNode(true);
    btn.parentNode.replaceChild(newBtn, btn);
    newBtn.addEventListener('click', handleFavClick);
}

async function handleFavClick() {
    const btn = document.getElementById('favBtn');
    if (!btn || btn.dataset.loading === 'true') return;
    btn.dataset.loading = 'true';
    try {
        const res  = await fetch(`/api/stores/${storeId}/favorite/toggle`, {
            method: 'POST', headers: { 'Content-Type': 'application/json' }
        });
        const json = await res.json();
        if (res.status === 401 || json.success === false) {
            await Swal.fire({ title: 'ログインが必要です', text: 'お気に入りはログイン後にご利用いただけます。', icon: 'warning', iconColor: '#F28940', confirmButtonColor: '#065f46', confirmButtonText: '確認' });
            window.location.href = '/';
            return;
        }
        _setFavState(btn, json.favorited);
    } catch (err) {
        console.error('[お気に入り] エラー:', err);
    } finally {
        btn.dataset.loading = 'false';
    }
}

function _setFavState(btn, favorited) {
    const icon = btn.querySelector('i');
    if (!icon) return;
    if (favorited) {
        icon.className = 'bi bi-heart-fill';
        btn.classList.add('active');
        btn.setAttribute('aria-label', 'お気に入り解除');
    } else {
        icon.className = 'bi bi-heart';
        btn.classList.remove('active');
        btn.setAttribute('aria-label', 'お気に入り追加');
    }
}

// ============================================
// 画像スライダー
// ============================================
let _sliderImages = [];
let _sliderIdx    = 0;

function initSlider(images) {
    _sliderImages = images;
    _sliderIdx    = 0;
    const track = document.getElementById('sliderTrack');
    const dots  = document.getElementById('sliderDots');
    if (!track) return;
    track.style.willChange = 'auto';
    if (!images.length) return;

    track.innerHTML = '';
    images.forEach(src => {
        const img = document.createElement('img');
        img.src = src;
        img.alt = '店舗画像';
        img.onerror = () => { img.style.background = 'var(--gray-200)'; img.removeAttribute('src'); };
        track.appendChild(img);
    });

    if (dots) {
        dots.innerHTML = '';
        images.forEach((_, i) => {
            const btn = document.createElement('button');
            btn.className = 'slider-dot' + (i === 0 ? ' active' : '');
            btn.onclick = () => sliderGoTo(i);
            dots.appendChild(btn);
        });
    }

    const showNav = images.length > 1;
    const prev = document.getElementById('sliderPrev');
    const next = document.getElementById('sliderNext');
    if (prev) prev.style.display = showNav ? '' : 'none';
    if (next) next.style.display = showNav ? '' : 'none';
    if (dots) dots.style.display = showNav ? '' : 'none';
    _sliderGo(0);
}

function sliderMove(delta) {
    if (!_sliderImages.length) return;
    sliderGoTo((_sliderIdx + delta + _sliderImages.length) % _sliderImages.length);
}
function sliderGoTo(idx) { _sliderIdx = idx; _sliderGo(idx); }

function _sliderGo(idx) {
    const track = document.getElementById('sliderTrack');
    if (!track) return;
    track.style.willChange = 'transform';
    track.style.transform  = `translateX(-${idx * 100}%)`;
    track.addEventListener('transitionend', () => {
        track.style.willChange = 'auto';
    }, { once: true });
    document.querySelectorAll('.slider-dot').forEach((d, i) => {
        d.classList.toggle('active', i === idx);
    });
}

// タッチスワイプ
(function initSwipe() {
    let startX = 0;
    document.addEventListener('DOMContentLoaded', () => {
        const slider = document.getElementById('storeSlider');
        if (!slider) return;
        slider.addEventListener('touchstart', e => { startX = e.touches[0].clientX; }, { passive: true });
        slider.addEventListener('touchend',   e => {
            const diff = startX - e.changedTouches[0].clientX;
            if (Math.abs(diff) > 40) sliderMove(diff > 0 ? 1 : -1);
        });
    });
})();

// ============================================
// レビュー写真 原本ポップアップ
// ============================================
(function injectPhotoOverlay() {
    const overlay = document.createElement('div');
    overlay.className = 'ri-photo-overlay';
    overlay.id = 'riPhotoOverlay';
    overlay.innerHTML = '<img id="riPhotoOverlayImg" src="" alt="レビュー写真 原本">';
    overlay.addEventListener('click', () => overlay.classList.remove('active'));
    document.body.appendChild(overlay);
})();

function openPhotoOverlay(url) {
    const overlay = document.getElementById('riPhotoOverlay');
    const img     = document.getElementById('riPhotoOverlayImg');
    if (!overlay || !img) return;
    img.src = url;
    overlay.classList.add('active');
}