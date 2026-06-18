import * as THREE from 'three';
import { Line2 } from 'three/addons/lines/Line2.js';
import { LineGeometry } from 'three/addons/lines/LineGeometry.js';
import { LineMaterial } from 'three/addons/lines/LineMaterial.js';

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

// --- Shooting stars with Line2 trails (thick glowing streaks, not dots) ---
var METEOR_COUNT = 4;
var TRAIL_LEN = 32;
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

var trailMaterials = [];
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

    // Trail line (Line2 = thick line with per-vertex color fade)
    var lineGeo = new LineGeometry();
    lineGeo.setPositions(new Float32Array(TRAIL_LEN * 3));
    lineGeo.setColors(new Float32Array(TRAIL_LEN * 3));

    var lineMat = new LineMaterial({
        color: 0xffffff,
        vertexColors: true,
        linewidth: 5,
        transparent: true,
        opacity: 0.9,
        blending: THREE.AdditiveBlending,
        depthWrite: false,
        resolution: new THREE.Vector2(window.innerWidth, window.innerHeight),
    });

    var line = new Line2(lineGeo, lineMat);
    line.visible = false;
    scene.add(line);

    trailMaterials.push(lineMat);

    meteors.push({
        sprite: spr,
        line: line,
        lineGeo: lineGeo,
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
            m.line.visible = false;
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

        // Head sprite
        m.sprite.position.set(m.x, m.y, m.z);
        var size = 10 + 22 * fadeIn * fadeOut;
        m.sprite.scale.set(size, size, 1);
        m.sprite.material.opacity = opacity;

        // Trail
        m.trail.unshift({ x: m.x, y: m.y, z: m.z });
        if (m.trail.length > TRAIL_LEN) m.trail.pop();

        if (m.trail.length > 1) {
            var posArr = new Float32Array(TRAIL_LEN * 3);
            var colArr = new Float32Array(TRAIL_LEN * 3);
            for (var j = 0; j < TRAIL_LEN; j++) {
                if (j < m.trail.length) {
                    var t = m.trail[j];
                    posArr[j * 3] = t.x;
                    posArr[j * 3 + 1] = t.y;
                    posArr[j * 3 + 2] = t.z;
                    // Fade from white (head) to black (tail) for additive blending
                    var fade = (1 - j / TRAIL_LEN) * opacity;
                    colArr[j * 3] = fade;
                    colArr[j * 3 + 1] = fade;
                    colArr[j * 3 + 2] = fade;
                } else {
                    // Fill remaining with last trail point (invisible due to black color)
                    var last = m.trail[m.trail.length - 1];
                    posArr[j * 3] = last.x;
                    posArr[j * 3 + 1] = last.y;
                    posArr[j * 3 + 2] = last.z;
                    colArr[j * 3] = 0;
                    colArr[j * 3 + 1] = 0;
                    colArr[j * 3 + 2] = 0;
                }
            }
            m.lineGeo.setPositions(posArr);
            m.lineGeo.setColors(colArr);
            m.line.visible = true;
        }

        if (m.life >= m.maxLife) {
            m.active = false;
            m.sprite.visible = false;
            m.line.visible = false;
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
    for (var i = 0; i < trailMaterials.length; i++) {
        trailMaterials[i].resolution.set(window.innerWidth, window.innerHeight);
    }
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
