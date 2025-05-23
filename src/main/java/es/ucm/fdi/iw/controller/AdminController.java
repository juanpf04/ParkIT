package es.ucm.fdi.iw.controller;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import es.ucm.fdi.iw.model.Enterprise;
import es.ucm.fdi.iw.model.Message;
import es.ucm.fdi.iw.model.Parking;
import es.ucm.fdi.iw.model.Request;
import es.ucm.fdi.iw.model.Reserve;
import es.ucm.fdi.iw.model.Spot;
import es.ucm.fdi.iw.model.Transferable;
import es.ucm.fdi.iw.model.User;
import es.ucm.fdi.iw.model.Message.Type;
import es.ucm.fdi.iw.model.Admin;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Site administration.
 *
 * Access to this end-point is authenticated - see SecurityConfig
 */
@Controller
@RequestMapping("admin")
public class AdminController {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @ModelAttribute
    public void populateModel(HttpSession session, Model model) {
        for (String name : new String[] { "u", "url", "ws" }) {
            model.addAttribute(name, session.getAttribute(name));
        }
    }

    private static final Logger log = LogManager.getLogger(AdminController.class);

    @GetMapping("/")
    public String index(Model model) {
        return "admin";
    }

    /**
     * Carga la vista de solicitudes de añadir parkings.
     *
     * @param model Modelo para la vista.
     * @return Redirección a la vista de solicitudes de añadir parkings.
     */
    @GetMapping("/request-add")
    public String adminRequestAdd(Model model) {
        List<Request> requests = entityManager
                .createNamedQuery("Request.findByEnabledAndType", Request.class)
                .setParameter("enabled", true)
                .setParameter("type", "AÑADIR")
                .getResultList();

        model.addAttribute("requests", requests);

        return "request-add";
    }

    /**
     * Guarda el parking al aceptar la solicitud de a.
     *
     * @param id ID de la request.
     * @param latitud Latitud del parking.
     * @param longitud Longitud del parking.
     * @param session Sesión HTTP del usuario.
     * @return Actualiza la vista con un modal de éxito o error.
     */
    @PostMapping("/guardarParking/{id}")
    @ResponseBody
    @Transactional
    public ResponseEntity<String> guardarParking(@PathVariable Long id, @RequestParam Double latitud,
            @RequestParam Double longitud, HttpSession session) {
        try {
            Request request = entityManager.createNamedQuery("Request.findById", Request.class)
                    .setParameter("id", id)
                    .getSingleResult();
            if (request == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No se ha encontrado la petición");
            }

            Parking p = new Parking();
            p.setName(request.getName());
            p.setAddress(request.getAddress());
            p.setCity(request.getCity());
            p.setCountry(request.getCountry());
            p.setCp(request.getCp());
            p.setTelephone(request.getTelephone());
            p.setEmail(request.getEmail());
            p.setEnterprise(request.getEnterprise());
            p.setOpeningTime(request.getOpeningTime());
            p.setClosingTime(request.getClosingTime());
            p.setFeePerHour(request.getFeePerHour());
            p.setEnabled(true);
            p.setLatitude(latitud);
            p.setLongitude(longitud);
            p.setWidth(20.0); // Example value
            p.setHeight(10.0); // Example value
            p.setEntryX(1.0); // Example value
            p.setEntryY(1.0); // Example value
            p.setExitX(2.0); // Example value
            p.setExitY(2.0); // Example value
            entityManager.persist(p);

            for (int j = 1; j <= request.getTotalSpots(); j++) { // por ahora las plazas estan en el parking 3
                Spot spot = new Spot();
                // spot.setId(j);
                spot.setParking(p);
                spot.setEnabled(true); // Example value
                spot.setSize(Math.random() > 0.5 ? "M" : "L"); // Example value
                spot.setHorizontal(true); // Example value
                spot.setX(j * 2.5); // Example value
                spot.setY(0.0); // Example value
                entityManager.persist(spot);
            }

            request.setEnabled(false);
            request.setState("Aceptada");
            entityManager.persist(request);
            Admin admin = (Admin) session.getAttribute("u");
            notificarEstadoParking(admin, p, request);

            return ResponseEntity.ok("Parking añadido correctamente");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al añadir el parking: " + e.getMessage());
        }
    }

