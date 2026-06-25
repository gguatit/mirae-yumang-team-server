(function (global) {
    'use strict';

    function attachUploadProgress(formSelector, progressRootId, submitBtnId) {
        const form = document.querySelector(formSelector);
        const root = document.getElementById(progressRootId);
        const submitBtn = submitBtnId ? document.getElementById(submitBtnId) : null;
        if (!form || !root) return;

        const fill = root.querySelector('.upload-fill');
        const pct = root.querySelector('.upload-pct');
        const status = root.querySelector('.upload-status');

        function setProgress(p) {
            const v = Math.max(0, Math.min(100, p));
            if (fill) fill.style.width = v.toFixed(1) + '%';
            if (pct) pct.textContent = v.toFixed(0) + '%';
        }

        function setStatus(text) {
            if (status) status.textContent = text;
        }

        function show() {
            root.hidden = false;
            setProgress(0);
            setStatus('업로드 중...');
            if (submitBtn) {
                submitBtn.disabled = true;
                submitBtn.dataset.origText = submitBtn.dataset.origText || submitBtn.textContent;
                submitBtn.textContent = '업로드 중...';
            }
        }

        function hide() {
            if (submitBtn) {
                submitBtn.disabled = false;
                if (submitBtn.dataset.origText) submitBtn.textContent = submitBtn.dataset.origText;
            }
        }

        form.addEventListener('submit', function (e) {
            e.preventDefault();

            const csrfToken = document.querySelector('meta[name="_csrf"]');
            const csrfHeader = document.querySelector('meta[name="_csrf_header"]');

            const xhr = new XMLHttpRequest();
            const fd = new FormData(form);

            show();

            xhr.upload.addEventListener('progress', function (ev) {
                if (ev.lengthComputable && ev.total > 0) {
                    setProgress((ev.loaded / ev.total) * 100);
                }
            });

            xhr.upload.addEventListener('load', function () {
                setProgress(100);
                setStatus('서버 처리 중...');
            });

            xhr.addEventListener('load', function () {
                if (xhr.status >= 200 && xhr.status < 400) {
                    setStatus('완료');
                    const target = xhr.responseURL || form.action;
                    if (target && target !== window.location.href) {
                        window.location.href = target;
                    } else {
                        window.location.reload();
                    }
                } else if (xhr.status === 0) {
                    setStatus('업로드가 중단되었습니다.');
                    hide();
                } else {
                    setStatus('업로드 실패 (' + xhr.status + ')');
                    hide();
                    try {
                        const body = xhr.responseText || '';
                        const msg = body.length < 300 ? body : '서버 오류가 발생했습니다.';
                        alert(msg);
                    } catch (_) {
                        alert('업로드 실패 (' + xhr.status + ')');
                    }
                }
            });

            xhr.addEventListener('error', function () {
                setStatus('네트워크 오류');
                hide();
                alert('네트워크 오류로 업로드에 실패했습니다.');
            });

            xhr.addEventListener('abort', function () {
                setStatus('중단됨');
                hide();
            });

            xhr.open(form.method || 'POST', form.action, true);
            if (csrfToken && csrfHeader && csrfToken.content && csrfHeader.content) {
                xhr.setRequestHeader(csrfHeader.content, csrfToken.content);
            }
            xhr.send(fd);
        });
    }

    global.attachUploadProgress = attachUploadProgress;
})(window);
