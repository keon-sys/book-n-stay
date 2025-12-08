(() => {
    const MAX_CAPACITY = 8;
    const bookings = [
        { guest: '김민수', start: '2025-12-02', end: '2025-12-04' },
        { guest: '박지현', start: '2025-12-07', end: '2025-12-09' },
        { guest: '홍은지', start: '2025-12-07', end: '2025-12-07' },
        { guest: '이재훈', start: '2025-12-11', end: '2025-12-14' },
        { guest: '최서윤', start: '2025-12-15', end: '2025-12-18' },
        { guest: '이가영', start: '2025-12-20', end: '2025-12-22' },
        { guest: '조수영', start: '2025-12-20', end: '2025-12-22' },
        { guest: '강태오', start: '2025-12-22', end: '2025-12-24' },
        { guest: '오하늘', start: '2025-12-27', end: '2025-12-30' },
        { guest: '정윤아', start: '2025-12-30', end: '2025-12-30' }
    ];

    const monthLabel = document.getElementById('month-label');
    const rangeDisplay = document.getElementById('range-display');
    const rangeMeta = document.getElementById('range-meta');
    const resetTodayBtn = document.getElementById('reset-today');
    const calendarGrid = document.getElementById('calendar-grid');
    const prevBtn = document.getElementById('prev-month');
    const nextBtn = document.getElementById('next-month');
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

    const today = new Date();
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
        const y = date.getFullYear();
        const m = String(date.getMonth() + 1).padStart(2, '0');
        const d = String(date.getDate()).padStart(2, '0');
        return `${y}-${m}-${d}`;
    }

    function parseDate(value) {
        const [y, m, d] = value.split('-').map(Number);
        return new Date(y, m - 1, d);
    }

    function dayDiff(startStr, endStr) {
        return Math.round((parseDate(endStr) - parseDate(startStr)) / (1000 * 60 * 60 * 24));
    }

    function updateRangeDisplay() {
        if (!selectedStart && !selectedEnd) {
            rangeDisplay.textContent = '선택 없음';
            rangeMeta.textContent = '날짜를 탭하거나 드래그해 선택하세요';
            return;
        }

        if (selectedStart && !selectedEnd) {
            rangeDisplay.textContent = `${selectedStart} ~ ?`;
            rangeMeta.textContent = '종료일을 선택하세요';
            return;
        }

        rangeDisplay.textContent = `${selectedStart} ~ ${selectedEnd}`;

        const diff = dayDiff(selectedStart, selectedEnd);
        if (diff === 0) {
            rangeMeta.textContent = '당일 예약';
        } else {
            rangeMeta.textContent = `${diff}박 · ${diff + 1}일`;
        }
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

        if (isSameDay(date, today)) {
            const dot = document.createElement('span');
            dot.className = 'today-dot';
            dayNumber.appendChild(dot);
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

    prevBtn.addEventListener('click', () => {
        currentMonth = new Date(currentMonth.getFullYear(), currentMonth.getMonth() - 1, 1);
        renderCalendar();
    });

    nextBtn.addEventListener('click', () => {
        currentMonth = new Date(currentMonth.getFullYear(), currentMonth.getMonth() + 1, 1);
        renderCalendar();
    });

    resetTodayBtn.addEventListener('click', () => {
        currentMonth = new Date(today.getFullYear(), today.getMonth(), 1);
        selectedStart = null;
        selectedEnd = null;
        awaitingEnd = false;
        renderCalendar();
    });

    function getBookingsByDate(dateStr) {
        return bookings.filter(b => isWithin(dateStr, b.start, b.end));
    }

    function clearSelecting() {
        calendarGrid.querySelectorAll('.selecting').forEach(el => el.classList.remove('selecting'));
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
        dragStartDate = dateStr;
        lastHoverDate = dateStr;
        dragging = false;
        longPressTriggered = false;
        clearSelecting();
        e.currentTarget.classList.add('selecting');
        startLongPress(dateStr);
    }

    function onDayPointerEnter(e) {
        if (!dragStartDate) return;
        const dateStr = e.currentTarget.dataset.date;
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
        if (dragging || longPressTriggered) return;
        cancelLongPress();
        handleSingleClick(e.currentTarget.dataset.date);
        dragStartDate = null;
        lastHoverDate = null;
    }

    function previewSelection(startStr, endStr) {
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
                guest.textContent = item.guest;
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

    renderCalendar();
})();
