package com.example.ebank.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = "eureka.client.enabled=false")
class GatewayServiceApplicationTests {

    @Autowired
    private RouteLocator routeLocator;

    @Autowired
    private Environment environment;

    @Test
    void contextLoads() {
    }

    @Test
    void discoveryLocatorIsEnabledAndStaticLocalhostRoutesAreGone() {
        assertThat(environment.getProperty("spring.cloud.gateway.server.webflux.discovery.locator.enabled"))
                .isEqualTo("true");

        List<Route> routes = routeLocator.getRoutes().collectList().block();

        assertThat(routes).isNotNull();
        assertThat(routes)
                .noneMatch(route -> "http://localhost:8056".equals(route.getUri().toString()))
                .noneMatch(route -> "http://localhost:8057".equals(route.getUri().toString()))
                .noneMatch(route -> "http://localhost:8058".equals(route.getUri().toString()));
    }
}
