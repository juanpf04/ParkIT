package es.ucm.fdi.iw.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.springframework.web.bind.annotation.ResponseBody;
import com.fasterxml.jackson.core.JsonProcessingException;

import es.ucm.fdi.iw.LocalData;
import es.ucm.fdi.iw.controller.UserController.NoEsTuPerfilException;
import es.ucm.fdi.iw.model.User;
import es.ucm.fdi.iw.model.Message.Type;
import es.ucm.fdi.iw.model.Enterprise;
import es.ucm.fdi.iw.model.Message;
import es.ucm.fdi.iw.model.Parker;
import es.ucm.fdi.iw.model.Request;
import es.ucm.fdi.iw.model.Reserve;
import es.ucm.fdi.iw.model.Spot;
import es.ucm.fdi.iw.model.Transferable;
import es.ucm.fdi.iw.model.Parking;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Controller
@RequestMapping("enterprise")
public class EnterpriseController {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private LocalData localData;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @ModelAttribute
    public void populateModel(HttpSession session, Model model) {
        for (String name : new String[] { "u", "url", "ws" }) {
            model.addAttribute(name, session.getAttribute(name));
        }
    }

    private boolean isEnterprise(HttpSession session) {
		Enterprise enterprise = (Enterprise) session.getAttribute("u");
		return enterprise != null;
	}

    private static final Logger log = LogManager.getLogger(EnterpriseController.class);

    @GetMapping("/")
    public String index(Model model) {
        return "enterprise";
    }

    /**
     * Landing page for a user profile
     */
    @GetMapping("{id}")
    public String index(@PathVariable long id, Model model, HttpSession session) {
        User target = (Enterprise) entityManager.find(Enterprise.class, id);

        model.addAttribute("user", target.toTransfer());
        return "enterprise-info";
    }

    /**
	 * Muestra los parkings de la empresa.
     * 
	 * @param model    Modelo para la vista.
	 * @return Carga la vista de los parkings de la empresa.
	 */
    @GetMapping("/parkings")
    public String enterpriseParkings(Model model) {
        User user = (User) model.getAttribute("u");

        List<Parking> parkings = entityManager
                .createNamedQuery("Parking.findByEnterpriseandEnabled", Parking.class)
                .setParameter("enterprise", user).setParameter("enabled", true)
                .getResultList();

        model.addAttribute("parkings", parkings);

        return "enterprise-parkings";
    }

    /**
     * Solicitudes de parking de la empresa.
     * 
     * @param model Modelo para la vista.
     * @return Carga la vista de las solicitudes de parking de la empresa.
     */
    @GetMapping("/requests")
    public String enterpriseRequests(Model model) {
        User user = (User) model.getAttribute("u");

        List<Request> requests = entityManager
                .createNamedQuery("Request.findByEnterprise", Request.class)
                .setParameter("enterprise", user)
                .getResultList();

        // El nombre que le pongamos el de entre comillas es el que usamos luego para
        // recorrer en la vista con thimeleaf
        model.addAttribute("requests", requests);

        return "enterprise-requests";
    }

    @GetMapping("/add-parking")
    public String addParkings(Model model) {
        return "add-parking";
    }

    /**
     * Muestra las plazas de un parking.
     * 
     * @param parkingId ID del parking.
     * @param model     Modelo para la vista.
     * @return Carga la vista de las plazas del parking.
     */
    @GetMapping("/parking/{parkingId}/plazas")
    public String enterprisePlazas(@PathVariable Long parkingId, Model model) {
        Parking parking = entityManager.find(Parking.class, parkingId);
        if (parking == null) {
            return "redirect:/error";
        }
        model.addAttribute("parking", parking);
        List<Spot> spots = entityManager
                .createNamedQuery("Spot.findByParking", Spot.class)
                .setParameter("parking", parking)
                .getResultList();

        List<Spot.Transfer> spotTransfers = new ArrayList<>();
        for (Spot spot : spots) {
            spotTransfers.add(spot.toTransfer());
        }
        model.addAttribute("spotTransfers", spotTransfers);
        model.addAttribute("spots", spots);
        return "enterprise-plazas";
    }

    @GetMapping("/add-plaza")
    public String addPlaza(Model model) {
        return "add-plaza";
    }

