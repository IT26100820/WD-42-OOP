/**
 * Dedicated Backend Testing Dashboard Logic
 * Executes real HTTP requests against the Spring Boot REST API
 * and displays genuine HTTP status codes, latency, and JSON payloads.
 */

document.addEventListener('DOMContentLoaded', () => {
    // DOM Elements
    const elements = {
        connectionStatusBadge: document.getElementById('connectionStatusBadge'),
        connectionStatusText: document.getElementById('connectionStatusText'),
        pingBackendBtn: document.getElementById('pingBackendBtn'),
        httpStatusBadge: document.getElementById('httpStatusBadge'),
        latencyBadge: document.getElementById('latencyBadge'),
        lastReqMethod: document.getElementById('lastReqMethod'),
        lastReqUrl: document.getElementById('lastReqUrl'),
        rawResponseCode: document.getElementById('rawResponseCode'),
        copyJsonBtn: document.getElementById('copyJsonBtn'),
        recordCounter: document.getElementById('recordCounter'),
        recordsTableBody: document.getElementById('recordsTableBody'),
        reloadStorageBtn: document.getElementById('reloadStorageBtn'),

        // Tab buttons & contents
        tabButtons: document.querySelectorAll('.tab-btn'),
        tabContents: document.querySelectorAll('.tab-content'),

        // Tab 1: Create
        testCreateForm: document.getElementById('testCreateForm'),
        createBookId: document.getElementById('createBookId'),
        createCustomerId: document.getElementById('createCustomerId'),
        createRating: document.getElementById('createRating'),
        createComment: document.getElementById('createComment'),

        // Tab 2: Read
        btnFetchAll: document.getElementById('btnFetchAll'),
        filterBookId: document.getElementById('filterBookId'),
        filterRating: document.getElementById('filterRating'),
        filterSort: document.getElementById('filterSort'),
        btnFetchSingle: document.getElementById('btnFetchSingle'),
        fetchSingleId: document.getElementById('fetchSingleId'),
        btnFetchSummary: document.getElementById('btnFetchSummary'),
        fetchSummaryBookId: document.getElementById('fetchSummaryBookId'),

        // Tab 3: Update
        testUpdateForm: document.getElementById('testUpdateForm'),
        updateReviewId: document.getElementById('updateReviewId'),
        updateCustomerId: document.getElementById('updateCustomerId'),
        updateBookId: document.getElementById('updateBookId'),
        updateRating: document.getElementById('updateRating'),
        updateComment: document.getElementById('updateComment'),

        // Tab 4: Delete
        testDeleteForm: document.getElementById('testDeleteForm'),
        deleteReviewId: document.getElementById('deleteReviewId'),
        deleteCustomerId: document.getElementById('deleteCustomerId')
    };

    let latestJsonResponse = '';

    // Initialize Dashboard
    function init() {
        setupTabs();
        setupQuickChips();
        setupEventHandlers();
        checkBackendHealth();
        loadAllStoredRecords();
    }

    // Tab switching logic
    function setupTabs() {
        elements.tabButtons.forEach(btn => {
            btn.addEventListener('click', () => {
                const targetTab = btn.dataset.tab;
                elements.tabButtons.forEach(b => b.classList.remove('active'));
                elements.tabContents.forEach(c => c.classList.remove('active'));

                btn.classList.add('active');
                const content = document.getElementById(targetTab);
                if (content) content.classList.add('active');
            });
        });
    }

    // Quick chips autofill
    function setupQuickChips() {
        document.querySelectorAll('.chip[data-book]').forEach(chip => {
            chip.addEventListener('click', () => {
                const bookId = chip.dataset.book;
                elements.createBookId.value = bookId;
                elements.filterBookId.value = bookId;
                elements.fetchSummaryBookId.value = bookId;
                elements.updateBookId.value = bookId;
            });
        });

        document.querySelectorAll('.chip[data-customer]').forEach(chip => {
            chip.addEventListener('click', () => {
                const custId = chip.dataset.customer;
                elements.createCustomerId.value = custId;
                elements.updateCustomerId.value = custId;
                elements.deleteCustomerId.value = custId;
            });
        });
    }

    // Setup all action listeners
    function setupEventHandlers() {
        // Ping button
        elements.pingBackendBtn.addEventListener('click', checkBackendHealth);

        // Copy JSON button
        elements.copyJsonBtn.addEventListener('click', () => {
            if (latestJsonResponse) {
                navigator.clipboard.writeText(latestJsonResponse);
                elements.copyJsonBtn.textContent = '✅ Copied!';
                setTimeout(() => elements.copyJsonBtn.textContent = '📋 Copy JSON', 1500);
            }
        });

        // Reload storage records
        elements.reloadStorageBtn.addEventListener('click', loadAllStoredRecords);

        // Tab 1: Create
        elements.testCreateForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const payload = {
                bookId: elements.createBookId.value.trim(),
                customerId: elements.createCustomerId.value.trim(),
                rating: parseInt(elements.createRating.value),
                comment: elements.createComment.value.trim()
            };
            await executeApiCall('POST', '/api/reviews', payload);
            loadAllStoredRecords();
        });

        // Tab 2: Read All
        elements.btnFetchAll.addEventListener('click', async () => {
            const params = new URLSearchParams();
            if (elements.filterBookId.value.trim()) params.append('bookId', elements.filterBookId.value.trim());
            if (elements.filterRating.value) params.append('rating', elements.filterRating.value);
            if (elements.filterSort.value) params.append('sortBy', elements.filterSort.value);

            const url = '/api/reviews' + (params.toString() ? '?' + params.toString() : '');
            await executeApiCall('GET', url);
        });

        // Tab 2: Read Single by ID
        elements.btnFetchSingle.addEventListener('click', async () => {
            const id = elements.fetchSingleId.value.trim();
            if (!id) {
                alert('Please enter a review ID.');
                return;
            }
            await executeApiCall('GET', `/api/reviews/${encodeURIComponent(id)}`);
        });

        // Tab 2: Read Summary
        elements.btnFetchSummary.addEventListener('click', async () => {
            const bookId = elements.fetchSummaryBookId.value.trim();
            if (!bookId) {
                alert('Please enter a book ID.');
                return;
            }
            await executeApiCall('GET', `/api/reviews/book/${encodeURIComponent(bookId)}/summary`);
        });

        // Tab 3: Update
        elements.testUpdateForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const id = elements.updateReviewId.value.trim();
            const custId = elements.updateCustomerId.value.trim();
            const payload = {
                bookId: elements.updateBookId.value.trim(),
                customerId: custId,
                rating: parseInt(elements.updateRating.value),
                comment: elements.updateComment.value.trim()
            };
            await executeApiCall('PUT', `/api/reviews/${encodeURIComponent(id)}`, payload, {
                'X-Customer-Id': custId
            });
            loadAllStoredRecords();
        });

        // Tab 4: Delete
        elements.testDeleteForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const id = elements.deleteReviewId.value.trim();
            const custId = elements.deleteCustomerId.value.trim();
            await executeApiCall('DELETE', `/api/reviews/${encodeURIComponent(id)}`, null, {
                'X-Customer-Id': custId
            });
            loadAllStoredRecords();
        });

    }

    function switchTab(tabId) {
        elements.tabButtons.forEach(b => {
            if (b.dataset.tab === tabId) b.click();
        });
    }

    // Health check ping
    async function checkBackendHealth() {
        elements.connectionStatusBadge.className = 'status-badge status-checking';
        elements.connectionStatusText.textContent = 'Connecting...';

        try {
            const startTime = performance.now();
            const res = await fetch('/api/reviews/health');
            const latency = Math.round(performance.now() - startTime);

            if (res.ok) {
                elements.connectionStatusBadge.className = 'status-badge status-online';
                elements.connectionStatusText.textContent = `Backend Live (HTTP 200 • ${latency}ms)`;
            } else {
                elements.connectionStatusBadge.className = 'status-badge status-offline';
                elements.connectionStatusText.textContent = `HTTP ${res.status} Error`;
            }
        } catch (err) {
            elements.connectionStatusBadge.className = 'status-badge status-offline';
            elements.connectionStatusText.textContent = 'Cannot connect to review service. Check backend.';
        }
    }

    // Core function to execute an API call and display full inspector telemetry
    async function executeApiCall(method, url, body = null, headers = {}, isRawString = false) {
        elements.lastReqMethod.textContent = method;
        elements.lastReqUrl.textContent = url;
        elements.rawResponseCode.textContent = 'Executing request...';
        elements.httpStatusBadge.className = 'http-status-badge status-init';
        elements.httpStatusBadge.textContent = 'WAITING...';

        const startTime = performance.now();

        try {
            const reqOptions = {
                method: method,
                headers: {
                    'Content-Type': 'application/json',
                    ...headers
                }
            };

            if (body) {
                reqOptions.body = isRawString ? body : JSON.stringify(body);
            }

            const response = await fetch(url, reqOptions);
            const latency = Math.round(performance.now() - startTime);

            // Display latency
            elements.latencyBadge.textContent = `Latency: ${latency} ms`;

            // Display HTTP Status Code
            const statusCode = response.status;
            const statusText = response.statusText || getStatusTextFallback(statusCode);
            elements.httpStatusBadge.textContent = `HTTP ${statusCode} ${statusText}`;

            if (statusCode >= 200 && statusCode < 300) {
                elements.httpStatusBadge.className = 'http-status-badge status-2xx';
            } else if (statusCode >= 400 && statusCode < 500) {
                elements.httpStatusBadge.className = 'http-status-badge status-4xx';
            } else {
                elements.httpStatusBadge.className = 'http-status-badge status-5xx';
            }

            // Parse response body
            let responseData;
            const contentType = response.headers.get('content-type');
            if (contentType && contentType.includes('application/json')) {
                responseData = await response.json();
                latestJsonResponse = JSON.stringify(responseData, null, 2);
            } else {
                latestJsonResponse = await response.text();
            }

            elements.rawResponseCode.textContent = latestJsonResponse;
            return { ok: response.ok, status: statusCode, data: responseData };
        } catch (error) {
            const latency = Math.round(performance.now() - startTime);
            elements.latencyBadge.textContent = `Latency: ${latency} ms`;
            elements.httpStatusBadge.className = 'http-status-badge status-5xx';
            elements.httpStatusBadge.textContent = 'CONNECTION FAILED';

            latestJsonResponse = JSON.stringify({
                success: false,
                message: "Cannot connect to the review service. Check that the backend is running.",
                error: error.message
            }, null, 2);

            elements.rawResponseCode.textContent = latestJsonResponse;
            return { ok: false, status: 0, error: error };
        }
    }

    // Load all stored records from file storage
    async function loadAllStoredRecords() {
        try {
            const res = await fetch('/api/reviews?sortBy=newest');
            const json = await res.json();

            if (json.success && Array.isArray(json.data)) {
                renderStorageTable(json.data);
            }
        } catch (err) {
            elements.recordsTableBody.innerHTML = `
                <tr>
                    <td colspan="7" class="text-center text-muted" style="color: #ef4444;">
                        Failed to fetch stored reviews from backend.
                    </td>
                </tr>
            `;
        }
    }

    // Render persistent storage table
    function renderStorageTable(reviews) {
        elements.recordCounter.textContent = `${reviews.length} ${reviews.length === 1 ? 'record' : 'records'} in storage`;

        if (reviews.length === 0) {
            elements.recordsTableBody.innerHTML = `
                <tr>
                    <td colspan="7" class="text-center text-muted">No records in storage file.</td>
                </tr>
            `;
            return;
        }

        elements.recordsTableBody.innerHTML = '';
        reviews.forEach(r => {
            const tr = document.createElement('tr');
            const created = r.createdAt ? r.createdAt.substring(0, 16).replace('T', ' ') : '-';
            const shortComment = r.comment && r.comment.length > 35 ? r.comment.substring(0, 35) + '...' : r.comment;

            tr.innerHTML = `
                <td><strong>${escapeHtml(r.reviewId)}</strong></td>
                <td>${escapeHtml(r.bookId)}</td>
                <td>${escapeHtml(r.customerId)}</td>
                <td><strong>${r.rating} ★</strong></td>
                <td title="${escapeHtml(r.comment)}">${escapeHtml(shortComment)}</td>
                <td style="font-size:0.75rem;">${created}</td>
                <td>
                    <button class="table-btn btn-inspect" data-id="${escapeHtml(r.reviewId)}">Inspect</button>
                    <button class="table-btn btn-fill-edit" data-id="${escapeHtml(r.reviewId)}">Edit</button>
                    <button class="table-btn btn-delete-row" data-id="${escapeHtml(r.reviewId)}" data-cust="${escapeHtml(r.customerId)}" style="color:#dc2626;">Delete</button>
                </td>
            `;

            // Inspect
            tr.querySelector('.btn-inspect').addEventListener('click', () => {
                switchTab('tab-read');
                elements.fetchSingleId.value = r.reviewId;
                executeApiCall('GET', `/api/reviews/${encodeURIComponent(r.reviewId)}`);
            });

            // Fill Edit
            tr.querySelector('.btn-fill-edit').addEventListener('click', () => {
                switchTab('tab-update');
                elements.updateReviewId.value = r.reviewId;
                elements.updateCustomerId.value = r.customerId;
                elements.updateBookId.value = r.bookId;
                elements.updateRating.value = r.rating;
                elements.updateComment.value = r.comment;
            });

            // Delete
            tr.querySelector('.btn-delete-row').addEventListener('click', () => {
                switchTab('tab-delete');
                elements.deleteReviewId.value = r.reviewId;
                elements.deleteCustomerId.value = r.customerId;
            });

            elements.recordsTableBody.appendChild(tr);
        });
    }

    function getStatusTextFallback(code) {
        const map = {
            200: 'OK',
            201: 'CREATED',
            204: 'NO CONTENT',
            400: 'BAD REQUEST',
            403: 'FORBIDDEN',
            404: 'NOT FOUND',
            409: 'CONFLICT',
            500: 'INTERNAL SERVER ERROR'
        };
        return map[code] || '';
    }

    function escapeHtml(str) {
        if (!str) return '';
        const div = document.createElement('div');
        div.textContent = str;
        return div.innerHTML;
    }

    // Start
    init();
});
