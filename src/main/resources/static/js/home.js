gsap.registerPlugin(ScrollTrigger, SplitText);

function initAnimations() {
    var mm = gsap.matchMedia();

    mm.add("(prefers-reduced-motion: no-preference)", function () {

        var heroTL = gsap.timeline({ defaults: { ease: "power4.out" } });
        heroTL
            .from("header a", {
                rotationX: -15, y: -20, z: -30, opacity: 0,
                stagger: 0.06, duration: 0.6,
            })
            .from(".MainTitle", {
                scale: 0.85, rotationX: 8, z: -50, opacity: 0,
                duration: 1.2,
            }, "-=0.2")
            .from(".header-menu > div > *", {
                rotationX: -10, y: 10, z: -20, opacity: 0,
                stagger: 0.08, duration: 0.5,
            }, "-=0.4");

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

            gsap.ticker.add(function () {
                if (window.starField) {
                    window.starField.updateCamera();
                }
            });
        }

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

        gsap.from(".fortune-list > li", {
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

        gsap.from(".card-bottom", {
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

document.fonts.ready.then(initAnimations);