    // Función para solicitar añadir un parking al administrador.
    // Ruta con la que llega a este método desde la vista
    /*
     * PARÁMETROS ELIMINADOS PARA QUE SE ENVIEN EN FORMATO DE JSON:
     * 
     * @RequestParam String name, @RequestParam String address, @RequestParam int
     * cp,
     * 
     * @RequestParam String city, @RequestParam String country, @RequestParam String
     * telephone,
     * 
     * @RequestParam String email, @RequestParam double feePerHour, @RequestParam
     * String openingTime,
     * 
     * @RequestParam String closingTime, @RequestParam Integer totalSpots
     */
    /**
     * Posts a message to a user.
     * 
     * @param id of target user (source user is from ID)
     * @param o  JSON-ized message, similar to {"message": "text goes here"}
     * @throws JsonProcessingException
     */
    @PostMapping("/request-parking")
    @Transactional
    @ResponseBody
    public String requestParking(@RequestBody JsonNode requestData, HttpSession session, Model model) {

        try {

            String name = requestData.get("name").asText();
            String address = requestData.get("address").asText();
            int cp = requestData.get("cp").asInt();
            String city = requestData.get("city").asText();
            String country = requestData.get("country").asText();
            String telephone = requestData.get("telephone").asText();
            String email = requestData.get("email").asText();
            double feePerHour = requestData.get("feePerHour").asDouble();
            String openingTime = requestData.get("openingTime").asText();
            String closingTime = requestData.get("closingTime").asText();
            Integer totalSpots = requestData.get("totalSpots").asInt();

            LocalTime parsedOpeningTime = LocalTime.parse(openingTime);
            LocalTime parsedClosingTime = LocalTime.parse(closingTime);

            Request request = new Request();
            request.setName(name);
            request.setAddress(address);
            request.setEnabled(true);
            request.setCp(cp);
            request.setCity(city);
            request.setCountry(country);
            request.setTelephone(Integer.parseInt(telephone));
            request.setEmail(email);
            request.setFeePerHour(feePerHour);
            request.setLatitude(0.0);
            request.setLongitude(0.0);
            request.setOpeningTime(parsedOpeningTime);
            request.setClosingTime(parsedClosingTime);
            request.setState("Pendiente");
            request.setType("AÑADIR");
            request.setTotalSpots(totalSpots);
            request.setEnterprise((Enterprise) session.getAttribute("u"));

            entityManager.persist(request);
            entityManager.flush();
            entityManager.clear();

            // Enviar notificación a los administradores
            // Obtener la empresa del usuario actual
            // y crear el mensaje de notificación
            // (suponiendo que la empresa está en la sesión)

            Enterprise enterprise = (Enterprise) session.getAttribute("u");
            String notificationText = "Nueva solicitud de parking de la empresa " + enterprise.getName() +
                    " con id: " + request.getId() + " en la dirección: " + request.getAddress();
            // ID del administrador al que se le envía el mensaje (puede ser null para
            // enviar a todos los administradores)
            Message message = new Message();
            message.setSender(enterprise);
            message.setRecipient(null); // null para enviar a todos los administradores
            message.setDateSent(LocalDateTime.now());
            message.setText(notificationText);
            message.setType(Type.MOSTRAR);
            
            entityManager.persist(message);

            // Convertir el mensaje a JSON y enviarlo
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(message.toTransfer());
            messagingTemplate.convertAndSend("/topic/admin", json);

            model.addAttribute("success", "Solicitud realizada con éxito. Esperando respuesta del administrador.");
            return "{\"result\": \"Solicitud realizada con éxito . Esperando respuesta del administrador.\"}";
        } catch (Exception e) {
            model.addAttribute("error", "Hubo un error al guardar la solicitud: " +
                    e.getMessage());
            return "redirect:/error";
        }

        /* A FALTA DE MÁS INFORMACIÓN */
        // return enterpriseRequests(model);
    }

    /**
     * Solicita eliminar un parking al administrador.
     * 
     * @param parkingId ID del parking a eliminar.
     * @param model     Modelo para la vista.
     * @return Redirige a la vista de solicitudes de la empresa.
     */
    @PostMapping("/delete-parking")
    @Transactional
    public String deleteParking(@RequestParam Long parkingId, Model model, HttpSession session) {
        try {
            Parking parking = entityManager.find(Parking.class, parkingId);

            if (parking != null) {

                Request request = new Request();
                request.setName(parking.getName());
                request.setAddress(parking.getAddress());
                request.setEnabled(true);
                request.setCp(parking.getCp());
                request.setCity(parking.getCity());
                request.setCountry(parking.getCountry());
                request.setTelephone(parking.getTelephone());
                request.setEmail(parking.getEmail());
                request.setFeePerHour(parking.getFeePerHour());
                request.setLatitude(0.0);
                request.setLongitude(0.0);
                request.setOpeningTime(parking.getOpeningTime());
                request.setClosingTime(parking.getClosingTime());
                request.setState("Pendiente");
                request.setType("ELIMINAR");
                request.setTotalSpots(parking.getSpots().size());
                request.setEnterprise((Enterprise) session.getAttribute("u"));
                request.setIdParking(parkingId);

                entityManager.persist(request);

                entityManager.flush();
                entityManager.clear();

                model.addAttribute("success", "Solicitud de eliminación de parking realizada con éxito.");
            } else {
                model.addAttribute("error", "No se encontró el parking con id " + parkingId);
            }
        } catch (Exception e) {
            model.addAttribute("error", "Hubo un error al procesar la solicitud: " + e.getMessage());
        }

        return "redirect:/enterprise/requests";
    }

