function setupGlobal401Interceptor() {
    const originalFetch = window.fetch;
    window.fetch = async function(...args) {
        const response = await originalFetch.apply(this, args);

        if (response.status === 401) {
            // 인증 페이지에서는 리디렉션하지 않음 (무한 루프 방지)
            if (window.location.pathname.startsWith('/auth/')) {
                return response;
            }

            const clonedResponse = response.clone();
            try {
                const errorData = await clonedResponse.json();
                if (errorData.redirectUrl) {
                    const currentUrl = encodeURIComponent(window.location.pathname + window.location.search);
                    window.location.href = `${errorData.redirectUrl}?redirect=${currentUrl}`;
                    return response;
                }
            } catch (e) {
                console.warn('Failed to parse 401 response', e);
            }

            const currentUrl = encodeURIComponent(window.location.pathname + window.location.search);
            window.location.href = `/auth/kakao?redirect=${currentUrl}`;
        }

        return response;
    };
}

async function fetchLoginStatus() {
    try {
        const res = await fetch('/api/v1/user/kakao/me', { credentials: 'include' });
        return res.ok;
    } catch (e) {
        console.warn('login status check failed', e);
        return false;
    }
}

function headerTemplate(loggedIn) {
    const logoutButton = loggedIn ? '<button class="header-button" id="header-logout">로그아웃</button>' : '';

    return `
        <div class="header-bar">
            <a class="header-brand" href="/">Book n Stay</a>
            <div class="header-actions">
                ${logoutButton}
            </div>
        </div>
    `;
}

async function injectHeader() {
    console.log('[Header] injectHeader called, pathname:', window.location.pathname);
    if (document.querySelector('.header-bar')) {
        console.log('[Header] Header already exists, skipping');
        return;
    }
    // 인증 페이지에서는 로그인 상태 체크 스킵 (무한 루프 방지)
    if (window.location.pathname.startsWith('/auth/')) {
        console.log('[Header] Auth page detected, rendering header without login check');
        renderHeader(false);
        return;
    }
    const loggedIn = await fetchLoginStatus();
    console.log('[Header] Login status:', loggedIn);
    renderHeader(loggedIn);
    startLoginPolling();
}

let loginCheckTimer = null;
let currentLoginState = null;

function renderHeader(loggedIn) {
    console.log('[Header] renderHeader called, loggedIn:', loggedIn);
    currentLoginState = loggedIn;
    const existing = document.querySelector('.header-bar');
    const wrapper = document.createElement('div');
    wrapper.innerHTML = headerTemplate(loggedIn).trim();
    const headerEl = wrapper.firstElementChild;
    console.log('[Header] headerEl:', headerEl);
    if (existing) {
        console.log('[Header] Replacing existing header');
        existing.replaceWith(headerEl);
    } else {
        console.log('[Header] Prepending new header to body');
        document.body.prepend(headerEl);
    }
    bindLogout();
}

function startLoginPolling() {
    if (loginCheckTimer) {
        return;
    }
    loginCheckTimer = setInterval(async () => {
        const loggedIn = await fetchLoginStatus();
        if (loggedIn !== currentLoginState) {
            renderHeader(loggedIn);
        }
    }, 5 * 60 * 1000);
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
            window.location.href = '/auth/kakao?redirect=' + encodeURIComponent(window.location.pathname);
        } catch (err) {
            alert('로그아웃에 실패했습니다. 다시 시도해주세요.');
            console.error(err);
            logoutBtn.disabled = false;
        }
    });
}

setupGlobal401Interceptor();
document.addEventListener('DOMContentLoaded', () => { void injectHeader(); });
