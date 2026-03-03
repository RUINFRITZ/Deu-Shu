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
let currentRating = 5;

starButtons.forEach((star, index) => {
    star.addEventListener('click', () => {
        currentRating = index + 1;
        updateStars();
        document.querySelector('.rating-value').textContent = currentRating + '.0';
    });

    star.addEventListener('mouseenter', () => {
        highlightStars(index + 1);
    });

    star.addEventListener('mouseleave', () => {
        updateStars();
    });
});

function updateStars() {
    starButtons.forEach((star, index) => {
        if (index < currentRating) {
            star.classList.remove('bi-star');
            star.classList.add('bi-star-fill', 'active');
        } else {
            star.classList.remove('bi-star-fill', 'active');
            star.classList.add('bi-star');
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

    fetch(`/api/stores/${storeId}`)
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
                <button class="btn-reserve">예약하기</button>
            </div>
        `;

        itemsGrid.appendChild(card);
    });
}


// ============================================
// 초기화
// ============================================
updateStars();