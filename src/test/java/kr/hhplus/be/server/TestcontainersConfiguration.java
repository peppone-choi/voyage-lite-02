package kr.hhplus.be.server;

import jakarta.annotation.PreDestroy;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

@Configuration
@Profile("test")
public class TestcontainersConfiguration {

	public static final MySQLContainer<?> MYSQL_CONTAINER;

	static {
		try {
			MYSQL_CONTAINER = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
				.withDatabaseName("hhplus")
				.withUsername("test")
				.withPassword("test")
				.withReuse(true);
			
			if (!MYSQL_CONTAINER.isRunning()) {
				MYSQL_CONTAINER.start();
			}

			System.setProperty("spring.datasource.url", MYSQL_CONTAINER.getJdbcUrl() + "?characterEncoding=UTF-8&serverTimezone=UTC");
			System.setProperty("spring.datasource.username", MYSQL_CONTAINER.getUsername());
			System.setProperty("spring.datasource.password", MYSQL_CONTAINER.getPassword());
			System.setProperty("spring.jpa.hibernate.ddl-auto", "create-drop");
		} catch (Exception e) {
			// Fallback to H2 database if Docker is not available
			System.setProperty("spring.datasource.url", "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
			System.setProperty("spring.datasource.username", "sa");
			System.setProperty("spring.datasource.password", "");
			System.setProperty("spring.datasource.driver-class-name", "org.h2.Driver");
			System.setProperty("spring.jpa.hibernate.ddl-auto", "create-drop");
			System.setProperty("spring.jpa.database-platform", "org.hibernate.dialect.H2Dialect");
			throw new RuntimeException("Docker not available, falling back to H2: " + e.getMessage(), e);
		}
	}

	@PreDestroy
	public void preDestroy() {
		try {
			if (MYSQL_CONTAINER != null && MYSQL_CONTAINER.isRunning()) {
				MYSQL_CONTAINER.stop();
			}
		} catch (Exception e) {
			// Ignore cleanup errors
		}
	}
}