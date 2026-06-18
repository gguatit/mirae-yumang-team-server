gsap.registerPlugin(MorphSVGPlugin, DrawSVGPlugin);

var constellationPaths = [
    "M720,100 L820,300 L700,500 L620,300 Z M400,200 L520,350 M900,150 L1050,280",
    "M300,400 L500,350 L700,420 L900,380 L1100,450 M200,600 L450,550 L650,580 L850,520 L1100,600",
    "M400,700 Q550,400 700,500 T1000,600 M300,450 Q600,200 900,500",
    "M200,300 Q400,600 700,400 T1200,300 M500,200 Q700,500 1000,400",
    "M300,200 C400,400 600,100 700,300 C800,500 1000,200 1140,400 M500,600 C600,400 800,700 900,500 M720,450 L720,100 M720,450 L400,600 M720,450 L1040,600",
];

var pathEl = document.getElementById('constellation-path');
if (!pathEl) {
    pathEl = document.querySelector('#constellation-svg path');
}

function applyMorph(index, triggerEl, startPos, endPos) {
    gsap.to(pathEl, {
        morphSVG: { shape: constellationPaths[index] },
        scrollTrigger: {
            trigger: triggerEl,
            start: startPos,
            end: endPos,
            scrub: 1,
        },
        ease: "none",
    });
}

applyMorph(0, ".hero", "top bottom", "bottom top");
applyMorph(1, ".intro0", "top bottom", "bottom top");
applyMorph(2, ".intro1", "top bottom", "bottom top");
applyMorph(3, ".today", "top bottom", "bottom top");
applyMorph(4, ".intro2", "top bottom", "bottom top");

gsap.from(pathEl, {
    drawSVG: "0%",
    duration: 1.5,
    ease: "power2.out",
    delay: 0.5,
});
