gsap.registerPlugin(MorphSVGPlugin, DrawSVGPlugin);

// Real constellation-like paths: disconnected line segments (stars + connections)
var constellationPaths = [
    // Hero: Compass star — radiating lines + diamond
    "M720,180 L720,720 M520,450 L920,450 M580,290 L860,610 M860,290 L580,610 M620,320 L720,200 L820,320 Z",
    // intro0: Wandering path — scattered connected dots
    "M300,320 L520,380 L720,300 L920,420 L1120,340 M380,620 L600,560 L820,660 L1040,580 M720,220 L720,680",
    // intro1: Cassiopeia W — 5-star zigzag
    "M380,400 L560,560 L740,380 L920,560 L1100,400 M380,400 L380,380 M1100,400 L1100,380",
    // today: Cup/Vessel — U-shape with rim
    "M480,280 L480,620 Q480,760 720,760 Q960,760 960,620 L960,280 M480,280 L960,280 M600,280 L600,200 M840,280 L840,200",
    // community: Network nodes — interconnected hexagon + diagonals
    "M400,300 L720,200 L1040,300 L1040,600 L720,700 L400,600 Z M400,300 L1040,600 M1040,300 L400,600 M720,200 L720,700",
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

// Draw-in on load
gsap.from(pathEl, {
    drawSVG: "0%",
    duration: 2,
    ease: "power2.out",
    delay: 0.8,
});
