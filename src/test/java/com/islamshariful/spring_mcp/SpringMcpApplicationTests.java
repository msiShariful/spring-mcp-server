package com.islamshariful.spring_mcp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:context-test")
class SpringMcpApplicationTests {

	@Test
	void contextLoads() {
	}

}
