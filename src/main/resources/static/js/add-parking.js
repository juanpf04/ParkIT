document.addEventListener("DOMContentLoaded", function () { //Para que no lo cargue directamente
    
    let b= document.getElementById("enviar-solicitud");
    console.log(b);
    b.onclick = (e) => {
        console.log("click", e);
        e.preventDefault();
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
            console.log("happy", d)
            window.location.href = "/enterprise/requests";
        })
        .catch(e => console.log("sad", e))
        
    }
});