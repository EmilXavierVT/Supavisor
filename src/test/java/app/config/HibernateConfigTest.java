package app.config;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HibernateConfigTest {

    @Test
    void productionAlwaysValidatesTheExistingSchema() {
        Map<String, String> environment = productionEnvironment();
        environment.put("HIBERNATE_HBM2DDL_AUTO", "update");

        assertEquals(
                "validate",
                HibernateConfig.buildProperties(environment).getProperty("hibernate.hbm2ddl.auto")
        );
    }

    @Test
    void developmentSchemaHandlingIsConfiguredSeparately() {
        assertEquals("update", HibernateConfig.resolveSchemaAction(Map.of(), false));
        assertEquals(
                "create-drop",
                HibernateConfig.resolveSchemaAction(Map.of("HIBERNATE_HBM2DDL_AUTO", "create-drop"), false)
        );
    }

    @Test
    void invalidDevelopmentSchemaActionFailsClearly() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> HibernateConfig.resolveSchemaAction(Map.of("HIBERNATE_HBM2DDL_AUTO", "drop"), false)
        );

        assertTrue(exception.getMessage().contains("HIBERNATE_HBM2DDL_AUTO"));
    }

    @Test
    void explicitDevelopmentModeOverridesConnectionStringDetection() {
        assertFalse(HibernateConfig.isDeployed(Map.of(
                "DEPLOYED", "false",
                "CONNECTION_STR", "jdbc:postgresql://db:5432/"
        )));
        assertTrue(HibernateConfig.isDeployed(Map.of("CONNECTION_STR", "jdbc:postgresql://db:5432/")));
    }

    private static Map<String, String> productionEnvironment() {
        Map<String, String> environment = new HashMap<>();
        environment.put("DEPLOYED", "true");
        environment.put("DB_NAME", "supavisor");
        environment.put("CONNECTION_STR", "jdbc:postgresql://database.example:5432/");
        environment.put("DB_USERNAME", "app");
        environment.put("DB_PASSWORD", "secret");
        return environment;
    }
}
