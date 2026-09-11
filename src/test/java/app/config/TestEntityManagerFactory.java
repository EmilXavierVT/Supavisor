package app.config;

import jakarta.persistence.EntityManagerFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.Properties;

public final class TestEntityManagerFactory {
    private TestEntityManagerFactory() {
    }

    public static EntityManagerFactory create(PostgreSQLContainer<?> postgres) {
        Properties props = HibernateBaseProperties.createBase();
        props.put("hibernate.hbm2ddl.auto", "create-drop");
        props.put("hibernate.connection.url", postgres.getJdbcUrl());
        props.put("hibernate.connection.username", postgres.getUsername());
        props.put("hibernate.connection.password", postgres.getPassword());
        props.put("hibernate.show_sql", "false");
        return HibernateEmfBuilder.build(props);
    }
}
