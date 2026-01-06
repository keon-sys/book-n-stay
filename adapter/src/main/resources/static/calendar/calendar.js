(() => {
    const MAX_CAPACITY = 8;
    const bookings = [];
    const bookingKeys = new Set();
    const loadedDates = new Set();

    const SEOUL_TIMEZONE = 'Asia/Seoul';
    const seoulDateFormatter = new Intl.DateTimeFormat('en-CA', {
        timeZone: SEOUL_TIMEZONE,
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
    });

    const monthLabel = document.getElementById('month-label');
    const rangeDisplay = document.getElementById('range-display');
    const rangeMeta = document.getElementById('range-meta');
    const resetTodayBtn = document.getElementById('reset-today');
    const calendarGrid = document.getElementById('calendar-grid');
    const prevBtn = document.getElementById('prev-month');
    const nextBtn = document.getElementById('next-month');
    const bookBtn = document.getElementById('book-btn');
    const modal = document.createElement('div');
    modal.className = 'modal';
    modal.innerHTML = `
        <div class="modal-card">
            <div class="modal-header">
                <span id="modal-title"></span>
                <button class="modal-close" id="modal-close" aria-label="닫기">✕</button>
            </div>
            <div id="modal-body"></div>
        </div>
    `;
    document.body.appendChild(modal);
    const modalTitle = modal.querySelector('#modal-title');
    const modalBody = modal.querySelector('#modal-body');
    const modalClose = modal.querySelector('#modal-close');

    const todayLabel = formatDate(new Date());
    const today = parseDate(todayLabel);
    const todayStart = today;
    let currentMonth = new Date(today.getFullYear(), today.getMonth(), 1);
    let selectedStart = null;
    let selectedEnd = null;
    let dragStartDate = null;
    let dragging = false;
    let longPressTimer = null;
    let longPressTriggered = false;
    let awaitingEnd = false;
    let lastHoverDate = null;
    let suppressClick = false;

    function formatDate(date) {
        return seoulDateFormatter.format(date);
    }

    function parseDate(value) {
        const [y, m, d] = value.split('-').map(Number);
        return new Date(y, m - 1, d);
    }

    function addDays(date, days) {
        const result = new Date(date);
        result.setDate(result.getDate() + days);
        return result;
    }

    function toEpochSecond(date) {
        const dateStr = formatDate(date);
        return Math.floor(Date.parse(`${dateStr}T00:00:00+09:00`) / 1000);
    }

    function extractBookingDateEpoch(payload) {
        const raw = payload.date ?? payload.from ?? payload.start;
        const epoch = typeof raw === 'string' ? Number(raw) : raw;
        return Number.isFinite(epoch) ? Number(epoch) : null;
    }

    function bookingKey(payload) {
        const id = payload.bookingId ?? payload.id;
        if (id != null) {
            return `id:${id}`;
        }
        const datePart = extractBookingDateEpoch(payload);
        const nickname = payload.nickname || '';
        return `${datePart ?? 'unknown'}-${nickname}`;
    }

    function storeBooking(payload) {
        const date = extractBookingDateEpoch(payload);
        if (!Number.isFinite(date)) return;
        const key = bookingKey(payload);
        if (bookingKeys.has(key)) return;
        bookingKeys.add(key);
        bookings.push({
            id: payload.bookingId ?? payload.id ?? null,
            nickname: payload.nickname ?? '알 수 없음',
            start: formatDate(new Date(date * 1000)),
            end: formatDate(new Date(date * 1000)),
        });
    }

    function isBeforeToday(dateStr) {
        return parseDate(dateStr) < todayStart;
    }

    function dayDiff(startStr, endStr) {
        return Math.round((parseDate(endStr) - parseDate(startStr)) / (1000 * 60 * 60 * 24));
    }

    function updateRangeDisplay() {
        if (!selectedStart && !selectedEnd) {
            rangeDisplay.textContent = '선택 없음';
            rangeMeta.textContent = '날짜를 탭하거나 드래그해 선택하세요';
            if (bookBtn) bookBtn.disabled = true;
            return;
        }

        if (selectedStart && !selectedEnd) {
            rangeDisplay.textContent = `${selectedStart} ~ ${selectedStart}`;
            rangeMeta.textContent = '종료일을 선택하세요';
            if (bookBtn) bookBtn.disabled = false;
            return;
        }

        rangeDisplay.textContent = `${selectedStart} ~ ${selectedEnd}`;

        const diff = dayDiff(selectedStart, selectedEnd);
        rangeMeta.textContent = `${diff + 1}일`;
        if (bookBtn) bookBtn.disabled = false;
    }

    function isSameDay(a, b) {
        return a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate();
    }

    function isWithin(date, start, end) {
        const target = typeof date === 'string' ? parseDate(date) : date;
        if (!start || !end) return false;
        return target >= parseDate(start) && target <= parseDate(end);
    }

    function buildDayCell(dayNum, monthDate) {
        const dayContainer = document.createElement('div');
        dayContainer.className = 'day';

        const date = new Date(monthDate.getFullYear(), monthDate.getMonth(), dayNum);
        const dateStr = formatDate(date);
        dayContainer.dataset.date = dateStr;
        const dayNumber = document.createElement('div');
        dayNumber.className = 'day-number';
        dayNumber.textContent = dayNum;
        const weekday = date.getDay();
        if (weekday === 0) {
            dayNumber.classList.add('sunday');
        } else if (weekday === 6) {
            dayNumber.classList.add('saturday');
        }

        if (isSameDay(date, today)) {
            dayNumber.classList.add('today');
        }

        if (selectedStart && selectedEnd && isWithin(date, selectedStart, selectedEnd)) {
            dayContainer.classList.add('in-range');
        } else if (selectedStart && !selectedEnd && isSameDay(date, parseDate(selectedStart))) {
            dayContainer.classList.add('start-only');
        }

        const todaysBookings = bookings.filter(b => isWithin(date, b.start, b.end));
        const capped = Math.min(todaysBookings.length, MAX_CAPACITY);
        if (capped > 0) {
            const ratio = capped / MAX_CAPACITY;
            let tone = 'mid';
            if (capped >= MAX_CAPACITY - 1) tone = 'full';
            else if (ratio < 0.4) tone = 'low';
            else if (ratio >= 0.4) tone = 'mid';
            dayContainer.classList.add(`count-${tone}`);
        }
        if (isBeforeToday(dateStr)) {
            dayContainer.classList.add('past');
        }

        dayContainer.appendChild(dayNumber);
        return dayContainer;
    }

    function renderCalendar() {
        const year = currentMonth.getFullYear();
        const month = currentMonth.getMonth();
        const firstDay = new Date(year, month, 1);
        const lastDay = new Date(year, month + 1, 0);
        const offset = firstDay.getDay();
        const totalCells = offset + lastDay.getDate();

        monthLabel.textContent = `${year}년 ${month + 1}월`;
        updateRangeDisplay();

        calendarGrid.innerHTML = '';

        for (let i = 0; i < offset; i++) {
            const empty = document.createElement('div');
            empty.className = 'day empty';
            calendarGrid.appendChild(empty);
        }

        for (let day = 1; day <= lastDay.getDate(); day++) {
            calendarGrid.appendChild(buildDayCell(day, currentMonth));
        }

        calendarGrid.querySelectorAll('.day').forEach(cell => {
            if (cell.classList.contains('empty')) return;
            cell.addEventListener('pointerdown', onDayPointerDown);
            cell.addEventListener('pointerenter', onDayPointerEnter);
            cell.addEventListener('pointermove', onDayPointerMove);
            cell.addEventListener('pointerup', onDayPointerUp);
            cell.addEventListener('click', onDayClick);
            cell.addEventListener('pointerleave', cancelLongPress);
            cell.addEventListener('pointercancel', cancelLongPress);
        });
    }

    async function loadBookingsForMonth(year, month) {
        const monthKey = `${year}-${month}`;
        if (loadedDates.has(monthKey)) return;

        const res = await fetch(`/api/v1/bookings?year=${year}&month=${month}`, { credentials: 'include' });
        if (!res.ok) {
            throw new Error(`예약을 불러오지 못했습니다. (${res.status})`);
        }
        const data = await res.json();
        const list = Array.isArray(data.bookings) ? data.bookings : [];
        list.forEach(storeBooking);
        loadedDates.add(monthKey);
    }

    async function loadBookingsForCurrentMonth() {
        const year = currentMonth.getFullYear();
        const month = currentMonth.getMonth() + 1; // Backend expects 1-indexed month

        try {
            await loadBookingsForMonth(year, month);
            renderCalendar();
        } catch (err) {
            console.error(err);
            rangeMeta.textContent = '예약 정보를 불러오는 데 실패했습니다.';
        }
    }

    async function changeMonth(offset) {
        currentMonth = new Date(currentMonth.getFullYear(), currentMonth.getMonth() + offset, 1);
        renderCalendar();
        await loadBookingsForCurrentMonth();
    }

    resetTodayBtn.addEventListener('click', () => {
        currentMonth = new Date(today.getFullYear(), today.getMonth(), 1);
        selectedStart = null;
        selectedEnd = null;
        awaitingEnd = false;
        renderCalendar();
        void loadBookingsForCurrentMonth();
    });

    function getBookingsByDate(dateStr) {
        return bookings.filter(b => isWithin(dateStr, b.start, b.end));
    }

    function clearSelecting() {
        calendarGrid.querySelectorAll('.selecting').forEach(el => el.classList.remove('selecting'));
    }

    function resetSelectionState() {
        selectedStart = null;
        selectedEnd = null;
        awaitingEnd = false;
        renderCalendar();
        if (bookBtn) bookBtn.disabled = true;
    }

    function startLongPress(dateStr) {
        cancelLongPress();
        longPressTimer = setTimeout(() => {
            longPressTriggered = true;
            dragStartDate = null;
            dragging = false;
            clearSelecting();
            showModal(dateStr);
        }, 650);
    }

    function cancelLongPress() {
        if (longPressTimer) {
            clearTimeout(longPressTimer);
            longPressTimer = null;
        }
    }

    function onDayPointerDown(e) {
        e.preventDefault();
        const dateStr = e.currentTarget.dataset.date;
        lastHoverDate = dateStr;
        dragging = false;
        longPressTriggered = false;
        clearSelecting();

        if (isBeforeToday(dateStr)) {
            dragStartDate = null;
            startLongPress(dateStr);
            return;
        }

        dragStartDate = dateStr;
        e.currentTarget.classList.add('selecting');
        startLongPress(dateStr);
    }

    function onDayPointerEnter(e) {
        if (!dragStartDate) return;
        const dateStr = e.currentTarget.dataset.date;
        if (isBeforeToday(dateStr)) return;
        lastHoverDate = dateStr;
        if (dateStr !== dragStartDate) {
            cancelLongPress();
            dragging = true;
            previewSelection(dragStartDate, dateStr);
        }
    }

    function onDayPointerMove(e) {
        if (!dragStartDate) return;
        const target = e.currentTarget;
        if (!target || !target.dataset) return;
        lastHoverDate = target.dataset.date;
        if (isBeforeToday(lastHoverDate)) return;
        if (lastHoverDate !== dragStartDate) {
            cancelLongPress();
            dragging = true;
            previewSelection(dragStartDate, target.dataset.date);
        }
    }

    function onDayPointerUp(e) {
        finalizePointer(e.currentTarget.dataset.date);
    }

    function onDayClick(e) {
        if (suppressClick) {
            suppressClick = false;
            return;
        }
        if (isBeforeToday(e.currentTarget.dataset.date)) return;
        if (dragging || longPressTriggered) return;
        cancelLongPress();
        handleSingleClick(e.currentTarget.dataset.date);
        dragStartDate = null;
        lastHoverDate = null;
    }

    function previewSelection(startStr, endStr) {
        if (isBeforeToday(startStr) || isBeforeToday(endStr)) return;
        clearSelecting();
        const start = parseDate(startStr);
        const end = parseDate(endStr);
        const [from, to] = start <= end ? [start, end] : [end, start];
        calendarGrid.querySelectorAll('.day:not(.empty)').forEach(cell => {
            const cellDate = parseDate(cell.dataset.date);
            if (cellDate >= from && cellDate <= to) cell.classList.add('selecting');
        });
    }

    function applyDraggedRange(startStr, endStr) {
        if (isBeforeToday(startStr) || isBeforeToday(endStr)) return;
        const start = parseDate(startStr);
        const end = parseDate(endStr);
        const [from, to] = start <= end ? [start, end] : [end, start];
        selectedStart = formatDate(from);
        selectedEnd = formatDate(to);
        awaitingEnd = false;
        renderCalendar();
    }

    function finalizePointer(dateStr) {
        cancelLongPress();
        const targetDate = lastHoverDate || dateStr || dragStartDate;
        if (targetDate && isBeforeToday(targetDate)) {
            clearSelecting();
            dragStartDate = null;
            dragging = false;
            lastHoverDate = null;
            return;
        }
        if (longPressTriggered) {
            dragStartDate = null;
            dragging = false;
            longPressTriggered = false;
            clearSelecting();
            lastHoverDate = null;
            return;
        }
        if (dragStartDate && dragging && targetDate) {
            applyDraggedRange(dragStartDate, targetDate);
        } else if (targetDate) {
            handleSingleClick(targetDate);
        }
        suppressClick = true;
        dragStartDate = null;
        dragging = false;
        clearSelecting();
        lastHoverDate = null;
    }

    function handleSingleClick(dateStr) {
        if (isBeforeToday(dateStr)) return;
        if (!awaitingEnd) {
            selectedStart = dateStr;
            selectedEnd = null;
            awaitingEnd = true;
            renderCalendar();
            return;
        }

        const startDate = parseDate(selectedStart);
        const endDate = parseDate(dateStr);
        const [from, to] = startDate <= endDate ? [startDate, endDate] : [endDate, startDate];
        selectedStart = formatDate(from);
        selectedEnd = formatDate(to);
        awaitingEnd = false;
        renderCalendar();
    }

    function showModal(dateStr) {
        const list = getBookingsByDate(dateStr);
        modalTitle.textContent = `${dateStr} 예약`;
        modalBody.innerHTML = '';
        if (!list.length) {
            const empty = document.createElement('div');
            empty.className = 'empty-state';
            empty.textContent = '예약이 없습니다.';
            modalBody.appendChild(empty);
        } else {
            list.forEach(item => {
                const row = document.createElement('div');
                row.className = 'booking';
                const guest = document.createElement('span');
                guest.textContent = item.nickname;
                const range = document.createElement('small');
                range.textContent = `${item.start} ~ ${item.end}`;
                row.appendChild(guest);
                row.appendChild(range);
                modalBody.appendChild(row);
            });
        }
        modal.classList.add('open');
    }

    modalClose.addEventListener('click', () => modal.classList.remove('open'));
    modal.addEventListener('click', (e) => {
        if (e.target === modal) modal.classList.remove('open');
    });

    window.addEventListener('pointermove', (e) => {
        if (!dragStartDate) return;
        const el = document.elementFromPoint(e.clientX, e.clientY);
        const cell = el ? el.closest('.day:not(.empty)') : null;
        if (cell && cell.dataset.date) {
            lastHoverDate = cell.dataset.date;
            if (lastHoverDate !== dragStartDate) {
                cancelLongPress();
                dragging = true;
                previewSelection(dragStartDate, cell.dataset.date);
            }
        }
    }, { passive: false });

    window.addEventListener('pointerup', (e) => {
        if (!dragStartDate) return;
        const el = document.elementFromPoint(e.clientX, e.clientY);
        const cell = el ? el.closest('.day:not(.empty)') : null;
        finalizePointer(cell?.dataset?.date);
    }, { passive: false });

    window.addEventListener('pointercancel', () => {
        if (dragStartDate) {
            finalizePointer(null);
        }
    });

    async function handleBookingSubmit() {
        if (selectedStart && !selectedEnd) {
            selectedEnd = selectedStart;
            awaitingEnd = false;
            renderCalendar();
        }

        if (!selectedStart || !selectedEnd) {
            alert('예약할 날짜를 먼저 선택해주세요.');
            resetSelectionState();
            return;
        }
        if (isBeforeToday(selectedStart) || isBeforeToday(selectedEnd)) {
            alert('지난 날짜는 예약할 수 없습니다.');
            resetSelectionState();
            return;
        }

        const startDate = parseDate(selectedStart);
        const endDate = parseDate(selectedEnd);
        const [fromDate, toDate] = startDate <= endDate
            ? [startDate, endDate]
            : [endDate, startDate];

        // Backend expects checkout day exclusive, so send the next day for "to"
        const checkoutDate = addDays(toDate, 1);

        if (bookBtn) bookBtn.disabled = true;
        try {
            const res = await fetch('/api/v1/booking', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'include',
                body: JSON.stringify({
                    from: toEpochSecond(fromDate),
                    to: toEpochSecond(checkoutDate),
                }),
            });

            if (!res.ok) {
                let message = '예약에 실패했습니다. 잠시 후 다시 시도해주세요.';
                try {
                    const errorData = await res.json();
                    if (errorData.message) {
                        message = errorData.message;
                    }
                } catch (err) {
                    console.warn('예약 실패 응답 파싱 실패', err);
                }
                throw new Error(message);
            }

            const created = await res.json();
            // Response now contains array of bookings
            const list = Array.isArray(created.bookings) ? created.bookings : [];
            list.forEach(storeBooking);
            alert('예약이 완료되었습니다.');
        } catch (err) {
            console.error('예약 생성 실패', err);
            alert(err.message || '예약 처리 중 문제가 발생했습니다.');
        } finally {
            resetSelectionState();
        }
    }

    prevBtn.addEventListener('click', () => { void changeMonth(-1); });
    nextBtn.addEventListener('click', () => { void changeMonth(1); });
    if (bookBtn) {
        bookBtn.addEventListener('click', () => { void handleBookingSubmit(); });
    }

    renderCalendar();
    void loadBookingsForCurrentMonth();
})();
