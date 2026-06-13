import sys
import os
import subprocess
import re

DRIVERS = {
    's3': {
        'name': 'S3',
        'extension_id': 'amazon-s3',
        'client_class': 'S3Client',
        'client_import': 'software.amazon.awssdk.services.s3.S3Client',
        'extra_dependencies': [
            {
                'groupId': 'software.amazon.awssdk',
                'artifactId': 'url-connection-client'
            }
        ],
        'extra_imports': """import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.core.sync.RequestBody;""",
        'driver_fields': """@Inject S3Client s3;
    @ConfigProperty(name = "storage.s3.bucket") String bucket;""",
        'put_bytes_logic': 's3.putObject(PutObjectRequest.builder().bucket(bucket).key(path).build(), RequestBody.fromBytes(content));',
        'put_stream_logic': 's3.putObject(PutObjectRequest.builder().bucket(bucket).key(path).build(), RequestBody.fromInputStream(content, content.available()));',
        'get_logic': 'return s3.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(path).build()).asByteArray();',
        'exists_logic': 's3.headObject(HeadObjectRequest.builder().bucket(bucket).key(path).build()); return true;',
        'delete_logic': 's3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(path).build());',
        'get_url_logic': 'return "https://" + bucket + ".s3.amazonaws.com/" + path;',
        'health_logic': 's3.listBuckets(); return true;',
        'helper_methods': '',
        'setup_logic': """driver.s3 = client;
        driver.bucket = "test-bucket";""",
        'mock_put_get': 'Mockito.when(client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(software.amazon.awssdk.core.ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), "hello".getBytes()));',
        'mock_put_stream': 'Mockito.when(client.headObject(any(HeadObjectRequest.class))).thenReturn(HeadObjectResponse.builder().build());',
        'mock_delete': '',
        'mock_health': 'Mockito.when(client.listBuckets()).thenReturn(ListBucketsResponse.builder().build());',
        'mock_health_failure': 'Mockito.when(client.listBuckets()).thenThrow(new RuntimeException("error"));',
        'mock_exists_failure': 'Mockito.when(client.headObject(any(HeadObjectRequest.class))).thenThrow(NoSuchKeyException.builder().message("not found").build());',
        'mock_put_failure': 'Mockito.when(client.putObject(any(PutObjectRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class))).thenThrow(new RuntimeException("S3 Error"));',
        'mock_get_failure': 'Mockito.when(client.getObjectAsBytes(any(GetObjectRequest.class))).thenThrow(new RuntimeException("S3 Error"));',
        'mock_delete_failure': 'Mockito.when(client.deleteObject(any(DeleteObjectRequest.class))).thenThrow(new RuntimeException("S3 Error"));',
        'extra_stream_test': '',
        'env_vars': {
            'STORAGE_S3_BUCKET': 'my-bucket',
            'QUARKUS_S3_AWS_REGION': 'us-east-1',
            'QUARKUS_S3_AWS_CREDENTIALS_TYPE': 'static',
            'QUARKUS_S3_DEVSERVICES_ENABLED': 'false',
            'QUARKUS_S3_AWS_CREDENTIALS_STATIC_PROVIDER_ACCESS_KEY_ID': 'dummy-key',
            'QUARKUS_S3_AWS_CREDENTIALS_STATIC_PROVIDER_SECRET_ACCESS_KEY': 'dummy-secret'
        }
    },
    'gcs': {
        'name': 'GCS',
        'extension_id': 'google-cloud-storage',
        'client_class': 'Storage',
        'client_import': 'com.google.cloud.storage.Storage',
        'extra_dependencies': [],
        'extra_imports': """import com.google.cloud.storage.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;""",
        'driver_fields': """@Inject Storage storage;
    @ConfigProperty(name = "storage.gcs.bucket") String bucket;""",
        'put_bytes_logic': 'storage.create(BlobInfo.newBuilder(bucket, path).build(), content);',
        'put_stream_logic': 'storage.create(BlobInfo.newBuilder(bucket, path).build(), inputStreamToBytes(content));',
        'get_logic': 'return storage.readAllBytes(bucket, path);',
        'exists_logic': 'return storage.get(bucket, path) != null;',
        'delete_logic': 'storage.delete(bucket, path);',
        'get_url_logic': 'return "https://storage.googleapis.com/" + bucket + "/" + path;',
        'health_logic': 'storage.get(bucket); return true;',
        'helper_methods': """private byte[] inputStreamToBytes(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[16384];
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }""",
        'setup_logic': """driver.storage = client;
        driver.bucket = "test-bucket";""",
        'mock_put_get': """Mockito.when(client.readAllBytes(anyString(), anyString())).thenReturn("hello".getBytes());
        Mockito.when(client.get(anyString(), anyString())).thenReturn(Mockito.mock(Blob.class));""",
        'mock_put_stream': 'Mockito.when(client.get(anyString(), anyString())).thenReturn(Mockito.mock(Blob.class));',
        'mock_delete': 'Mockito.when(client.delete(anyString(), anyString())).thenReturn(true);',
        'mock_health': 'Mockito.when(client.get(anyString())).thenReturn(Mockito.mock(Bucket.class));',
        'mock_health_failure': 'Mockito.when(client.get(anyString())).thenThrow(new RuntimeException("error"));',
        'mock_exists_failure': """Mockito.when(client.get(anyString(), anyString())).thenReturn(null);
        assertFalse(driver.exists("not-found.txt"));
        Mockito.when(client.get(anyString(), anyString())).thenThrow(new RuntimeException("error"));""",
        'mock_put_failure': 'Mockito.when(client.create(any(BlobInfo.class), any(byte[].class))).thenThrow(new RuntimeException("GCS Error"));',
        'mock_get_failure': 'Mockito.when(client.readAllBytes(anyString(), anyString())).thenThrow(new RuntimeException("GCS Error"));',
        'mock_delete_failure': 'Mockito.when(client.delete(anyString(), anyString())).thenThrow(new RuntimeException("GCS Error"));',
        'extra_stream_test': """// Simular erro de leitura no InputStream
        InputStream badStream = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("Stream Read Error");
            }
        };
        assertThrows(RuntimeException.class, () -> driver.put("error.txt", badStream));""",
        'env_vars': {
            'STORAGE_GCS_BUCKET': 'my-bucket',
            'QUARKUS_GOOGLE_CLOUD_PROJECT_ID': 'my-project'
        }
    },
    'azure': {
        'name': 'Azure',
        'extension_id': 'azure-storage-blob',
        'client_class': 'BlobServiceClient',
        'client_import': 'com.azure.storage.blob.BlobServiceClient',
        'extra_dependencies': [],
        'extra_imports': """import com.azure.storage.blob.*;
import com.azure.core.util.BinaryData;""",
        'driver_fields': """@Inject BlobServiceClient blobServiceClient;
    @ConfigProperty(name = "storage.azure.container") String container;""",
        'put_bytes_logic': 'blobServiceClient.getBlobContainerClient(container).getBlobClient(path).upload(BinaryData.fromBytes(content));',
        'put_stream_logic': 'blobServiceClient.getBlobContainerClient(container).getBlobClient(path).upload(BinaryData.fromStream(content));',
        'get_logic': 'return blobServiceClient.getBlobContainerClient(container).getBlobClient(path).downloadContent().toBytes();',
        'exists_logic': 'return blobServiceClient.getBlobContainerClient(container).getBlobClient(path).exists();',
        'delete_logic': 'blobServiceClient.getBlobContainerClient(container).getBlobClient(path).delete();',
        'get_url_logic': 'return blobServiceClient.getBlobContainerClient(container).getBlobClient(path).getBlobUrl();',
        'health_logic': 'blobServiceClient.getBlobContainerClient(container).exists(); return true;',
        'helper_methods': '',
        'setup_logic': """BlobContainerClient containerMock = Mockito.mock(BlobContainerClient.class);
        BlobClient blobMock = Mockito.mock(BlobClient.class);
        Mockito.when(client.getBlobContainerClient(anyString())).thenReturn(containerMock);
        Mockito.when(containerMock.getBlobClient(anyString())).thenReturn(blobMock);
        Mockito.when(blobMock.getBlobUrl()).thenReturn("http://azure/storage/test.txt");
        driver.blobServiceClient = client;
        driver.container = "test-container";""",
        'mock_put_get': """BlobContainerClient containerMock = client.getBlobContainerClient("any");
        BlobClient blobMock = containerMock.getBlobClient("any");
        Mockito.when(blobMock.downloadContent()).thenReturn(BinaryData.fromBytes("hello".getBytes()));
        Mockito.when(blobMock.exists()).thenReturn(true);""",
        'mock_put_stream': """BlobClient blobMock = client.getBlobContainerClient("any").getBlobClient("any");
        Mockito.when(blobMock.exists()).thenReturn(true);""",
        'mock_delete': '',
        'mock_health': 'Mockito.when(client.getBlobContainerClient(any())).thenReturn(Mockito.mock(BlobContainerClient.class));',
        'mock_health_failure': 'Mockito.when(client.getBlobContainerClient(any())).thenThrow(new RuntimeException("error"));',
        'mock_exists_failure': """BlobClient blobMock = client.getBlobContainerClient("any").getBlobClient("any");
        Mockito.when(blobMock.exists()).thenReturn(false);
        assertFalse(driver.exists("not-found.txt"));
        Mockito.when(blobMock.exists()).thenThrow(new RuntimeException("error"));""",
        'mock_put_failure': """BlobClient blobMock = client.getBlobContainerClient("any").getBlobClient("any");
        Mockito.doThrow(new RuntimeException("Azure Error")).when(blobMock).upload(any(com.azure.core.util.BinaryData.class));""",
        'mock_get_failure': """BlobClient blobMock = client.getBlobContainerClient("any").getBlobClient("any");
        Mockito.when(blobMock.downloadContent()).thenThrow(new RuntimeException("Azure Error"));""",
        'mock_delete_failure': """BlobClient blobMock = client.getBlobContainerClient("any").getBlobClient("any");
        Mockito.doThrow(new RuntimeException("Azure Error")).when(blobMock).delete();""",
        'extra_stream_test': '',
        'env_vars': {
            'STORAGE_AZURE_CONTAINER': 'my-container',
            'QUARKUS_AZURE_STORAGE_BLOB_CONNECTION_STRING': 'your-connection-string'
        }
    }
}

