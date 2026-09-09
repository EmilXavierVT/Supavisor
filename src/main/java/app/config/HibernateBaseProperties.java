package app.config;

import java.util.Properties;

final class HibernateBaseProperties {

    private HibernateBaseProperties() {}

    static Properties createBase() {
        Properties props = new Properties();
        props.put("hibernate.connection.driver_class", "org.postgresql.Driver");
        props.put("hibernate.current_session_context_class", "thread");
        props.put("hibernate.show_sql", "true");
        props.put("hibernate.format_sql", "false");
        props.put("hibernate.use_sql_comments", "false");
        props.put("hibernate.hikari.maximumPoolSize", "10");
        props.put("hibernate.hikari.minimumIdle", "2");
        props.put("hibernate.hikari.connectionTimeout", "20000");
        props.put("hibernate.hikari.validationTimeout", "5000");
        props.put("hibernate.hikari.idleTimeout", "600000");
        props.put("hibernate.hikari.maxLifetime", "1800000");
        props.put("hibernate.hikari.keepaliveTime", "300000");
        props.put("hibernate.hikari.leakDetectionThreshold", "30000");
        return props;
    }
}
