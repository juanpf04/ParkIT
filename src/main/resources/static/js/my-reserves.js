
document.addEventListener('DOMContentLoaded', () => {
    const cancelButtons = document.querySelectorAll('a[data-bs-target="#confirmCancelModal"]');
    const form = document.getElementById('cancelForm');

    cancelButtons.forEach(btn => {
        btn.addEventListener('click', () => {
            const reservaId = btn.getAttribute('data-id');
            const action = `/user/reserve/${reservaId}/cancel`;
            form.setAttribute('action', action);
        });
    });
});
