async function handleLikeHate(postId, type) {
    const url = `/posts/api/${postId}/like-hate?type=${type}`;

    const csrfToken = document.querySelector('meta[name="_csrf"]').content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]').content;

    try {
        const response = await fetch(url, {
            method: 'POST',
            headers: { [csrfHeader]: csrfToken }
        });

        if (response.status === 401) {
            alert("로그인이 필요한 서비스입니다.");
            window.location.href = "/auth/login";
            return;
        }

        if (response.ok) {
            const data = await response.json();
            const likeEl = document.getElementById('like-count');
            const hateEl = document.getElementById('hate-count');
            if (likeEl) likeEl.textContent = data.likeCount;
            if (hateEl) hateEl.textContent = data.hateCount;
        } else {
            console.error("Response Status:", response.status);
            if (response.status === 404) {
                alert("에러 404: 주소를 찾을 수 없습니다. 컨트롤러 매핑을 확인하세요.");
            } else if (response.status === 400) {
                alert("에러 400: 보낸 데이터(type)가 서버의 Enum 형식과 맞지 않습니다.");
            } else if (response.status === 403) {
                alert("에러 403: 보안(CSRF) 권한 문제입니다.");
            } else {
                alert("처리 중 오류가 발생했습니다. 코드: " + response.status);
            }
        }
    } catch (error) {
        console.error("Error:", error);
        alert("서버와 통신할 수 없습니다.");
    }
}

function toggleReplyForm(commentId) {
    const form = document.getElementById('reply-form-' + commentId);
    form.style.display = form.style.display === 'none' ? 'block' : 'none';
}

async function submitComment(event, form) {
    event.preventDefault();

    const formData = new FormData(form);
    const url = form.action;

    try {
        const csrfToken = document.querySelector('meta[name="_csrf"]').content;
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]').content;

        const response = await fetch(url, {
            method: 'POST',
            headers: { [csrfHeader]: csrfToken },
            body: formData,
        });

        if (response.status === 401) {
            alert("로그인이 필요한 서비스입니다.");
            window.location.href = "/auth/login";
            return;
        }

        if (response.ok) {
            const newComment = await response.json();
            appendComment(newComment);
            resetTextarea(form);
        }
    } catch (error) {
        console.error("Error:", error);
        alert("댓글 등록 중 오류가 발생했습니다.");
    }
    return false;
}

function appendComment(comment) {
    const commentList = document.querySelector('.comment-list');

    if (!commentList) {
        console.error("댓글 리스트 영역을 찾을 수 없습니다!");
        return;
    }

    if (comment.parentId) {
        const parentForm = document.getElementById('reply-form-' + comment.parentId);
        parentForm.closest('.comment-item').insertAdjacentHTML('beforeend', `
            <div class="reply-item" style="margin-left: 30px; margin-top: 10px; border-left: 2px solid #444; padding-left: 15px;">
                <span style="color: #888;">ㄴ</span>
                <strong>${comment.username}</strong>
                <span style="font-size: 12px;">방금 전</span>
                <p>${comment.content}</p>
            </div>
        `);
    } else {
        const commentHtml = `
            <div class="comment-item">
                <strong>${comment.username}</strong>
                <span style="font-size: 12px;">방금 전</span>
                <p>${comment.content}</p>
                <button type="button" onclick="toggleReplyForm(${comment.id})" 
                        class="btn-reply">[답글 달기]</button>
                <div id="reply-form-${comment.id}" style="display: none; margin: 10px 0 10px 20px;">
                    <form action="/posts/${window.location.pathname.split('/').pop()}/comments" method="post" onsubmit="return submitComment(event, this)">
                        <input type="hidden" name="parentId" value="${comment.id}">
                        <textarea name="content" class="comment-textarea" placeholder="대댓글을 입력하세요" required></textarea>
                        <button type="submit">등록</button>
                    </form>
                </div>
            </div>
        `;
        commentList.insertAdjacentHTML('beforeend', commentHtml);
    }

    const countSpan = document.querySelector('.comment-list h3 span');
    if (countSpan) countSpan.innerText = parseInt(countSpan.innerText) + 1;
}

// textarea 자동 높이 조절 (내용/줄바꿈 기준)
function autoResize(el) {
    el.style.height = 'auto';
    const scrollH = el.scrollHeight;
    const maxH = parseInt(getComputedStyle(el).maxHeight) || 300;
    if (scrollH >= maxH) {
        el.style.height = maxH + 'px';
        el.classList.add('is-overflow');
    } else {
        el.style.height = scrollH + 'px';
        el.classList.remove('is-overflow');
    }
}

document.addEventListener('input', function (e) {
    if (e.target.tagName === 'TEXTAREA' && e.target.name === 'content') {
        autoResize(e.target);
    }
});

// Enter / Ctrl+Enter → 전송, Shift+Enter → 줄바꿈
document.addEventListener('keydown', function (e) {
    if (e.target.tagName === 'TEXTAREA' && e.target.name === 'content') {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            const form = e.target.closest('form');
            if (form) submitComment(e, form);
        }
    }
});

// 전송 후 textarea 높이 초기화
function resetTextarea(form) {
    const ta = form.querySelector('textarea[name="content"]');
    if (ta) {
        ta.value = '';
        ta.style.height = '';
        ta.classList.remove('is-overflow');
    }
}