def load_template(path):
    with open(path, 'r') as f:
        return f.read()

def run_command(command):
    print(f"Running: {command}")
    result = subprocess.run(command, shell=True, capture_output=True, text=True)
    if result.returncode != 0:
        print(f"Error: {result.stderr}")
    return result.stdout

def add_quarkus_extension(extension_id):
    print(f"📦 Installing extension: {extension_id}...")
    run_command(f"./mvnw quarkus:add-extension -Dextensions={extension_id}")

def add_extra_dependencies(dependencies):
    if not dependencies:
        return
    
    print("📦 Adding extra dependencies to pom.xml...")
    with open('pom.xml', 'r') as f:
        content = f.read()
    
    new_deps = ""
    for dep in dependencies:
        dep_str = f"""        <dependency>
            <groupId>{dep['groupId']}</groupId>
            <artifactId>{dep['artifactId']}</artifactId>
        </dependency>\n"""
        if dep_str.strip() not in content:
            new_deps += dep_str
            
    if new_deps:
        pos = content.find('<build>')
        if pos != -1:
            deps_pos = content.rfind('</dependencies>', 0, pos)
            if deps_pos != -1:
                content = content[:deps_pos] + new_deps + content[deps_pos:]
                with open('pom.xml', 'w') as f:
                    f.write(content)

