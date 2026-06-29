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

    renderer.render(scene, camera);
}
animate._lastTime = 0;
animate(0);
