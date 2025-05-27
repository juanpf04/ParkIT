package es.ucm.fdi.iw.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import io.micrometer.common.lang.Nullable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import es.ucm.fdi.iw.model.*;
import es.ucm.fdi.iw.model.Message.Type;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import jakarta.transaction.Transactional;

/**
 * Non-authenticated requests only.
 */
@Controller
public class RootController {

    private static final Logger log = LogManager.getLogger(RootController.class);

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @ModelAttribute
    public void populateModel(HttpSession session, Model model) {
        for (String name : new String[] { "u", "url", "ws" }) {
            model.addAttribute(name, session.getAttribute(name));
        }
    }

    @GetMapping("/login")
    public String login(Model model, HttpServletRequest request) {
        boolean error = request.getQueryString() != null && request.getQueryString().indexOf("error") != -1;
        model.addAttribute("loginError", error);
        return "login";
    }

    @GetMapping("/register")
    public String register(Model model) {
        return "register";
    }

    @PostMapping("confirm-register")
    @Transactional
    public String postRegister(Model model,
            @RequestParam @Nullable String username,
            @RequestParam @Nullable String password,
            @RequestParam @Nullable String repeatPassword,
            @RequestParam @Nullable String role,
            @RequestParam @Nullable String telephone,
            @RequestParam @Nullable String email,
            @RequestParam @Nullable String dni,
            @RequestParam @Nullable String firstName,
            @RequestParam @Nullable String secondName,
            @RequestParam @Nullable String name,
            @RequestParam @Nullable String cif,
            RedirectAttributes redirectAttributes) {

        if (!password.equals(repeatPassword)) {
            redirectAttributes.addFlashAttribute("error", "Las contraseñas no coinciden");
            return "redirect:/register";
        }

        List<User> users = entityManager.createNamedQuery("User.byUsername", User.class)
                .setParameter("username", username)
                .getResultList();

        if (users.size() > 0) {
            redirectAttributes.addFlashAttribute("error", "Nombre de usuario existente");
            return "redirect:/register";
        }

        User.Role r = User.Role.valueOf(role);
        switch (r) {
            case ENTERPRISE:
                Enterprise enterprise = new Enterprise();

                enterprise.setUsername(username);
                enterprise.setPassword(passwordEncoder.encode(password));
                enterprise.setEnabled(true);
                enterprise.setRole(r);
                enterprise.setWallet(0);
                enterprise.setTelephone(Integer.parseInt(telephone));
                enterprise.setEmail(email);
                enterprise.setName(name);
                enterprise.setCIF(cif);
                
                entityManager.persist(enterprise);

                notificarRegistro(enterprise);
                
                break;

            case USER:
                Parker parker = new Parker();

                parker.setUsername(username);
                parker.setPassword(passwordEncoder.encode(password));
                parker.setEnabled(true);
                parker.setRole(r);
                parker.setWallet(0);
                parker.setTelephone(Integer.parseInt(telephone));
                parker.setEmail(email);
                parker.setDNI(dni);
                parker.setFirstName(firstName);
                parker.setSecondName(secondName);

                entityManager.persist(parker);

                notificarRegistro(parker);
                
                break;

            default:
                break;
        }

        

        redirectAttributes.addFlashAttribute("success", "Usuario registrado con exito!");
        
        return "redirect:/login";
    }

    private void notificarRegistro(User user) {
		try {
			Message m = new Message();
			m.setRecipient(null);
			m.setSender(user);
			m.setDateSent(LocalDateTime.now());
			
			ObjectMapper mapper = new ObjectMapper();
			// Ejemplo de pasar un JSON como cuerpo del mensaje
			m.setText(mapper.writeValueAsString(user.toTransfer()));
			m.setType(Type.ACTUALIZAR_TABLA_ADMIN);
			entityManager.persist(m);
			entityManager.flush(); 
			String json = mapper.writeValueAsString(m.toTransfer());
			messagingTemplate.convertAndSend("/topic/admin", json);
		} catch (JsonProcessingException e) {
			log.error("Error al enviar la notificación de registro", e);
		}
	}

    @GetMapping("/")
    public String index(Model model) {
        return "index";
    }

    @GetMapping("/info")
    public String info(Model model) {
        return "info";
    }

    @GetMapping("/help")
    public String ayuda(Model model) {
        return "help";
    }

}
