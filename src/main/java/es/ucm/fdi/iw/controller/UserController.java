package es.ucm.fdi.iw.controller;

import es.ucm.fdi.iw.AppConfig;
import es.ucm.fdi.iw.LocalData;
import es.ucm.fdi.iw.model.Enterprise;
import es.ucm.fdi.iw.model.Message;
import es.ucm.fdi.iw.model.Parking;
import es.ucm.fdi.iw.model.Parking.Transfer;
import es.ucm.fdi.iw.model.Parker;
import es.ucm.fdi.iw.model.Reserve;
import es.ucm.fdi.iw.model.Spot;
import es.ucm.fdi.iw.model.Transferable;
import es.ucm.fdi.iw.model.User;
import es.ucm.fdi.iw.model.Vehicle;
import es.ucm.fdi.iw.model.Message.Type;
import es.ucm.fdi.iw.model.User.Role;
import io.micrometer.common.lang.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.method.P;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.ArrayList;

//Importame el transfer

/**
 * User management.
 *
 * Access to this end-point is authenticated.
 */
@Controller()
@RequestMapping("user")
public class UserController {

	private final AppConfig appConfig;

	private final AdminController adminController;

	private static final Logger log = LogManager.getLogger(UserController.class);

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private LocalData localData;

	@Autowired
	private SimpMessagingTemplate messagingTemplate;

	@Autowired
	private PasswordEncoder passwordEncoder;

	UserController(AdminController adminController, AppConfig appConfig) {
		this.adminController = adminController;
		this.appConfig = appConfig;
	}

	@ModelAttribute
	public void populateModel(HttpSession session, Model model) {

		for (String name : new String[] { "u", "url", "ws" }) {
			model.addAttribute(name, session.getAttribute(name));
		}
	}

	private boolean isParker(HttpSession session) {
		Parker parker = (Parker) session.getAttribute("u");
		return parker != null;
	}

	/**
	 * Muestra un mapa con parkings disponibles según fechas, horas y ubicación.
	 *
	 * @param startDate Fecha de inicio de la reserva.
	 * @param endDate   Fecha de fin de la reserva.
	 * @param startTime Hora de inicio de la reserva .
	 * @param endTime   Hora de fin de la reserva.
	 * @param latitude  Latitud para filtrar parkings.
	 * @param longitude Longitud para filtrar parkings.
	 * @param radius    Radio de búsqueda en metros .
	 * @param session   Sesión HTTP del usuario.
	 * @param model     Modelo para la vista.
	 * @return Nombre de la vista "map" o "login" si no está autenticado como
	 *         Parker.
	 */
	@GetMapping("/map")
	public String map(
			@RequestParam @Nullable LocalDate startDate, @RequestParam @Nullable LocalDate endDate,
			@RequestParam @Nullable LocalTime startTime, @RequestParam @Nullable LocalTime endTime,
			@RequestParam @Nullable String latitude,
			@RequestParam @Nullable String longitude,
			@RequestParam @Nullable String radius,
			HttpSession session,
			Model model) {

		if (isParker(session)) {
			List<Parking> parkings = entityManager.createNamedQuery("Parking.findByEnabled", Parking.class)
					.setParameter("enabled", true).getResultList();

			LocalDate today = LocalDate.now();
			LocalTime timeNow = LocalTime.now();

			log.info("generando transfers de parkings");
			List<Transfer> transferParkings = new ArrayList<>();

			if (startDate != null && endDate != null && startTime != null && endTime != null
					&& (startDate.isAfter(today) || startDate.isEqual(today) && startTime.isAfter(timeNow))) {
				for (Parking p : parkings) {
					log.info("comprobando parking " + p.getName());
					List<Spot> spots = p.getSpots();
					List<Reserve> reserves = new ArrayList<>();
					for (Spot s : spots) {
						reserves.addAll(s.getReserves());
					}
					if (reserves.size() == 0) {
						transferParkings.add(p.toTransfer());
					} else {
						for (Reserve r : reserves) {
							if ((r.getStartDate().isBefore(endDate) && r.getEndDate().isAfter(startDate)) ||
									(r.getStartDate().isEqual(startDate) && r.getStartTime().isBefore(endTime)) ||
									(r.getEndDate().isEqual(endDate) && r.getEndTime().isAfter(startTime))) {
								log.info("reservado -> no se añade");
							} else {
								log.info("no reservado -> se añade");
								transferParkings.add(p.toTransfer());
								break;
							}
						}
					}

				}
			}

			model.addAttribute("parkings", transferParkings);
			model.addAttribute("latitude", latitude);
			model.addAttribute("longitude", longitude);
			model.addAttribute("startDate", startDate);
			model.addAttribute("endDate", endDate);
			model.addAttribute("startTime", startTime);
			model.addAttribute("endTime", endTime);
			model.addAttribute("radius", radius);

			return "map";
		} else {
			return "login";
		}
	}

