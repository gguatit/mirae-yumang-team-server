gsap.registerPlugin(ScrollTrigger, SplitText);

function initAnimations() {
    var mm = gsap.matchMedia();

    // 모바일/태블릿: 영웅 진입 + 가벼운 페이드인만, 스크롤 패럴럭스 제거
    mm.add("(max-width: 1023px) and (prefers-reduced-motion: no-preference)", function () {
        var heroTL = gsap.timeline({ defaults: { ease: "power4.out" } });
        heroTL
            .from("header a", {
                y: -20, opacity: 0,
                stagger: 0.06, duration: 0.7,
            })
            .from(".MainTitle", {
                scale: 0.6, opacity: 0,
                duration: 1.2, ease: "power3.out",
            }, "-=0.3")
            .from(".header-menu > div > *", {
                y: 16, opacity: 0,
                stagger: 0.06, duration: 0.5,
            }, "-=0.5");

        var introP = document.querySelector(".intro0 p");
        if (introP) {
            var split = SplitText.create(introP, { type: "words" });
            gsap.from(split.words, {
                y: 20, opacity: 0,
                stagger: 0.03,
                duration: 0.5,
                ease: "power2.out",
                scrollTrigger: {
                    trigger: ".intro0",
                    start: "top 85%",
                    once: true,
                },
            });
        }

        return function () {
            mm.revert();
        };
    });

    // 데스크탑: 풀 애니메이션
    mm.add("(min-width: 1024px) and (prefers-reduced-motion: no-preference)", function () {

        // --- Dramatic hero entry (scale 0.4 → 1) ---
        var heroTL = gsap.timeline({ defaults: { ease: "power4.out" } });
        heroTL
            .from("header a", {
                rotationX: -20, y: -30, z: -60, opacity: 0,
                stagger: 0.06, duration: 0.8,
            })
            .from(".MainTitle", {
                scale: 0.4, rotationX: 15, z: -120, opacity: 0,
                duration: 1.8, ease: "power3.out",
            }, "-=0.3")
            .from(".header-menu > div > *", {
                rotationX: -15, y: 20, z: -40, opacity: 0,
                stagger: 0.08, duration: 0.7,
            }, "-=0.6");
        // Idle floating
        heroTL.to(".MainTitle", {
            y: -12, duration: 3.5, ease: "sine.inOut", yoyo: true, repeat: -1,
        }, "+=0.5");

        // --- Three.js camera scroll control (no GSAP ticker, Three.js loop handles lerp) ---
        if (window.starField) {
            gsap.to(window.starField.cameraTarget, {
                z: -400,
                ease: "none",
                scrollTrigger: {
                    trigger: "body",
                    start: "top top",
                    end: "bottom bottom",
                    scrub: 1.5,
                },
            });

            // Scroll velocity → star field energy
            var lastScroll = window.scrollY;
            ScrollTrigger.create({
                trigger: "body",
                start: "top top",
                end: "bottom bottom",
                onUpdate: function (self) {
                    var vel = self.getVelocity() / 1000;
                    if (window.starField) {
                        window.starField.setScrollVel(vel);
                    }
                },
            });
        }

        // --- SplitText for intro0 quote ---
        var introP = document.querySelector(".intro0 p");
        if (introP) {
            var split = SplitText.create(introP, { type: "words" });
            gsap.from(split.words, {
                rotationX: -90, z: -100, opacity: 0,
                stagger: 0.04,
                duration: 0.7,
                ease: "back.out(1.4)",
                scrollTrigger: {
                    trigger: ".intro0",
                    start: "top 80%",
                    once: true,
                },
            });
        }

        gsap.from(".intro0 span", {
            rotationX: 5, z: -20, opacity: 0,
            duration: 0.8,
            ease: "power3.out",
            scrollTrigger: {
                trigger: ".intro0",
                start: "top 75%",
                once: true,
            },
            delay: 0.3,
        });

        // --- Section cinematic reveals ---
        function cinematicSection(sel) {
            gsap.from(sel + " .wrap", {
                rotationX: 8, scale: 0.95, z: -50, opacity: 0,
                scrollTrigger: {
                    trigger: sel,
                    start: "top 85%",
                    end: "top 40%",
                    scrub: 1,
                },
                ease: "none",
            });
        }

        cinematicSection(".intro1");
        cinematicSection(".today");
        cinematicSection(".intro2");
        cinematicSection(".community");

        // --- Parallax layers ---
        document.querySelectorAll(".intro1ImgBox img, .intro2 .intro1ImgBox img").forEach(function (img) {
            gsap.from(img, {
                yPercent: -15,
                scrollTrigger: {
                    trigger: img.closest("section"),
                    start: "top bottom",
                    end: "bottom top",
                    scrub: 2,
                },
                ease: "none",
            });
        });

        gsap.from(".today-box1", {
            yPercent: -5,
            scrollTrigger: {
                trigger: ".today", start: "top bottom", end: "bottom top", scrub: 1.5,
            },
            ease: "none",
        });
        gsap.from(".today-box2", {
            yPercent: -10,
            scrollTrigger: {
                trigger: ".today", start: "top bottom", end: "bottom top", scrub: 1.5,
            },
            ease: "none",
        });

        gsap.from(".community-box1", {
            yPercent: -5,
            scrollTrigger: {
                trigger: ".community", start: "top bottom", end: "bottom top", scrub: 1.5,
            },
            ease: "none",
        });
        gsap.from(".community-box2", {
            yPercent: -10,
            scrollTrigger: {
                trigger: ".community", start: "top bottom", end: "bottom top", scrub: 1.5,
            },
            ease: "none",
        });

        // --- Fortune lists (distribute-based, scoped) ---
        gsap.from(".today .fortune-list > li", {
            rotation: -2, scale: 0.97, opacity: 0,
            scrollTrigger: {
                trigger: ".today", start: "top 75%", once: true,
            },
            ease: "elastic.out(1, 0.5)",
            duration: 0.8,
            stagger: { from: "center", each: 0.08 },
        });

        gsap.from(".community .fortune-list > li", {
            rotation: -2, scale: 0.97, opacity: 0,
            scrollTrigger: {
                trigger: ".community", start: "top 75%", once: true,
            },
            ease: "elastic.out(1, 0.5)",
            duration: 0.8,
            stagger: { from: "center", each: 0.08 },
        });

        gsap.from(".today .card-bottom", {
            rotationX: 5, z: -20, opacity: 0,
            duration: 0.6,
            ease: "power3.out",
            scrollTrigger: {
                trigger: ".today", start: "top 70%", once: true,
            },
            delay: 0.2,
        });

        gsap.from(".community .card-bottom", {
            rotationX: 5, z: -20, opacity: 0,
            duration: 0.6,
            ease: "power3.out",
            scrollTrigger: {
                trigger: ".community", start: "top 70%", once: true,
            },
            delay: 0.2,
        });

        return function () {
            mm.revert();
        };
    });
}

// Font-ready with 2s timeout fallback (prevents stuck animations if fonts fail)
var fontReady = false;
Promise.race([
    document.fonts.ready,
    new Promise(function (resolve) { setTimeout(resolve, 2000); }),
]).then(function () {
    if (!fontReady) {
        fontReady = true;
        initAnimations();
    }
});
