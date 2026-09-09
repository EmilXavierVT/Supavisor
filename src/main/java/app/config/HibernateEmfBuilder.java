package app.config;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;

import java.util.Properties;

// This class is just a factory that converts properties into an EntityManagerFactory

final class HibernateEmfBuilder {

    private HibernateEmfBuilder() {}

    static EntityManagerFactory build(Properties props) {
        try {
            Configuration configuration = new Configuration();
            configuration.setProperties(props);

            // Register entities to make Hibernate aware
            EntityRegistry.registerEntities(configuration);

            ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
                    .applySettings(configuration.getProperties())
                    .build();

            // Make JPA compliant version of Hibernates sf
            SessionFactory sf = configuration.buildSessionFactory(serviceRegistry);
            return sf.unwrap(EntityManagerFactory.class);

        } catch (Throwable ex) {
            String message = "Failed to initialize Hibernate SessionFactory. " +
                    "Check database configuration, database availability, and registered entities.";
            System.err.println(message);
            ex.printStackTrace(System.err);
            throw new IllegalStateException(message, ex);
        }
    }
}
