# STARLOG Cinematic Main Page Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Transform the STARLOG main page into a cinematic experience using Three.js + GLSL shaders, GSAP ScrollTrigger/SplitText/MorphSVG, Lenis smooth scroll, and SVG constellation morph — replacing the current Spline viewer and PowerPoint-style fade animations.

**Architecture:** Layered WebGL + DOM — fixed Three.js star field (z:-1), SVG constellation overlay (z:0), DOM content with semi-transparent rgba sections (z:1), fixed header (z:10). Lenis drives smooth scroll, ScrollTrigger coordinates all scroll-driven animations.

**Tech Stack:** Three.js 0.170.0 (CDN importmap), GSAP 3.15.0 + ScrollTrigger + SplitText + MorphSVGPlugin + DrawSVGPlugin (CDN), Lenis 1.0.42 (CDN), custom GLSL vertex/fragment shaders.

---

### Task 1: CSS Updates (home.css)

**Files:**
- Modify: `src/main/resources/static/css/home.css`

- [ ] **Step 1: Remove Spline viewer CSS**

Find and delete the `spline-viewer` and `.spline-logo-overlay` CSS blocks (lines 45-63). The hero section no longer uses Spline.

- [ ] **Step 2: Add canvas and SVG positioning**

```css
#star-canvas {
    position: fixed;
    inset: 0;
    width: 100vw;
    height: 100vh;
    z-index: -1;
    pointer-events: none;
}
#constellation-svg {
    position: fixed;
    inset: 0;
    width: 100vw;
    height: 100vh;
    z-index: 0;
    pointer-events: none;
}
```

- [ ] **Step 3: Change section backgrounds to rgba**

Replace solid background colors with semi-transparent versions so the star field shows through:

```css
.intro0 {
    background: rgba(22, 24, 56, 0.85);
}
.intro1 {
    background: rgba(92, 104, 178, 0.88);
}
.today {
    background: rgba(32, 35, 83, 0.88);
}
.intro2 {
    background: rgba(157, 178, 245, 0.85);
}
.community {
    background: rgba(92, 104, 178, 0.88);
}
```

- [ ] **Step 4: Add 3D perspective**

```css
.hero {
    perspective: 1000px;
}
.card {
    transform-style: preserve-3d;
}
```

- [ ] **Step 5: Add reduced-motion fallback**

```css
@media (prefers-reduced-motion: reduce) {
    #star-canvas {
        display: none;
    }
    #constellation-svg {
        display: none;
    }
    .hero {
        background: var(--deep);
    }
    .card {
        transform-style: flat;
    }
}
```

- [ ] **Step 6: Set hero background to transparent**

The hero no longer needs a background color since the star field serves as background:

```css
.hero {
    background: transparent;
}
```

- [ ] **Step 7: Verify CSS**

Run: `grep -c 'spline-viewer' src/main/resources/static/css/home.css`
Expected: 0 (Spline CSS removed)

---

### Task 2: HTML Updates (home.html)

**Files:**
- Modify: `src/main/resources/templates/home.html`

- [ ] **Step 1: Add Three.js importmap in `<head>`**

After the viewport meta tag:

```html
<script type="importmap">
{
    "imports": {
        "three": "https://unpkg.com/three@0.170.0/build/three.module.js"
    }
}
</script>
```

- [ ] **Step 2: Remove Spline viewer elements**

Delete the `<spline-viewer>` element and the `.spline-logo-overlay` div. Keep all other hero content (header, title, buttons).

Also remove the Spline CDN script: `<script type="module" src="https://unpkg.com/@splinetool/viewer@1.12.81/build/spline-viewer.js"></script>`

- [ ] **Step 3: Add SVG constellation element before `</body>`**

```html
<svg id="constellation-svg" viewBox="0 0 1440 900">
    <path id="constellation-path" d="M720,450" fill="none" stroke="rgba(130,190,220,0.25)" stroke-width="1.5"/>
</svg>
```

- [ ] **Step 4: Replace all scripts**

Remove existing script tags. Add these in order:

```html
<script src="https://unpkg.com/@studio-freight/lenis@1.0.42/dist/lenis.min.js"></script>
<script src="https://unpkg.com/gsap@3.15.0/dist/gsap.min.js"></script>
<script src="https://unpkg.com/gsap@3.15.0/dist/ScrollTrigger.min.js"></script>
<script src="https://unpkg.com/gsap@3.15.0/dist/SplitText.min.js"></script>
<script src="https://unpkg.com/gsap@3.15.0/dist/MorphSVGPlugin.min.js"></script>
<script src="https://unpkg.com/gsap@3.15.0/dist/DrawSVGPlugin.min.js"></script>
<script type="module" src="/static/js/three-background.js"></script>
<script src="/static/js/svg-constellation.js"></script>
<script src="/static/js/home.js"></script>
```

