document.addEventListener("DOMContentLoaded", function () { //Para que no lo cargue directamente
    
    let b= document.getElementById("enviar-solicitud");
    console.log(b);
    b.onclick = (e) => {
        console.log("click", e);
        e.preventDefault();
        // Le pasamos esto al controller en formato JSON
        go("/enterprise/request-parking", 'POST', {
            name: document.getElementById("name").value,
            address: document.getElementById("address").value,
            city: document.getElementById("city").value,
            country: document.getElementById("country").value,
            cp: document.getElementById("cp").value,
            openingTime: document.getElementById("openingTime").value,
            closingTime: document.getElementById("closingTime").value,
            totalSpots: document.getElementById("totalSpots").value,
            feePerHour: document.getElementById("feePerHour").value,
            telephone: document.getElementById("telephone").value,
            email: document.getElementById("email").value
            
        })
        .then(d => {
            //Recibe una respuesta exitosa...
            console.log("happy", d);
            //Guardamos el mensaje de tipo result que viene del controller en formato JSON en el success
            const successMessage = encodeURIComponent(d.result || "Solicitud realizada con éxito. Esperando respuesta del administrador")
            //Esto le pasa a la vista el mensaje de éxito a través de la URL. Pero necesitamos meterlo en el modelo a través del controller para que pueda usarlo.
            window.location.href = `/enterprise/requests?success=${successMessage}`;
        }).catch(e => {
            console.log("sad", e);
            window.location.href = `/enterprise/requests?error=${encodeURIComponent("Hubo un error al enviar la solicitud")}`;
        });
        
    };
});