	/**
	 * Muestra la página para reservar una plaza en un parking específico.
	 *
	 * @param model        Modelo para la vista.
	 * @param session      Sesión HTTP del usuario.
	 * @param id           ID del parking.
	 * @param selectedSlot Plaza seleccionada (opcional).
	 * @param vehicleId    ID del vehículo seleccionado (opcional).
	 * @param startDate    Fecha de inicio de la reserva (opcional).
	 * @param endDate      Fecha de fin de la reserva (opcional).
	 * @param startTime    Hora de inicio de la reserva (opcional).
	 * @param endTime      Hora de fin de la reserva (opcional).
	 * @return Nombre de la vista "reserve" o "error" si el parking no es válido.
	 */
	@GetMapping("/reserve/{id}")
	public String reserve(Model model,
			HttpSession session,
			@PathVariable long id,
			@RequestParam(required = false) Integer selectedSlot,
			@RequestParam(required = false) Long vehicleId,
			@RequestParam @Nullable String startDate, @RequestParam @Nullable String endDate,
			@RequestParam @Nullable String startTime, @RequestParam @Nullable String endTime) {

		Parking parking = entityManager.find(Parking.class, id);
		if (parking == null) {
			model.addAttribute("erro", "Parking no válido");
			return "error";
		}
		Parker parker = (Parker) session.getAttribute("u");
		log.info("realizando reserva de parker" + parker.getId());
		List<Vehicle> vehicles = entityManager
				.createQuery("SELECT v FROM Vehicle v WHERE v.parker.id = :parkerId", Vehicle.class)
				.setParameter("parkerId", parker.getId())
				.getResultList();

		model.addAttribute("parking", parking.toTransfer());
		model.addAttribute("vehicles", vehicles);
		model.addAttribute("startDate", startDate);
		model.addAttribute("endDate", endDate);
		model.addAttribute("startTime", startTime);
		model.addAttribute("endTime", endTime);
		model.addAttribute("id", id);
		model.addAttribute("vehicleId", vehicleId);
		model.addAttribute("selectedSlot", selectedSlot);

		return "reserve";
	}

	/**
	 * Muestra la página para seleccionar una plaza de parking.
	 *
	 * @param id           ID del parking.
	 * @param selectedSlot Plaza seleccionada.
	 * @param vehicleId    ID del vehículo seleccionado.
	 * @param startDate    Fecha de inicio de la reserva.
	 * @param endDate      Fecha de fin de la reserva.
	 * @param startTime    Hora de inicio de la reserva.
	 * @param endTime      Hora de fin de la reserva.
	 * @param model        Modelo para la vista.
	 * @return Nombre de la vista "select-parking.html".
	 */
	@GetMapping("/select-parking/{id}")
	public String selectParkingView(@PathVariable long id,
			@RequestParam(required = false) Integer selectedSlot,
			@RequestParam(required = false) Long vehicleId,
			@RequestParam @Nullable LocalDate startDate, @RequestParam @Nullable LocalDate endDate,
			@RequestParam @Nullable LocalTime startTime, @RequestParam @Nullable LocalTime endTime,
			Model model) {
		List<Integer> occupiedSpots = new ArrayList<>();
		List<Integer> spotsList = new ArrayList<>();
		List<List<Integer>> spotsListFormat = new ArrayList<>();
		int bloqueSize = 10;
		Parking parking = entityManager.find(Parking.class, id);
		if (parking != null) {
			List<Spot> spots = parking.getSpots();
			List<Reserve> reserves = new ArrayList<>();
			for (Spot s : spots) {
				spotsList.add((int) s.getId());
				reserves.addAll(s.getReserves());
			}
			for (int i = 0; i < spotsList.size(); i += bloqueSize) {
				int fin = Math.min(i + bloqueSize, spotsList.size());
				spotsListFormat.add(spotsList.subList(i, fin));
			}
			for (Reserve r : reserves) {
				if ((r.getStartDate().isBefore(endDate) && r.getEndDate().isAfter(startDate)) ||
						(r.getStartDate().isEqual(startDate) && r.getStartTime().isBefore(endTime)) ||
						(r.getEndDate().isEqual(endDate) && r.getEndTime().isAfter(startTime))) {
					Integer spotId = Integer.valueOf((int) r.getSpot().getId());
					occupiedSpots.add(spotId);
				}
			}
		}
		model.addAttribute("spots", spotsListFormat);
		model.addAttribute("occupiedSpots", occupiedSpots);
		model.addAttribute("startDate", startDate);
		model.addAttribute("endDate", endDate);
		model.addAttribute("startTime", startTime);
		model.addAttribute("endTime", endTime);
		model.addAttribute("id", id);
		model.addAttribute("selectedSlot", selectedSlot);
		model.addAttribute("vehicleId", vehicleId);
		return "select-parking";
	}

