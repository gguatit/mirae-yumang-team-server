import * as THREE from 'three';

var scene = new THREE.Scene();
var camera = new THREE.PerspectiveCamera(75, window.innerWidth / window.innerHeight, 0.1, 1000);
camera.position.z = 200;

var renderer = new THREE.WebGLRenderer({ alpha: true, antialias: true });
renderer.setSize(window.innerWidth, window.innerHeight);
renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
renderer.domElement.id = 'star-canvas';
document.body.prepend(renderer.domElement);

var starCount = window.innerWidth < 768 ? 1000 : 2000;
var positions = new Float32Array(starCount * 3);
var sizes = new Float32Array(starCount);
var colors = new Float32Array(starCount * 3);
var phases = new Float32Array(starCount);

for (var i = 0; i < starCount; i++) {
    positions[i * 3] = (Math.random() - 0.5) * 600;
    positions[i * 3 + 1] = (Math.random() - 0.5) * 400;
    positions[i * 3 + 2] = (Math.random() - 0.5) * 700 - 200;

    sizes[i] = 0.5 + Math.random() * 2.5;

    var temp = Math.random();
    if (temp < 0.6) {
        colors[i * 3] = 1; colors[i * 3 + 1] = 1; colors[i * 3 + 2] = 1;
    } else if (temp < 0.8) {
        colors[i * 3] = 0.8; colors[i * 3 + 1] = 0.85; colors[i * 3 + 2] = 1;
    } else {
        colors[i * 3] = 1; colors[i * 3 + 1] = 0.9; colors[i * 3 + 2] = 0.7;
    }

    phases[i] = Math.random();
}

var geometry = new THREE.BufferGeometry();
geometry.setAttribute('position', new THREE.BufferAttribute(positions, 3));
geometry.setAttribute('aSize', new THREE.BufferAttribute(sizes, 1));
geometry.setAttribute('aColor', new THREE.BufferAttribute(colors, 3));
geometry.setAttribute('aPhase', new THREE.BufferAttribute(phases, 1));

var vertexShader = '\
attribute float aSize;\
attribute vec3 aColor;\
attribute float aPhase;\
uniform float uTime;\
varying vec3 vColor;\
varying float vPhase;\
\
void main() {\
    vec4 viewPos = viewMatrix * modelMatrix * vec4(position, 1.0);\
    gl_Position = projectionMatrix * viewPos;\
    gl_PointSize = aSize * (300.0 / -viewPos.z);\
    gl_PointSize = clamp(gl_PointSize, 1.0, 100.0);\
    vColor = aColor;\
    vPhase = aPhase;\
}';

var fragmentShader = '\
uniform float uTime;\
varying vec3 vColor;\
varying float vPhase;\
\
void main() {\
    float dist = distance(gl_PointCoord, vec2(0.5));\
    if (dist > 0.5) discard;\
    float alpha = 1.0 - smoothstep(0.0, 0.5, dist);\
    alpha = pow(alpha, 1.5);\
    float twinkle = 0.6 + 0.4 * sin(uTime * (0.3 + vPhase * 0.7) + vPhase * 6.283);\
    gl_FragColor = vec4(vColor, alpha * twinkle);\
}';

var material = new THREE.ShaderMaterial({
    uniforms: {
        uTime: { value: 0 },
    },
    vertexShader: vertexShader,
    fragmentShader: fragmentShader,
    transparent: true,
    depthWrite: false,
    blending: THREE.AdditiveBlending,
});

var stars = new THREE.Points(geometry, material);
scene.add(stars);

function animate() {
    requestAnimationFrame(animate);
    material.uniforms.uTime.value += 0.01;
    renderer.render(scene, camera);
}

animate();

var cameraTarget = { z: 200 };
var mouse = { x: 0, y: 0 };

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
