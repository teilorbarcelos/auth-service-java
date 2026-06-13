import sys
import os
import re

def camel_to_snake(name):
    return re.sub(r'(?<!^)(?=[A-Z])', '_', name).lower()

def map_sql_type(sql_type):
    sql_type = sql_type.upper()
    if any(t in sql_type for t in ['VARCHAR', 'TEXT', 'CHAR', 'UUID']):
        return 'String'
    if any(t in sql_type for t in ['INTEGER', 'INT', 'SERIAL']):
        return 'Integer'
    if any(t in sql_type for t in ['BIGINT', 'BIGSERIAL']):
        return 'Long'
    if any(t in sql_type for t in ['BOOLEAN', 'BOOL']):
        return 'Boolean'
    if any(t in sql_type for t in ['TIMESTAMP', 'DATE', 'DATETIME']):
        return 'LocalDateTime'
    if any(t in sql_type for t in ['DECIMAL', 'NUMERIC', 'DOUBLE', 'FLOAT']):
        return 'java.math.BigDecimal'
    return 'Object'

def load_template(name):
    path = f"scripts/templates/{name}.java.tpl"
    with open(path, 'r') as f:
        return f.read()

def get_next_migration_version(migration_dir):
    if not os.path.exists(migration_dir):
        os.makedirs(migration_dir, exist_ok=True)
        return 1
    
    versions = []
    for f in os.listdir(migration_dir):
        match = re.match(r'V(\d+)__', f)
        if match:
            versions.append(int(match.group(1)))
    
    return max(versions) + 1 if versions else 1

def create_basic_migration(module_name, module_snake):
    migration_dir = 'src/main/resources/db/migration'
    version = get_next_migration_version(migration_dir)
    filename = f"V{version}__create_{module_snake}_table.sql"
    path = os.path.join(migration_dir, filename)
    
    # Simple pluralization (adds 's')
    table_name = module_snake + 's'
    
    content = f"""CREATE TABLE {table_name} (
    id          VARCHAR(40) PRIMARY KEY,
    active      BOOLEAN DEFAULT TRUE,
    is_deleted  BOOLEAN DEFAULT FALSE,
    deleted_at  TIMESTAMP,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
"""
    with open(path, 'w') as f:
        f.write(content)
    
    print(f"Created basic migration: {path}")
    return path

def register_feature(module_snake, module_name):
    bootstrap_path = "src/main/java/com/app/infrastructure/seed/DatabaseBootstrap.java"
    if not os.path.exists(bootstrap_path):
        return

    with open(bootstrap_path, 'r') as f:
        content = f.read()

    # Check if already registered
    if f'Map.of("id", "{module_snake}"' in content:
        print(f"Feature '{module_snake}' already registered in DatabaseBootstrap.")
        return

    # Find the feature list block
    feature_pattern = r'(Map\.of\("id",\s*"(\w+)",\s*"name",\s*"([^"]+)",\s*"description",\s*"([^"]+)"\))'
    matches = list(re.finditer(feature_pattern, content))
    
    if not matches:
        print("Could not find feature list in DatabaseBootstrap to inject new feature.")
        return

    last_match = matches[-1]
    insertion_point = last_match.end()
    
    new_feature = f',\n                    Map.of("id", "{module_snake}", "name", "{module_name}s", "description", "Gestão de {module_name.lower()}s")'
    
    new_content = content[:insertion_point] + new_feature + content[insertion_point:]
    
    with open(bootstrap_path, 'w') as f:
        f.write(new_content)
    
    print(f"Feature '{module_snake}' registered in DatabaseBootstrap successfully!")

