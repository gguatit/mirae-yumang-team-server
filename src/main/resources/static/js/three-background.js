import * as THREE from 'three';

var scene = new THREE.Scene();
var camera = new THREE.PerspectiveCamera(75, window.innerWidth / window.innerHeight, 0.1, 2000);
camera.position.z = 200;

var renderer = new THREE.WebGLRenderer({ alpha: true, antialias: true });
renderer.setSize(window.innerWidth, window.innerHeight);
renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
renderer.domElement.id = 'star-canvas';
document.body.prepend(renderer.domElement);

// --- Star field: 5000 stars, rich color spectrum, depth layers ---
var starCount = window.innerWidth < 768 ? 2500 : 5000;
var positions = new Float32Array(starCount * 3);
var sizes = new Float32Array(starCount);
var colors = new Float32Array(starCount * 3);
var phases = new Float32Array(starCount);
var depths = new Float32Array(starCount);

for (var i = 0; i < starCount; i++) {
    positions[i * 3] = (Math.random() - 0.5) * 800;
    positions[i * 3 + 1] = (Math.random() - 0.5) * 500;
    positions[i * 3 + 2] = (Math.random() - 0.5) * 900 - 200;

    sizes[i] = 0.5 + Math.random() * 3.5;

    var temp = Math.random();
    if (temp < 0.5) {
        colors[i * 3] = 1; colors[i * 3 + 1] = 1; colors[i * 3 + 2] = 1;
    } else if (temp < 0.65) {
        colors[i * 3] = 0.8; colors[i * 3 + 1] = 0.85; colors[i * 3 + 2] = 1;
    } else if (temp < 0.8) {
        colors[i * 3] = 1; colors[i * 3 + 1] = 0.9; colors[i * 3 + 2] = 0.7;
    } else if (temp < 0.9) {
        colors[i * 3] = 1; colors[i * 3 + 1] = 0.7; colors[i * 3 + 2] = 0.6;
    } else {
        colors[i * 3] = 0.6; colors[i * 3 + 1] = 0.9; colors[i * 3 + 2] = 1;
    }

    phases[i] = Math.random();
    depths[i] = Math.random();
}

var geometry = new THREE.BufferGeometry();
geometry.setAttribute('position', new THREE.BufferAttribute(positions, 3));
geometry.setAttribute('aSize', new THREE.BufferAttribute(sizes, 1));
geometry.setAttribute('aColor', new THREE.BufferAttribute(colors, 3));
geometry.setAttribute('aPhase', new THREE.BufferAttribute(phases, 1));
geometry.setAttribute('aDepth', new THREE.BufferAttribute(depths, 1));

var vertexShader = [
    'attribute float aSize;',
    'attribute vec3 aColor;',
    'attribute float aPhase;',
    'attribute float aDepth;',
    'uniform float uTime;',
    'uniform float uScrollVel;',
    'varying vec3 vColor;',
    'varying float vPhase;',
    'varying float vDepth;',
    'void main() {',
    '    vec3 pos = position;',
    '    pos.x += sin(uTime * 0.1 + aPhase * 6.28) * 2.0;',
    '    pos.y += cos(uTime * 0.08 + aPhase * 6.28) * 2.0;',
    '    vec4 viewPos = viewMatrix * modelMatrix * vec4(pos, 1.0);',
    '    gl_Position = projectionMatrix * viewPos;',
    '    float size = aSize * (300.0 / -viewPos.z);',
    '    size *= (1.0 + abs(uScrollVel) * 0.4);',
    '    gl_PointSize = clamp(size, 1.0, 120.0);',
    '    vColor = aColor;',
    '    vPhase = aPhase;',
    '    vDepth = aDepth;',
    '}',
].join('\n');

var fragmentShader = [
    'uniform float uTime;',
    'varying vec3 vColor;',
    'varying float vPhase;',
    'varying float vDepth;',
    'void main() {',
    '    float dist = distance(gl_PointCoord, vec2(0.5));',
    '    if (dist > 0.5) discard;',
    '    float alpha = 1.0 - smoothstep(0.0, 0.5, dist);',
    '    alpha = pow(alpha, 1.5);',
    '    float twinkle = 0.6 + 0.4 * sin(uTime * (0.3 + vPhase * 0.7) + vPhase * 6.283);',
    '    alpha *= (1.0 - vDepth * 0.4);',
    '    gl_FragColor = vec4(vColor, alpha * twinkle);',
    '}',
].join('\n');

var material = new THREE.ShaderMaterial({
    uniforms: {
        uTime: { value: 0 },
        uScrollVel: { value: 0 },
    },
    vertexShader: vertexShader,
    fragmentShader: fragmentShader,
    transparent: true,
    depthWrite: false,
    blending: THREE.AdditiveBlending,
});

