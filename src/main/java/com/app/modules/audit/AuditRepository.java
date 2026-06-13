package com.app.modules.audit;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AuditRepository implements PanacheRepositoryBase<AuditModel, String> {
}
