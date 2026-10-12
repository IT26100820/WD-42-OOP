/**
 * PageTurner Bookstore - Customer Review & Feedback Module
 * Connects frontend UI to real Spring Boot REST API and persistent storage.
 */

document.addEventListener('DOMContentLoaded', () => {
    // State management
    const state = {
        sampleBooks: [],
        sampleCustomers: [],
        currentBookId: 'BK-101',
        currentCustomerId: 'CUST-001',
        selectedRating: 0,
        isEditing: false,
        editingReviewId: null,
        deletingReviewId: null,
        searchDebounceTimer: null
    };

    // DOM Elements
    const elements = {
        bookSelect: document.getElementById('bookSelect'),
        currentCustomerSelect: document.getElementById('currentCustomerSelect'),
        refreshBtn: document.getElementById('refreshBtn'),
        displayBookTitle: document.getElementById('displayBookTitle'),
        displayBookAuthor: document.getElementById('displayBookAuthor'),
        displayBookIdTag: document.getElementById('displayBookIdTag'),
        displayBookPriceTag: document.getElementById('displayBookPriceTag'),
        avgRatingNumber: document.getElementById('avgRatingNumber'),
        avgStarsDisplay: document.getElementById('avgStarsDisplay'),
        totalReviewsCount: document.getElementById('totalReviewsCount'),
        ratingBars: document.getElementById('ratingBars'),
        reviewForm: document.getElementById('reviewForm'),
        formHeaderTitle: document.getElementById('formHeaderTitle'),
        editModeBadge: document.getElementById('editModeBadge'),
        editReviewId: document.getElementById('editReviewId'),
        formBookId: document.getElementById('formBookId'),
        formCustomerId: document.getElementById('formCustomerId'),
        starRatingGroup: document.getElementById('starRatingGroup'),
        formRating: document.getElementById('formRating'),
        ratingLabelHint: document.getElementById('ratingLabelHint'),
        ratingError: document.getElementById('ratingError'),
        formComment: document.getElementById('formComment'),
        commentCharCounter: document.getElementById('commentCharCounter'),
        commentError: document.getElementById('commentError'),
        submitReviewBtn: document.getElementById('submitReviewBtn'),
        btnSpinner: document.getElementById('btnSpinner'),
        submitBtnText: document.getElementById('submitBtnText'),
        cancelEditBtn: document.getElementById('cancelEditBtn'),
        commentSearchInput: document.getElementById('commentSearchInput'),
        clearSearchBtn: document.getElementById('clearSearchBtn'),
        ratingFilterSelect: document.getElementById('ratingFilterSelect'),
        sortSelect: document.getElementById('sortSelect'),
        reviewsListContainer: document.getElementById('reviewsListContainer'),
        toastContainer: document.getElementById('toastContainer'),
        deleteModal: document.getElementById('deleteModal'),
        deleteTargetId: document.getElementById('deleteTargetId'),
        closeDeleteModalBtn: document.getElementById('closeDeleteModalBtn'),
        cancelDeleteBtn: document.getElementById('cancelDeleteBtn'),
        confirmDeleteBtn: document.getElementById('confirmDeleteBtn')
    };

    // Rating text mapping
    const ratingLabels = {
        1: '1 Star - Poor',
        2: '2 Stars - Fair',
        3: '3 Stars - Good',
        4: '4 Stars - Very Good',
        5: '5 Stars - Excellent'
    };

    // Initialize application
    async function init() {
        setupEventListeners();
        await loadSampleData();
        updateBookDisplay();
        loadBookSummary();
        loadReviews();
    }

    // Set up all event handlers
    function setupEventListeners() {
        // Book selector changed
        elements.bookSelect.addEventListener('change', (e) => {
            state.currentBookId = e.target.value;
            elements.formBookId.value = state.currentBookId;
            updateBookDisplay();
            loadBookSummary();
            loadReviews();
            if (state.isEditing) {
                cancelEdit();
            }
        });

        // User switcher changed
        elements.currentCustomerSelect.addEventListener('change', (e) => {
            state.currentCustomerId = e.target.value;
            elements.formCustomerId.value = state.currentCustomerId;
            loadReviews(); // Refresh review buttons based on permissions
        });

        // Refresh button
        elements.refreshBtn.addEventListener('click', () => {
            loadBookSummary();
            loadReviews();
            showToast('Refreshed data from backend storage.', 'success');
        });

        // Interactive Star Rating
        const starButtons = elements.starRatingGroup.querySelectorAll('.star-btn');
        starButtons.forEach(btn => {
            btn.addEventListener('click', () => {
                const val = parseInt(btn.dataset.value);
                setRating(val);
                clearFieldError('rating');
            });

            btn.addEventListener('mouseenter', () => {
                const hoverVal = parseInt(btn.dataset.value);
                highlightStars(hoverVal);
            });
        });

        elements.starRatingGroup.addEventListener('mouseleave', () => {
            highlightStars(state.selectedRating);
        });

        // Comment textarea input (live character counter and clear error)
        elements.formComment.addEventListener('input', () => {
            const len = elements.formComment.value.length;
            elements.commentCharCounter.textContent = `${len} / 1000`;
            if (len >= 1000) {
                elements.commentCharCounter.className = 'char-counter at-limit';
            } else if (len > 900) {
                elements.commentCharCounter.className = 'char-counter near-limit';
            } else {
                elements.commentCharCounter.className = 'char-counter';
            }
            clearFieldError('comment');
        });

        // Form Submission (Create or Update)
        elements.reviewForm.addEventListener('submit', handleFormSubmit);

        // Cancel Edit Button
        elements.cancelEditBtn.addEventListener('click', cancelEdit);

        // Search Input (debounced)
        elements.commentSearchInput.addEventListener('input', (e) => {
            const query = e.target.value;
            elements.clearSearchBtn.style.display = query ? 'inline-block' : 'none';
            clearTimeout(state.searchDebounceTimer);
            state.searchDebounceTimer = setTimeout(() => {
                loadReviews();
            }, 300);
        });

        // Clear Search Button
        elements.clearSearchBtn.addEventListener('click', () => {
            elements.commentSearchInput.value = '';
            elements.clearSearchBtn.style.display = 'none';
            loadReviews();
        });

        // Filters and Sorting
        elements.ratingFilterSelect.addEventListener('change', loadReviews);
        elements.sortSelect.addEventListener('change', loadReviews);

        // Delete Modal actions
        elements.closeDeleteModalBtn.addEventListener('click', closeDeleteModal);
        elements.cancelDeleteBtn.addEventListener('click', closeDeleteModal);
        elements.confirmDeleteBtn.addEventListener('click', executeDelete);
    }

    // Set star rating
    function setRating(val) {
        state.selectedRating = val;
        elements.formRating.value = val;
        elements.ratingLabelHint.textContent = ratingLabels[val] || 'Select rating';
        highlightStars(val);
    }

    // Highlight star buttons up to given value
    function highlightStars(count) {
        const starButtons = elements.starRatingGroup.querySelectorAll('.star-btn');
        starButtons.forEach(btn => {
            const btnVal = parseInt(btn.dataset.value);
            if (btnVal <= count) {
                btn.classList.add('active');
            } else {
                btn.classList.remove('active');
            }
        });
    }

    // Load sample books and customers from backend
    async function loadSampleData() {
        try {
            const res = await fetch('/api/reviews/sample-data');
            const data = await res.json();
            if (data.success && data.data) {
                state.sampleBooks = data.data.books || [];
                state.sampleCustomers = data.data.customers || [];

                // Populate book dropdown
                elements.bookSelect.innerHTML = '';
                state.sampleBooks.forEach(b => {
                    const opt = document.createElement('option');
                    opt.value = b.id;
                    opt.textContent = `${b.title} (${b.id})`;
                    elements.bookSelect.appendChild(opt);
                });

                if (state.sampleBooks.length > 0) {
                    state.currentBookId = state.sampleBooks[0].id;
                    elements.bookSelect.value = state.currentBookId;
                    elements.formBookId.value = state.currentBookId;
                }
                elements.formCustomerId.value = state.currentCustomerId;
            }
        } catch (err) {
            console.error('Failed to load sample data:', err);
            showToast('Unable to connect to backend service. Check if server is running.', 'error');
        }
    }

    // Update book showcase card
    function updateBookDisplay() {
        const book = state.sampleBooks.find(b => b.id === state.currentBookId) || {
            id: state.currentBookId,
            title: 'Book: ' + state.currentBookId,
            author: 'Unknown Author',
            price: 0.00
        };

        elements.displayBookTitle.textContent = book.title;
        elements.displayBookAuthor.textContent = 'by ' + book.author;
        elements.displayBookIdTag.textContent = 'ID: ' + book.id;
        elements.displayBookPriceTag.textContent = '$' + (book.price ? book.price.toFixed(2) : '0.00');
        elements.formBookId.value = book.id;
    }

    // Fetch and display rating summary for the current book
    async function loadBookSummary() {
        try {
            const res = await fetch(`/api/reviews/book/${encodeURIComponent(state.currentBookId)}/summary`);
            const json = await res.json();
            if (json.success && json.data) {
                const s = json.data;
                elements.avgRatingNumber.textContent = s.averageRating.toFixed(1);
                elements.totalReviewsCount.textContent = `${s.totalReviews} ${s.totalReviews === 1 ? 'review' : 'reviews'}`;
                elements.avgStarsDisplay.textContent = renderStarGlyphs(Math.round(s.averageRating));

                // Render breakdown bars
                elements.ratingBars.innerHTML = '';
                for (let star = 5; star >= 1; star--) {
                    const count = (s.ratingDistribution && s.ratingDistribution[star]) || 0;
                    const pct = s.totalReviews > 0 ? (count / s.totalReviews) * 100 : 0;

                    const row = document.createElement('div');
                    row.className = 'bar-row';
                    row.innerHTML = `
                        <span class="bar-label">${star} ★</span>
                        <div class="bar-track">
                            <div class="bar-fill" style="width: ${pct}%"></div>
                        </div>
                        <span class="bar-count">${count}</span>
                    `;
                    elements.ratingBars.appendChild(row);
                }
            }
        } catch (err) {
            console.error('Failed to load book summary:', err);
        }
    }

    // Load reviews with current filters & search
    async function loadReviews() {
        elements.reviewsListContainer.innerHTML = `
            <div class="loading-state">
                <div class="spinner"></div>
                <p>Loading reviews from backend...</p>
            </div>
        `;

        try {
            const params = new URLSearchParams();
            params.append('bookId', state.currentBookId);
            if (elements.ratingFilterSelect.value) {
                params.append('rating', elements.ratingFilterSelect.value);
            }
            if (elements.commentSearchInput.value.trim()) {
                params.append('search', elements.commentSearchInput.value.trim());
            }
            if (elements.sortSelect.value) {
                params.append('sortBy', elements.sortSelect.value);
            }

            const res = await fetch(`/api/reviews?${params.toString()}`);
            const json = await res.json();

            if (json.success && Array.isArray(json.data)) {
                renderReviewsList(json.data);
            } else {
                renderEmptyState('Failed to load reviews.');
            }
        } catch (err) {
            console.error('Error fetching reviews:', err);
            renderEmptyState('Cannot connect to review service. Please verify backend is running on port 8080.');
        }
    }

    // Render list of reviews
    function renderReviewsList(reviews) {
        if (!reviews || reviews.length === 0) {
            renderEmptyState('No reviews found for this book yet. Be the first to share your feedback!');
            return;
        }

        elements.reviewsListContainer.innerHTML = '';
        reviews.forEach(r => {
            const card = document.createElement('article');
            card.className = 'review-item';
            card.dataset.reviewId = r.reviewId;

            // Find reviewer name
            const cust = state.sampleCustomers.find(c => c.id === r.customerId);
            const reviewerName = cust ? cust.name : r.customerId;
            const initials = reviewerName.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase();

            // Format date
            const createdDate = formatDate(r.createdAt);
            const isUpdated = r.updatedAt && r.updatedAt !== r.createdAt;
            const updatedDate = isUpdated ? formatDate(r.updatedAt) : '';

            // Authorization: logged in customer or admin
            const canModify = (state.currentCustomerId === 'admin' || state.currentCustomerId === r.customerId);

            card.innerHTML = `
                <div class="review-item-header">
                    <div class="reviewer-profile">
                        <div class="avatar-circle">${escapeHtml(initials)}</div>
                        <div class="reviewer-meta">
                            <span class="reviewer-name">${escapeHtml(reviewerName)}</span>
                            <span class="review-date">Submitted on ${createdDate}</span>
                        </div>
                    </div>
                    <div class="review-badge-row">
                        <span class="review-id-tag">${escapeHtml(r.reviewId)}</span>
                    </div>
                </div>

                <div class="review-stars">${renderStarGlyphs(r.rating)}</div>
                <div class="review-body">${escapeHtml(r.comment)}</div>

                <div class="review-footer">
                    <span class="updated-indicator">${isUpdated ? `Edited on ${updatedDate}` : ''}</span>
                    <div class="review-item-actions">
                        ${canModify ? `
                            <button type="button" class="btn btn-secondary btn-sm edit-review-btn" data-id="${escapeHtml(r.reviewId)}">✏️ Edit</button>
                            <button type="button" class="btn btn-outline btn-sm delete-review-btn" data-id="${escapeHtml(r.reviewId)}">🗑️ Delete</button>
                        ` : `
                            <span class="text-muted" style="font-size:0.75rem;">Verified Reader</span>
                        `}
                    </div>
                </div>
            `;

            // Attach action listeners
            if (canModify) {
                const editBtn = card.querySelector('.edit-review-btn');
                const deleteBtn = card.querySelector('.delete-review-btn');

                editBtn.addEventListener('click', () => startEditReview(r));
                deleteBtn.addEventListener('click', () => promptDelete(r.reviewId));
            }

            elements.reviewsListContainer.appendChild(card);
        });
    }

    // Render empty state
    function renderEmptyState(message) {
        elements.reviewsListContainer.innerHTML = `
            <div class="empty-state">
                <div class="empty-icon">💬</div>
                <h3 class="empty-title">No Reviews Found</h3>
                <p>${escapeHtml(message)}</p>
            </div>
        `;
    }

    // Handle Form Submit (Create or Update)
    async function handleFormSubmit(e) {
        e.preventDefault();
        clearAllErrors();

        const rating = state.selectedRating;
        const comment = elements.formComment.value.trim();

        // Frontend validation
        let hasError = false;
        if (!rating || rating < 1 || rating > 5) {
            showFieldError('rating', 'Please select a rating between 1 and 5 stars.');
            hasError = true;
        }

        if (!comment) {
            showFieldError('comment', 'Comment is required and cannot be empty.');
            hasError = true;
        } else if (comment.length > 1000) {
            showFieldError('comment', 'Comment cannot exceed 1000 characters.');
            hasError = true;
        }

        if (hasError) return;

        // Prepare payload
        const payload = {
            bookId: state.currentBookId,
            customerId: state.currentCustomerId,
            rating: rating,
            comment: comment
        };

        // Disable submit button during request
        setButtonLoading(true);

        try {
            let res;
            if (state.isEditing) {
                // PUT /api/reviews/{id}
                res = await fetch(`/api/reviews/${encodeURIComponent(state.editingReviewId)}`, {
                    method: 'PUT',
                    headers: {
                        'Content-Type': 'application/json',
                        'X-Customer-Id': state.currentCustomerId
                    },
                    body: JSON.stringify(payload)
                });
            } else {
                // POST /api/reviews
                res = await fetch('/api/reviews', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'X-Customer-Id': state.currentCustomerId
                    },
                    body: JSON.stringify(payload)
                });
            }

            const json = await res.json();

            if (res.ok && json.success) {
                showToast(json.message || (state.isEditing ? 'Review updated!' : 'Review submitted!'), 'success');
                resetForm();
                loadBookSummary();
                loadReviews();
            } else {
                // Handle backend validation or business error
                if (json.fieldErrors) {
                    for (const [field, msg] of Object.entries(json.fieldErrors)) {
                        showFieldError(field, msg);
                    }
                }
                showToast(json.message || 'Operation failed. Please check inputs.', 'error');
            }
        } catch (err) {
            console.error('Submission network error:', err);
            showToast('Unable to reach server. Check connection.', 'error');
        } finally {
            setButtonLoading(false);
        }
    }

    // Start editing an existing review
    function startEditReview(review) {
        state.isEditing = true;
        state.editingReviewId = review.reviewId;
        elements.editReviewId.value = review.reviewId;

        // Switch to the review's customer or keep admin
        if (state.currentCustomerId !== 'admin') {
            state.currentCustomerId = review.customerId;
            elements.currentCustomerSelect.value = review.customerId;
            elements.formCustomerId.value = review.customerId;
        }

        // Set form fields
        setRating(review.rating);
        elements.formComment.value = review.comment;
        elements.commentCharCounter.textContent = `${review.comment.length} / 1000`;

        // Update UI headers
        elements.formHeaderTitle.textContent = `Edit Review (${review.reviewId})`;
        elements.editModeBadge.style.display = 'inline-block';
        elements.submitBtnText.textContent = 'Update Review';
        elements.cancelEditBtn.style.display = 'inline-flex';

        // Scroll to form smoothly
        elements.reviewForm.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }

    // Cancel edit mode
    function cancelEdit() {
        state.isEditing = false;
        state.editingReviewId = null;
        resetForm();
    }

    // Reset form to blank state
    function resetForm() {
        state.isEditing = false;
        state.editingReviewId = null;
        elements.editReviewId.value = '';
        elements.formComment.value = '';
        elements.commentCharCounter.textContent = '0 / 1000';
        elements.commentCharCounter.className = 'char-counter';
        setRating(0);
        clearAllErrors();

        elements.formHeaderTitle.textContent = 'Write a Review';
        elements.editModeBadge.style.display = 'none';
        elements.submitBtnText.textContent = 'Submit Review';
        elements.cancelEditBtn.style.display = 'none';
    }

    // Delete confirmation prompt
    function promptDelete(reviewId) {
        state.deletingReviewId = reviewId;
        elements.deleteTargetId.textContent = reviewId;
        elements.deleteModal.style.display = 'flex';
    }

    function closeDeleteModal() {
        state.deletingReviewId = null;
        elements.deleteModal.style.display = 'none';
    }

    // Execute deletion against backend
    async function executeDelete() {
        if (!state.deletingReviewId) return;

        elements.confirmDeleteBtn.disabled = true;
        elements.confirmDeleteBtn.textContent = 'Deleting...';

        try {
            const res = await fetch(`/api/reviews/${encodeURIComponent(state.deletingReviewId)}`, {
                method: 'DELETE',
                headers: {
                    'X-Customer-Id': state.currentCustomerId
                }
            });
            const json = await res.json();

            if (res.ok && json.success) {
                showToast(json.message || 'Review deleted successfully.', 'success');
                closeDeleteModal();
                loadBookSummary();
                loadReviews();
                if (state.isEditing && state.editingReviewId === state.deletingReviewId) {
                    cancelEdit();
                }
            } else {
                showToast(json.message || 'Failed to delete review.', 'error');
            }
        } catch (err) {
            console.error('Error during deletion:', err);
            showToast('Network error while deleting review.', 'error');
        } finally {
            elements.confirmDeleteBtn.disabled = false;
            elements.confirmDeleteBtn.textContent = 'Delete Review';
        }
    }

    // Button loading state
    function setButtonLoading(loading) {
        elements.submitReviewBtn.disabled = loading;
        elements.btnSpinner.style.display = loading ? 'inline-block' : 'none';
        if (loading) {
            elements.submitBtnText.textContent = state.isEditing ? 'Updating...' : 'Submitting...';
        } else {
            elements.submitBtnText.textContent = state.isEditing ? 'Update Review' : 'Submit Review';
        }
    }

    // Field errors helpers
    function showFieldError(field, message) {
        if (field === 'rating') {
            elements.ratingError.textContent = message;
        } else if (field === 'comment') {
            elements.commentError.textContent = message;
            elements.formComment.classList.add('is-invalid');
        }
    }

    function clearFieldError(field) {
        if (field === 'rating') {
            elements.ratingError.textContent = '';
        } else if (field === 'comment') {
            elements.commentError.textContent = '';
            elements.formComment.classList.remove('is-invalid');
        }
    }

    function clearAllErrors() {
        elements.ratingError.textContent = '';
        elements.commentError.textContent = '';
        elements.formComment.classList.remove('is-invalid');
    }

    // Toast Notification helper
    function showToast(message, type = 'info') {
        const toast = document.createElement('div');
        toast.className = `toast toast-${type}`;
        const icons = { success: '✅', error: '❌', warning: '⚠️', info: 'ℹ️' };
        toast.innerHTML = `<span>${icons[type] || 'ℹ️'}</span> <span>${escapeHtml(message)}</span>`;

        elements.toastContainer.appendChild(toast);
        setTimeout(() => {
            toast.style.opacity = '0';
            toast.style.transform = 'translateY(10px)';
            toast.style.transition = 'all 0.3s ease';
            setTimeout(() => toast.remove(), 300);
        }, 4000);
    }

    // Helpers
    function renderStarGlyphs(count) {
        let stars = '';
        for (let i = 1; i <= 5; i++) {
            stars += (i <= count) ? '★' : '☆';
        }
        return stars;
    }

    function formatDate(isoStr) {
        if (!isoStr) return '';
        try {
            const d = new Date(isoStr);
            return d.toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
        } catch {
            return isoStr;
        }
    }

    function escapeHtml(str) {
        if (!str) return '';
        const div = document.createElement('div');
        div.textContent = str;
        return div.innerHTML;
    }

    // Start application
    init();
});