def generate_module(module_name):
    module_name = module_name[0].upper() + module_name[1:]
    module_snake = camel_to_snake(module_name)
    module_lower = module_name.lower()
    
    # 1. Find or create migration file
    migration_dir = 'src/main/resources/db/migration'
    migration_file = None
    if os.path.exists(migration_dir):
        for f in sorted(os.listdir(migration_dir), reverse=True):
            if module_snake in f.lower() and f.endswith('.sql'):
                migration_file = os.path.join(migration_dir, f)
                break
    
    if not migration_file:
        print(f"No migration found for '{module_name}'.")
        choice = input("Do you want to create a basic migration file? [y/N]: ").lower()
        if choice == 'y':
            migration_file = create_basic_migration(module_name, module_snake)
    
    fields = []
    table_name_found = module_snake + 's'
    
    if migration_file:
        print(f"Using migration: {migration_file}")
        with open(migration_file, 'r') as f:
            content = f.read()
            
        match = re.search(r'CREATE TABLE\s+(\w+)\s*\((.*?)\);', content, re.DOTALL | re.IGNORECASE)
        if match:
            table_name_found = match.group(1)
            columns_raw = match.group(2)
            
            base_fields = {'id', 'is_deleted', 'deleted_at', 'created_at', 'updated_at'}
            
            for line in columns_raw.split(','):
                line = line.strip()
                if not line or line.upper().startswith('CONSTRAINT') or line.upper().startswith('PRIMARY KEY') or line.upper().startswith('FOREIGN KEY'):
                    continue
                    
                parts = line.split()
                if len(parts) < 2: continue
                
                col_name = parts[0].strip('"')
                col_type = parts[1]
                
                if col_name.lower() in base_fields:
                    continue
                    
                java_type = map_sql_type(col_type)
                java_name = ''.join(word if i == 0 else word.title() for i, word in enumerate(col_name.split('_')))
                
                fields.append({
                    'java_name': java_name,
                    'java_type': java_type,
                    'setter': 'set' + java_name[0].upper() + java_name[1:],
                    'getter': 'get' + java_name[0].upper() + java_name[1:],
                    'is_inherited': col_name.lower() == 'active'
                })
        else:
            print("Warning: Could not parse CREATE TABLE in migration. Using defaults.")
    else:
        print(f"Proceeding with base fields only.")
        fields.append({
            'java_name': 'active',
            'java_type': 'Boolean',
            'setter': 'setActive',
            'getter': 'getActive',
            'is_inherited': True
        })

    # 3. Prepare replacement blocks
    fields_block = ""
    gs_block = ""
    merge_block = ""
    test_merge_setup = ""
    test_merge_assertions = ""
    test_resource_setup = ""
    
    schema_fields_block = ""
    
    for f in fields:
        if not f.get('is_inherited'):
            fields_block += f"    private {f['java_type']} {f['java_name']};\n"
            schema_fields_block += f"        public {f['java_type']} {f['java_name']};\n"
            gs_block += f"    public {f['java_type']} {f['getter']}() {{ return {f['java_name']}; }}\n"
            gs_block += f"    public void {f['setter']}({f['java_type']} {f['java_name']}) {{ this.{f['java_name']} = {f['java_name']}; }}\n\n"
        
        merge_block += f"        if (incoming.{f['getter']}() != null)\n            existing.{f['setter']}(incoming.{f['getter']}());\n"
        
        # Test values
        val = 'null'
        if f['java_type'] == 'String': val = f'"test_{f["java_name"]}"'
        elif f['java_type'] == 'Integer': val = '1'
        elif f['java_type'] == 'Long': val = '1L'
        elif f['java_type'] == 'Boolean': val = 'true'
        elif f['java_type'] == 'java.math.BigDecimal': val = 'new java.math.BigDecimal("10.0")'
        elif f['java_type'] == 'LocalDateTime': val = 'java.time.LocalDateTime.now()'
        
        test_merge_setup += f"        incoming.{f['setter']}({val});\n"
        test_merge_assertions += f"        assertNotNull(existing.{f['getter']}());\n"
        
        if f['java_type'] != 'LocalDateTime' and val != 'null':
             test_resource_setup += f"        entity.{f['setter']}({val});\n"

    # 4. Create directories
    module_dir = f"src/main/java/com/app/modules/{module_lower}"
    test_dir = f"src/test/java/com/app/modules/{module_lower}"
    os.makedirs(module_dir, exist_ok=True)
    os.makedirs(test_dir, exist_ok=True)
    
    package = f"com.app.modules.{module_lower}"
    
    replacements = {
        '{{package}}': package,
        '{{module_name}}': module_name,
        '{{module_name_lower}}': module_lower,
        '{{module_snake}}': module_snake,
        '{{table_name}}': table_name_found,
        '{{fields}}': fields_block,
        '{{schema_fields}}': schema_fields_block,
        '{{getters_setters}}': gs_block,
        '{{merge_fields}}': merge_block,
        '{{test_merge_fields_setup}}': test_merge_setup,
        '{{test_merge_fields_assertions}}': test_merge_assertions,
        '{{test_resource_create_setup}}': test_resource_setup
    }
    
    def apply_replacements(content):
        for k, v in replacements.items():
            content = content.replace(k, v)
        return content

    # 5. Generate files
    files = [
        ('Model', f"{module_dir}/{module_name}Model.java"),
        ('Repository', f"{module_dir}/{module_name}Repository.java"),
        ('Service', f"{module_dir}/{module_name}Service.java"),
        ('Schemas', f"{module_dir}/{module_name}Schemas.java"),
        ('Resource', f"{module_dir}/{module_name}Resource.java"),
        ('ServiceUnitTest', f"{test_dir}/{module_name}ServiceUnitTest.java"),
        ('ResourceTest', f"{test_dir}/{module_name}ResourceTest.java"),
        ('SchemasUnitTest', f"{test_dir}/{module_name}SchemasUnitTest.java")
    ]
    
    for tpl_name, out_path in files:
        content = load_template(tpl_name)
        content = apply_replacements(content)
        with open(out_path, 'w') as f:
            f.write(content)
            
    # 6. Register feature in DatabaseBootstrap
    register_feature(module_snake, module_name)
            
    print(f"\nModule {module_name} generated successfully!")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python3 scripts/generate_module.py ModuleName")
        sys.exit(1)
    generate_module(sys.argv[1])
