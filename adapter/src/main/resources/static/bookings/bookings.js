(() => {
    const listEl = document.getElementById('bookings-list');
    const statusEl = document.getElementById('status-text');
    let bookings = [];

    function formatDate(epochSecond) {
        const date = new Date(Number(epochSecond) * 1000);
        return date.toLocaleDateString('ko-KR', {
            year: 'numeric',
            month: 'short',
            day: 'numeric',
            weekday: 'short',
        });
    }

    function setStatus(message, isError = false) {
        statusEl.textContent = message;
        statusEl.classList.toggle('error', isError);
    }

    function renderEmpty() {
        listEl.innerHTML = '';
        const empty = document.createElement('div');
        empty.className = 'empty';
        empty.textContent = '아직 예약이 없습니다.';
        listEl.appendChild(empty);
    }

    function renderList() {
        listEl.innerHTML = '';
        if (!bookings.length) {
            renderEmpty();
            return;
        }

        bookings.forEach(item => {
            const bookingId = item.bookingId ?? item.id;
            const card = document.createElement('div');
            card.className = 'booking-card';
            card.dataset.id = bookingId ? String(bookingId) : '';

            const meta = document.createElement('div');
            meta.className = 'booking-meta';

            const title = document.createElement('p');
            title.className = 'booking-title';
            title.textContent = item.nickname || '예약자';

            const range = document.createElement('p');
            range.className = 'booking-range';
            // Backend now returns single date field instead of from/to
            range.textContent = formatDate(item.date);

            const badge = document.createElement('div');
            badge.className = 'badge';
            badge.innerHTML = '<span class="dot"></span><span>내 예약</span>';

            meta.appendChild(title);
            meta.appendChild(range);
            meta.appendChild(badge);

            const actions = document.createElement('div');
            actions.className = 'actions';

            const cancelBtn = document.createElement('button');
            cancelBtn.className = 'btn btn-outline';
            cancelBtn.textContent = bookingId ? '예약 취소' : '취소 불가';
            if (bookingId) {
                cancelBtn.addEventListener('click', () => handleCancel(bookingId, cancelBtn));
            } else {
                cancelBtn.disabled = true;
                cancelBtn.title = '예약 번호가 없어 취소할 수 없습니다.';
            }

            actions.appendChild(cancelBtn);

            card.appendChild(meta);
            card.appendChild(actions);
            listEl.appendChild(card);
        });
    }

    async function loadBookings() {
        setStatus('불러오는 중...');
        listEl.innerHTML = '';
        try {
            const res = await fetch('/api/v1/user/me/bookings', { credentials: 'include' });
            if (!res.ok) throw new Error(`불러오기에 실패했습니다 (${res.status})`);
            const data = await res.json();
            bookings = Array.isArray(data.bookings) ? data.bookings : [];
            setStatus(`${bookings.length}건`);
            renderList();
        } catch (err) {
            console.error(err);
            setStatus(err.message || '예약을 불러오지 못했습니다.', true);
            renderEmpty();
        }
    }

    async function handleCancel(id, buttonEl) {
        if (!id) return;
        const ok = confirm('이 예약을 취소하시겠습니까?');
        if (!ok) return;

        buttonEl.disabled = true;
        try {
            const res = await fetch(`/api/v1/booking/${id}`, {
                method: 'DELETE',
                credentials: 'include',
            });
            if (!res.ok) throw new Error('취소에 실패했습니다. 잠시 후 다시 시도해주세요.');
            bookings = bookings.filter(b => (b.bookingId ?? b.id) !== id);
            setStatus(`${bookings.length}건`);
            renderList();
        } catch (err) {
            console.error(err);
            alert(err.message || '예약 취소 중 오류가 발생했습니다.');
            buttonEl.disabled = false;
        }
    }

    document.addEventListener('DOMContentLoaded', () => { void loadBookings(); });
})();
