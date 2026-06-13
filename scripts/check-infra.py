import socket
import sys

def check_port(host, port):
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.settimeout(1)
        try:
            s.connect((host, port))
            return True
        except:
            return False

# Para o Java, o único serviço necessário no localhost para os testes é o Redis
infra = [
    (6379, "Redis")
]

failed = False
for port, name in infra:
    if not check_port('localhost', port):
        print(f"\033[91mERRO: {name} (porta {port}) não está rodando no localhost.\033[0m")
        failed = True

if failed:
    print("\n\033[93mOs testes de integração do Java dependem do Redis real.")
    print("Suba a infraestrutura antes de executar os testes:")
    print("  make infra-up\033[0m\n")
    sys.exit(1)

print("\033[92mInfraestrutura OK!\033[0m")
