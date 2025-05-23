Feature: Hacer una request al admin
  Pruebas para el flujo de login, y petición para añadir un parking.

  Background:
    * configure driver = { type: 'chrome' }  # Configuramos Chrome como driver

  Scenario: Flujo completo de reserva de parking como usuario
    # Paso 1: Iniciar sesión reutilizando login_a
    Given driver baseUrl
    And call read('login.feature@login_e')  
    And delay(5000)
    Then waitForUrl(baseUrl + '/enterprise/977')      

    # Paso 2: Ir a la vista de añadir parking. 
    Given driver baseUrl + '/enterprise/add-parking'  
    And delay(2000)      

    # Paso 3: Rellenar el formulario de solcitar añadir un parking.
    And input('#name', 'Palomitas')
    And input('#address', 'Calle de la piruleta')
    And input('#city', 'Madrid')
    And input('#country', 'Espanya')
    And input('#cp', '28320')
    And input('#openingTime', '1200')
    And input('#closingTime', '2100')
    And input('#totalSpots', '10')
    And input('#feePerHour', '1')
    And input('#telephone', '673407145')
    And input('#email', 's@gmail.com')
    And delay(2000)   
    When submit().click("{button}Añadir")
    And delay(2000)  
    Then waitForUrl(baseUrl + '/enterprise/request-parking')

    # Paso 4: Redireccion a la pagina de solicitudes.
    And delay(2000)  
    When click("{a}Solicitudes")
    Then waitForUrl(baseUrl + '/enterprise/requests')  