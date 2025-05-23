package es.ucm.fdi.iw;

import java.time.LocalDate;
import java.time.LocalTime;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import es.ucm.fdi.iw.model.*;
import jakarta.persistence.EntityManager;
import jakarta.servlet.ServletContext;
import jakarta.transaction.Transactional;

/**
 * This code will execute when the application first starts.
 * 
 * @author mfreire
 */
@Component
public class StartupConfig {

    private static final Logger log = LogManager.getLogger(StartupConfig.class);

    @Autowired
    private Environment env;

    @Autowired
    private ServletContext context;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @EventListener(ContextRefreshedEvent.class)
    @Transactional
    public void contextRefreshedEvent() {
        String debugProperty = env.getProperty("es.ucm.fdi.debug");
        context.setAttribute("debug", debugProperty != null
                && Boolean.parseBoolean(debugProperty.toLowerCase()));
        log.info("Setting global debug property to {}",
                context.getAttribute("debug"));

        poblarBBDD();
    }

    @Transactional
    public void poblarBBDD() {

        Admin a = new Admin();
        // a.setId(1);
        a.setEnabled(true);
        a.setRole(User.Role.ADMIN);
        a.setUsername("a");
        a.setPassword(passwordEncoder.encode("aa"));
        a.setWallet(10000);
        a.setEmail("1234@park-it.com");
        a.setTelephone(000000000);
        a.setCodigo("1234");
        entityManager.persist(a);

        Parker u = new Parker();
        // u.setId(2);
        u.setEnabled(true);
        u.setRole(User.Role.USER);
        u.setUsername("b");
        u.setPassword(passwordEncoder.encode("aa"));
        u.setWallet(10000);
        u.setEmail("pepe@gmail.com");
        u.setTelephone(000000001);
        u.setDNI("aaa");
        u.setFirstName("Pepe");
        u.setSecondName("Perez");
        entityManager.persist(u);

        Enterprise e = new Enterprise();
        // e.setId(3);
        e.setEnabled(true);
        e.setRole(User.Role.ENTERPRISE);
        e.setUsername("e");
        e.setPassword(passwordEncoder.encode("aa"));
        e.setWallet(0);
        e.setEmail("e@empresa1.com");
        e.setTelephone(000000002);
        e.setName("Empresa1");
        e.setCIF("ES1234");
        entityManager.persist(e);

        Parker u1 = new Parker();
        // u1.setId(4);
        u1.setEnabled(true);
        u1.setRole(User.Role.USER);
        u1.setUsername("paulal10");
        u1.setPassword(passwordEncoder.encode("aa"));
        u1.setWallet(517);
        u1.setEmail("lamejor@ucm.es");
        u1.setTelephone(000000002);
        u1.setDNI("aab");
        u1.setFirstName("Paula");
        u1.setSecondName("López");
        entityManager.persist(u1);

        Parker u2 = new Parker();
        // u2.setId(5);
        u2.setEnabled(true);
        u2.setRole(User.Role.USER);
        u2.setUsername("secsanc");
        u2.setPassword(passwordEncoder.encode("aa"));
        u2.setWallet(517);
        u2.setEmail("lamejor@ucm.es");
        u2.setTelephone(000000003);
        u2.setDNI("aac");
        u2.setFirstName("Sergio");
        u2.setSecondName("Sanchez");
        entityManager.persist(u2);

        Parking parking1 = new Parking();
        // parking1.setId(1);
        parking1.setName("Parking 1");
        parking1.setAddress("Calle de la piruleta");
        parking1.setCity("Madrid");
        parking1.setCountry("España");
        parking1.setCp(28888);
        parking1.setTelephone(123456789);
        parking1.setEmail("parking1@gmail.com");
        parking1.setEnterprise(e);
        parking1.setOpeningTime(LocalTime.parse("08:00"));
        parking1.setClosingTime(LocalTime.parse("20:00"));
        parking1.setFeePerHour(1.5);
        parking1.setEnabled(true);
        parking1.setLatitude(40.416775);
        parking1.setLongitude(-3.703790);
        parking1.setWidth(20.0); // Example value
        parking1.setHeight(10.0); // Example value
        parking1.setEntryX(1.0); // Example value
        parking1.setEntryY(1.0); // Example value
        parking1.setExitX(2.0); // Example value
        parking1.setExitY(2.0); // Example value
        entityManager.persist(parking1);

        for (int j = 1; j <= 13; j++) { // por ahora las plazas estan en el parking 3
            Spot spot = new Spot();
            // spot.setId(j);
            spot.setParking(parking1);
            spot.setEnabled(true); // Example value
            spot.setSize(Math.random() > 0.5 ? "M" : "L"); // Example value
            spot.setHorizontal(true); // Example value
            spot.setX(j * 2.5); // Example value
            spot.setY(0.0); // Example value
            entityManager.persist(spot);
        }

        Parking parking2 = new Parking();
        // parking2.setId(2);
        parking2.setName("Parking 2");
        parking2.setAddress("Calle de la fresa");
        parking2.setCity("Madrid");
        parking2.setCountry("España");
        parking2.setCp(28888);
        parking2.setTelephone(123456789);
        parking2.setEmail("parking2@gmail.com");
        parking2.setEnterprise(e);
        parking2.setOpeningTime(LocalTime.parse("08:00"));
        parking2.setClosingTime(LocalTime.parse("20:00"));
        parking2.setFeePerHour(1.5);
        parking2.setEnabled(true);
        parking2.setLatitude(42.416775);
        parking2.setLongitude(-5.703790);
        parking2.setWidth(20.0); // Example value
        parking2.setHeight(10.0); // Example value
        parking2.setEntryX(1.0); // Example value
        parking2.setEntryY(1.0); // Example value
        parking2.setExitX(2.0); // Example value
        parking2.setExitY(2.0); // Example value
        entityManager.persist(parking2);

        for (int j = 1; j <= 13; j++) { // por ahora las plazas estan en el parking 3
            Spot spot = new Spot();
            // spot.setId(j);
            spot.setParking(parking2);
            spot.setEnabled(true); // Example value
            spot.setSize(Math.random() > 0.5 ? "M" : "L"); // Example value
            spot.setHorizontal(true); // Example value
            spot.setX(j * 2.5); // Example value
            spot.setY(0.0); // Example value
            entityManager.persist(spot);
        }

        Parking parking3 = new Parking();
        // parking3.setId(3);
        parking3.setName("Parking 3");
        parking3.setAddress("Paseo de los Olmos");
        parking3.setCity("Madrid");
        parking3.setCountry("España");
        parking3.setCp(28888);
        parking3.setTelephone(123456789);
        parking3.setEmail("parking3@gmail.com");
        parking3.setEnterprise(e);
        parking3.setOpeningTime(LocalTime.parse("08:00"));
        parking3.setClosingTime(LocalTime.parse("20:00"));
        parking3.setFeePerHour(1.5);
        parking3.setEnabled(true);
        parking3.setLatitude(40.404711);
        parking3.setLongitude(-3.710862);
        parking3.setWidth(20.0); // Example value
        parking3.setHeight(10.0); // Example value
        parking3.setEntryX(1.0); // Example value
        parking3.setEntryY(1.0); // Example value
        parking3.setExitX(2.0); // Example value
        parking3.setExitY(2.0); // Example value
        entityManager.persist(parking3);

        for (int j = 1; j <= 13; j++) { // por ahora las plazas estan en el parking 3
            Spot spot = new Spot();
            // spot.setId(j);
            spot.setParking(parking3);
            spot.setEnabled(true); // Example value
            spot.setSize(Math.random() > 0.5 ? "M" : "L"); // Example value
            spot.setHorizontal(true); // Example value
            spot.setX(j * 2.5); // Example value
            spot.setY(0.0); // Example value
            entityManager.persist(spot);
        }

        Vehicle v1 = new Vehicle();
        // v1.setId(1);
        v1.setEnabled(true);
        v1.setPlate("ABC1234");
        v1.setBrand("Toyota");
        v1.setModel("Corolla");
        v1.setSize("M");
        v1.setParker(u);
        entityManager.persist(v1);

        Vehicle v2 = new Vehicle();
        // v2.setId(2);
        v2.setEnabled(true);
        v2.setPlate("XYZ5678");
        v2.setBrand("Honda");
        v2.setModel("Civic");
        v2.setSize("L");
        v2.setParker(u);
        entityManager.persist(v2);

        Vehicle v3 = new Vehicle();
        // v3.setId(3);
        v3.setEnabled(true);
        v3.setPlate("DEF9877");
        v3.setBrand("BMW");
        v3.setModel("X5");
        v3.setSize("XL");
        v3.setParker(u);
        entityManager.persist(v3);

        Vehicle v4 = new Vehicle();
        // v4.setId(4);
        v4.setEnabled(true);
        v4.setPlate("PAULA");
        v4.setBrand("Toyota");
        v4.setModel("Corolla");
        v4.setSize("M");
        v4.setParker(u1);
        entityManager.persist(v4);

        Vehicle v5 = new Vehicle();
        // v5.setId(5);
        v5.setEnabled(true);
        v5.setPlate("ARTURO");
        v5.setBrand("Honda");
        v5.setModel("Civic");
        v5.setSize("L");
        v5.setParker(u1);
        entityManager.persist(v5);

        Vehicle v6 = new Vehicle();
        // v6.setId(6);
        v6.setEnabled(true);
        v6.setPlate("DEF9876");
        v6.setBrand("BMW");
        v6.setModel("X5");
        v6.setSize("XL");
        v6.setParker(u1);
        entityManager.persist(v6);

        Reserve r1 = new Reserve();
        // r1.setId(1);
        r1.setState(Reserve.State.CONFIRMED);
        r1.setStartDate(LocalDate.parse("2025-03-06"));
        r1.setEndDate(LocalDate.parse("2025-03-07"));
        r1.setStartTime(LocalTime.parse("10:00"));
        r1.setEndTime(LocalTime.parse("12:00"));
        r1.setPrice(5.0);
        r1.setComments("Reserva para evento");
        r1.setSpot(entityManager.find(Spot.class, 1076));
        r1.setVehicle(v1);
        entityManager.persist(r1);

        Reserve r2 = new Reserve();
        // r2.setId(2);
        r2.setState(Reserve.State.CONFIRMED);
        r2.setStartDate(LocalDate.parse("2025-03-06"));
        r2.setEndDate(LocalDate.parse("2025-03-07"));
        r2.setStartTime(LocalTime.parse("10:00"));
        r2.setEndTime(LocalTime.parse("12:00"));
        r2.setPrice(8.0);
        r2.setComments("Reserva para evento");
        r2.setSpot(entityManager.find(Spot.class, 1075));
        r2.setVehicle(v2);
        entityManager.persist(r2);

        Reserve r3 = new Reserve();
        // r2.setId(2);
        r3.setState(Reserve.State.CONFIRMED);
        r3.setStartDate(LocalDate.parse("2025-03-06"));
        r3.setEndDate(LocalDate.parse("2025-03-07"));
        r3.setStartTime(LocalTime.parse("13:00"));
        r3.setEndTime(LocalTime.parse("14:00"));
        r3.setPrice(8.0);
        r3.setComments("Reserva para evento");
        r3.setSpot(entityManager.find(Spot.class, 1075));
        r3.setVehicle(v2);
        entityManager.persist(r3);

        Reserve r4 = new Reserve();
        // r2.setId(2);
        r4.setState(Reserve.State.CONFIRMED);
        r4.setStartDate(LocalDate.parse("2025-03-06"));
        r4.setEndDate(LocalDate.parse("2025-03-07"));
        r4.setStartTime(LocalTime.parse("15:00"));
        r4.setEndTime(LocalTime.parse("17:00"));
        r4.setPrice(8.0);
        r4.setComments("Reserva para evento");
        r4.setSpot(entityManager.find(Spot.class, 1075));
        r4.setVehicle(v2);
        entityManager.persist(r4);

        Reserve r5 = new Reserve();
        // r2.setId(2);
        r5.setState(Reserve.State.CONFIRMED);
        r5.setStartDate(LocalDate.parse("2025-03-06"));
        r5.setEndDate(LocalDate.parse("2025-03-07"));
        r5.setStartTime(LocalTime.parse("18:00"));
        r5.setEndTime(LocalTime.parse("19:00"));
        r5.setPrice(8.0);
        r5.setComments("Reserva para evento");
        r5.setSpot(entityManager.find(Spot.class, 1075));
        r5.setVehicle(v2);
        entityManager.persist(r5);

        Request req1 = new Request();
        req1.setEnabled(true);
        req1.setName("Interparking el mercado");
        req1.setAddress("Calle cerrajeros s/n");
        req1.setCp(28801);
        req1.setCity("Alcala de Henares");
        req1.setTelephone(918798072);
        req1.setEmail("interparking@gmail.com");
        req1.setFeePerHour(1.1);
        req1.setOpeningTime(LocalTime.parse("00:00"));
        req1.setClosingTime(LocalTime.parse("00:00"));
        req1.setIdParking(-1);
        req1.setTotalSpots(20); // TODO
        req1.setType("AÑADIR");
        req1.setState("Pendiente");
        req1.setCountry("España");
        req1.setEnterprise(e);
        entityManager.persist(req1);

        Request req2 = new Request();
        req2.setEnabled(true);
        req2.setName("Interparking San Lucas");
        req2.setAddress("Vía Complutense");
        req2.setCp(28805);
        req2.setCity("Alcala de Henares");
        req2.setTelephone(918898518);
        req2.setEmail("interparkingsanlucas@gmail.com");
        req2.setFeePerHour(1.1);
        req2.setOpeningTime(LocalTime.parse("00:00"));
        req2.setClosingTime(LocalTime.parse("00:00"));
        req2.setLatitude(40.48528);
        req2.setLongitude(-3.36322);
        req2.setIdParking(1027);
        req2.setTotalSpots(20); // TODO
        req2.setType("ELIMINAR");
        req2.setState("Pendiente");
        req2.setCountry("España");
        req2.setEnterprise(e);
        entityManager.persist(req2);

        Request req3 = new Request();
        req3.setEnabled(true);
        req3.setName("Interparking el sol");
        req3.setAddress("Calle cerrajeros s/n");
        req3.setCp(28808);
        req3.setCity("Madrid");
        req3.setTelephone(918745672);
        req3.setEmail("interparkingSol@gmail.com");
        req3.setFeePerHour(1.7);
        req3.setOpeningTime(LocalTime.parse("00:00"));
        req3.setClosingTime(LocalTime.parse("00:00"));
        req3.setIdParking(-1);
        req3.setTotalSpots(20); // TODO
        req3.setType("AÑADIR");
        req3.setState("Pendiente");
        req3.setCountry("España");
        req3.setEnterprise(e);
        entityManager.persist(req3);

        Request req4 = new Request();
        req4.setEnabled(true);
        req4.setName("Interparking San Lucas");
        req4.setAddress("Calle de la piruleta");
        req4.setCp(28805);
        req4.setCity("Javier");
        req4.setTelephone(918898518);
        req4.setEmail("interparkingsanlucas@gmail.com");
        req4.setFeePerHour(1.1);
        req4.setOpeningTime(LocalTime.parse("00:00"));
        req4.setClosingTime(LocalTime.parse("00:00"));
        req4.setIdParking(1027);
        req4.setTotalSpots(20); // TODO
        req4.setType("AÑADIR");
        req4.setState("Pendiente");
        req4.setCountry("Paula");
        req4.setEnterprise(e);
        entityManager.persist(req4);

        Request req5 = new Request();
        // parking2.setId(2);
        req5.setName("Parking 2");
        req5.setAddress("Calle de la fresa");
        req5.setCity("Madrid");
        req5.setCountry("España");
        req5.setCp(28888);
        req5.setTelephone(123456789);
        req5.setEmail("parking2@gmail.com");
        req5.setEnterprise(e);
        req5.setOpeningTime(LocalTime.parse("08:00"));
        req5.setClosingTime(LocalTime.parse("20:00"));
        req5.setFeePerHour(1.5);
        req5.setEnabled(true);
        req5.setLatitude(42.416775);
        req5.setLongitude(-5.703790);
        req5.setIdParking(parking2.getId());
        req5.setTotalSpots(parking2.getSpots().size()); // TODO
        req5.setType("ELIMINAR");
        req5.setState("Pendiente");
        entityManager.persist(req5);

        /*
         * TODO
         * INSERT INTO REQUEST (id, enabled, name, address, cp, city, country,
         * telephone, email, feePerHour, openingTime, closingTime, longitude, latitude,
         * idParking, totalSpots, state, enterprise_id)
         * VALUES (1, true, 'InterParking El Mercado', 'Calle Cerrajeros s/n', 28801,
         * 'Alcalá de Henares', 'Espanya', '918798072', 'interparking@gmial.com', 1.1,
         * '00:00', '00:00', 40.48205, -3.36553, null, 20, 'ANYADIR', 3);
         */
    }
}