// ============================================
// XSS 방지 (최상단 - 모든 함수보다 먼저)
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
// 리뷰 작성 모달
// ============================================
function openReviewModal() {
    document.getElementById('reviewModal').classList.add('active');
}
function closeReviewModal() {
    document.getElementById('reviewModal').classList.remove('active');
}
function handleModalClick(event) {
    if (event.target.id === 'reviewModal') closeReviewModal();
}

// ============================================
// 별점 선택
// ============================================
const starButtons = document.querySelectorAll('.star-btn');
let currentRating = 5.0;

starButtons.forEach((star, index) => {
    star.addEventListener("mousemove", (e) => {
        const rect = star.getBoundingClientRect();
        const x = e.clientX - rect.left;
        currentRating = x < rect.width / 2 ? index + 0.5 : index + 1;
        updateStars();
        const ratingValue = document.querySelector('.rating-value');
        if (ratingValue) ratingValue.textContent = currentRating.toFixed(1);
    });
    star.addEventListener("click", () => {
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
// 글자 수 카운터
// ============================================
const textarea = document.getElementById('reviewContent');
const currentCount = document.getElementById('currentCount');
textarea?.addEventListener('input', () => {
    currentCount.textContent = textarea.value.length;
});

// ============================================
// 이미지 업로드 (리뷰 모달)
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
// 리뷰 더보기/접기
// ============================================
function toggleReviewText(button) {
    const reviewItem = button.closest('.review-item');
    const reviewText = reviewItem.querySelector('.review-text');
    if (button.classList.contains('expanded')) {
        reviewText.textContent = reviewText.dataset.short;
        button.innerHTML = '더보기 <i class="bi bi-chevron-down"></i>';
        button.classList.remove('expanded');
    } else {
        reviewText.dataset.short = reviewText.textContent;
        reviewText.textContent = reviewText.dataset.full || reviewText.textContent;
        button.innerHTML = '접기 <i class="bi bi-chevron-up"></i>';
        button.classList.add('expanded');
    }
}

// ============================================
// 상세 데이터 로딩
// ============================================
document.addEventListener("DOMContentLoaded", () => {
    if (!storeId) return;

    fetch(`/api/stores/${storeId}`)
        .then(res => {
            if (!res.ok) throw new Error("상세 조회 실패: " + res.status);
            return res.json();
        })
        .then(data => renderStore(data))
        .catch(err => console.error("상세 데이터 오류:", err));

    if (starButtons.length) updateStars();
});

// ============================================
// renderStore - API 응답 구조에 맞게 파싱
// ============================================
function renderStore(data) {

    // ── API 응답 구조 정규화 ──
    // /api/stores/{id} 응답은 flat 구조이므로 내부에서 분리
    const store = {
        name:         data.name,
        category:     data.category,
        address:      data.address,
        lat:          data.lat,
        lng:          data.lng,
        openTime:     data.openTime  || "",
        closeTime:    data.closeTime || "",
        thumbnailUrl: data.thumbnailUrl
    };

    const images = data.imageUrls || [];

    const rating = {
        avg:   data.avgRating   ?? 0,
        count: data.reviewCount ?? 0
    };

    const items = data.items || [];

    // ========================
    // 가게 기본 정보
    // ========================
    document.getElementById("storeName").textContent    = store.name;
    document.getElementById("storeCategory").textContent = store.category;
    document.getElementById("storeAddress").textContent  = store.address;
    document.getElementById("storeHours").textContent    = `${store.openTime} ~ ${store.closeTime}`;
    document.getElementById("ratingAvg").textContent     = Number(rating.avg).toFixed(1);
    document.getElementById("reviewCount").textContent   = `리뷰 ${rating.count}`;
    document.getElementById("reviewsTitle").textContent  = `리뷰 (${rating.count})`;

    // ========================
    // 이미지 갤러리
    // ========================
    if (images.length > 0) {
        document.getElementById("mainImage").src = images[0];

        const subImagesDiv = document.getElementById("subImages");
        subImagesDiv.innerHTML = "";
        images.slice(1, 5).forEach(img => {
            const imageTag = document.createElement("img");
            imageTag.src = img;
            subImagesDiv.appendChild(imageTag);
        });
    }

    // ========================
    // 지도 초기화
    // ========================
    const minDiscountPrice = items.length > 0
        ? Math.min(...items.map(i => i.discountPrice))
        : null;
    const totalStock = items.reduce((sum, i) => sum + i.stock, 0);

    initMap(
        store.lat,
        store.lng,
        store.name,
        store.category,
        store.closeTime,
        store.address,
        minDiscountPrice,
        totalStock
    );

    // ========================
    // 상품 목록
    // ========================
    renderItems(items);

    // ========================
    // 리뷰 로딩
    // ========================
    loadReviews(storeId);
}

// ============================================
// 리뷰 API 호출
// ============================================
function loadReviews(storeId) {
    fetch(`/api/reviews?storeId=${storeId}`)
        .then(res => {
            if (!res.ok) throw new Error("리뷰 조회 실패");
            return res.json();
        })
        .then(data => {
            const list = data.items || data || [];
            renderReviews(Array.isArray(list) ? list : []);
        })
        .catch(err => console.error("리뷰 데이터 오류:", err));
}

// ============================================
// 리뷰 렌더링
// ============================================
function renderReviews(reviews) {
    const reviewsList = document.getElementById("reviewsList");
    reviewsList.innerHTML = "";

    if (!reviews.length) {
        reviewsList.innerHTML = `<div class="empty">리뷰가 없습니다.</div>`;
        return;
    }

    reviews.forEach(r => {
        const div = document.createElement("div");
        div.className = "review-item";
        div.innerHTML = `
            <div class="review-head">
                <strong>${escapeHtml(r.title || "")}</strong>
                <span>⭐ ${r.rating ?? 0}</span>
            </div>
            <div class="review-text">${escapeHtml(r.content || "")}</div>
            ${r.photoUrl ? `<img src="${r.photoUrl}" style="max-width:100%;margin-top:8px;">` : ""}
            <div class="review-meta">${escapeHtml(r.memberName || "익명")}</div>
        `;
        reviewsList.appendChild(div);
    });
}

// ============================================
// 지도 (Leaflet) - 리스트 페이지 스타일 통일
// ============================================
let _map = null;

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

function _buildDetailPopup(name, category, closeTime, address, minDiscountPrice, totalStock) {
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
            <a href="https://www.openstreetmap.org/?mlat=${_map ? _map.getCenter().lat : 0}&mlon=${_map ? _map.getCenter().lng : 0}#map=16"
               target="_blank" rel="noopener"
               style="display:block;margin-top:8px;text-align:center;
                      background:#4F7F68;color:#fff;border-radius:6px;
                      padding:4px 0;font-size:0.8rem;text-decoration:none;">
                大きな地図で見る ↗
            </a>
        </div>`;
}

function initMap(lat, lng, name, category, closeTime, address, minDiscountPrice, totalStock) {
    // lat/lng 없으면 지도 스킵
    if (!lat || !lng) {
        console.warn("[initMap] lat/lng 없음 - 지도 스킵");
        return;
    }

    const section     = document.querySelector('.map-section');
    const placeholder = section.querySelector('.map-placeholder');

    if (_map) {
        _map.setView([lat, lng], 16);
        return;
    }

    placeholder.style.display = 'none';

    const mapDiv = document.createElement('div');
    mapDiv.id = 'leafletMap';
    mapDiv.style.cssText = 'width:100%;height:24rem;border-radius:0.75rem;z-index:0;';
    section.appendChild(mapDiv);

    _map = L.map('leafletMap', {
        center: [lat, lng],
        zoom: 16,
        zoomControl: true,
        scrollWheelZoom: false,
    });

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
        maxZoom: 19,
    }).addTo(_map);

    const stock  = totalStock ?? 99;
    const marker = L.marker([lat, lng], { icon: _createDetailMarkerIcon(stock) }).addTo(_map);

    marker.bindPopup(
        _buildDetailPopup(name, category, closeTime, address, minDiscountPrice, stock),
        { maxWidth: 240 }
    );
    marker.openPopup();

    _map.on('click', () => {
        if (!_map.scrollWheelZoom.enabled()) _map.scrollWheelZoom.enable();
    });
}

// ============================================
// 장바구니 (cart-sidebar)
// ============================================
const CART_KEY = `deushu_cart_${storeId}`;
let _cart = [];

function cartLoad() {
    try { _cart = JSON.parse(localStorage.getItem(CART_KEY)) || []; }
    catch { _cart = []; }
}
function cartSave() {
    localStorage.setItem(CART_KEY, JSON.stringify(_cart));
}
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

// ── 패널 DOM 삽입 ──
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
        <span class="cft-label">장바구니</span>
        <span class="cft-price" id="cftPrice">0원</span>
    `;

    const panel = document.createElement('div');
    panel.className = 'reservation-panel';
    panel.id = 'reservationPanel';
    panel.innerHTML = `
        <div class="rp-header">
            <h3>장바구니 <span class="rp-cart-badge" id="cartBadge" style="display:none;">0</span></h3>
            <button class="rp-close" onclick="closeCart()"><i class="bi bi-x-lg"></i></button>
        </div>
        <div class="rp-body" id="cartBody"></div>
        <div class="rp-footer" id="cartFooter" style="display:none;">
            <div class="rp-price-section">
                <div class="rp-price-row">
                    <span class="label">정가</span>
                    <span class="value strike" id="summaryOriginal">0원</span>
                </div>
                <div class="rp-price-row">
                    <span class="label">할인 금액</span>
                    <span class="value save" id="summarySave">-0원</span>
                </div>
                <div class="rp-price-total">
                    <span class="label">선택 금액</span>
                    <span class="total-value" id="summaryFinal">0원</span>
                </div>
            </div>
            <button class="btn-pay" onclick="onClickPay()">
                <i class="bi bi-credit-card"></i> 결제하기
            </button>
            <p class="rp-footer-note"><i class="bi bi-shield-check"></i> 안전하게 결제됩니다</p>
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
                <p>장바구니가 비어있어요</p>
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
                <p class="rp-cart-item-price">${c.discountPrice.toLocaleString()}원</p>
                <div class="rp-qty-control rp-qty-sm">
                    <div class="rp-qty-btn ${c.quantity <= 1 ? 'disabled' : ''}"
                         onclick="cartChangeQty(${c.id}, -1)">
                        <i class="bi bi-dash"></i>
                    </div>
                    <div class="rp-qty-num">${c.quantity}</div>
                    <div class="rp-qty-btn ${c.quantity >= c.stock ? 'disabled' : ''}"
                         onclick="cartChangeQty(${c.id}, 1)">
                        <i class="bi bi-plus"></i>
                    </div>
                </div>
            </div>
            <div class="rp-cart-item-right">
                <button class="rp-cart-remove" onclick="cartRemove(${c.id})">
                    <i class="bi bi-trash3"></i>
                </button>
                <span class="rp-cart-line-total">${lineFinal.toLocaleString()}원</span>
            </div>
        `;
        body.appendChild(div);
    });

    const saved = totalOrig - totalFinal;
    document.getElementById('summaryOriginal').textContent = `${totalOrig.toLocaleString()}원`;
    document.getElementById('summarySave').textContent     = `-${saved.toLocaleString()}원`;
    document.getElementById('summaryFinal').textContent    = `${totalFinal.toLocaleString()}원`;
}

function updateCartBadge() {
    const count      = _cart.reduce((s, c) => s + c.quantity, 0);
    const totalPrice = _cart.reduce((s, c) => s + c.discountPrice * c.quantity, 0);

    const badge = document.getElementById('cartBadge');
    if (badge) {
        badge.textContent = count;
        badge.style.display = count > 0 ? 'inline-flex' : 'none';
    }
    const cftBadge = document.getElementById('cftBadge');
    if (cftBadge) {
        cftBadge.textContent = count;
        cftBadge.classList.toggle('show', count > 0);
    }
    const cftPrice = document.getElementById('cftPrice');
    if (cftPrice) {
        cftPrice.textContent = `${totalPrice.toLocaleString()}원`;
        cftPrice.classList.toggle('show', count > 0);
    }
}

function onClickPay() {
    if (!_cart.length) return;

    // 1. バックエンド(OrderCreateRequestDto)が要求するフォーマットにデータを変換
    const cartItems = _cart.map(c => ({
        itemId: c.id,
        quantity: c.quantity
        // orderPriceはバックエンドで安全に再計算されるため、フロントからは送信不要です
    }));

    // 2. フロントエンドでの合計計算金額（サーバーで検証されます）
    const totalPrice = _cart.reduce((s, c) => s + c.discountPrice * c.quantity, 0);

    console.log('[決済リクエスト準備完了]', { storeId, cartItems, totalPrice });

    // 3. パクさんが作成した PortOne 決済メイン関数を呼び出し
    processCheckout(cartItems, totalPrice, storeId);
}

// ============================================
// 상품 카드 렌더
// ============================================
function renderItems(items) {
    const itemsGrid = document.getElementById('itemsGrid');
    itemsGrid.innerHTML = '';

    if (!items.length) {
        itemsGrid.innerHTML = `<p style="color:var(--gray-500);padding:1rem;">등록된 상품이 없습니다.</p>`;
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
                    <span class="discount-price">${item.discountPrice.toLocaleString()}원</span>
                    <span class="original-price">${item.originalPrice.toLocaleString()}원</span>
                </div>
                <div class="product-meta">
                    <div class="meta-item">
                        <i class="bi bi-box"></i>
                        <span>재고 ${item.stock}개</span>
                    </div>
                </div>
                <div class="card-add-row">
                    <div class="rp-qty-control rp-qty-sm">
                        <div class="rp-qty-btn ${initQty <= 1 ? 'disabled' : ''}"
                             onclick="cardChangeQty(this, ${item.id}, -1)">
                            <i class="bi bi-dash"></i>
                        </div>
                        <div class="rp-qty-num card-qty-num">${initQty || 1}</div>
                        <div class="rp-qty-btn ${initQty >= item.stock ? 'disabled' : ''}"
                             onclick="cardChangeQty(this, ${item.id}, 1)">
                            <i class="bi bi-plus"></i>
                        </div>
                    </div>
                    <button class="btn-add-cart-card ${initQty > 0 ? 'in-cart' : ''}"
                            onclick="cardAddToCart(this, ${JSON.stringify(item).replace(/"/g, '&quot;')})">
                        ${initQty > 0
                            ? `<i class="bi bi-bag-check-fill"></i> ${initQty}개 담김`
                            : `<i class="bi bi-bag-plus"></i> 담기`}
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
    btn.innerHTML = `<i class="bi bi-bag-check-fill"></i> ${qty}개 담김`;
    setTimeout(() => openCart(), 300);
}

