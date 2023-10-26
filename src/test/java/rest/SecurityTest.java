package rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dk.cphbusiness.data.HibernateConfig;
import dk.cphbusiness.rest.ApplicationConfig;
import dk.cphbusiness.rest.RestRoutes;
import dk.cphbusiness.security.Role;
import dk.cphbusiness.security.SecurityRoutes;
import dk.cphbusiness.security.User;
import io.javalin.http.ContentType;
import io.restassured.RestAssured;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import lombok.SneakyThrows;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

public class SecurityTest {

    private static ApplicationConfig appConfig;
    private static EntityManagerFactory emfTest;
    private static ObjectMapper jsonMapper = new ObjectMapper();
    private static final String BASE_URL = "http://localhost:7777/api";


    @BeforeAll
    static void setUpAll() {
        RestAssured.baseURI = BASE_URL;
        RestRoutes restRoutes = new RestRoutes();

        // Setup test database using docker testcontainers
        HibernateConfig.setTestMode(true);
        emfTest = HibernateConfig.getEntityManagerFactory();

        // Start server
        appConfig = ApplicationConfig.
                getInstance()
                .initiateServer()
                .startServer(7777)
                .setErrorHandling()
                .setGeneralExceptionHandling()
                .setRoutes(restRoutes.getPersonRoutes())
                .setRoutes(SecurityRoutes.getSecurityRoutes())
                .setRoutes(SecurityRoutes.getSecuredRoutes())
                .checkSecurityRoles()
                .setApiExceptionHandling()
        ;
    }

    @AfterAll
    static void afterAll() {
        appConfig.stopServer();
        HibernateConfig.setTestMode(false);
    }

    @BeforeEach
    void setUpEach() {
        // Setup test database for each test
        new TestUtils().createUsersAndRoles(emfTest);
    }

    @Test
    public void testServerIsUp() {
        System.out.println("Testing is server UP");
        given().when().get("/person").then().statusCode(200);
    }
    private static String securityToken;

    private static void login(String username, String password) {
        ObjectNode objectNode = jsonMapper.createObjectNode()
                .put("username", username)
                .put("password", password);
        String loginInput = objectNode.toString();
        securityToken = given()
                .contentType("application/json")
                .body(loginInput)
                //.when().post("/api/login")
                .when().post("/auth/login")
                .then()
                .extract().path("token");
        System.out.println("TOKEN ---> " + securityToken);
    }
    @Test
    @DisplayName("Test login for user")
    public void testRestForUser() {
        login("user", "user123");
        given()
                .contentType("application/json")
                .accept("application/json")
                .header("Authorization", "Baerer "+securityToken)
                .when()
                .get("/protected/user_demo").then()
                .statusCode(200)
                .body("msg", equalTo("Hello from USER Protected"));
    }

    @Test
    @DisplayName("Test login for admin not authorized")
    public void testRestForUserProtection() {
        login("user", "user123");
        given()
                .contentType("application/json")
                .accept("application/json")
                .header("Authorization", "Baerer "+securityToken)
                .when()
                .get("/protected/admin_demo").then()
                .statusCode(401)
                .body("msg", equalTo("Unauthorized"));
    }
}
