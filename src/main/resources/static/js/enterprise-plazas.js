document.addEventListener('DOMContentLoaded', function () {


//     const reservaId = 1175;
// if (reservaId) {
//     ws.initialize(config.socketUrl, [`/enterprise/reserva/${reservaId}`]);

// ws.receive = (msg) => {
//       const oldFn = ws.receive; // guarda referencia a manejador anterior
//         console.log("Mensaje recibido en el chat de reserva:", msg);
//         appendMessage(`${msg.sender}: ${msg.text}`);
//     };
// }

    // Update the state every 5 seconds
        setInterval(fetchSpots, 5000); 
      

    function fetchSpots() {
        var now = new Date();
        // console.log("Fetching spots at: " + now.toLocaleTimeString());
        for(var i=0; i < spots.length; i++) {
            var spot = spots[i];
            var state = document.getElementById("state-"+spot.id);
            var reserves = document.getElementById("reserves-"+spot.id);

            var lis = reserves.getElementsByTagName("li"); 

            state.innerHTML = "Sí";

            for (var j = 0; j < lis.length; j++) {
                var li = lis[j];
                
                var startDate = li.innerHTML.split(" ")[0];
                var startTime = li.innerHTML.split(" ")[1];

                var endDate = li.innerHTML.split(" ")[3];
                var endTime = li.innerHTML.split(" ")[4];

                var startDateTime = new Date(startDate + "T" + startTime);
                var endDateTime = new Date(endDate + "T" + endTime);
                if (now >= startDateTime && now <= endDateTime) {
                    state.classList.add("mt-2");
                    state.classList.add("bg-danger");
                    state.classList.add("badge");
                    state.innerHTML = "No";
                    break;
                }
            }
        }
    }

});