function resetCardQty(card) {
    const qtyEl = card.querySelector('.card-qty-num');
    const btn   = card.querySelector('.btn-add-cart-card');
    if (qtyEl) qtyEl.textContent = '1';
    if (btn) {
        btn.classList.remove('in-cart');
        btn.innerHTML = '<i class="bi bi-bag-plus"></i> 담기';
    }
}

/*
 * 決済実行のメイン関数
 * 1. バックエンドに注文を生成(排他制御による在庫確保)
 * 2. 成功時、PortOneの決済窓口を呼び出し
 */
async function processCheckout(cartItems, totalPrice, storeId) {
	// 0. 未ログイン状態の防御コード
    const userNameElement = document.getElementById('headerUserName');
    if (!userNameElement || !userNameElement.innerText) { // isUserLoggedIn() の代替案
        showToast('決済を行うにはログインが必要です。', 'error');
        openModal('user');
        return;
    }

    try {
        // =====================================================================
        // STEP 1: バックエンドの注文生成APIを呼び出し（ここで悲観的ロックが発動します）
        // =====================================================================
        const orderRequest = {
            storeId: storeId,
            cartItems: cartItems,
            totalPrice: totalPrice
        };

        // 先ほどパクさんが作成した /api/v1/orders へのPOSTリクエスト
        const orderResponse = await fetch('/api/v1/orders', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(orderRequest)
        });

        const orderData = await orderResponse.json();

        if (!orderData.isSuccess) {
            // 在庫不足(Lost Update防御)などのエラー時はここで弾かれます
            showToast(orderData.message || '注文の生成に失敗しました。', 'error');
            return;
        }

        // サーバーから発行された「決済待機(PAYMENT_PENDING)」状態の注文ID
        const pendingOrderId = orderData.data;

        // =====================================================================
        // STEP 2: PortOne (旧 Iamport) 決済モジュールの呼び出し
        // =====================================================================
        // 管理者コンソールで発行された「ショップ識別コード(Store ID)」で初期化
        IMP.init("imp41118237");

        // 決済リクエストオブジェクトの構成
        const paymentData = {
            pg: "tosspayments", // テスト用カカオペイ（最速でテスト可能）
            pay_method: "card",
            merchant_uid: `ORDER_${pendingOrderId}_${new Date().getTime()}`, // 加盟店側の固有注文番号
            name: "ドゥーシュー マルトク割引商品", // 決済窓口に表示される商品名
            amount: totalPrice, // 実際の決済金額
            buyer_email: document.getElementById('headerUserName').innerText + "@test.com", // テスト用
            buyer_name: document.getElementById('headerUserName').innerText,
        };
		
        // 決済窓口のレンダリングとコールバック関数の登録
		IMP.request_pay(paymentData, async function (rsp) {
		            
            // [デバッグ用] PortOneからの応答をすべてコンソールに出力します
            console.log("========== [PortOne 決済応答データ] ==========");
            console.log(JSON.stringify(rsp, null, 2));
            console.log("==============================================");

			// 🚨 [核心] Toss Payments等で 'success' フィールドが欠落するPG独自のバグ(Edge Case)を防御します。
            // error_msg が存在せず、imp_uid (決済番号) がきちんと発給されていれば「成功」とみなします。
            const isPaymentSuccessful = rsp.success || (rsp.imp_uid && !rsp.error_msg && !rsp.error_code);

            if (isPaymentSuccessful) {
                console.log(`✅ 決済成功を感知！ imp_uid: ${rsp.imp_uid}, orderId: ${pendingOrderId}`);
                console.log("🔄 バックエンド(Spring)へ検証APIをリクエストします...");
                
                // STEP 3: 決済成功時、バックエンドに最終検証(Validation)を要請
                verifyPayment(rsp.imp_uid, pendingOrderId);
            } else {
                console.error(`❌ 決済失敗またはキャンセル: ${rsp.error_msg || '理由不明'}`);
                showToast(`決済がキャンセルされました: ${rsp.error_msg || '残高不足、またはユーザーキャンセル'}`, 'error');
            }
        });

    } catch (error) {
        console.error("決済プロセス中にエラーが発生しました:", error);
        showToast('サーバーとの通信に失敗しました。', 'error');
    }
}

/*
 * PortOneでの決済完了後、金額改ざんを防ぐためのバックエンド検証要請
 */
async function verifyPayment(impUid, orderId) {
    try {
        const verifyRes = await fetch(`/api/v1/orders/${orderId}/payment`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ impUid: impUid })
        });

        const verifyData = await verifyRes.json();

        if (verifyData.isSuccess) {
            // トーストメッセージを表示
            showToast('決済が正常に完了しました！マイページへ移動します。', 'success');
            
            // ユーザーがメッセージを読む時間(1.5秒)を与えてからマイページへリダイレクト
            setTimeout(() => {
                window.location.href = '/mypage';
            }, 1500);
            
        } else {
            showToast(verifyData.message || '決済の検証に失敗しました。', 'error');
        }
    } catch (error) {
        console.error("検証プロセス中にエラーが発生:", error);
        showToast('サーバー通信エラーが発生しました。', 'error');
    }
}