	/**
	 * Añade un vehículo al usuario y redirige a la página de reserva.
	 *
	 * @param model              Modelo para la vista.
	 * @param session            Sesión HTTP del usuario.
	 * @param parkingId          ID del parking (opcional).
	 * @param selectedSlot       Plaza seleccionada (opcional).
	 * @param vehicleId          ID del vehículo seleccionado (opcional).
	 * @param startDate          Fecha de inicio de la reserva (opcional).
	 * @param endDate            Fecha de fin de la reserva (opcional).
	 * @param startTime          Hora de inicio de la reserva (opcional).
	 * @param endTime            Hora de fin de la reserva (opcional).
	 * @param brand              Marca del vehículo.
	 * @param modelo             Modelo del vehículo.
	 * @param plate              Matrícula del vehículo.
	 * @param size               Tamaño del vehículo.
	 * @param redirectAttributes Atributos para la redirección.
	 * @return Nombre de la vista "reserve" o "login" si no está autenticado como
	 *         Parker.
	 */
	@GetMapping("/add-vehicle")
	public String addVehicle(
			Model model,
			HttpSession session,
			@RequestParam(required = false) String parkingId,
			@RequestParam(required = false) Integer selectedSlot,
			@RequestParam(required = false) Long vehicleId,
			@RequestParam @Nullable String startDate, @RequestParam @Nullable String endDate,
			@RequestParam @Nullable String startTime, @RequestParam @Nullable String endTime,
			@RequestParam String brand,
			@RequestParam String modelo,
			@RequestParam String plate,
			@RequestParam String size,
			RedirectAttributes redirectAttributes) {
		Long id = Long.parseLong(parkingId);
		// redirectAttributes.addAttribute("selectedSlot", selectedSlot);
		// redirectAttributes.addAttribute("startDate", startDate);
		// redirectAttributes.addAttribute("endDate", endDate);
		// redirectAttributes.addAttribute("startTime", startTime);
		// redirectAttributes.addAttribute("endTime", endTime);
		// redirectAttributes.addAttribute("id", id);
		// redirectAttributes.addAttribute("vehicleId", vehicleId);
		if (isParker(session)) {
			Parker parker = (Parker) session.getAttribute("u");
			List<Vehicle> vehicles = parker.getVehicles();
			Vehicle v = new Vehicle();
			v.setBrand(brand);
			v.setEnabled(true);
			v.setModel(modelo);
			v.setPlate(plate);
			v.setSize(size);
			v.setParker(parker);
			vehicles.add(v);
			parker.setVehicles(vehicles);
			entityManager.persist(v);
			return reserve(model, session, id, selectedSlot, vehicleId, startDate, endDate, startTime, endTime);
		} else
			return "login";
	}

