package es.ucm.fdi.iw.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.security.crypto.password.PasswordEncoder;

import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.TransactionScoped;
import jakarta.transaction.Transactional;
import es.ucm.fdi.iw.model.*;
import es.ucm.fdi.iw.model.User.Role;

import java.lang.ProcessBuilder.Redirect;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


/**
 *  Non-authenticated requests only.
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
        for (String name : new String[] {"u", "url", "ws"}) {
            model.addAttribute(name, session.getAttribute(name));
        }
    }

	@GetMapping("/login")
    public String login(Model model, HttpServletRequest request,
    @RequestParam(required = false) String RegisterSuccess) {
        boolean error = request.getQueryString() != null && request.getQueryString().indexOf("error") != -1;
        model.addAttribute("loginError", error);
        if (RegisterSuccess != null) {
            model.addAttribute("success", true);
        }
        return "login";
    }

    @GetMapping("/register")
    public String register(Model model, HttpServletRequest request) {
        boolean error = request.getQueryString() != null && request.getQueryString().indexOf("error") != -1;
        model.addAttribute("loginError", error);
        return "register";
    }

    @PostMapping("/postRegister")
    @Transactional
    @ResponseBody
    public ResponseEntity<Map<String,String>> postRegister(@RequestParam Map<String, String> params) {
        String rol= params.get("rol");
        Map<String, String> errores = new HashMap<>();

        if(rol.equals("PARKER")) {
            List<User> users = entityManager.createNamedQuery("User.byUsername", User.class)
                .setParameter("username", params.get("parkerUserName"))
                .getResultList();
            if (!users.isEmpty()) {
                errores.put("parkerUserName", "El nombre de usuario ya está en uso.");
                return ResponseEntity.badRequest().body(errores);
            }
            //check dni
            List<Parker>parkers = entityManager.createNamedQuery("negocio.Parker.findByDNI", Parker.class)
                .setParameter("DNI", params.get("parkerDNI"))
                .getResultList();
            if (!parkers.isEmpty()) {
                errores.put("parkerDNI", "El DNI ya está en uso.");
                return ResponseEntity.badRequest().body(errores);
            }

            Parker parker = new Parker();
            parker.setUsername(params.get("parkerUserName"));
            parker.setFirstName(params.get("parkerName"));
            parker.setSecondName(params.get(("parkerSecondName")));
            parker.setEmail(params.get("parkerEmail"));
            parker.setPassword(passwordEncoder.encode(params.get("parkerPassword")));
            parker.setDNI(params.get("parkerDNI"));
            parker.setTelephone(Integer.parseInt(params.get("parkerPhone")));
            parker.setRole(Role.USER);
            parker.setWallet(0.0);
            parker.setEnabled(true);
            entityManager.persist(parker);
        } else {
            List<User> users = entityManager.createNamedQuery("User.byUsername", User.class)
                .setParameter("username", params.get("enterpriseUserName"))
                .getResultList();
            if (!users.isEmpty()) {
                errores.put("enterpriseUserName", "El nombre de usuario ya está en uso.");
                return ResponseEntity.badRequest().body(errores);
            }
            Enterprise enterprise = new Enterprise();
            enterprise.setUsername(params.get("enterpriseUserName"));
            enterprise.setName(params.get("enterpriseName"));
            enterprise.setCIF(params.get("enterpriseCIF"));
            enterprise.setTelephone(Integer.parseInt(params.get("enterprisePhone")));
            enterprise.setEmail(params.get("enterpriseEmail"));
            enterprise.setPassword(passwordEncoder.encode(params.get("enterprisePassword")));
            enterprise.setRole(Role.ENTERPRISE);
            enterprise.setWallet(0.0);
            enterprise.setEnabled(true);
            entityManager.persist(enterprise);
        }
        return ResponseEntity.ok(Map.of("status", "ok"));
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
