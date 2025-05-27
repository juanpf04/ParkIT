
let dropdownMenu = document.getElementById('menuDropdownNotis');
function renderNoti(msg) {
    const li = document.createElement('li');
            li.className = 'dropdown-item';
            li.innerHTML = `
                        <strong>Notificación de: ${msg.from}</strong><br>
                        <small>${msg.text}</small><br>
                        <small class="text-muted">${new Date(msg.sent).toLocaleString('es-ES')}</small>
                    `;
    console.log("rendering notification: ", msg);
    li.addEventListener('click', () => {
        li.remove();
        if(dropdownMenu.children.length === 0) {
            dropdownMenu.appendChild(sinNotis);
        }
    });
    li.addEventListener('mouseover', () => {
        li.style.backgroundColor = '#f0f0f0'; // Cambia el color de fondo al pasar el mouse
    });
    li.addEventListener('mouseout', () => {
        li.style.backgroundColor = ''; // Restaura el color de fondo al salir el mouse
    });

    return li;
}

const sinNotis = document.createElement('li');
sinNotis.className = 'dropdown-item text-muted';
sinNotis.textContent = 'No hay notificaciones';
if(config.user){
    go(config.rootUrl + "/user/received", "GET").then(ms =>{
        if(ms.length === 0) {
            dropdownMenu.appendChild(sinNotis);
        }else {
            ms.forEach(m => dropdownMenu.appendChild(renderNoti(m)))
        }
        
    });
}else if(config.enterprise){
    go(config.rootUrl + "/enterprise/received", "GET").then(ms =>{
        if(ms.length === 0) {
            dropdownMenu.appendChild(sinNotis);
        }else {
            ms.forEach(m => dropdownMenu.appendChild(renderNoti(m)))
        }
        
    });
}else if(config.admin){
    go(config.rootUrl + "/admin/received", "GET").then(ms =>{
        if(ms.length === 0) {
            dropdownMenu.appendChild(sinNotis);
        }else {
            ms.forEach(m => dropdownMenu.appendChild(renderNoti(m)))
        }
        
    });
}




// y aquí pinta mensajes según van llegando
if (ws.receive) {
    const oldFn = ws.receive; // guarda referencia a manejador anterior
    ws.receive = (m) => {

        oldFn(m); // llama al manejador anterior
        
        if (m.type == "MOSTRAR") {

            if(dropdownMenu.contains(sinNotis)) {
                dropdownMenu.removeChild(sinNotis);
            }
            dropdownMenu.appendChild(renderNoti(m));
            mostrarNuevaNotificacion(m);
        }
        else if (m.type == "ACTUALIZAR"){ // ACTUALIZAR
            console.log(m.text);
            //Ejemplo de cómo convertir a JSON
            let reservaJSON = JSON.parse(m.text);
            let spotId = reservaJSON.spotId;
            console.log(spotId);

            let reservasTd = document.getElementById('reserves-' + spotId);
            let ul = reservasTd.querySelector("ul");

            const reserva = document.createElement('li');
            reserva.textContent = reservaJSON.startDate + " " + reservaJSON.startTime + " - " + reservaJSON.endDate + " " + reservaJSON.endTime;
            ul.appendChild(reserva);
        }
        else if(m.type == "ACTUALIZAR_TABLA_PARKING") {
            console.log(m.text);
            let jsonParking = JSON.parse(m.text);
            let table = document.getElementById("tabla-parkings-empresa");
            let row = document.createElement("tr");
            
            row.innerHTML = `<td id="fila1"><a class="nav-link bg-success" >${jsonParking.name}</a></td>
            <td id="fila1"><a class="nav-link bg-success">${jsonParking.address}</a></td>
            <td id="fila1"><a class="nav-link bg-success">${jsonParking.cp}</a></td>
            <td id="fila1"><a class="nav-link bg-success">${jsonParking.city}</a></td>
            <td id="fila1"><a class="nav-link bg-success">${jsonParking.country}</a></td>
            <td id="fila1"><a class="nav-link bg-success">${jsonParking.telephone}</a></td>
            <td id="fila1"><a class="nav-link bg-success">${jsonParking.openingTime}</a></td>
            <td id="fila1"><a class="nav-link bg-success">${jsonParking.closingTime}</a></td>
            <td id="fila1"><a class="nav-link bg-success">${jsonParking.feePerHour}</a></td>`;
            row.classList.add('bg-success');
            row.className="bg-success";
            table.appendChild(row);
        }
        else if(m.type == "ACTUALIZAR_ESTADO_REQUEST"){

            let jsonState = JSON.parse(m.text);
            let id = document.getElementById("estado-" + jsonState.id);
            let id2 = document.getElementById("estado2-" + jsonState.id );

            id.innerHTML = jsonState.state;
            id2.innerHTML = jsonState.state;
            if(jsonState.state == "Aceptada"){
                id.className = "estado-aceptado";
                id2.className = "estado-aceptado";

            }else if(jsonState.state == "Rechazada"){
                id.className = "estado-rechazado";
                id2.className = "estado-rechazado";
            }
            
        }
    }
}


function obtenerEventoAleatorio() {
    fetch('/evento-aleatorio')
        .then(response => response.json())
        .then(data => {
            if (data.mensaje) {
                console.log(data.mensaje);
            } else {
                mostrarmensajeEvento(data);
            }
        })
        .catch(error => console.error('Error al obtener evento:', error));
}

function mostrarNuevaNotificacion(msg) {
    const container = document.getElementById('notification-container');

    // Crear el elemento de la notificación
    const notification = document.createElement('div');
    notification.className = 'notification';
    notification.innerHTML = `
        <strong>Mensaje de: ${msg.from}</strong><br>
        <small>${msg.text}</small><br>
        <small class="text-muted">${new Date(msg.sent).toLocaleString('es-ES')}</small>
    `;

    container.appendChild(notification);
    container.style.display = 'block';

    setTimeout(() => {
        notification.remove();
        if (container.childElementCount === 0) {
            container.style.display = 'none';
        }
    }, 5000);
}

