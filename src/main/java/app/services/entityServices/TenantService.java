package app.services.entityServices;

import app.dao.TenantDAO;
import app.entities.Tenant;
import jakarta.persistence.EntityManagerFactory;

import java.util.List;

public class TenantService {
    private final TenantDAO tenantDAO;

    public TenantService(EntityManagerFactory emf) {
        this.tenantDAO = new TenantDAO(emf);
    }

    public List<Tenant> getAll() {
        return tenantDAO.getAll();
    }

    public Tenant getById(Long id) {
        return tenantDAO.getById(id);
    }

    public Tenant create(Tenant tenant) {
        return tenantDAO.create(tenant);
    }

    public Tenant update(Tenant tenant) {
        return tenantDAO.update(tenant);
    }

    public Tenant delete(Long id) {
        return tenantDAO.delete(id);
    }
}
