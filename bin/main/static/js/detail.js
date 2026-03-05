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
    if (event.target.id === 'reviewModal') {
        closeReviewModal();
    }
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

        if (x < rect.width / 2) {
            currentRating = index + 0.5;
        } else {
            currentRating = index + 1;
        }

        updateStars();

        const ratingValue = document.querySelector('.rating-value');
        if (ratingValue) {
            ratingValue.textContent = currentRating.toFixed(1);
        }

    });

    star.addEventListener("click", () => {
        const ratingValue = document.querySelector('.rating-value');
        if (ratingValue) {
            ratingValue.textContent = currentRating.toFixed(1);
        }
    });

});

function updateStars() {

    starButtons.forEach((star, index) => {

        const starValue = index + 1;

        if (currentRating >= starValue) {
            star.className = "bi bi-star-fill star-btn";

        } else if (currentRating >= starValue - 0.5) {
            star.className = "bi bi-star-half star-btn";

        } else {
            star.className = "bi bi-star star-btn";
        }

    });

}

function highlightStars(count) {
    starButtons.forEach((star, index) => {
        if (index < count) {
            star.classList.remove('bi-star');
            star.classList.add('bi-star-fill');
        } else {
            star.classList.remove('bi-star-fill');
            star.classList.add('bi-star');
        }
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
// 이미지 업로드
// ============================================
const imageInput = document.getElementById('imageInput');
const imageUploadArea = document.getElementById('imageUploadArea');
const imagePreview = document.getElementById('imagePreview');
const previewImage = document.getElementById('previewImage');

imageInput?.addEventListener('change', (e) => {
    const file = e.target.files[0];
    if (file) {
        const reader = new FileReader();
        reader.onload = (event) => {
            previewImage.src = event.target.result;
            imageUploadArea.style.display = 'none';
            imagePreview.style.display = 'block';
        };
        reader.readAsDataURL(file);
    }
});

function removeImage() {
    imageInput.value = '';
    previewImage.src = '';
    imageUploadArea.style.display = 'block';
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
        button.innerHTML = '접기 <i class="bi bi-chevron-down"></i>';
        button.classList.add('expanded');
    }
}

// ============================================
// 상세 데이터 로딩
// ============================================
document.addEventListener("DOMContentLoaded", () => {

    if (!storeId) return;

    fetch(`/api/stores/detail/${storeId}`)
        .then(res => {
            if (!res.ok) throw new Error("상세 조회 실패");
            return res.json();
        })
        .then(data => {
            renderStore(data);
        })
        .catch(err => {
            console.error("상세 데이터 오류:", err);
        });

    // 별점 UI 초기화
    if (starButtons.length) {
        updateStars();
    }
});

function renderStore(data) {

    const { store, images, rating, items } = data;

    // ========================
    // 가게 기본 정보
    // ========================
    document.getElementById("storeName").textContent = store.name;
    document.getElementById("storeCategory").textContent = store.category;
    document.getElementById("storeAddress").textContent = store.address;
    document.getElementById("mapAddress").textContent = store.address;

    document.getElementById("storeHours").textContent =
        `${store.openTime} ~ ${store.closeTime}`;

    document.getElementById("ratingAvg").textContent = rating.avg.toFixed(1);
    document.getElementById("reviewCount").textContent =
        `리뷰 ${rating.count}`;

    document.getElementById("reviewsTitle").textContent =
        `리뷰 (${rating.count})`;

    // ========================
    // 이미지 갤러리
    // ========================
    if (images && images.length > 0) {
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
    // 상품 목록
    // ========================
    const itemsGrid = document.getElementById("itemsGrid");
    itemsGrid.innerHTML = "";

    items.forEach(item => {

        const card = document.createElement("div");
        card.className = "product-card";

		card.innerHTML = `
		    <div class="product-image">
		        <img src="${item.thumbnailUrl}" alt="${item.name}">
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
		        <button class="btn-reserve" onclick='openReservation(${JSON.stringify(item)})'>예약하기</button>
		    </div>
		`;

        itemsGrid.appendChild(card);
    });

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
            const list = data.items || [];
            renderReviews(list);
        })
        .catch(err => {
            console.error("리뷰 데이터 오류:", err);
        });
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

            <div class="review-text">
                ${escapeHtml(r.content || "")}
            </div>

            ${r.photoUrl ? `<img src="${r.photoUrl}" style="max-width:100%;margin-top:8px;">` : ""}

            <div class="review-meta">
                ${escapeHtml(r.memberName || "익명")}
            </div>
        `;

        reviewsList.appendChild(div);
    });
}

// ============================================
// 예약 사이드바 (reservation-sidebar.js)
// detail.js 하단에 붙이거나 별도 파일로 로드하세요
// ============================================

// ── DOM 삽입 ──────────────────────────────
(function injectReservationPanel() {

    // 오버레이
    const overlay = document.createElement("div");
    overlay.className = "reservation-overlay";
    overlay.id = "reservationOverlay";
    overlay.addEventListener("click", closeReservation);

    // 패널
    const panel = document.createElement("div");
    panel.className = "reservation-panel";
    panel.id = "reservationPanel";

    panel.innerHTML = `
        <!-- 헤더 -->
        <div class="rp-header">
            <h3>예약하기</h3>
            <button class="rp-close" onclick="closeReservation()" aria-label="닫기">
                <i class="bi bi-x-lg"></i>
            </button>
        </div>

        <!-- 본문 -->
        <div class="rp-body" id="rpBody">

            <!-- 상품 정보 -->
            <div class="rp-item-card">
                <img class="rp-item-img" id="rpItemImg" src="" alt="상품 이미지">
                <div class="rp-item-info">
                    <p class="rp-item-name" id="rpItemName">상품명</p>
                    <p class="rp-item-desc" id="rpItemDesc">상품 설명</p>
                    <div class="rp-item-badges">
                        <span class="rp-badge discount" id="rpDiscountBadge">0% 할인</span>
                        <span class="rp-badge stock" id="rpStockBadge">재고 0개</span>
                    </div>
                </div>
            </div>

            <hr class="rp-divider">

            <!-- 수량 선택 -->
            <div class="rp-qty-section">
                <label>수량 선택</label>
                <div class="rp-qty-control">
                    <div class="rp-qty-btn" id="rpQtyMinus" onclick="changeQty(-1)">
                        <i class="bi bi-dash"></i>
                    </div>
                    <div class="rp-qty-num" id="rpQtyNum">1</div>
                    <div class="rp-qty-btn" id="rpQtyPlus" onclick="changeQty(1)">
                        <i class="bi bi-plus"></i>
                    </div>
                </div>
                <p class="rp-stock-info">
                    <i class="bi bi-box-seam"></i>
                    최대 <strong id="rpStockMax">0</strong>개 구매 가능
                </p>
            </div>

            <hr class="rp-divider">

            <!-- 가격 요약 -->
            <div class="rp-price-section">
                <div class="rp-price-row">
                    <span class="label">정가</span>
                    <span class="value strike" id="rpOriginalTotal">0원</span>
                </div>
                <div class="rp-price-row">
                    <span class="label">할인 (<span id="rpDiscountRateLabel">0</span>%)</span>
                    <span class="value save" id="rpSaveAmount">-0원</span>
                </div>
                <div class="rp-price-total">
                    <span class="label">최종 결제금액</span>
                    <span class="total-value" id="rpFinalTotal">0원</span>
                </div>
            </div>

        </div>

        <!-- 푸터 -->
        <div class="rp-footer">
            <button class="btn-pay" id="rpPayBtn" onclick="onClickPay()">
                <i class="bi bi-credit-card"></i>
                결제하기
            </button>
            <p class="rp-footer-note">
                <i class="bi bi-shield-check"></i>
                안전하게 결제됩니다
            </p>
        </div>
    `;

    document.body.appendChild(overlay);
    document.body.appendChild(panel);
})();


// ── 상태 ──────────────────────────────────
let _item = null;   // 현재 선택된 상품 { name, thumbnailUrl, originalPrice, discountPrice, discountRate, stock }
let _qty  = 1;

// ── 열기 ──────────────────────────────────
function openReservation(item) {
    _item = item;
    _qty  = 1;
    _renderPanel();

    document.getElementById("reservationOverlay").classList.add("active");
    document.getElementById("reservationPanel").classList.add("open");
    document.body.style.overflow = "hidden";
}

// ── 닫기 ──────────────────────────────────
function closeReservation() {
    document.getElementById("reservationOverlay").classList.remove("active");
    document.getElementById("reservationPanel").classList.remove("open");
    document.body.style.overflow = "";
}

// ── 패널 렌더 ──────────────────────────────
function _renderPanel() {
    if (!_item) return;

    document.getElementById("rpItemImg").src        = _item.thumbnailUrl || "";
    document.getElementById("rpItemImg").alt        = _item.name || "";
    document.getElementById("rpItemName").textContent  = _item.name || "-";
    document.getElementById("rpItemDesc").textContent  = _item.description || "맛있는 상품입니다.";
    document.getElementById("rpDiscountBadge").textContent = `${_item.discountRate}% 할인`;
    document.getElementById("rpStockBadge").textContent    = `재고 ${_item.stock}개`;
    document.getElementById("rpStockMax").textContent      = _item.stock;
    document.getElementById("rpDiscountRateLabel").textContent = _item.discountRate;

    _updatePrice();
}

// ── 수량 변경 ──────────────────────────────
function changeQty(delta) {
    const max = _item?.stock ?? 1;
    _qty = Math.min(Math.max(1, _qty + delta), max);
    _updateQtyUI();
    _updatePrice();
}

function _updateQtyUI() {
    document.getElementById("rpQtyNum").textContent = _qty;

    const minusBtn = document.getElementById("rpQtyMinus");
    const plusBtn  = document.getElementById("rpQtyPlus");
    const max      = _item?.stock ?? 1;

    minusBtn.classList.toggle("disabled", _qty <= 1);
    plusBtn.classList.toggle("disabled", _qty >= max);
}

// ── 가격 계산 ──────────────────────────────
function _updatePrice() {
    if (!_item) return;

    const originalTotal = _item.originalPrice * _qty;
    const finalTotal    = _item.discountPrice  * _qty;
    const saved         = originalTotal - finalTotal;

    document.getElementById("rpOriginalTotal").textContent = `${originalTotal.toLocaleString()}원`;
    document.getElementById("rpSaveAmount").textContent    = `-${saved.toLocaleString()}원`;
    document.getElementById("rpFinalTotal").textContent    = `${finalTotal.toLocaleString()}원`;
}

// ── 결제 버튼 ──────────────────────────────
function onClickPay() {
    if (!_item) return;

    const payload = {
        itemId:         _item.id,
        itemName:       _item.name,
        quantity:       _qty,
        discountPrice:  _item.discountPrice,
        totalPrice:     _item.discountPrice * _qty,
    };

    console.log("[예약 페이로드]", payload);

    // TODO: 팀원과 연동 시 아래 주석 해제
    // fetch("/api/reservations", {
    //     method: "POST",
    //     headers: { "Content-Type": "application/json" },
    //     body: JSON.stringify(payload)
    // }).then(res => { ... });

    alert(`${_item.name} ${_qty}개 예약 요청!\n(결제 API 연동 예정)`);
}

// ============================================
// XSS 방지
// ============================================
function escapeHtml(str) {
    return String(str).replace(/[&<>"']/g, m => ({
        "&":"&amp;",
        "<":"&lt;",
        ">":"&gt;",
        '"':"&quot;",
        "'":"&#39;"
    }[m]));
}