    /**
     * Returns JSON with all received messages
     */
    @GetMapping(path = "received", produces = "application/json")
    @Transactional // para no recibir resultados inconsistentes
    @ResponseBody // para indicar que no devuelve vista, sino un objeto (jsonizado)
    public List<Message.Transfer> retrieveMessages(HttpSession session) {
        long userId = ((User) session.getAttribute("u")).getId();
        User u = entityManager.find(User.class, userId);
        log.info("Generating message list for user {} ({} messages)",
                u.getUsername(), u.getReceived().size());
        return u.getReceived().stream().map(Transferable::toTransfer).collect(Collectors.toList());
    }

    /**
     * Returns JSON with count of unread messages
     */
    @GetMapping(path = "unread", produces = "application/json")
    @ResponseBody
    public String checkUnread(HttpSession session) {
        long userId = ((User) session.getAttribute("u")).getId();
        long unread = entityManager.createNamedQuery("Message.countUnread", Long.class)
                .setParameter("userId", userId)
                .getSingleResult();
        session.setAttribute("unread", unread);
        return "{\"unread\": " + unread + "}";
    }

    /* EJERCICIO MÍO. TENGO QUE CREAR UNA VISTA QUE MUESTRE PARA CADA PARKING DE LA EMPRESA LOS SPOTS QUE ESTÁN RESERVADOS*/
    @GetMapping("/enterprise-reserved-spots")
    @Transactional
    public String reservedSpots(HttpSession session, Model model) {

        /* Verificamos que el usuario sea una enterprise y el id coincida con el de la sesión */
        if (isEnterprise(session)) {
            List<Parking> parkings = entityManager
                    .createNamedQuery("Parking.findByEnterpriseandEnabled", Parking.class)
                    .setParameter("enterprise", session.getAttribute("u"))
                    .setParameter("enabled", true)
                    .getResultList();
            List<Map<String,Object>> parkingStats = new ArrayList<>();
            LocalDate currentDate = LocalDate.now();
            LocalTime currentTime = LocalTime.now();

            for (Parking parking : parkings) {
                List<Spot> spots = entityManager
                        .createNamedQuery("Spot.findByParking", Spot.class)
                        .setParameter("parking", parking)
                        .getResultList();

                int reservedSpots = 0;
                for (Spot spot : spots) {
                    for (Reserve reserve : spot.getReserves()) {
                        if ((reserve.getState() == Reserve.State.CONFIRMED || reserve.getState() == Reserve.State.PENDING) &&
                        ((reserve.getStartDate().isBefore(currentDate) ||  reserve.getStartDate().isEqual(currentDate) && reserve.getStartTime().isBefore(currentTime)) &&
                        (reserve.getEndDate().isAfter(currentDate) || reserve.getEndDate().isEqual(currentDate) && reserve.getEndTime().isAfter(currentTime)))) {
                            reservedSpots++;
                            break;
                        }
                    }
                }
                
                //Porcentaje de plazas ocupadas
                double occupancyPercentage = reservedSpots > 0 ? (reservedSpots * 100.0 / reservedSpots) : 0.0;

                int freeSpots = spots.size() - reservedSpots;
                
                Map<String, Object> stats = Map.of(
                        "parkingName", parking.getName(),
                        "reservedSpots", reservedSpots,
                        "totalSpots", spots.size(),
                        "occupancyPercentage", occupancyPercentage,
                        "freeSpots", freeSpots
                );

                parkingStats.add(stats);
            }
            model.addAttribute("parkingStats", parkingStats);
            return "enterprise-reserved-spots";
        }
        else {
            return "login";
        }

    }

    // Muestra los detalles de un parking
    @GetMapping("/{eid}/parking/{pid}")
    public String parkingDetails(@PathVariable long eid, @PathVariable long pid, Model model, HttpSession session) {
        Enterprise enterprise = (Enterprise) session.getAttribute("u");
        if (enterprise == null || enterprise.getId() != eid) {
            model.addAttribute("error", "Acceso no autorizado");
            return "error";
        }

        Parking parking = entityManager.find(Parking.class, pid);
        if (parking == null || parking.getEnterprise().getId() != eid) {
            model.addAttribute("error", "Parking no válido");
            return "error";
        }

        model.addAttribute("parking", parking);
        model.addAttribute("enterprise", enterprise);
        return "parking-details"; // Devuelve la vista de detalles del parking
    }
    


}
