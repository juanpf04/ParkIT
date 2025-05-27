package es.ucm.fdi.iw.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import es.ucm.fdi.iw.model.*;
import es.ucm.fdi.iw.model.User.Role;
import io.micrometer.common.lang.Nullable;

import java.util.ArrayList;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

    // Simplemente devuelve la vista.
    @GetMapping("/register")
    public String getRegister(Model model) {
        return "register";
    }

    // Ejecuta el post...
    @PostMapping("/register")
    @Transactional
    public String postRegister(Model model, @RequestParam @Nullable String username,
            @RequestParam @Nullable String role, @RequestParam @Nullable String password,
            @RequestParam @Nullable String passwordConfirmation, @RequestParam @Nullable String email,
            @RequestParam @Nullable String telephone, @RequestParam @Nullable String firstName,
            @RequestParam @Nullable String secondName, @RequestParam @Nullable String dni,
            @RequestParam @Nullable String CIF, @RequestParam @Nullable String enterpriseName,
            RedirectAttributes redirectAttributes) {
        
        if (!password.equals(passwordConfirmation)) {
            redirectAttributes.addFlashAttribute("error", "Las contraseñas no coinciden");
            return "redirect:/register";
        }
        List<User> users = entityManager
                .createNamedQuery("User.byUsername", User.class)
                .setParameter("username", username)
                .getResultList();
        
        //Si hay algún user con ese username...
        if (users.size() >0) {
            redirectAttributes.addFlashAttribute("error", "El nombre de usuario ya existe en nuestra base de datos.");
            return "redirect:/register";
        }

        User.Role r = User.Role.valueOf(role);
        if (r == Role.ENTERPRISE) {
            
            Enterprise e = new Enterprise();
            e.setName(enterpriseName);
            e.setUsername(username);
            e.setPassword(passwordEncoder.encode(password));
            e.setEmail(email);
            e.setCIF(CIF);
            e.setTelephone(Integer.parseInt(telephone));
            e.setWallet(0);
            e.setRole(r);
            e.setEnabled(true);

            entityManager.persist(e);
        }
        else if (r== Role.USER) {
            Parker p = new Parker();
            p.setDNI(dni);
            p.setEmail(email);
            p.setFirstName(firstName);
            p.setSecondName(secondName);
            p.setUsername(username);
            p.setPassword(passwordEncoder.encode(password));
            p.setTelephone(Integer.parseInt(telephone));
            p.setRole(r);
            p.setWallet(0);
            p.setEnabled(true);

            entityManager.persist(p);
        }
        
        log.info("Entra en el método post");
        redirectAttributes.addFlashAttribute("success", "Se ha creado el usurario");

        //Debe de redirigir al login si ha ido todo bien
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
