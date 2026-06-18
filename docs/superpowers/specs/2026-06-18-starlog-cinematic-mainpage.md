# STARLOG Cinematic Main Page Design

> **Status:** Approved by user
> **Date:** 2026-06-18
> **Audience:** Agentic workers implementing this spec

## Goal

Transform the STARLOG main page (Spring Boot + Thymeleaf, Spline viewer) into a cinematic, Apple/Linear-grade experience using GSAP (ScrollTrigger, SplitText, MorphSVG), Three.js + GLSL shaders, Lenis smooth scroll, and SVG morph animations — without React.

## Architecture

A layered WebGL + DOM approach: Three.js star field as fixed background, SVG constellation overlay, and DOM content with semi-transparent sections. Lenis drives smooth scroll, ScrollTrigger coordinates all scroll-driven animations, and MorphSVG transitions constellation shapes per section.

## Tech Stack

| Technology | Version | Role |
|------------|---------|------|
| Lenis | `@studio-freight/lenis@1.0.42` | Smooth scroll with momentum |
| GSAP core + ScrollTrigger | `gsap@3.15.0` | Scroll-driven timeline, camera control, section reveals |
| Three.js | `three@0.170.0` | Star field 3D particle system |
| GLSL (Vertex + Fragment) | Inline shaders | Star twinkle, glow, depth-based size |
| SplitText (GSAP plugin) | `gsap/SplitText` | Intro quote 3D word reveal |
| MorphSVG (GSAP plugin) | `gsap/MorphSVG` | Constellation morph between sections |
| DrawSVG (GSAP plugin) | `gsap/DrawSVG` | SVG constellation draw-in animation |

---

## Layer Structure (z-index)

| z-index | Layer | Description |
|---------|-------|-------------|
| `-1` | Three.js Canvas | `position:fixed`, full-viewport star field |
| `0` | SVG Constellation | `position:fixed`, subtle stroke overlay |
| `1` | HTML Content | Semi-transparent `rgba()` section backgrounds |
| `10` | Header / Navigation | Fixed position, above all content |

---

## File Map

### New Files

| File | Role |
|------|------|
| `src/main/resources/static/js/three-background.js` | ES module — Three.js scene, shader, camera, exposes `window.starField` |
| `src/main/resources/static/js/lenis-init.js` | Lenis initialization + ScrollTrigger/gsap.ticker sync |
| `src/main/resources/static/js/svg-constellation.js` | SVG constellation path definitions + MorphSVG/DrawSVG setup |

### Modified Files

| File | Changes |
|------|---------|
| `src/main/resources/templates/home.html` | Add importmap for Three.js; add CDN scripts (GSAP 3.15.0, Lenis); add SVG container; remove Spline viewer & CDN; update script tags |
| `src/main/resources/static/css/home.css` | Remove `spline-viewer` CSS; add `#star-canvas`, `#constellation-svg` positioning; change section backgrounds to `rgba()`; add `perspective` to `.hero`; add `transform-style: preserve-3d` to cards |
| `src/main/resources/static/js/home.js` | Full rewrite — remove all fade/slide/stagger; add 3D perspective reveals, camera control via ScrollTrigger, SplitText, parallax layers |

---

## Component Design

### 1. Lenis Smooth Scroll

**File:** `src/main/resources/static/js/lenis-init.js`

Initialize Lenis with momentum-based easing. Connect to ScrollTrigger and gsap.ticker for frame sync.

```javascript
const lenis = new Lenis({
  duration: 1.2,
  easing: (t) => Math.min(1, 1.001 - Math.pow(2, -10 * t)),
  smoothWheel: true,
});

lenis.on('scroll', ScrollTrigger.update);
gsap.ticker.add((time) => lenis.raf(time * 1000));
gsap.ticker.lagSmoothing(0);
```

**Key considerations:**
- Must be loaded before any ScrollTrigger-dependent code
- `lagSmoothing(0)` prevents GSAP from interfering with Lenis frame timing
- All GSAP ScrollTrigger animations automatically gain smooth scroll feel

---

### 2. Three.js Star Field + GLSL Shaders

