package {{package}};

import com.app.core.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "{{table_name}}")
public class {{module_name}}Model extends BaseEntity {

{{fields}}

{{getters_setters}}
}
