package es.ucm.fdi.iw.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import es.ucm.fdi.iw.model.*;
import io.micrometer.common.lang.Nullable;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;

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

    @PostMapping("/register")
    @Transactional
    public String registerPost(Model model,
            @RequestParam @Nullable String role,
            @RequestParam @Nullable String username,
            @RequestParam @Nullable String password,
            @RequestParam @Nullable String confirmPassword,
            @RequestParam @Nullable String email,
            @RequestParam @Nullable String telefono,
            @RequestParam @Nullable String dni,
            @RequestParam @Nullable String nombre,
            @RequestParam @Nullable String apellidos,
            @RequestParam @Nullable String cif,
            @RequestParam @Nullable String nombreEmpresa,
            RedirectAttributes redirectAttributes) {

        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Las contraseñas no coinciden.");
            return "redirect:/register";
        }

        List<User> users = entityManager.createNamedQuery("User.byUsername", User.class)
                .setParameter("username", username).getResultList();

        if (!users.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "El nombre de usuario ya está en uso.");
            return "redirect:/register";
        }

        User.Role r = User.Role.valueOf(role);
        switch (r) {
            case USER:
                Parker parker = new Parker();
                parker.setUsername(username);
                parker.setEnabled(true);
                parker.setPassword(passwordEncoder.encode(password));
                parker.setRole(r);
                parker.setWallet(0.0);
                parker.setEmail(email);
                parker.setTelephone(Integer.parseInt(telefono));
                parker.setDNI(dni);
                parker.setFirstName(nombre);
                parker.setSecondName(apellidos);
                entityManager.persist(parker);
                break;

            case ENTERPRISE:
                Enterprise enterprise = new Enterprise();
                enterprise.setUsername(username);
                enterprise.setEnabled(true);
                enterprise.setPassword(passwordEncoder.encode(password));
                enterprise.setRole(r);
                enterprise.setWallet(0.0);
                enterprise.setEmail(email);
                enterprise.setTelephone(Integer.parseInt(telefono));
                enterprise.setCIF(cif);
                enterprise.setName(nombreEmpresa);
                entityManager.persist(enterprise);
                break;

            default:
                break;
        }

        redirectAttributes.addFlashAttribute("success", "Usuario registrado correctamente");

        return "redirect:/login";
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