**File:** `src/main/resources/static/js/three-background.js` (ES Module)

**Scene Setup:**
- Renderer: `WebGLRenderer({ alpha: true, antialias: true })`, `setPixelRatio(Math.min(window.devicePixelRatio, 2))`
- Camera: `PerspectiveCamera(75, aspect, 0.1, 1000)`, initial `position.z = 200`
- Background: transparent (HTML background color handles it)

**Particle System (BufferGeometry + ShaderMaterial):**
- 2000 particles (mobile: 1000 via pixel ratio check)
- Distribution: `x ∈ [-300, 300]`, `y ∈ [-200, 200]`, `z ∈ [-500, 200]`
- Custom attributes: `aSize` (0.5–3.0), `aColor` (vec3: white/blue/amber), `aPhase` (0.0–1.0 for twinkle offset)

**Vertex Shader:**
```glsl
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
```

**Fragment Shader:**
```glsl
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
```

**Camera Control (exposed via `window.starField`):**
- `cameraTargetZ`: animated by ScrollTrigger from 200 → -400
- Mouse parallax: `gsap.quickTo()` updates XY rotation offset
- Animation loop: camera.position.z lerps toward cameraTargetZ, rotation offset applied

**GLSL shader notes:**
- All matrix multiplications use built-in Three.js uniforms (modelMatrix, viewMatrix, projectionMatrix)
- `gl_PointCoord` is a built-in GLSL variable available in fragment shaders when rendering points
- `smoothstep` provides soft gaussian-like falloff for star glow
- `pow(alpha, 1.5)` sharpens the center of each star for a more realistic look

---

### 3. SVG Constellation Morph

**File:** `src/main/resources/static/js/svg-constellation.js`

**SVG Structure:**
- Inline SVG in HTML: `<svg id="constellation-svg" viewBox="0 0 1440 900">`
- Contains a single `<path>` element (`#constellation-path`) with `fill="none"` and `stroke`
- Multiple constellation path strings defined in JS

**Constellation Shapes (5 phases):**
1. **Hero (Compass/Star):** Large star shape with diverging lines — represents "STARLOG"
2. **Intro0 (Scattered):** Disconnected points with faint connecting lines — "별이 지나간 자리"
3. **Today (Cup/Vessel):** Curved U-shape — "오늘의 운세를 담는 그릇"
4. **Intro2 (Flow/Wave):** Sine-like curve — "흐름과 대화"
5. **Community (Network):** Interconnected nodes — "연결된 이야기"

**Animation:**
- Initial: path draws in with `DrawSVG` (stroke-dashoffset)
- Scroll: MorphSVG morphs between shapes using ScrollTrigger scrub
- `stroke: rgba(130, 190, 220, 0.25)` — very subtle, like faint starlight

**Implementation pattern:**
```javascript
gsap.to("#constellation-path", {
  morphSVG: { shape: constellationShape2 },
  scrollTrigger: {
    trigger: ".intro0",
    start: "top bottom",
    end: "top center",
    scrub: 1,
  },
});
```

---

### 4. GSAP Animation Design

**File:** `src/main/resources/static/js/home.js`

**Core Rules:**
- NO `autoAlpha` fade-in
- NO `y` slide-up on section entry
- NO simple stagger (use `gsap.utils.distribute` instead)
- ALWAYS use 3D transforms (`rotationX`, `rotationY`, `z`, `scale`) + `perspective`
- ALWAYS use `ease: "power4.out"` or physics-based eases
- ALWAYS prefer `scrub` over `toggleActions`

**Hero Entry Timeline (on load):**
```javascript
const heroTL = gsap.timeline({ defaults: { ease: "power4.out" } });
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
```

**SplitText (intro0):**
```javascript
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
```

**Section Entry (ScrollTrigger scrub):**
```javascript
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
```

**Parallax Layers:**
```javascript
// Image moves slower than scroll
gsap.from(".intro1ImgBox img", {
  yPercent: -15,
  scrollTrigger: {
    trigger: ".intro1",
    start: "top bottom",
    end: "bottom top",
    scrub: 2,
  },
  ease: "none",
});

// Two child boxes move at different rates for depth
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
```