    /**
     * Notificar si se ha aceptado la solicitud de añadir o eliminar
     *
     * @param admin Administrador que envía el mensaje
     * @param parking Parking que se añade o se elimina
     * @param request Solicitud de añadir o eliminar.
     */
    private void notificarEstadoParking(Admin admin, Parking parking, Request request) {
        Message m = new Message();
        Enterprise enterprise = parking.getEnterprise();
        m.setRecipient(enterprise);
        m.setSender(admin);
        m.setDateSent(LocalDateTime.now());
        m.setType(Type.MOSTRAR);
        if (request.getType().equals("AÑADIR")) {
            m.setText("Se ha aceptado la solicitud de añadir el parking " + parking.getName() + " en la dirección "
                    + parking.getAddress());
        } else {
            m.setText("Se ha aceptado la solicitud de eliminar el parking " + parking.getName() + " en la dirección "
                    + parking.getAddress());
        }

        entityManager.persist(m);
        entityManager.flush(); // to get Id before commit
        ObjectMapper mapper = new ObjectMapper();
        try {
            String json = mapper.writeValueAsString(m.toTransfer());
            messagingTemplate.convertAndSend("/enterprise/" + enterprise.getId() + "/queue/updates", json);
        } catch (JsonProcessingException e) {
            log.error("Error al enviar la notificación", e);
        }
    }

    /**
     * Elimina el parking al aceptar la solicitud de eliminar.
     *
     * @param id ID de la request.
     * @param session Sesión HTTP del usuario.
     * @return Actualiza la vista con un modal de éxito o error.
     */
    @PostMapping("/eliminarParking/{id}")
    @ResponseBody
    @Transactional
    public ResponseEntity<String> eliminarParking(@PathVariable Long id, HttpSession session) {
        try {
            Request request = entityManager.createNamedQuery("Request.findById", Request.class)
                    .setParameter("id", id)
                    .getSingleResult();
            if (request == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No se ha encontrado la petición");
            }

            Parking parking = entityManager.createNamedQuery("Parking.findById", Parking.class)
                    .setParameter("id", request.getIdParking())
                    .getSingleResult();
            if (parking == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No se ha encontrado el parking");
            }

            parking.setEnabled(false);
            entityManager.persist(parking);
            request.setEnabled(false);
            request.setState("Aceptada");
            entityManager.persist(request);
            Admin admin = (Admin) session.getAttribute("u");
            notificarEstadoParking(admin, parking, request);

            return ResponseEntity.ok("Parking eliminado correctamente");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al eliminar el parking: " + e.getMessage());
        }
    }

    /**
     * Elimina la request al rechazarla.
     *
     * @param id ID de la request.
     * @param session Sesión HTTP del usuario.
     * @return Actualiza la vista con un modal de éxito o error.
     */
    @DeleteMapping("/eliminarRequest/{id}")
    @ResponseBody
    @Transactional
    public ResponseEntity<String> eliminarRequest(@PathVariable Long id, HttpSession session) {
        try {
            Request request = entityManager.createNamedQuery("Request.findById", Request.class)
                    .setParameter("id", id)
                    .getSingleResult();
            if (request == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No se ha encontrado la petición");
            }

            request.setEnabled(false);
            request.setState("Rechazada");
            entityManager.persist(request);
            Admin admin = (Admin) session.getAttribute("u");
            notificarEliminarRequest(admin, request);

            return ResponseEntity.ok("Request eliminada correctamente");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al eliminar la request: " + e.getMessage());
        }
    }

    /**
     * Notificar si se ha rechazado la solicitud de añadir o eliminar
     *
     * @param admin Administrador que envía el mensaje
     * @param request Solicitud de añadir o eliminar.
     */
    private void notificarEliminarRequest(Admin admin, Request request) {
        Message m = new Message();
        Enterprise enterprise = request.getEnterprise();
        m.setRecipient(enterprise);
        m.setSender(admin);
        m.setDateSent(LocalDateTime.now());
        m.setType(Type.MOSTRAR);
        if (request.getType().equals("AÑADIR")) {
            m.setText("Se ha rechazado la solicitud de añadir el parking " + request.getName() + " en la dirección "
                    + request.getAddress());
        } else {
            m.setText("Se ha rechazado la solicitud de eliminar el parking " + request.getName() + " en la dirección "
                    + request.getAddress());
        }

        entityManager.persist(m);
        entityManager.flush(); // to get Id before commit
        ObjectMapper mapper = new ObjectMapper();
        try {
            String json = mapper.writeValueAsString(m.toTransfer());
            messagingTemplate.convertAndSend("/enterprise/" + enterprise.getId() + "/queue/updates", json);
        } catch (JsonProcessingException e) {
            log.error("Error al enviar la notificación", e);
        }
    }

    /**
     * Carga la vista de solicitudes de eliminar parkings.
     *
     * @param model Modelo para la vista.
     * @return Redirección a la vista de solicitudes de eliminar parkings.
     */
    @GetMapping("/request-delete")
    public String adminRequestDelete(Model model) {
        List<Request> requests = entityManager
                .createNamedQuery("Request.findByEnabledAndType", Request.class)
                .setParameter("enabled", true)
                .setParameter("type", "ELIMINAR")
                .getResultList();

        model.addAttribute("requests", requests);

        return "request-delete";
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
}
