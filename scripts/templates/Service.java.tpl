package {{package}};

import com.app.core.BaseService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.util.List;

@ApplicationScoped
public class {{module_name}}Service extends BaseService<{{module_name}}Model, {{module_name}}Repository> {

    @Inject
    public {{module_name}}Service({{module_name}}Repository repository, EntityManager em) {
        this.repository = repository;
        this.em = em;
        this.entityClass = {{module_name}}Model.class;
        this.filterableFields = List.of("active", "createdAt"); // TODO: Add more fields
        this.searchableFields = List.of(); // TODO: Add more fields
    }

    @Override
    protected void mergeFields({{module_name}}Model existing, {{module_name}}Model incoming) {
{{merge_fields}}
    }
}