	/**
	 * Notifica a la empresa sobre una nueva reserva mediante un mensaje.
	 *
	 * @param user    Usuario que realiza la reserva.
	 * @param reserve Reserva realizada.
	 * @param parking Parking asociado a la reserva.
	 * @throws JsonProcessingException Si ocurre un error al serializar el mensaje a
	 *                                 JSON.
	 */
	private void notificarReserva(User user, Reserve reserve, Parking parking) {
		try {
			Message m = new Message();
			Enterprise enterprise = parking.getEnterprise();
			m.setRecipient(enterprise);
			m.setSender(user);
			m.setDateSent(LocalDateTime.now());

			ObjectMapper mapper = new ObjectMapper();
			// Ejemplo de pasar un JSON como cuerpo del mensaje
			m.setText(mapper.writeValueAsString(reserve.toTransfer()));
			System.out.println(m.getText());
			m.setType(Type.ACTUALIZAR);
			entityManager.persist(m);
			entityManager.flush(); // to get Id before commit
			String json = mapper.writeValueAsString(m.toTransfer());
			messagingTemplate.convertAndSend("/enterprise/" + enterprise.getId() + "/queue/updates", json);
		} catch (JsonProcessingException e) {
			log.error("Error al enviar la notificación de reserva", e);
		}
	}

	/**
	 * Procesa la creación de una reserva y actualiza la cartera del usuario.
	 *
	 * @param startDate           Fecha de inicio de la reserva.
	 * @param endDate             Fecha de fin de la reserva.
	 * @param startTime           Hora de inicio de la reserva.
	 * @param endTime             Hora de fin de la reserva.
	 * @param vehicleId           ID del vehículo seleccionado.
	 * @param parkingId           ID del parking.
	 * @param totalPrice          Precio total de la reserva.
	 * @param selectedParkingSpot ID de la plaza seleccionada.
	 * @param model               Modelo para la vista.
	 * @param redirectAttributes  Atributos para la redirección.
	 * @return Nombre de la vista "my-reserves" o redirección a "reserve" o "error"
	 *         en caso de error.
	 */
	@PostMapping("/confirm-reserve")
	@Transactional
	public String postReserve(
			@ModelAttribute("startDate") LocalDate startDate,
			@ModelAttribute("endDate") LocalDate endDate,
			@ModelAttribute("startTime") LocalTime startTime,
			@ModelAttribute("endTime") LocalTime endTime,
			@RequestParam("vehicleId") Long vehicleId,
			@RequestParam("parkingId") Long parkingId,
			@RequestParam("totalPrice") Double totalPrice,
			@RequestParam("selectedParkingSpot") Integer selectedParkingSpot,
			Model model,
			RedirectAttributes redirectAttributes) {

		User user = (User) model.getAttribute("u");
		User target = entityManager.find(User.class, user.getId());
		if (!(user instanceof Parker parker)) {
			redirectAttributes.addFlashAttribute("error", "No eres un parker válido.");
			return "redirect:/error";
		}

		if (startDate == null || endDate == null || startTime == null || endTime == null || vehicleId == null
				|| totalPrice == null) {
			redirectAttributes.addFlashAttribute("error", "Faltan campos por rellenar");
			return "redirect:/user/reserve/" + parkingId;
		}
		if (startDate.isAfter(endDate) || (startDate.isEqual(endDate) && startTime.isAfter(endTime))) {
			redirectAttributes.addFlashAttribute("error", "La fecha de inicio no puede ser posterior a la de fin");
			return "redirect:/user/reserve/" + parkingId;
		}

		Vehicle vehicle = entityManager.find(Vehicle.class, vehicleId);
		if (vehicle == null) {
			redirectAttributes.addFlashAttribute("error", "Vehículo no válido");
			return "redirect:/user/reserve/" + parkingId;

		}
		Long spotId = selectedParkingSpot.longValue();
		Spot spot = entityManager.find(Spot.class, spotId);
		if (spot == null) {
			redirectAttributes.addFlashAttribute("error", "Plaza no válida");
			return "redirect:/user/reserve/" + parkingId;
		}

		List<Reserve> reservas = entityManager
				.createQuery("SELECT r FROM Reserve r WHERE r.spot = :spot", Reserve.class)
				.setParameter("spot", spot)
				.getResultList();
		for (Reserve r : reservas) {
			if ((r.getStartDate().isBefore(endDate) && r.getEndDate().isAfter(startDate)) ||
					(r.getStartDate().isEqual(startDate) && r.getStartTime().isBefore(endTime)) ||
					(r.getEndDate().isEqual(endDate) && r.getEndTime().isAfter(startTime))) {

				redirectAttributes.addFlashAttribute("error", "No se puede reservar esta plaza en esas fechas y horas");
				return "redirect:/user/reserve/" + parkingId;
			}
		}

		Reserve reserve = new Reserve();
		reserve.setStartDate(startDate);
		reserve.setEndDate(endDate);
		reserve.setStartTime(startTime);
		reserve.setEndTime(endTime);
		reserve.setPrice(totalPrice);
		reserve.setState(Reserve.State.PENDING);
		reserve.setSpot(spot);
		reserve.setVehicle(vehicle);

		try {
			entityManager.persist(reserve);
			entityManager.flush();
			entityManager.clear();
			double wallet = user.getWallet();
			wallet -= totalPrice;
			user.setWallet(wallet);
			User userBD = entityManager.find(User.class, user.getId());
			userBD.setWallet(wallet);
			// avisamos a la empresa
			notificarReserva(target, reserve, spot.getParking());
			model.addAttribute("success", "Reserva realizada con éxito");
		} catch (Exception e) {
			model.addAttribute("error", "Hubo un error al guardar la reserva: " + e.getMessage());
			return "redirect:/error";
		}

		return myReserves(model);
	}

