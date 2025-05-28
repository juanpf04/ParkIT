document.addEventListener('DOMContentLoaded', function () {

    const switchInput = document.getElementById('userTypeSwitch');
    const parkerForm = document.getElementById('registerFormParker');
    const enterpriseForm = document.getElementById('registerFormEnterprise');
    const switchLabel = document.getElementById('switchLabel');
    const formContainer = document.getElementById('formRegister');

    switchInput.addEventListener('change', function () {
        if (this.checked) {
            parkerForm.classList.remove('active');
            enterpriseForm.classList.add('active');
            formContainer.style.backgroundColor = '#e6ffee';
        } else {
            enterpriseForm.classList.remove('active');
            parkerForm.classList.add('active');
            formContainer.style.backgroundColor = '#e0f0ff';
        }
    });
    // Validación en tiempo real por campo
    const allInputs = document.querySelectorAll('.form-control');
    allInputs.forEach(input => {
        input.addEventListener('blur', validateField);
        input.addEventListener('input', validateField);
    });

    function validateField(event) {
        const input = event.target;
        if (input.checkValidity()) {
            input.classList.remove('is-invalid');
            input.classList.add('is-valid');
        } else {
            input.classList.remove('is-valid');
            input.classList.add('is-invalid');
        }
    }


    const enviarFormulario = async (form) => {
        if (!form.checkValidity()) {
            form.classList.add('was-validated');
            return;
        }

        const formData = new FormData(form);

        try {
            const response = await fetch('/postRegister', {
                method: 'POST',
                body: formData
            });

            if (response.ok) {
                const data = await response.json();
                if(data.status==="ok"){
                    window.location.href = "/login?RegisterSuccess=true";
                }
            } else {
                const errors = await response.json();
                Object.entries(errors).forEach(([field, message]) => {
                const input = document.querySelector(`[name="${field}"]`);
                if (input) {
                    input.classList.add('is-invalid');
                    let errorDiv = input.nextElementSibling;
                    if (!errorDiv || !errorDiv.classList.contains('invalid-feedback')) {
                        errorDiv = document.createElement('div');
                        errorDiv.className = 'invalid-feedback';
                        input.parentNode.appendChild(errorDiv);
                    }
                    errorDiv.innerText = message;
                }
            });
            }
        } catch (error) {
            console.error("Fallo en el envío:", error);
        }
    };
    // Submit Parker
    parkerForm.addEventListener('submit', (e) => {
        e.preventDefault();
        enviarFormulario(parkerForm);
    });

    // Submit Enterprise
    enterpriseForm.addEventListener('submit', (e) => {
        e.preventDefault();
        enviarFormulario(enterpriseForm);
    });
});