def update_env_files(env_vars):
    for filename in ['.env', '.env.example']:
        if not os.path.exists(filename):
            continue
            
        print(f"⚙️ Updating {filename}...")
        with open(filename, 'r') as f:
            lines = f.readlines()
            
        for key, value in env_vars.items():
            if not any(line.startswith(f"{key}=") for line in lines):
                lines.append(f"{key}={value}\n")
                
        with open(filename, 'w') as f:
            f.writelines(lines)

def update_test_properties(driver_name, env_vars, driver_id):
    path = "src/test/resources/application.properties"
    if not os.path.exists(path):
        return
        
    print(f"⚙️ Updating {path}...")
    with open(path, 'r') as f:
        content = f.read()
    
    header = f"\n# Storage {driver_name}\n"
    if header not in content:
        content += header
        
    for key, value in env_vars.items():
        prop_key = key.lower().replace('_', '.')
        if f"{prop_key}=" not in content:
            content += f"{prop_key}={value}\n"
            
    # Add extra hacks for GCS
    if driver_id == 'gcs':
        if "quarkus.google.cloud.storage.base-url" not in content:
            content += "quarkus.google.cloud.storage.base-url=http://localhost:8081\n"
            
    with open(path, 'w') as f:
        f.write(content)

def generate_storage(driver_id):
    if driver_id not in DRIVERS:
        print(f"Error: Driver [{driver_id}] not supported.")
        sys.exit(1)
        
    config = DRIVERS[driver_id]
    driver_name = config['name']
    
    # 1. Add extension and dependencies
    add_quarkus_extension(config['extension_id'])
    add_extra_dependencies(config['extra_dependencies'])
    
    # 2. Generate Driver Class
    print(f"📝 Generating {driver_name}Driver.java...")
    tpl_path = "scripts/templates/storage/Driver.java.tpl"
    output_path = f"src/main/java/com/app/infrastructure/storage/drivers/{driver_name}Driver.java"
    
    content = load_template(tpl_path)
    replacements = {
        '{{DRIVER_NAME}}': driver_name,
        '{{DRIVER_LOWER}}': driver_id,
        '{{EXTRA_IMPORTS}}': config['extra_imports'],
        '{{DRIVER_FIELDS}}': config['driver_fields'],
        '{{PUT_BYTES_LOGIC}}': config['put_bytes_logic'],
        '{{PUT_STREAM_LOGIC}}': config['put_stream_logic'],
        '{{GET_LOGIC}}': config['get_logic'],
        '{{EXISTS_LOGIC}}': config['exists_logic'],
        '{{DELETE_LOGIC}}': config['delete_logic'],
        '{{GET_URL_LOGIC}}': config['get_url_logic'],
        '{{HEALTH_LOGIC}}': config['health_logic'],
        '{{HELPER_METHODS}}': config['helper_methods']
    }
    
    for k, v in replacements.items():
        content = content.replace(k, v)
        
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    with open(output_path, 'w') as f:
        f.write(content)
        
    # 3. Generate Test Class
    print(f"🧪 Generating {driver_name}DriverTest.java...")
    tpl_test_path = "scripts/templates/storage/DriverTest.java.tpl"
    output_test_path = f"src/test/java/com/app/infrastructure/storage/drivers/{driver_name}DriverTest.java"
    
    content_test = load_template(tpl_test_path)
    replacements_test = {
        '{{DRIVER_NAME}}': driver_name,
        '{{DRIVER_LOWER}}': driver_id,
        '{{CLIENT_CLASS}}': config['client_class'],
        '{{EXTRA_IMPORTS}}': config['extra_imports'],
        '{{SETUP_LOGIC}}': config['setup_logic'],
        '{{MOCK_PUT_GET}}': config['mock_put_get'],
        '{{MOCK_PUT_STREAM}}': config['mock_put_stream'],
        '{{MOCK_DELETE}}': config['mock_delete'],
        '{{MOCK_HEALTH}}': config['mock_health'],
        '{{MOCK_HEALTH_FAILURE}}': config['mock_health_failure'],
        '{{MOCK_EXISTS_FAILURE}}': config['mock_exists_failure'],
        '{{MOCK_PUT_FAILURE}}': config['mock_put_failure'],
        '{{MOCK_GET_FAILURE}}': config['mock_get_failure'],
        '{{MOCK_DELETE_FAILURE}}': config['mock_delete_failure'],
        '{{EXTRA_STREAM_TEST}}': config['extra_stream_test']
    }
    
    for k, v in replacements_test.items():
        content_test = content_test.replace(k, v)
        
    os.makedirs(os.path.dirname(output_test_path), exist_ok=True)
    with open(output_test_path, 'w') as f:
        f.write(content_test)
        
    # 4. Generate Mock Producer
    print(f"⚙️ Generating {driver_name}MockProducer.java...")
    tpl_mock_path = "scripts/templates/storage/MockProducer.java.tpl"
    output_mock_path = f"src/test/java/com/app/infrastructure/storage/drivers/{driver_name}MockProducer.java"
    
    content_mock = load_template(tpl_mock_path)
    replacements_mock = {
        '{{DRIVER_NAME}}': driver_name,
        '{{CLIENT_CLASS}}': config['client_class'],
        '{{CLIENT_IMPORT}}': config['client_import']
    }
    
    for k, v in replacements_mock.items():
        content_mock = content_mock.replace(k, v)
        
    os.makedirs(os.path.dirname(output_mock_path), exist_ok=True)
    with open(output_mock_path, 'w') as f:
        f.write(content_mock)
        
    # 5. Update environment files
    update_env_files(config['env_vars'])
    
    # 6. Update test properties
    update_test_properties(driver_name, config['env_vars'], driver_id)
    
    print(f"✅ Storage driver [{driver_name}] installed successfully!")
    print(f"💡 Set STORAGE_DISK={driver_id} in your .env to use it.")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python3 scripts/generate_storage.py <s3|gcs|azure>")
        sys.exit(1)
    generate_storage(sys.argv[1].lower())
