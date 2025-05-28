if(document.getElementById("seleccionar-plaza")){
        document.getElementById("seleccionar-plaza").addEventListener('click', function() {
                let id = document.getElementById("parkingId").value;
                let vehicleId= document.getElementById("vehicleId").value;
                let startDate = document.getElementById("ini-f").value;
                let endDate = document.getElementById("fin-f").value;
                let startTime = document.getElementById("ini-h").value;
                let endTime = document.getElementById("fin-h").value;
                window.location.href = `/user/select-parking/${id}?&startDate=${startDate}&endDate=${endDate}&startTime=${startTime}&endTime=${endTime}&vehicleId=${vehicleId}`;
        });
    }
    if(document.getElementById("cambiar-plaza")){
        document.getElementById("cambiar-plaza").addEventListener('click', function() {
                let id = document.getElementById("parkingId").value;
                let vehicleId= document.getElementById("vehicleId").value;
                let selectedSlot=document.getElementById("selectedParkingSpot").value;
                let startDate = document.getElementById("ini-f").value;
                let endDate = document.getElementById("fin-f").value;
                let startTime = document.getElementById("ini-h").value;
                let endTime = document.getElementById("fin-h").value;
                window.location.href = `/user/select-parking/${id}?&startDate=${startDate}&endDate=${endDate}&startTime=${startTime}&endTime=${endTime}&vehicleId=${vehicleId}&selectedSlot=${selectedSlot}`;
            });
    }