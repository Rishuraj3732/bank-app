package com.javatechie;

import com.example.authservice.IdentityServiceApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
		classes = IdentityServiceApplication.class,
		properties = {
				"spring.datasource.url=jdbc:h2:mem:identitytest;MODE=MySQL;DB_CLOSE_DELAY=-1",
				"spring.datasource.driver-class-name=org.h2.Driver",
				"spring.datasource.username=sa",
				"spring.datasource.password=",
				"spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
				"eureka.client.enabled=false"
		}
)
class IdentityServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
