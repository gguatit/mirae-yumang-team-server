// ─────────────────────────────────────────────
// es-hangul 라이브러리 사용
// 출처: https://github.com/toss/es-hangul (MIT)
// CDN: https://esm.sh/es-hangul
// ─────────────────────────────────────────────
import { getChoseong } from 'https://esm.sh/es-hangul@2.3.8';

function matchesQuery(text, query) {
    if (!query) return true;
    const lower = text.toLowerCase();
    const q = query.toLowerCase();
    if (lower.includes(q)) return true;
    if (/^[ㄱ-ㅎ\s]+$/.test(query)) {
        return getChoseong(text).includes(query);
    }
    return false;
}

// 현재 페이지 내 실시간 초성 필터링
const allRows = Array.from(document.querySelectorAll('.post-table tbody tr'));
const searchInput = document.getElementById('searchInput');

if (searchInput) {
    searchInput.addEventListener('input', () => {
        const query = searchInput.value.trim();
        allRows.forEach(row => {
            const title = row.querySelector('.col-title')?.textContent.trim() ?? '';
            const author = row.querySelector('.col-author')?.textContent.trim() ?? '';
            row.style.display = matchesQuery(title, query) || matchesQuery(author, query) ? '' : 'none';
        });
    });
}

// ─────────────────────────────────────────────
// 실시간 게시글 업데이트 (폴링)
// PAGE_CONFIG는 post-list.html에서 window 전역으로 주입
// ─────────────────────────────────────────────
const { currentPage, hasKeyword } = window.PAGE_CONFIG || {};
// LocalDateTime.parse() 호환 형식: "2026-03-11T12:00:00" (Z, 밀리초 제거)
let lastCheckTime = new Date().toISOString().slice(0, 19);

if (currentPage === 0 && !hasKeyword) {
    setInterval(async () => {
        try {
            const response = await fetch(`/posts/api/new?since=${encodeURIComponent(lastCheckTime)}`);
            const newPosts = await response.json();

            if (newPosts && newPosts.length > 0) {
                const tbody = document.querySelector('.post-table tbody');
                if (tbody) {
                    newPosts.reverse().forEach(post => {
                        const existingRow = document.querySelector(`tr[onclick*="id=${post.id}"]`);
                        if (existingRow) return;

                        const row = document.createElement('tr');
                        row.style.cursor = 'pointer';
                        row.setAttribute('onclick', `location.href='/posts/${post.id}'`);
                        row.style.backgroundColor = '#f0f8ff';

                        const createdAt = new Date(post.createdAt);
                        const dateStr = `${createdAt.getFullYear()}-${String(createdAt.getMonth() + 1).padStart(2, '0')}-${String(createdAt.getDate()).padStart(2, '0')} ${String(createdAt.getHours()).padStart(2, '0')}:${String(createdAt.getMinutes()).padStart(2, '0')}`;

                        row.innerHTML = `
                            <td class="col-id">NEW</td>
                            <td class="col-title">
                                <a href="/posts/${post.id}" class="post-title">${escapeHtml(post.title)}</a>
                            </td>
                            <td class="col-author">${escapeHtml(post.user.username)}</td>
                            <td class="col-date">${dateStr}</td>
                            <td class="col-views">${post.viewCount || 0}</td>
                            <td class="col-likes">${post.likeCount || 0}</td>
                        `;

                        tbody.insertBefore(row, tbody.firstChild);

                        // GSAP 애니메이션 적용 (post-list-animation.js에서 노출한 함수)
                        window.animateNewRow?.(row);

                        setTimeout(() => {
                            row.style.backgroundColor = '';
                        }, 3000);
                    });

                    showNotification(`새 게시글 ${newPosts.length}개가 추가되었습니다.`);
                }

                lastCheckTime = new Date().toISOString().slice(0, 19);
            }
        } catch (error) {
            console.error('새 게시글 확인 실패:', error);
        }
    }, 5000);
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function showNotification(message) {
    const notification = document.createElement('div');
    notification.textContent = message;
    notification.style.cssText = `
        position: fixed;
        top: 80px;
        right: 20px;
        background: linear-gradient(135deg, #667EEA, #9DB2F5);
        color: white;
        padding: 15px 25px;
        border-radius: 8px;
        box-shadow: 0 4px 12px rgba(0,0,0,0.15);
        z-index: 10000;
        font-size: 14px;
        animation: slideIn 0.3s ease-out;
    `;
    document.body.appendChild(notification);
    setTimeout(() => {
        notification.style.animation = 'slideOut 0.3s ease-out';
        setTimeout(() => notification.remove(), 300);
    }, 3000);
}

const style = document.createElement('style');
style.textContent = `
    @keyframes slideIn {
        from { transform: translateX(400px); opacity: 0; }
        to { transform: translateX(0); opacity: 1; }
    }
    @keyframes slideOut {
        from { transform: translateX(0); opacity: 1; }
        to { transform: translateX(400px); opacity: 0; }
    }
`;
document.head.appendChild(style);

// ─────────────────────────────────────────────
// 인기 게시글 실시간 갱신 (30초마다 폴링)
// ─────────────────────────────────────────────
let lastBestSnapshot = '';

async function refreshBestPosts() {
    try {
        const res = await fetch('/posts/api/best');
        const posts = await res.json();

        // 변경 여부 확인 (JSON 문자열 비교)
        const snapshot = JSON.stringify(posts);
        if (snapshot === lastBestSnapshot) return;
        lastBestSnapshot = snapshot;

        const bestList = document.getElementById('best-list');
        if (!bestList) return;

        if (posts.length === 0) {
            bestList.innerHTML = '<li class="best-item"><p class="best-content" style="text-align:center;color:#9DB2F5;">인기 게시글이 없습니다.</p></li>';
            return;
        }

        // 기존 목록과 비교해 달라지면 교체
        const newHtml = posts.map(post => `
            <li class="best-item">
                <a href="/posts/${post.id}" class="best-link">
                    <div class="best-text">
                        <span class="best-nickname">${escapeHtml(post.username)}</span>
                        <p class="best-content">${escapeHtml(post.title)}</p>
                    </div>
                    <span class="best-likes">${post.likeCount}</span>
                </a>
            </li>
        `).join('');

        bestList.innerHTML = newHtml;

        // GSAP 애니메이션 적용 (로드 완료 후 사용 가능한 경우)
        window.animateBestItems?.(bestList.querySelectorAll('.best-item'));

    } catch (e) {
        console.error('인기 게시글 갱신 실패:', e);
    }
}

// 페이지 로드 직후 스냅샷 저장 (초기 렌더링 상태)
(async () => {
    try {
        const res = await fetch('/posts/api/best');
        lastBestSnapshot = JSON.stringify(await res.json());
    } catch (_) {}
})();

setInterval(refreshBestPosts, 30000);
