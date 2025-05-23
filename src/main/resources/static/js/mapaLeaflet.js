// Ejemplo de uso de Leaflet para mostrar un mapa y buscar parkings

//esperar que este cargado el documento
document.addEventListener('DOMContentLoaded', function () {

    // Inicializar el mapa
    let map = L.map('map');

    // Obtener la latitud y longitud
    let latlng = [
        parseFloat(document.getElementById('latitude').value), 
        parseFloat(document.getElementById('longitude').value)
    ];
    console.log(latlng);

    // Obtener el radio de búsqueda
    let radius = parseInt(document.getElementById('customRange3').value);
    console.log(radius);

    // Círculo de búsqueda
    let circleFind = null;

    // Array para almacenar marcadores personalizados
    const markers = [];   

    // Icono para la ubicación del usuario
    let myLocationIcon = L.icon({
        iconUrl: '/img/my_location.png',
        iconSize: [40, 40],
        iconAnchor: [20, 40],
        popupAnchor: [0, 40]
    });

    // Icono para los parkings
    let markerIcon = L.icon({
        iconUrl: '/img/marker.png',
        iconSize: [60, 60],
        iconAnchor: [30, 60], 
        popupAnchor: [0, -50]
    });

    // Función para modificar el círculo de búsqueda
    function setCircle() {
        console.log("setCircle", latlng, radius);
        if (circleFind) {
            map.removeLayer(circleFind);
        }
        circleFind = L.circle(latlng, radius).addTo(map);
    }

    // Función para calcular la distancia entre dos puntos geográficos
    function calcularDistancia(lat1, lon1, lat2, lon2) {
        const R = 6371; // Radio de la Tierra en km
        const dLat = (lat2 - lat1) * Math.PI / 180;
        const dLon = (lon2 - lon1) * Math.PI / 180;
        const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2);
        const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    // Función personalizada para buscar marcadores por nombre
    function buscarMarcador(nombre) {
        markers.forEach(marker => {
            if (marker.getPopup() && marker.getPopup().getContent().includes(nombre)) {
                map.setView(marker.getLatLng(), 16);
                marker.openPopup();
            }
        });
    }

    // Inicializar el mapa en la ubicación del usuario
    function onLocationFound(e) {
        console.log("onLocationFound", e.latlng, radius);

        latlng = e.latlng;

        L.marker(latlng, { icon: myLocationIcon }).addTo(map).bindTooltip("Tu Ubicación").openTooltip();
        
        setCircle();

        parkings.forEach(parking => {
            console.log(latlng[0], latlng[1]);
            console.log(calcularDistancia(latlng[0], latlng[1], parking.latitude, parking.longitude));
            if(true || calcularDistancia(latlng[0], latlng[1], parking.latitude, parking.longitude) <= radius) {
                const marker = L.marker([parking.latitude, parking.longitude], { icon: markerIcon }).addTo(map);
                const id = parking.id;
                const url = `/user/reserve/${id}`;
                marker.bindPopup(`<b>${parking.name}</b><br>${parking.address}</br><a href="${url}" class="btn btn-outline-secondary">Reservar</a>`);
                markers.push(marker);
            }
        });
    }

    if (isNaN(latlng[0]) || isNaN(latlng[1])) {
        map.locate({ setView: true, maxZoom: 13 });
        map.on('locationfound', onLocationFound);
    } else {
        map.setView(latlng, 13);
        setCircle();

        parkings.forEach(parking => {
            console.log(latlng[0], latlng[1]);
            console.log(calcularDistancia(latlng[0], latlng[1], parking.latitude, parking.longitude));
            if(calcularDistancia(latlng[0], latlng[1], parking.latitude, parking.longitude) <= radius) {
                const marker = L.marker([parking.latitude, parking.longitude], { icon: markerIcon }).addTo(map);
                const id = parking.id;
                const url = `/user/reserve/${id}`;
                marker.bindPopup(`<b>${parking.name}</b><br>${parking.address}</br><a href="${url}" class="btn btn-outline-secondary">Reservar</a>`);
                markers.push(marker);
            }
        });
    }

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '© OpenStreetMap contributors'
    }).addTo(map);
    
    let geocoder = L.Control.geocoder({
        placeholder: 'Buscar calle...',
        defaultMarkGeocode: false,
        collapsed: false
    }).on('markgeocode', function (e) {
        const fullAddress = e.geocode.name; // Dirección completa
        const latitude = e.geocode.center.lat;
        const longitude = e.geocode.center.lng;

        // Extraer componentes de la dirección
        const addressComponents = fullAddress.split(",").map(item => item.trim());

        let calle = addressComponents.length == 3 ? '' : addressComponents[0]; // Primera parte (nombre de la calle)
        let ciudad = addressComponents.length == 3 ? addressComponents[addressComponents.length - 3] : ''; // Penúltimo elemento (ciudad)
        ciudad = addressComponents.length > 4 ? addressComponents[addressComponents.length - 4] : ciudad; // Penúltimo elemento (ciudad)
        let codigoPostal = addressComponents.length > 1 ? addressComponents[addressComponents.length - 2].match(/\d{4,5}/) : ''; // Último elemento (CP)
        let pais = addressComponents.length > 2 ? addressComponents[addressComponents.length - 1] : ''; // último elemento (ciudad)

        codigoPostal = codigoPostal ? codigoPostal[0] : ''; // Obtener el CP si existe

        // Actualizar los campos en el formulario
        // document.getElementById('fullAddress').value = fullAddress;
        // document.getElementById('calle').value = calle;
        // document.getElementById('ciudad').value = ciudad;
        // document.getElementById('codigoPostal').value = codigoPostal;
        document.getElementById('latitude').value = latitude;
        document.getElementById('longitude').value = longitude;

        // // Mover el mapa a la ubicación seleccionada
        // map.setView(e.geocode.center, 16);
        // if(circleFind){
        //     map.removeLayer(circleFind);
        // }
        // circleFind=L.circle(e.geocode.center, 30000).addTo(map);
    }).addTo(map);

    const geocoderContainer = geocoder.getContainer();
    document.getElementById('buscador').appendChild(geocoderContainer);

    document.getElementById('customRange3').addEventListener('input', function () {
        radius = parseInt(this.value);

        document.getElementById('rangeValue').innerHTML = radius;

        setCircle();

        // Double radio = 30.0; 
			// double lat, lon;
			// List<Parking> parkingsInRange;
			// if (latitude == null || longitude == null || latitude == "" || longitude == "") {
			// 	parkingsInRange = parkings;
			// } else {
			// 	lat = Double.parseDouble(latitude);
			// 	lon = Double.parseDouble(longitude);
			// 	parkingsInRange = parkings.stream()
			// 			.filter(p -> calcularDistancia(lat, lon, p.getLatitude(), p.getLongitude()) <= radio)
			// 			.collect(Collectors.toList());
			// }
    });
});