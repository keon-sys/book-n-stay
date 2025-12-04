const headerTemplate = `
    <div class="header-bar">
        <div class="header-brand">Book n Stay</div>
        <div class="header-actions">
            <button class="header-button" id="header-logout">로그아웃</button>
        </div>
    </div>
`;

function injectHeader() {
    if (document.getElementById('header-logout')) {
        return;
    }
    const wrapper = document.createElement('div');
    wrapper.innerHTML = headerTemplate.trim();
    document.body.prepend(wrapper.firstElementChild);
    bindLogout();
}

function bindLogout() {
    const logoutBtn = document.getElementById('header-logout');
    if (!logoutBtn) {
        return;
    }
    logoutBtn.addEventListener('click', async () => {
        logoutBtn.disabled = true;
        try {
            await fetch('/api/v1/auth/kakao/logout', { method: 'POST', credentials: 'include' });
            if (window.Kakao && window.Kakao.Auth && typeof window.Kakao.Auth.logout === 'function') {
                try {
                    window.Kakao.Auth.logout(() => {});
                } catch (e) {
                    console.warn('Kakao SDK logout failed', e);
                }
            }
            window.location.href = '/v1/auth/kakao?redirect=' + encodeURIComponent(window.location.pathname);
        } catch (err) {
            alert('로그아웃에 실패했습니다. 다시 시도해주세요.');
            console.error(err);
            logoutBtn.disabled = false;
        }
    });
}

document.addEventListener('DOMContentLoaded', injectHeader);