- [ ] **Step 5: Remove old CSS cache-busting version**

Update CSS links to new version:
```html
<link rel="stylesheet" href="/static/css/reset.css?v=3">
<link rel="stylesheet" href="/static/css/home.css?v=3">
```

- [ ] **Step 6: Verify HTML**

Run: `grep -c 'spline-viewer' src/main/resources/templates/home.html`
Expected: 0
Run: `grep -c 'three-background.js' src/main/resources/templates/home.html`
Expected: 1
Run: `grep -c 'lenis' src/main/resources/templates/home.html`
Expected: 2 (script src + lenis.min.js)

---

### Task 3: Lenis Smooth Scroll

**Files:**
- Create: `src/main/resources/static/js/lenis-init.js`

- [ ] **Step 1: Create lenis-init.js**

```javascript
var lenis = new Lenis({
    duration: 1.2,
    easing: function (t) { return Math.min(1, 1.001 - Math.pow(2, -10 * t)); },
    smoothWheel: true,
});

lenis.on('scroll', ScrollTrigger.update);

gsap.ticker.add(function (time) {
    lenis.raf(time * 1000);
});
gsap.ticker.lagSmoothing(0);
```

- [ ] **Step 2: Verify file created**

Run: `ls -la src/main/resources/static/js/lenis-init.js`
Expected: file exists, non-zero size

---

### Task 4: Three.js Star Field + GLSL Shaders

**Files:**
- Create: `src/main/resources/static/js/three-background.js` (ES Module)

- [ ] **Step 1: Create the Three.js ES module**

```javascript
import * as THREE from 'three';

const scene = new THREE.Scene();
const camera = new THREE.PerspectiveCamera(75, window.innerWidth / window.innerHeight, 0.1, 1000);
camera.position.z = 200;

const renderer = new THREE.WebGLRenderer({ alpha: true, antialias: true });
renderer.setSize(window.innerWidth, window.innerHeight);
renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
renderer.domElement.id = 'star-canvas';
document.body.prepend(renderer.domElement);

const starCount = window.innerWidth < 768 ? 1000 : 2000;
const positions = new Float32Array(starCount * 3);
const sizes = new Float32Array(starCount);
const colors = new Float32Array(starCount * 3);
const phases = new Float32Array(starCount);

for (let i = 0; i < starCount; i++) {
    positions[i * 3] = (Math.random() - 0.5) * 600;
    positions[i * 3 + 1] = (Math.random() - 0.5) * 400;
    positions[i * 3 + 2] = (Math.random() - 0.5) * 700 - 200;

    sizes[i] = 0.5 + Math.random() * 2.5;

    const temp = Math.random();
    if (temp < 0.6) {
        colors[i * 3] = 1; colors[i * 3 + 1] = 1; colors[i * 3 + 2] = 1;
    } else if (temp < 0.8) {
        colors[i * 3] = 0.8; colors[i * 3 + 1] = 0.85; colors[i * 3 + 2] = 1;
    } else {
        colors[i * 3] = 1; colors[i * 3 + 1] = 0.9; colors[i * 3 + 2] = 0.7;
    }

    phases[i] = Math.random();
}

const geometry = new THREE.BufferGeometry();
geometry.setAttribute('position', new THREE.BufferAttribute(positions, 3));
geometry.setAttribute('aSize', new THREE.BufferAttribute(sizes, 1));
geometry.setAttribute('aColor', new THREE.BufferAttribute(colors, 3));
geometry.setAttribute('aPhase', new THREE.BufferAttribute(phases, 1));

const vertexShader = `
attribute float aSize;
attribute vec3 aColor;
attribute float aPhase;
uniform float uTime;
varying vec3 vColor;
varying float vPhase;

void main() {
    vec4 viewPos = viewMatrix * modelMatrix * vec4(position, 1.0);
    gl_Position = projectionMatrix * viewPos;
    gl_PointSize = aSize * (300.0 / -viewPos.z);
    gl_PointSize = clamp(gl_PointSize, 1.0, 100.0);
    vColor = aColor;
    vPhase = aPhase;
}
`;

const fragmentShader = `
uniform float uTime;
varying vec3 vColor;
varying float vPhase;