var stars = new THREE.Points(geometry, material);
scene.add(stars);

// --- Shooting stars: glowing head sprite + dotted trail (THREE.Points) ---
var METEOR_COUNT = 4;
var TRAIL_LEN = 18;
var meteorCanv = document.createElement('canvas');
meteorCanv.width = 64;
meteorCanv.height = 64;
var mctx = meteorCanv.getContext('2d');
var grd = mctx.createRadialGradient(32, 32, 0, 32, 32, 32);
grd.addColorStop(0, 'rgba(255,255,255,1)');
grd.addColorStop(0.2, 'rgba(255,255,255,0.6)');
grd.addColorStop(0.5, 'rgba(255,255,255,0.2)');
grd.addColorStop(1, 'rgba(255,255,255,0)');
mctx.fillStyle = grd;
mctx.fillRect(0, 0, 64, 64);
var glowTex = new THREE.CanvasTexture(meteorCanv);

var meteors = [];

for (var mi = 0; mi < METEOR_COUNT; mi++) {
    // Head sprite (bright glowing tip)
    var spr = new THREE.Sprite(new THREE.SpriteMaterial({
        map: glowTex,
        color: 0xffffff,
        transparent: true,
        opacity: 0,
        blending: THREE.AdditiveBlending,
        depthWrite: false,
    }));
    spr.scale.set(0, 0, 1);
    scene.add(spr);

    // Trail dots (THREE.Points) — 32개 도트가 머리(크고 밝음)에서 꼬리(작고 어두움)로 페이드
    var trailGeo = new THREE.BufferGeometry();
    var tPos = new Float32Array(TRAIL_LEN * 3);
    var tCol = new Float32Array(TRAIL_LEN * 3);
    var tSize = new Float32Array(TRAIL_LEN);
    trailGeo.setAttribute('position', new THREE.BufferAttribute(tPos, 3));
    trailGeo.setAttribute('aColor', new THREE.BufferAttribute(tCol, 3));
    trailGeo.setAttribute('aSize', new THREE.BufferAttribute(tSize, 1));

    // Per-vertex color/size driven by ShaderMaterial (rounded glow point)
    // aSize: 픽셀 단위 (1.5 ~ 3.0). gl_PointSize는 device pixel에 자동 매핑됨.
    var trailMat = new THREE.ShaderMaterial({
        vertexShader: [
            'attribute vec3 aColor;',
            'attribute float aSize;',
            'varying vec3 vColor;',
            'void main() {',
            '    vColor = aColor;',
            '    vec4 mv = modelViewMatrix * vec4(position, 1.0);',
            '    gl_Position = projectionMatrix * mv;',
            '    // 약한 거리 감쇠 — z=100일 때 0.85배, z=300일 때 0.5배',
            '    float distAtt = clamp(200.0 / max(-mv.z, 50.0), 0.4, 1.0);',
            '    gl_PointSize = aSize * distAtt;',
            '}',
        ].join('\n'),
        fragmentShader: [
            'varying vec3 vColor;',
            'void main() {',
            '    float d = distance(gl_PointCoord, vec2(0.5));',
            '    if (d > 0.5) discard;',
            '    float a = 1.0 - smoothstep(0.0, 0.5, d);',
            '    a = pow(a, 1.6);',
            '    gl_FragColor = vec4(vColor, a);',
            '}',
        ].join('\n'),
        transparent: true,
        depthWrite: false,
        blending: THREE.AdditiveBlending,
    });

    var trailPoints = new THREE.Points(trailGeo, trailMat);
    trailPoints.visible = false;
    trailPoints.frustumCulled = false;
    scene.add(trailPoints);

    meteors.push({
        sprite: spr,
        trailPoints: trailPoints,
        trailGeo: trailGeo,
        tPos: tPos,
        tCol: tCol,
        tSize: tSize,
        active: false,
        life: 0,
        maxLife: 0.9 + Math.random() * 0.6,
        x: 0, y: 0, z: 0,
        vx: 0, vy: 0, vz: 0,
        trail: [],
    });
}

var mt = 0;
function spawnMeteor() {
    for (var i = 0; i < meteors.length; i++) {
        if (!meteors[i].active) {
            var m = meteors[i];
            m.active = true;
            m.life = 0;
            m.maxLife = 0.9 + Math.random() * 0.6;
            m.x = (Math.random() - 0.5) * 500;
            m.y = 150 + Math.random() * 150;
            m.z = 40 + Math.random() * 150;
            var ang = Math.PI * (0.15 + Math.random() * 0.35);
            var sp = 320 + Math.random() * 220;
            var ang2 = Math.random() * 0.4 - 0.2;
            m.vx = Math.cos(ang) * sp;
            m.vy = -Math.sin(ang) * sp;
            m.vz = ang2 * sp;
            m.trail = [];
            m.sprite.visible = true;
            m.sprite.material.opacity = 0;
            m.trailPoints.visible = false;
            return;
        }
    }
}

