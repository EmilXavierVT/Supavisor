package app.config;

import app.entities.*;
import org.hibernate.cfg.Configuration;

final class EntityRegistry {

    private EntityRegistry() {}

    static void registerEntities(Configuration configuration) {
        configuration.addAnnotatedClass(User.class);
        configuration.addAnnotatedClass(Tenant.class);
        configuration.addAnnotatedClass(Assignment.class);
        configuration.addAnnotatedClass(Project.class);
        configuration.addAnnotatedClass(ProjectStatusHistory.class);
        configuration.addAnnotatedClass(Role.class);
        configuration.addAnnotatedClass(EmployeeCategory.class);
    }
}