	/**
	 * Muestra la página para modificar una reserva.
	 *
	 * @param model Modelo para la vista.
	 * @return Nombre de la vista "modify-reserve".
	 */
	@GetMapping("/modify-reserve")
	public String modifyReserve(Model model) {
		return "modify-reserve";
	}

	/**
	 * Muestra las reservas del usuario autenticado.
	 *
	 * @param model Modelo para la vista.
	 * @return Nombre de la vista "my-reserves" o "error" si no es un Parker.
	 */
	@GetMapping("/my-reserves")
	public String myReserves(Model model) {

		User user = (User) model.getAttribute("u");
		List<Reserve> reservas = new ArrayList<>();
		if (user instanceof Parker) {
			Parker parker = (Parker) user;
			List<Vehicle> vehicles = entityManager
					.createQuery("SELECT v FROM Vehicle v WHERE v.parker = :parker", Vehicle.class)
					.setParameter("parker", parker)
					.getResultList();

			for (Vehicle v : vehicles) {
				List<Reserve> r = entityManager
						.createQuery("SELECT r FROM Reserve r WHERE r.vehicle = :vehicle", Reserve.class)
						.setParameter("vehicle", v)
						.getResultList();
				reservas.addAll(r);
			}

			model.addAttribute("reservas", reservas);
		} else {
			model.addAttribute("error", "No eres un parker válido.");
			return "error";
		}

		return "my-reserves";
	}

	/**
	 * Cancela una reserva y actualiza las carteras del usuario y la empresa.
	 *
	 * @param id    ID de la reserva a cancelar.
	 * @param model Modelo para la vista.
	 * @return Nombre de la vista "my-reserves" o "error" si la reserva no es
	 *         válida.
	 */
	@PostMapping("/cancel-reserve/{id}")
	@Transactional
	public String cancelReserve(@PathVariable long id, Model model) {
		Reserve reserve = entityManager.find(Reserve.class, id);
		if (reserve != null && reserve.getState() == Reserve.State.CONFIRMED) {
			// Actualizamos el saldo tanto en la base de datos como en la sesión
			User user = reserve.getVehicle().getParker();
			User sessionUser = (User) model.getAttribute("u");

			if (user == null || sessionUser == null) {
				model.addAttribute("error", "Usuario no válido");
				return "error";
			}

			double userWallet = user.getWallet();
			double price = reserve.getPrice();

			reserve.setState(Reserve.State.CANCELLED);
			user.setWallet(userWallet + price);
			sessionUser.setWallet(userWallet + price);

			Enterprise enterprise = reserve.getSpot().getParking().getEnterprise();
			double enterpriseWallet = enterprise.getWallet();
			enterprise.setWallet(enterpriseWallet - price);

			notificarCancelacionReserva(user, reserve, enterprise);

			model.addAttribute("success", "Reserva cancelada con éxito");
		} else {
			model.addAttribute("error", "Reserva ya canelada o no válida");
		}
		return myReserves(model);
	}