function updateMeteors(delta) {
    mt += delta;
    if (mt > 0.8 + Math.random() * 2.0) {
        spawnMeteor();
        mt = 0;
    }

    for (var i = 0; i < meteors.length; i++) {
        var m = meteors[i];
        if (!m.active) {
            m.trailPoints.visible = false;
            m.sprite.visible = false;
            continue;
        }

        m.life += delta;
        var progress = m.life / m.maxLife;
        var fadeIn = Math.min(1, progress * 5);
        var fadeOut = Math.max(0, 1 - (progress - 0.7) / 0.3);
        var opacity = fadeIn * fadeOut;

        m.x += m.vx * delta;
        m.y += m.vy * delta;
        m.z += m.vz * delta;

        // Head sprite (가산 블렌딩 텍스처라 sprite 스케일 1.0~1.5가 적절)
        m.sprite.position.set(m.x, m.y, m.z);
        var headSize = 1.0 + 1.8 * fadeIn * fadeOut;
        m.sprite.scale.set(headSize, headSize, 1);
        m.sprite.material.opacity = opacity * 0.9;

        // Trail dots: 2프레임마다 추가해서 시각적 간격 확보
        m.dotAcc = (m.dotAcc || 0) + delta;
        if (m.dotAcc >= 0.025) {
            m.trail.unshift({ x: m.x, y: m.y, z: m.z });
            if (m.trail.length > TRAIL_LEN) m.trail.pop();
            m.dotAcc = 0;
        }

        for (var j = 0; j < TRAIL_LEN; j++) {
            if (j < m.trail.length) {
                var t = m.trail[j];
                m.tPos[j * 3] = t.x;
                m.tPos[j * 3 + 1] = t.y;
                m.tPos[j * 3 + 2] = t.z;
                var fade = (1 - j / TRAIL_LEN) * opacity;
                m.tCol[j * 3] = fade;
                m.tCol[j * 3 + 1] = fade;
                m.tCol[j * 3 + 2] = fade;
                // 픽셀 단위: 머리 ~2.6, 꼬리 ~0.5
                m.tSize[j] = 2.4 * (1 - j / TRAIL_LEN) + 0.5;
            } else {
                // 비활성 도트는 머리 위치에 뭉치고 size 0 → 안 보임
                m.tPos[j * 3] = m.x;
                m.tPos[j * 3 + 1] = m.y;
                m.tPos[j * 3 + 2] = m.z;
                m.tCol[j * 3] = 0;
                m.tCol[j * 3 + 1] = 0;
                m.tCol[j * 3 + 2] = 0;
                m.tSize[j] = 0;
            }
        }
        m.trailGeo.attributes.position.needsUpdate = true;
        m.trailGeo.attributes.aColor.needsUpdate = true;
        m.trailGeo.attributes.aSize.needsUpdate = true;
        m.trailPoints.visible = true;

        if (m.life >= m.maxLife) {
            m.active = false;
            m.sprite.visible = false;
            m.trailPoints.visible = false;
        }
    }
}

// --- Camera control (integrated into Three.js loop, no GSAP ticker) ---
var cameraTarget = { z: 200 };
var mouse = { x: 0, y: 0 };
var scrollVel = 0;

window.starField = {
    camera: camera,
    cameraTarget: cameraTarget,
    mouse: mouse,
    setScrollVel: function (v) { scrollVel = v; },
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

// --- Unified animation loop ---
function animate(time) {
    var delta = Math.min(0.05, (time - (animate._lastTime || time)) / 1000);
    animate._lastTime = time;
    requestAnimationFrame(animate);

    material.uniforms.uTime.value += 0.01;
    material.uniforms.uScrollVel.value = scrollVel;
    scrollVel *= 0.92;

    stars.rotation.y += 0.0002;
    stars.rotation.x += 0.0001;

    camera.position.z += (cameraTarget.z - camera.position.z) * 0.1;
    camera.position.x += (mouse.x * 60 - camera.position.x) * 0.06;
    camera.position.y += (mouse.y * 40 - camera.position.y) * 0.06;
    camera.lookAt(0, 0, 0);

    updateMeteors(delta);
    renderer.render(scene, camera);
}
animate._lastTime = 0;
animate(0);