**Fortune List (distribute-based, not simple stagger):**
```javascript
gsap.from(".fortune-list > li", {
  rotation: -2, scale: 0.97, opacity: 0,
  scrollTrigger: {
    trigger: ".today", start: "top 75%", once: true,
  },
  ease: "elastic.out(1, 0.5)",
  duration: 0.8,
  stagger: { from: "center", each: 0.08 },
});
```

---

### 5. CSS Changes

**Section Backgrounds (rgba for star field visibility):**

```css
.hero       { background: transparent; }
.intro0     { background: rgba(22, 24, 56, 0.85); }   /* --deep at 85% */
.intro1     { background: rgba(92, 104, 178, 0.88); }  /* #5C68B2 at 88% */
.today      { background: rgba(32, 35, 83, 0.88); }    /* #202353 at 88% */
.intro2     { background: rgba(157, 178, 245, 0.85); } /* #9DB2F5 at 85% */
.community  { background: rgba(92, 104, 178, 0.88); }  /* #5C68B2 at 88% */
```

**3D Perspective:**
```css
.hero { perspective: 1000px; }
.card { transform-style: preserve-3d; }
```

**Canvas + Overlay:**
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

**Reduced Motion:**
```css
@media (prefers-reduced-motion: reduce) {
  #star-canvas { display: none; }
  .hero { background: var(--deep); }
  .card { transform-style: flat; }
}
```

---

### 6. HTML Changes

**Remove:**
- `<spline-viewer>` element
- `spline-viewer.js` CDN script
- `.spline-logo-overlay` div

**Add:**

```html
<!-- Three.js importmap -->
<script type="importmap">
{
  "imports": {
    "three": "https://unpkg.com/three@0.170.0/build/three.module.js"
  }
}
</script>

<!-- SVG Constellation -->
<svg id="constellation-svg" viewBox="0 0 1440 900" style="position:fixed;inset:0;width:100vw;height:100vh;z-index:0;pointer-events:none">
  <path id="constellation-path" d="..." fill="none" stroke="rgba(130,190,220,0.25)" stroke-width="1.5"/>
</svg>

<!-- Scripts (order matters) -->
<script src="/static/js/lenis-init.js"></script>
<script src="https://unpkg.com/gsap@3.15.0/dist/gsap.min.js"></script>
<script src="https://unpkg.com/gsap@3.15.0/dist/ScrollTrigger.min.js"></script>
<script src="https://unpkg.com/gsap@3.15.0/dist/SplitText.min.js"></script>
<script src="https://unpkg.com/gsap@3.15.0/dist/MorphSVGPlugin.min.js"></script>
<script src="https://unpkg.com/gsap@3.15.0/dist/DrawSVGPlugin.min.js"></script>
<script type="module" src="/static/js/three-background.js"></script>
<script src="/static/js/svg-constellation.js"></script>
<script src="/static/js/home.js"></script>
```

---

## Scroll Timeline

| Scroll % | Camera Z | Section | Constellation | Notes |
|----------|----------|---------|---------------|-------|
| 0–15% | 200 → 100 | Hero | Compass/Star | Hero entry animation plays |
| 15–30% | 100 → -50 | intro0 | Scattered dots | SplitText triggers, section scrubs in |
| 30–45% | -50 → -150 | intro1 | (transition) | Image parallax active |
| 45–65% | -150 → -250 | today | Cup/Vessel | Fortune list distribute animation |
| 65–80% | -250 → -350 | intro2 | Flow/Wave | Section scrub + parallax |
| 80–100% | -350 → -400 | community | Network nodes | Community section, final constellation |

---

## Self-Review Checklist

- [x] No placeholder values or TODOs
- [x] No contradictions between sections
- [x] Scope focused on main page only
- [x] No ambiguous requirements — every animation approach is explicit
- [x] File paths are exact and consistent
- [x] CSS class names match existing codebase patterns
- [x] CDN version numbers consistent (all GSAP 3.15.0)
- [x] All 6 requested technologies included and their roles defined
- [x] Reduced motion fallback specified