	/**
	 * Notifica a la empresa sobre la cancelación de una reserva.
	 *
	 * @param user       Usuario que cancela la reserva.
	 * @param reserve    Reserva cancelada.
	 * @param enterprise Empresa asociada al parking.
	 * @throws JsonProcessingException Si ocurre un error al serializar el mensaje a
	 *                                 JSON.
	 */
	private void notificarCancelacionReserva(User user, Reserve reserve, Enterprise enterprise) {
		Message m = new Message();
		m.setRecipient(enterprise);
		m.setSender(user);
		m.setDateSent(LocalDateTime.now());
		m.setText("El usuario " + user.getUsername() + "ha cancelado una reserva en "
				+ reserve.getSpot().getParking().getName() + " desde "
				+ reserve.getStartDate() + " a " + reserve.getEndDate() + " de " + reserve.getStartTime() + " a "
				+ reserve.getEndTime());
		entityManager.persist(m);
		entityManager.flush(); // to get Id before commit
		ObjectMapper mapper = new ObjectMapper();
		try {
			String json = mapper.writeValueAsString(m.toTransfer());
			messagingTemplate.convertAndSend("/enterprise/" + enterprise.getId() + "/queue/updates", json);
		} catch (JsonProcessingException e) {
			log.error("Error al enviar la notificación de reserva", e);
		}
	}

	/**
	 * Exception to use when denying access to unauthorized users.
	 * 
	 * In general, admins are always authorized, but users cannot modify
	 * each other's profiles.
	 */
	@ResponseStatus(value = HttpStatus.FORBIDDEN, reason = "No eres administrador, y éste no es tu perfil") // 403
	public static class NoEsTuPerfilException extends RuntimeException {
	}

	/**
	 * Encodes a password, so that it can be saved for future checking. Notice
	 * that encoding the same password multiple times will yield different
	 * encodings, since encodings contain a randomly-generated salt.
	 * 
	 * @param rawPassword to encode
	 * @return the encoded password (typically a 60-character string)
	 *         for example, a possible encoding of "test" is
	 *         {bcrypt}$2y$12$XCKz0zjXAP6hsFyVc8MucOzx6ER6IsC1qo5zQbclxhddR1t6SfrHm
	 */
	public String encodePassword(String rawPassword) {
		return passwordEncoder.encode(rawPassword);
	}

	/**
	 * Generates random tokens. From https://stackoverflow.com/a/44227131/15472
	 * 
	 * @param byteLength
	 * @return
	 */
	public static String generateRandomBase64Token(int byteLength) {
		SecureRandom secureRandom = new SecureRandom();
		byte[] token = new byte[byteLength];
		secureRandom.nextBytes(token);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(token); // base64 encoding
	}

	/**
	 * Muestra la página de perfil de un usuario.
	 *
	 * @param id      ID del usuario.
	 * @param model   Modelo para la vista.
	 * @param session Sesión HTTP del usuario.
	 * @return Nombre de la vista "user".
	 */
	@GetMapping("{id}")
	public String index(@PathVariable long id, Model model, HttpSession session) {
		User target = entityManager.find(User.class, id);
		// Parker parker = entityManager.find(Parker.class, id);
		model.addAttribute("user", target);
		return "user";
	}

	/**
	 * Crea o modifica un usuario.
	 *
	 * @param response Respuesta HTTP.
	 * @param id       ID del usuario a modificar (-1 para crear uno nuevo).
	 * @param edited   Objeto con los datos editados del usuario.
	 * @param pass2    Confirmación de la contraseña (opcional).
	 * @param model    Modelo para la vista.
	 * @param session  Sesión HTTP del usuario.
	 * @return Nombre de la vista "user".
	 * @throws IOException           Si ocurre un error de entrada/salida.
	 * @throws NoEsTuPerfilException Si el usuario no tiene permisos para modificar
	 *                               el perfil.
	 */
	@PostMapping("/{id}")
	@Transactional
	public String postUser(
			HttpServletResponse response,
			@PathVariable long id,
			@ModelAttribute User edited,
			@RequestParam(required = false) String pass2,
			Model model, HttpSession session) throws IOException {

		User requester = (User) session.getAttribute("u");
		User target = null;
		if (id == -1 && requester.hasRole(Role.ADMIN)) {
			// create new user with random password
			target = new User();
			target.setPassword(encodePassword(generateRandomBase64Token(12)));
			target.setEnabled(true);
			entityManager.persist(target);
			entityManager.flush(); // forces DB to add user & assign valid id
			id = target.getId(); // retrieve assigned id from DB
		}

		// retrieve requested user
		target = entityManager.find(User.class, id);
		model.addAttribute("user", target);

		if (requester.getId() != target.getId() &&
				!requester.hasRole(Role.ADMIN)) {
			throw new NoEsTuPerfilException();
		}

		if (edited.getPassword() != null) {
			if (!edited.getPassword().equals(pass2)) {
				log.warn("Passwords do not match - returning to user form");
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				model.addAttribute("user", target);
				return "user";
			} else {
				// save encoded version of password
				target.setPassword(encodePassword(edited.getPassword()));
			}
		}
		target.setUsername(edited.getUsername());
		// target.setFirstName(edited.getFirstName());
		// target.setLastName(edited.getLastName());

		// update user session so that changes are persisted in the session, too
		if (requester.getId() == target.getId()) {
			session.setAttribute("u", target);
		}

		return "user";
	}

