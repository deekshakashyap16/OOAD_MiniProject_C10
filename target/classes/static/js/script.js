// Campus Marketplace — script.js

// Auto-dismiss flash alerts after 4 seconds
document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('.alert').forEach(el => {
        setTimeout(() => {
            el.style.transition = 'opacity .6s';
            el.style.opacity = '0';
            setTimeout(() => el.remove(), 650);
        }, 4000);
    });

    // Confirm before destructive actions (remove / deactivate / delete)
    document.querySelectorAll('form[data-confirm]').forEach(form => {
        form.addEventListener('submit', e => {
            if (!confirm(form.dataset.confirm)) e.preventDefault();
        });
    });
});
