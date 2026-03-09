// 프로필 이미지 선택 시 즉시 업로드
document.getElementById('profileImageInput').addEventListener('change', function () {
    if (this.files && this.files[0]) {
        const reader = new FileReader();
        reader.onload = function (e) {
            document.getElementById('profileImgPreview').src = e.target.result;
        };
        reader.readAsDataURL(this.files[0]);
        document.getElementById('profileImageForm').submit();
    }
});

// 자기소개 편집기 표시
function showBioEditor() {
    document.getElementById('bioDisplay').style.display = 'none';
    document.getElementById('bioForm').style.display = 'block';
    const textarea = document.getElementById('bioTextarea');
    updateCharCount(textarea.value.length);
    textarea.focus();
}

// 자기소개 편집기 숨김
function hideBioEditor() {
    document.getElementById('bioForm').style.display = 'none';
    document.getElementById('bioDisplay').style.display = 'flex';
}

// 글자수 카운트
function updateCharCount(len) {
    document.getElementById('bioCharCount').textContent = len;
}

document.getElementById('bioTextarea').addEventListener('input', function () {
    updateCharCount(this.value.length);
});

// 초기 글자수
const initBio = document.getElementById('bioTextarea').value;
updateCharCount(initBio ? initBio.length : 0);
