document.addEventListener('DOMContentLoaded', function () {

    // Update the state every 5 seconds
    setInterval(fetchSpots, 5000); 

    function fetchSpots() {
        var now = new Date();
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
                    state.innerHTML = "No";
                    break;
                }
            }
        }
    }

});