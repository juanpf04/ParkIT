package es.ucm.fdi.iw.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import es.ucm.fdi.iw.model.*;
import io.micrometer.common.lang.Nullable;

import java.beans.Transient;
import java.util.ArrayList;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;

/**
 *  Non-authenticated requests only.
 */
@Controller
public class RootController {

    private final EnterpriseController enterpriseController;

    private static final Logger log = LogManager.getLogger(RootController.class);

    @Autowired
	private EntityManager entityManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    RootController(EnterpriseController enterpriseController) {
        this.enterpriseController = enterpriseController;
    }

    @ModelAttribute
    public void populateModel(HttpSession session, Model model) {        
        for (String name : new String[] {"u", "url", "ws"}) {
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
    public String getRegister(Model model) {
        return "register";
    }

    @PostMapping("/register")
    @Transactional
    public String postRegister(Model model, 
            @RequestParam @Nullable String username,
			@RequestParam @Nullable String password,
			@RequestParam @Nullable String repeatPassword,
            @RequestParam @Nullable String role,
            @RequestParam @Nullable String telephone,
            @RequestParam @Nullable String email,
            @RequestParam @Nullable String DNI,
            @RequestParam @Nullable String firstName,
            @RequestParam @Nullable String secondName,
            @RequestParam @Nullable String name,
            @RequestParam @Nullable String CIF,
            RedirectAttributes redirectAttributes) {

        if(!password.equals(repeatPassword)) {
            redirectAttributes.addFlashAttribute("error", "Las contraseñas no coinciden");
            return "redirect:/register";
        }
        
        List<User> users = entityManager.createNamedQuery("User.byUsername", User.class)
                .setParameter("username", username)
                .getResultList();

        if (users.size() > 0) {
            redirectAttributes.addFlashAttribute("error", "Usuario existente");
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
                enterprise.setCIF(CIF);
                
                entityManager.persist(enterprise);
                
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
                parker.setDNI(DNI);
                parker.setFirstName(firstName);
                parker.setSecondName(secondName);

                entityManager.persist(parker);
                
                break;

            default:
                break;
        }

        redirectAttributes.addFlashAttribute("success", "Usuario registrado con exito!");
        
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
