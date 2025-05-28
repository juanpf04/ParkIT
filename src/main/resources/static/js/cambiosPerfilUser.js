document.addEventListener("DOMContentLoaded", function () {
  let botonEditar = document.getElementById("btn-editar");
  let botonSubmit = document.getElementById("btn-submit");
  let usuario = document.getElementById("usuario");
  let telefono = document.getElementById("telefono");
  let correo = document.getElementById("correo");
  let userId = document.getElementById("usuarioId");
  let nav1 = document.getElementById("nav-username1");
  let nav2 = document.getElementById("nav-username2");
  let usernamePerfilUsuario = document.getElementById("usuarioPerfilUsuario");
  let correoPerfilUsuario = document.getElementById("correoPerfilUsuario");

  botonEditar.addEventListener("click", function () {
    usuario.disabled = false;
    telefono.disabled = false;
    correo.disabled = false;
    botonSubmit.disabled = false;
  });

  botonSubmit.addEventListener("click", function (e) {
    e.preventDefault();

    let id = userId.value;
    let username = usuario.value;
    let telephone = telefono.value;
    let email = correo.value;
    console.log("entraaaaaa");
    console.log(id);
    console.log(username);
    console.log(telephone);
    console.log(email);

    go(`/user/${id}/save-info`, "POST", {
      username: username,
      telephone: telephone,
      email: email,
    }).then(function (response) {
      //Si ha ido bien...
      if (response.result) {
        let divSuccess = document.getElementById("success");
        divSuccess.classList.remove("d-none");
        divSuccess.innerHTML = response.result;
        nav1.innerHTML = username;
        nav1.innerHTML = username;
        usernamePerfilUsuario.innerHTML = username;
        correoPerfilUsuario.innerHTML = email;

        //5 segundos
        setTimeout(() => {
          // AÑado a su lista de clases d-none
          divSuccess.classList.add("d-none");
        }, 5000);
      } else if (response.error) {
        let divError = document.getElementById("error");
        divError.classList.remove("d-none");
        divError.innerHTML = response.error;

        setTimeout(() => {
          // AÑado a su lista de clases d-none
          divError.classList.add("d-none");
        }, 5000);
      }

      usuario.disabled = true;
      telefono.disabled = true;
      correo.disabled = true;
      botonSubmit.disabled = true;
    });
  });
});
