package app.config;

import app.utils.Utils;
import jakarta.persistence.EntityManagerFactory;

import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public final class HibernateConfig {

    private static final String SCHEMA_ACTION = "hibernate.hbm2ddl.auto";
    private static final Set<String> DEVELOPMENT_SCHEMA_ACTIONS = Set.of(
            "none", "validate", "update", "create", "create-drop"
    );

    private static volatile EntityManagerFactory emf;

    private HibernateConfig() {}

    public static EntityManagerFactory getEntityManagerFactory() {
        if (emf == null) {
            synchronized (HibernateConfig.class) {
                if (emf == null) {
                    emf = HibernateEmfBuilder.build(buildProperties(System.getenv()));
                }
            }
        }
        return emf;
    }

    static Properties buildProperties(Map<String, String> environment) {
        Properties props = HibernateBaseProperties.createBase();
        boolean isProduction = isDeployed(environment);
        props.setProperty(SCHEMA_ACTION, resolveSchemaAction(environment, isProduction));

        if (isProduction) {
            setDeployedProperties(props, environment);
        } else {
            setDevProperties(props);
        }
        return props;
    }

    private static void setDeployedProperties(Properties props, Map<String, String> environment) {
        String dbName = requireEnvironmentValue(environment, "DB_NAME");
        props.setProperty("hibernate.connection.url", requireEnvironmentValue(environment, "CONNECTION_STR") + dbName);
        props.setProperty("hibernate.connection.username", requireEnvironmentValue(environment, "DB_USERNAME"));
        props.setProperty("hibernate.connection.password", requireEnvironmentValue(environment, "DB_PASSWORD"));
    }

    private static void setDevProperties(Properties props) {
        String dbName = Utils.getPropertyValue("DB_NAME", "config.properties");
        String username = Utils.getPropertyValue("LOCAL_DB_USERNAME", "config.properties");
        String password = Utils.getPropertyValue("LOCAL_DB_PASSWORD", "config.properties");
        String ConnectionStr = Utils.getPropertyValue("LOCAL_CONNECTION_STR", "config.properties") + dbName;

        props.put("hibernate.connection.url", ConnectionStr);
        props.put("hibernate.connection.username", username);
        props.put("hibernate.connection.password", password);
    }

    static boolean isDeployed(Map<String, String> environment) {
        String deployed = environment.get("DEPLOYED");
        if (hasText(deployed)) {
            return Boolean.parseBoolean(deployed.trim());
        }
        return hasText(environment.get("CONNECTION_STR"));
    }

    static String resolveSchemaAction(Map<String, String> environment, boolean isProduction) {
        if (isProduction) {
            return "validate";
        }

        String configuredAction = environment.getOrDefault("HIBERNATE_HBM2DDL_AUTO", "update")
                .trim()
                .toLowerCase(Locale.ROOT);
        if (!DEVELOPMENT_SCHEMA_ACTIONS.contains(configuredAction)) {
            throw new IllegalStateException(
                    "HIBERNATE_HBM2DDL_AUTO must be one of: " + String.join(", ", DEVELOPMENT_SCHEMA_ACTIONS)
            );
        }
        return configuredAction;
    }

    private static String requireEnvironmentValue(Map<String, String> environment, String key) {
        String value = environment.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(key + " must be configured in the container environment");
        }
        return value.trim();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
