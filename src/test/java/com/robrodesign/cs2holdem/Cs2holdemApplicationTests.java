package com.robrodesign.cs2holdem;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// Startar hela appen mot Postgres i Docker. Testet går igenom om Flyway-migreringarna
// körs utan fel OCH Hibernate godkänner att entiteterna matchar tabellerna (ddl-auto: validate).
// Docker Compose-stödet är avstängt i tester som standard, därför slår vi på det här.
@SpringBootTest(properties = "spring.docker.compose.skip.in-tests=false")
class Cs2holdemApplicationTests {

    @Test
    void contextLoads() {
    }
}
