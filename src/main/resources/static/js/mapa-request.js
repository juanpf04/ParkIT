document.addEventListener("DOMContentLoaded", function () {
  document.querySelectorAll(".accordion-collapse").forEach(function (accordion) {
    accordion.addEventListener("shown.bs.collapse", function (event) {
      const mapaDiv = event.target.querySelector(".map-container");
      console.log(mapaDiv);
      let errordiv = event.target.querySelector(".error");

      if (mapaDiv && !mapaDiv.dataset.loaded) {
        const address = mapaDiv.getAttribute("data-address");

        if (address) {
          fetch(`https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(address)}`)
            .then(response => response.json())
            .then(data => {
              console.log(data);
              if (data.length > 0) {
                console.log(data);
                const lat = parseFloat(data[0].lat);
                const lng = parseFloat(data[0].lon);

                // Crear un nuevo mapa con ID único
                
                const map = L.map(mapaDiv.id).setView([lat, lng], 15);

                const id = mapaDiv.id.split("-")[1]; // Extraes el ID del parking (asumiendo formato "mapa-123")

                console.log(id);
                const latSpan = document.getElementById(`latitud-${id}`);
                const lngSpan = document.getElementById(`longitud-${id}`);

                if (latSpan && lngSpan) {
                  latSpan.innerHTML = lat.toFixed(6);
                  lngSpan.innerHTML = lng.toFixed(6);
                }
                

                L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
                  attribution: "© OpenStreetMap contributors",
                }).addTo(map);
                

                // Agregar un marcador con popup
                L.marker([lat, lng]).addTo(map)
                  .bindPopup(`<strong>Ubicación del Parking</strong><br>${address}`)
                  .openPopup();
                  
                

                setTimeout(() => {
                  map.invalidateSize();
                }, 300);

                mapaDiv.dataset.loaded = "true"; // Marcarlo como cargado
              } else {
                const map = L.map(mapaDiv.id).setView([40.404711, -3.710862], 15);
                L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
                  attribution: "© OpenStreetMap contributors",
                }).addTo(map);
                mapaDiv.dataset.loaded = "true"; // Marcarlo como cargado
                
                document.getElementById(errordiv.id).innerHTML = "Dirección no encontrada";
                document.getElementById(errordiv.id).className = "alert alert-danger";
                
                console.error("No se encontraron coordenadas para:", address);
                
                // map.on('movestart', function(){
                //   document.getElementById(errordiv.id).style.display = 'block';
                // });

                // var customDivIcon = L.divIcon({
                //   html: '<div class="alert alert-danger">Dirección no encontrada</div>',
                //   iconSize: [150, 40], // Ajusta el tamaño del icono según el contenido
                //   iconAnchor: [75, 20]  // Ajusta la posición del icono
                // });
          
                // // Agregar el marcador con el divIcon
                // L.marker([40.404711, -3.710862], { icon: customDivIcon }).addTo(map);

                // L.popup().setLatLng([40.404711, -3.710862])
                // .setContent(`<div class="alert alert-danger">Dirección no encontrada</div>`)
                // .openOn(map);
                // L.bindPopup("Dirección no encontrada").openPopup().addTo(map);
              }
            })
            .catch(error => console.error("Error obteniendo coordenadas:", error));
        }
      }
    });
  });
});

function eliminarRequest(button) {
  let id = button.getAttribute("data-id");

  fetch("/admin/eliminarRequest/" + id, {
    method: "DELETE",
    headers: {
      "Content-Type": "application/json",
    },
  }).then((response) => {
    if (response.ok) {
      let successMessage = document.createElement("div");
      successMessage.className = "alert alert-success";
      successMessage.textContent = "Request eliminado con éxito";
      let container = document.querySelector("header + .container-fluid");
      container.prepend(successMessage);

      setTimeout(() => {
        successMessage.remove();
      }, 3000);

      let accordionItem = button.closest(".accordion-item");
      if (accordionItem) {
        accordionItem.remove();
      }
    } else {
      response.text().then((errorMessage) => {
        let error = document.createElement("div");
        error.className = "alert alert-danger";
        error.textContent =
          "Error al eliminar el request: " + errorMessage;
        let container = document.querySelector(
          "header + .container-fluid"
        );
        container.prepend(error);

        setTimeout(() => {
          error.remove();
        }, 300000);
      });
    }
  });
}

function guardarParking(button) {
  let id = button.getAttribute("data-id");
  let latitud = document.getElementById("latitud-" + id).innerHTML;
  let longitud = document.getElementById("longitud-" + id).innerHTML;

  // Ejemplo de fetch para guardar el parking
  // LLama al controlador para guardar el parking
  fetch(`/admin/guardarParking/${id}?latitud=${latitud}&longitud=${longitud}`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    credentials: "include",
  }) //Recibe la respuesta del controlador
    .then((response) => {
      if (response.ok) {
        let successMessage = document.createElement("div");
        successMessage.className = "alert alert-success";
        successMessage.textContent = "Parking guardado con éxito";
        let container = document.querySelector(
          "header + .container-fluid"
        );
        container.prepend(successMessage);

        setTimeout(() => {
          successMessage.remove();
        }, 3000);

        let accordionItem = button.closest(".accordion-item");
        if (accordionItem) {
          accordionItem.remove();
        }
      } else {
        response.text().then((errorMessage) => {
          let error = document.createElement("div");
          error.className = "alert alert-danger";
          error.textContent =
            "Error al guardar el parking: " + errorMessage;
          let container = document.querySelector(
            "header + .container-fluid"
          );
          container.prepend(error);

          setTimeout(() => {
            error.remove();
          }, 3000);
        });
      }
    })
    .catch((error) => {
      console.error("Error al guardar el parking", error);
    });
}

function eliminarParking(button) {
  let id = button.getAttribute('data-id');

  fetch('/admin/eliminarParking/' + id, {
      method: 'POST',
      headers: {
          'Content-Type': 'application/json'
      }
  }).then(response => {
      if (response.ok) {
          let successMessage = document.createElement('div');
          successMessage.className = 'alert alert-success';
          successMessage.textContent = 'Parking eliminado con éxito';
          let container = document.querySelector('header + .container-fluid');
          container.prepend(successMessage);

          setTimeout(() => {
              successMessage.remove();
          }, 3000);

          let accordionItem = button.closest('.accordion-item');
          if (accordionItem) {
              accordionItem.remove();
          }
      } else {
          response.text().then(errorMessage => {
              let error = document.createElement('div');
              error.className = 'alert alert-danger';
              error.textContent = 'Error al eliminar el parking: ' + errorMessage;
              let container = document.querySelector('header + .container-fluid');
              container.prepend(error);

              setTimeout(() => {
                  error.remove();
              }, 3000);
          });
      }
  }).catch(error => {
      console.error('Error al eliminar el parking', error);
  });
}