	/**
	 * Returns the default profile pic
	 * 
	 * @return
	 */
	private static InputStream defaultPic() {
		return new BufferedInputStream(Objects.requireNonNull(
				UserController.class.getClassLoader().getResourceAsStream(
						"static/img/default-pic.jpg")));
	}

	/**
	 * Descarga la imagen de perfil de un usuario.
	 *
	 * @param id ID del usuario.
	 * @return Cuerpo de respuesta con el flujo de la imagen.
	 * @throws IOException Si ocurre un error de entrada/salida.
	 */
	@GetMapping("{id}/pic")
	public StreamingResponseBody getPic(@PathVariable long id) throws IOException {
		File f = localData.getFile("user", "" + id + ".jpg");
		InputStream in = new BufferedInputStream(f.exists() ? new FileInputStream(f) : UserController.defaultPic());
		return os -> FileCopyUtils.copy(in, os);
	}

	/**
	 * Sube una nueva imagen de perfil para un usuario.
	 *
	 * @param photo    Archivo de la imagen.
	 * @param id       ID del usuario.
	 * @param response Respuesta HTTP.
	 * @param session  Sesión HTTP del usuario.
	 * @param model    Modelo para la vista.
	 * @return Respuesta JSON con el estado de la subida.
	 * @throws IOException           Si ocurre un error de entrada/salida.
	 * @throws NoEsTuPerfilException Si el usuario no tiene permisos.
	 */
	@PostMapping("{id}/pic")
	@ResponseBody
	public String setPic(@RequestParam("photo") MultipartFile photo, @PathVariable long id,
			HttpServletResponse response, HttpSession session, Model model) throws IOException {

		User target = entityManager.find(User.class, id);
		model.addAttribute("user", target);

		// check permissions
		User requester = (User) session.getAttribute("u");
		if (requester.getId() != target.getId() &&
				!requester.hasRole(Role.ADMIN)) {
			throw new NoEsTuPerfilException();
		}

		log.info("Updating photo for user {}", id);
		File f = localData.getFile("user", "" + id + ".jpg");
		if (photo.isEmpty()) {
			log.info("failed to upload photo: emtpy file?");
		} else {
			try (BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(f))) {
				byte[] bytes = photo.getBytes();
				stream.write(bytes);
				log.info("Uploaded photo for {} into {}!", id, f.getAbsolutePath());
			} catch (Exception e) {
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				log.warn("Error uploading " + id + " ", e);
			}
		}
		return "{\"status\":\"photo uploaded correctly\"}";
	}

	/**
	 * Actualiza la imagen de perfil de un usuario.
	 *
	 * @param id   ID del usuario.
	 * @param file Archivo de la imagen.
	 * @return Mapa con la URL de la nueva imagen.
	 * @throws RuntimeException Si ocurre un error al guardar la imagen.
	 */
	@PostMapping("/user/{id}/pic")
	@ResponseBody
	public Map<String, String> updateProfilePic(@PathVariable long id, @RequestParam("file") MultipartFile file) {
		// Guarda la imagen en el servidor
		File f = localData.getFile("user", id + ".jpg");
		try (BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(f))) {
			stream.write(file.getBytes());
		} catch (IOException e) {
			throw new RuntimeException("Error al guardar la imagen", e);
		}

		// Devuelve la URL de la nueva imagen
		String newPicUrl = "/user/" + id + "/pic";
		return Map.of("newPicUrl", newPicUrl);
	}

	/**
	 * Muestra una página de error genérica.
	 *
	 * @param model   Modelo para la vista.
	 * @param session Sesión HTTP del usuario.
	 * @param request Petición HTTP.
	 * @return Nombre de la vista "error".
	 */
	@GetMapping("error")
	public String error(Model model, HttpSession session, HttpServletRequest request) {
		model.addAttribute("sess", session);
		model.addAttribute("req", request);
		return "error";
	}

	/**
	 * Recupera todos los mensajes recibidos por el usuario en formato JSON.
	 *
	 * @param session Sesión HTTP del usuario.
	 * @return Lista de mensajes en formato Transfer.
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
	 * Devuelve el número de mensajes no leídos en formato JSON.
	 *
	 * @param session Sesión HTTP del usuario.
	 * @return JSON con el número de mensajes no leídos.
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

	/**
	 * Envía un mensaje a un usuario.
	 *
	 * @param id      ID del usuario destinatario.
	 * @param o       Nodo JSON con el contenido del mensaje.
	 * @param model   Modelo para la vista.
	 * @param session Sesión HTTP del usuario.
	 * @return Respuesta JSON con el estado del envío.
	 * @throws JsonProcessingException Si ocurre un error al serializar el mensaje.
	 */
	@PostMapping("/{id}/msg")
	@ResponseBody
	@Transactional
	public String postMsg(@PathVariable long id,
			@RequestBody JsonNode o, Model model, HttpSession session)
			throws JsonProcessingException {

		String text = o.get("message").asText();
		User u = entityManager.find(User.class, id);
		User sender = entityManager.find(
				User.class, ((User) session.getAttribute("u")).getId());
		model.addAttribute("user", u);

		// construye mensaje, lo guarda en BD
		Message m = new Message();
		m.setRecipient(u);
		m.setSender(sender);
		m.setDateSent(LocalDateTime.now());
		m.setText(text);
		m.setType(Type.MOSTRAR);

		entityManager.persist(m);
		entityManager.flush(); // to get Id before commit

		ObjectMapper mapper = new ObjectMapper();
		/*
		 * // construye json: método manual
		 * ObjectNode rootNode = mapper.createObjectNode();
		 * rootNode.put("from", sender.getUsername());
		 * rootNode.put("to", u.getUsername());
		 * rootNode.put("text", text);
		 * rootNode.put("id", m.getId());
		 * String json = mapper.writeValueAsString(rootNode);
		 */
		// persiste objeto a json usando Jackson
		String json = mapper.writeValueAsString(m.toTransfer());

		log.info("Sending a message to {} with contents '{}'", id, json);

		messagingTemplate.convertAndSend("/user/" + u.getUsername() + "/queue/updates", json);
		return "{\"result\": \"message sent.\"}";
	}

	/**
	 * Añade saldo a la cartera del usuario.
	 *
	 * @param id      ID del usuario.
	 * @param session Sesión HTTP del usuario.
	 * @param model   Modelo para la vista.
	 * @param monto   Cantidad a añadir.
	 * @return Redirección al perfil del usuario.
	 */
	@PostMapping("/{id}/cargar-saldo")
	@Transactional
	public String cargarSaldo(@PathVariable long id, HttpSession session, Model model,
			@RequestParam("monto") double monto) {

		User user = entityManager.find(User.class, id);

		if (monto > 0) {
			User sessionUser = (User) model.getAttribute("u");
			sessionUser.setWallet(sessionUser.getWallet() + monto);

			user.setWallet(user.getWallet() + monto);
		}

		return "redirect:/user/" + user.getId();
	}

	@PostMapping("/{id}/save-info")
	@Transactional
	@ResponseBody
	public String guardarDatos(@PathVariable long id, @RequestBody JsonNode requestData, Model model) {

		User user = entityManager.find(User.class, id);
		if (user == null) {
			return "{\"error\": \"Error al procesar la solicitud\"}";
		}

		String usuario = requestData.get("usuario").asText();
		String telefono = requestData.get("telefono").asText();
		String correo = requestData.get("correo").asText();

		user.setUsername(usuario);
		user.setTelephone(Integer.parseInt(telefono));
		user.setEmail(correo);

		entityManager.persist(user);

		User sessionUser = (User) model.getAttribute("u");
		sessionUser.setUsername(usuario);
		sessionUser.setTelephone(Integer.parseInt(telefono));
		sessionUser.setEmail(correo);

		return "{\"result\": \"Datos guardados correctamente\"}";
	}

}
