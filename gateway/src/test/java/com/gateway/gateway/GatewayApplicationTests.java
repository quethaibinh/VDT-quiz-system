package com.gateway.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "gateway.trusted-secret=test-gateway-secret")
class GatewayApplicationTests {

	@Autowired
	private RouteDefinitionLocator routeDefinitionLocator;

	@Test
	void contextLoads() {
	}

	@Test
	void serviceRoutesAcceptBothRegularAndAdminPaths() {
		List<RouteDefinition> routes = routeDefinitionLocator.getRouteDefinitions()
				.collectList()
				.block();

		assertRoutePaths(routes, "auth-service-route",
				"/v1/api/auth-service/**",
				"/v1/api/admin/auth-service/**");
		assertRoutePaths(routes, "question-service-route",
				"/v1/api/question-service/**",
				"/v1/api/admin/question-service/**");
		assertRoutePaths(routes, "exam-service-route",
				"/v1/api/exam-service/**",
				"/v1/api/admin/exam-service/**");
	}

	private void assertRoutePaths(List<RouteDefinition> routes, String routeId, String... expectedPaths) {
		RouteDefinition route = routes.stream()
				.filter(candidate -> routeId.equals(candidate.getId()))
				.findFirst()
				.orElseThrow();

		assertThat(route.getPredicates()).hasSize(1);
		Map<String, String> predicateArgs = route.getPredicates().getFirst().getArgs();
		assertThat(predicateArgs.values()).containsExactlyInAnyOrder(expectedPaths);
	}

}
