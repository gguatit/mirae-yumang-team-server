// ============================================
// GSAP Animations - STARLOG Community Page
// 고급스럽고 절제된 애니메이션
// ============================================

gsap.registerPlugin(ScrollTrigger);

// ============================================
// 1. 네비게이션 바 - 부드러운 슬라이드 다운
// ============================================
gsap.from('.navbar', {
    duration: 0.7,
    opacity: 0,
    y: -20,
    ease: 'power2.out'
});

// ============================================
// 2. 커뮤니티 헤더 - 페이드인 + 미세한 스케일
// ============================================
gsap.from('.community-header', {
    duration: 1.0,
    opacity: 0,
    scale: 0.98,
    ease: 'power3.out',
    delay: 0.15
});

gsap.from('.titlebox h1', {
    duration: 0.9,
    opacity: 0,
    x: -30,
    ease: 'power3.out',
    delay: 0.35
});

gsap.from('.titlebox span', {
    duration: 0.8,
    opacity: 0,
    x: -20,
    ease: 'power2.out',
    delay: 0.5
});

gsap.from('.search-box', {
    duration: 0.7,
    opacity: 0,
    y: 10,
    ease: 'power2.out',
    delay: 0.65
});

// ============================================
// 3. 게시글 수 / 페이지 헤더 - 페이드인
// ============================================
gsap.from('.post-count', {
    duration: 0.6,
    opacity: 0,
    y: 8,
    ease: 'power2.out',
    delay: 0.5
});

// ============================================
// 4. 게시글 테이블 헤더
// ============================================
gsap.from('.post-table thead tr', {
    duration: 0.5,
    opacity: 0,
    ease: 'power2.out',
    scrollTrigger: {
        trigger: '.post-table',
        start: 'top 85%',
        toggleActions: 'play none none none'
    }
});

// ============================================
// 5. 테이블 행 - 순차적 페이드인 (스태거)
//    게시글이 없을 때 메시지도 포함
// ============================================
gsap.from('.post-table tbody tr', {
    duration: 0.45,
    opacity: 0,
    y: 12,
    stagger: 0.055,
    ease: 'power2.out',
    scrollTrigger: {
        trigger: '.post-table',
        start: 'top 80%',
        toggleActions: 'play none none none'
    }
});

gsap.from('.empty-message', {
    duration: 0.7,
    opacity: 0,
    y: 16,
    ease: 'power2.out',
    scrollTrigger: {
        trigger: '.empty-message',
        start: 'top 85%',
        toggleActions: 'play none none none'
    }
});

// ============================================
// 6. 글쓰기 버튼
// ============================================
gsap.from('.btn', {
    duration: 0.6,
    opacity: 0,
    scale: 0.92,
    ease: 'back.out(1.4)',
    scrollTrigger: {
        trigger: '.bastpost-main',
        start: 'top 85%',
        toggleActions: 'play none none none'
    }
});

// ============================================
// 7. 인기 게시글 패널 - 위에서 부드럽게
// ============================================
gsap.from('.bastpost', {
    duration: 0.8,
    opacity: 0,
    y: 20,
    ease: 'power3.out',
    scrollTrigger: {
        trigger: '.bastpost',
        start: 'top 85%',
        toggleActions: 'play none none none'
    }
});

gsap.from('.best-item', {
    duration: 0.45,
    opacity: 0,
    x: 15,
    stagger: 0.08,
    ease: 'power2.out',
    scrollTrigger: {
        trigger: '.best-list',
        start: 'top 85%',
        toggleActions: 'play none none none'
    }
});

// ============================================
// 8. 페이지네이션 - 부드러운 페이드업 (ScrollTrigger 없이 항상 표시)
// ============================================
gsap.from('.pagination', {
    duration: 0.6,
    opacity: 0,
    y: 10,
    ease: 'power2.out',
    delay: 0.4
});

// ============================================
// 9. 테이블 행 호버 마이크로 인터랙션
//    (CSS transition과 충돌 없이 살짝만)
// ============================================
document.querySelectorAll('.post-table tbody tr').forEach(row => {
    row.addEventListener('mouseenter', () => {
        gsap.to(row, { x: 3, duration: 0.2, ease: 'power1.out' });
    });
    row.addEventListener('mouseleave', () => {
        gsap.to(row, { x: 0, duration: 0.2, ease: 'power1.out' });
    });
});

// ============================================
// 10. 인기 게시글 아이템 호버
// ============================================
document.querySelectorAll('.best-item').forEach(item => {
    item.addEventListener('mouseenter', () => {
        gsap.to(item, { x: 4, duration: 0.2, ease: 'power1.out' });
    });
    item.addEventListener('mouseleave', () => {
        gsap.to(item, { x: 0, duration: 0.2, ease: 'power1.out' });
    });
});

// ============================================
// 11. 동적 삽입 행 애니메이션 (post-list.js에서 호출)
//     폴링으로 추가되는 NEW 게시글 행에 적용
// ============================================
function animateNewRow(row) {
    // 위에서 슬라이드 인 + 페이드
    gsap.from(row, {
        opacity: 0,
        y: -14,
        duration: 0.55,
        ease: 'power3.out'
    });
    // 호버 인터랙션 추가
    row.addEventListener('mouseenter', () => {
        gsap.to(row, { x: 3, duration: 0.2, ease: 'power1.out' });
    });
    row.addEventListener('mouseleave', () => {
        gsap.to(row, { x: 0, duration: 0.2, ease: 'power1.out' });
    });
}
window.animateNewRow = animateNewRow;

// ============================================
// 12. 인기 게시글 실시간 갱신 시 애니메이션 (post-list.js에서 호출)
// ============================================
function animateBestItems(items) {
    gsap.from(items, {
        opacity: 0,
        x: 20,
        duration: 0.4,
        stagger: 0.07,
        ease: 'power2.out'
    });
    items.forEach(item => {
        item.addEventListener('mouseenter', () => {
            gsap.to(item, { x: 4, duration: 0.2, ease: 'power1.out' });
        });
        item.addEventListener('mouseleave', () => {
            gsap.to(item, { x: 0, duration: 0.2, ease: 'power1.out' });
        });
    });
}
window.animateBestItems = animateBestItems;