void main() {
    float dist = distance(gl_PointCoord, vec2(0.5));
    if (dist > 0.5) discard;
    float alpha = 1.0 - smoothstep(0.0, 0.5, dist);
    alpha = pow(alpha, 1.5);
    float twinkle = 0.6 + 0.4 * sin(uTime * (0.3 + vPhase * 0.7) + vPhase * 6.283);
    gl_FragColor = vec4(vColor, alpha * twinkle);
}
`;

const material = new THREE.ShaderMaterial({
    uniforms: {
        uTime: { value: 0 },
    },
    vertexShader: vertexShader,
    fragmentShader: fragmentShader,
    transparent: true,
    depthWrite: false,
    blending: THREE.AdditiveBlending,
});

const stars = new THREE.Points(geometry, material);
scene.add(stars);

function animate() {
    requestAnimationFrame(animate);
    material.uniforms.uTime.value += 0.01;
    renderer.render(scene, camera);
}

animate();

const cameraTarget = { z: 200 };
const mouse = { x: 0, y: 0 };

window.starField = {
    camera: camera,
    cameraTarget: cameraTarget,
    mouse: mouse,
    updateCamera: function () {
        camera.position.z += (cameraTarget.z - camera.position.z) * 0.08;
        camera.position.x += (mouse.x * 30 - camera.position.x) * 0.05;
        camera.position.y += (mouse.y * 20 - camera.position.y) * 0.05;
        camera.lookAt(0, 0, 0);
    },
};

function onMouseMove(e) {
    mouse.x = (e.clientX / window.innerWidth - 0.5) * 2;
    mouse.y = (e.clientY / window.innerHeight - 0.5) * 2;
}
document.addEventListener('mousemove', onMouseMove);

function onResize() {
    camera.aspect = window.innerWidth / window.innerHeight;
    camera.updateProjectionMatrix();
    renderer.setSize(window.innerWidth, window.innerHeight);
}
window.addEventListener('resize', onResize);
```

- [ ] **Step 2: Verify file created**

Run: `ls -la src/main/resources/static/js/three-background.js`
Expected: file exists, non-zero size

---

### Task 5: SVG Constellation Morph

**Files:**
- Create: `src/main/resources/static/js/svg-constellation.js`

- [ ] **Step 1: Create svg-constellation.js**

```javascript
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
```

- [ ] **Step 2: Verify file created**

Run: `ls -la src/main/resources/static/js/svg-constellation.js`
Expected: file exists, non-zero size

---

### Task 6: GSAP Cinematic Animation Rewrite (home.js)

**Files:**
- Rewrite: `src/main/resources/static/js/home.js`

- [ ] **Step 1: Rewrite home.js with cinematic animations**

Remove all existing content. Replace with:

```javascript
gsap.registerPlugin(ScrollTrigger, SplitText);

function initAnimations() {
    var mm = gsap.matchMedia();

    mm.add("(prefers-reduced-motion: no-preference)", function () {

        // Hero entry timeline — 3D perspective
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

        // Three.js camera — scroll-driven Z position
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

        // Three.js camera update loop
        gsap.ticker.add(function () {
            if (window.starField) {
                window.starField.updateCamera();
            }
        });

        // SplitText for intro0 quote
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

        // Section cinematic reveals — 3D scrub
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

        // Parallax layers — different speeds
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

        // Fortune/community list — distribute-based entry
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

        // Card bottom — subtle 3D rotation entry
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

        return function () { mm.revert(); };
    });
}

document.fonts.ready.then(initAnimations);
```

- [ ] **Step 2: Verify file**

Run: `grep -c 'autoAlpha' src/main/resources/static/js/home.js`
Expected: 0 (no fade-in animations)
Run: `grep -c 'rotationX' src/main/resources/static/js/home.js`
Expected: > 5 (3D animations present)

---

### Task 7: Build and Verify

**Files:**
- Rebuild: `target/demo-0.0.1-SNAPSHOT.jar`

- [ ] **Step 1: Kill old server**

```bash
kill $(ps aux | grep 'demo-0.0.1-SNAPSHOT.jar' | grep -v grep | awk '{print $2}') 2>/dev/null
sleep 2
```

- [ ] **Step 2: Rebuild project**

```bash
./mvnw package -DskipTests -q
```

Expected: BUILD SUCCESS (no errors)

- [ ] **Step 3: Start server**

```bash
nohup /usr/bin/java -jar target/demo-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod > /dev/null 2>&1 &
```

- [ ] **Step 4: Verify server responds**

```bash
sleep 25
curl -s -o /dev/null -w "%{http_code}" http://localhost:8090/home
```

Expected: 200

- [ ] **Step 5: Verify JS loads (check for no 404s)**

```bash
grep -E "home\.js|three-background|lenis|svg-constellation" src/main/resources/templates/home.html
```

Expected: All 4 JS files referenced in order

- [ ] **Step 6: Commit all changes**

```bash
git add src/main/resources/static/js/lenis-init.js \
        src/main/resources/static/js/three-background.js \
        src/main/resources/static/js/svg-constellation.js \
        src/main/resources/static/js/home.js \
        src/main/resources/static/css/home.css \
        src/main/resources/templates/home.html
git commit -m "🎬 feat: 시네마틱 메인페이지 — Three.js + GSAP + Lenis + Shader + SVG Morph"
```
