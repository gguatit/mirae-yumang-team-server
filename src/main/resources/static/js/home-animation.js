// ============================================
// GSAP Elegant Animations - STARLOG
// 부드럽고 우아한 애니메이션
// ============================================
gsap.registerPlugin(ScrollTrigger);

// 커스텀 ease 함수
const customEase = "power3.out";
const smoothEase = "power2.out";

// ============================================
// 페이지 로드 애니메이션
// ============================================
window.addEventListener('DOMContentLoaded', () => {
    
    // 페이지 로딩 페이드인
    gsap.from('body', {
        opacity: 0,
        duration: 0.4,
        ease: 'power2.out'
    });

    // ============================================
    // 1. MainTitle - 부드러운 페이드인 & 스케일
    // ============================================
    gsap.from('.MainTitle', {
        duration: 1.5,
        opacity: 0,
        scale: 0.9,
        y: 20,
        ease: 'power3.out',
        delay: 0.2
    });

    // ============================================
    // 2. 헤더 메뉴 - 순차적 페이드인
    // ============================================
    gsap.from('header a', {
        duration: 0.8,
        opacity: 0,
        y: -20,
        stagger: 0.1,
        ease: 'power2.out',
        delay: 0.5
    });

    // ============================================
    // 3. intro0 섹션 - 심플한 페이드인
    // ============================================
    gsap.from('.intro0 p', {
        scrollTrigger: {
            trigger: '.intro0',
            start: 'top 75%',
            toggleActions: 'play none none none'
        },
        duration: 1,
        opacity: 0,
        y: 30,
        ease: customEase
    });

    gsap.from('.intro0 span', {
        scrollTrigger: {
            trigger: '.intro0',
            start: 'top 75%',
            toggleActions: 'play none none none'
        },
        duration: 1,
        opacity: 0,
        y: 20,
        delay: 0.3,
        ease: customEase
    });

    // ============================================
    // 4. intro1 섹션 - 이미지 & 텍스트
    // ============================================
    gsap.fromTo('.intro1ImgBox', 
        {
            opacity: 0,
            scale: 0.95
        },
        {
            scrollTrigger: {
                trigger: '.intro1',
                start: 'top 70%',
                toggleActions: 'play none none none'
            },
            duration: 1.2,
            opacity: 1,
            scale: 1,
            ease: customEase
        }
    );

    gsap.fromTo('.intro1 p',
        {
            opacity: 0,
            y: 30
        },
        {
            scrollTrigger: {
                trigger: '.intro1',
                start: 'top 70%',
                toggleActions: 'play none none none'
            },
            duration: 1,
            opacity: 1,
            y: 0,
            delay: 0.3,
            ease: customEase
        }
    );

    // ============================================
    // 5. Today 섹션 - 좌우 슬라이드
    // ============================================
    gsap.from('.today-box1', {
        scrollTrigger: {
            trigger: '.today',
            start: 'top 70%',
            toggleActions: 'play none none none'
        },
        duration: 1.2,
        opacity: 0,
        x: -50,
        ease: customEase
    });

    // 카드 등장
    gsap.from('.today-box2 .card', {
        scrollTrigger: {
            trigger: '.today-box2',
            start: 'top 70%',
            toggleActions: 'play none none none'
        },
        duration: 1.2,
        opacity: 0,
        y: 30,
        ease: customEase
    });

    // 리스트 아이템 순차 등장
    gsap.from('.today-box2 .fortune-list li', {
        scrollTrigger: {
            trigger: '.today-box2',
            start: 'top 70%',
            toggleActions: 'play none none none'
        },
        duration: 0.8,
        opacity: 0,
        x: -20,
        stagger: 0.12,
        delay: 0.4,
        ease: customEase
    });

    // 버튼 등장
    gsap.from('.today-box2 .community-buttom', {
        scrollTrigger: {
            trigger: '.today-box2',
            start: 'top 70%',
            toggleActions: 'play none none none'
        },
        duration: 0.8,
        opacity: 0,
        scale: 0.95,
        delay: 0.8,
        ease: 'back.out(1.2)'
    });

    // ============================================
    // 6. intro2 섹션
    // ============================================
    gsap.fromTo('.intro2 .intro1ImgBox',
        {
            opacity: 0,
            scale: 0.95
        },
        {
            scrollTrigger: {
                trigger: '.intro2',
                start: 'top 70%',
                toggleActions: 'play none none none'
            },
            duration: 1.2,
            opacity: 1,
            scale: 1,
            ease: customEase
        }
    );

    gsap.fromTo('.intro2 p',
        {
            opacity: 0,
            y: 30
        },
        {
            scrollTrigger: {
                trigger: '.intro2',
                start: 'top 70%',
                toggleActions: 'play none none none'
            },
            duration: 1,
            opacity: 1,
            y: 0,
            delay: 0.3,
            ease: customEase
        }
    );

    // ============================================
    // 7. Community 섹션
    // ============================================
    gsap.from('.community-box2 .card', {
        scrollTrigger: {
            trigger: '.community-box2',
            start: 'top 70%',
            toggleActions: 'play none none none'
        },
        duration: 1.2,
        opacity: 0,
        y: 30,
        ease: customEase
    });

    gsap.from('.community-box2 .fortune-list li', {
        scrollTrigger: {
            trigger: '.community-box2',
            start: 'top 70%',
            toggleActions: 'play none none none'
        },
        duration: 0.8,
        opacity: 0,
        x: -20,
        stagger: 0.12,
        delay: 0.4,
        ease: customEase
    });

    gsap.from('.community-box1', {
        scrollTrigger: {
            trigger: '.community-box1',
            start: 'top 70%',
            toggleActions: 'play none none none'
        },
        duration: 1.2,
        opacity: 0,
        x: 50,
        ease: customEase
    });

    // ============================================
    // 8. 부드러운 버튼 호버 효과
    // ============================================
    const buttons = document.querySelectorAll('.community-buttom, .today-buttom');
    buttons.forEach(button => {
        button.addEventListener('mouseenter', () => {
            gsap.to(button, {
                scale: 1.05,
                duration: 0.3,
                ease: 'power2.out'
            });
        });
        
        button.addEventListener('mouseleave', () => {
            gsap.to(button, {
                scale: 1,
                duration: 0.3,
                ease: 'power2.out'
            });
        });
    });

    // ============================================
    // 9. 카드 호버 효과 (살짝만)
    // ============================================
    const cards = document.querySelectorAll('.card');
    cards.forEach(card => {
        card.addEventListener('mouseenter', () => {
            gsap.to(card, {
                y: -8,
                boxShadow: '0 15px 35px rgba(0,0,0,0.25)',
                duration: 0.4,
                ease: 'power2.out'
            });
        });
        
        card.addEventListener('mouseleave', () => {
            gsap.to(card, {
                y: 0,
                boxShadow: '0 0 0 rgba(0,0,0,0)',
                duration: 0.4,
                ease: 'power2.out'
            });
        });
    });

    // ============================================
    // 10. 스크롤 진행도 프로그레스 바
    // ============================================
    const progressBar = document.createElement('div');
    progressBar.style.cssText = `
        position: fixed;
        top: 0;
        left: 0;
        width: 0%;
        height: 4px;
        background: linear-gradient(90deg, #667EEA, #9DB2F5, #C8A8FF, #E8C4FF);
        z-index: 9999;
        opacity: 0.9;
    `;
    document.body.appendChild(progressBar);

    window.addEventListener('scroll', () => {
        const scrollHeight = document.documentElement.scrollHeight - window.innerHeight;
        const scrolled = (window.pageYOffset / scrollHeight) * 100;
        progressBar.style.width = scrolled + '%';
    });

    console.log('✨ Elegant GSAP Animations Loaded Successfully!');
});
