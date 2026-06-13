import xml.etree.ElementTree as ET
import os

def check_coverage():
    xml_path = 'target/jacoco-report/jacoco.xml'
    if not os.path.exists(xml_path):
        print(f"Erro: Arquivo {xml_path} não encontrado. Execute 'make test' primeiro.")
        return

    tree = ET.parse(xml_path)
    root = tree.getroot()

    print("\n--- Detalhes de Cobertura (Java) ---")
    
    total_instructions = 0
    covered_instructions = 0
    
    for package in root.findall('package'):
        package_name = package.get('name').replace('/', '.')
        for sourcefile in package.findall('sourcefile'):
            file_name = sourcefile.get('name')
            
            line_counter = sourcefile.find("counter[@type='LINE']")
            if line_counter is None:
                continue
                
            missed = int(line_counter.get('missed'))
            covered = int(line_counter.get('covered'))
            
            if missed > 0:
                print(f"File: {package_name}.{file_name}")
                uncovered_lines = []
                for line in sourcefile.findall('line'):
                    if int(line.get('ci')) == 0:
                        uncovered_lines.append(line.get('nr'))
                
                if uncovered_lines:
                    print(f"  - Uncovered lines: {', '.join(uncovered_lines)}")
                print()

    # Total coverage
    for counter in root.findall('counter'):
        if counter.get('type') == 'INSTRUCTION':
            missed = int(counter.get('missed'))
            covered = int(counter.get('covered'))
            total = missed + covered
            percentage = (covered / total * 100) if total > 0 else 100
            print(f"Cobertura total (Instruções): {percentage:.2f}%")
            
            # Print report URL
            report_path = os.path.abspath('target/jacoco-report/index.html')
            print(f"Relatório detalhado: file://{report_path}")

            if percentage < 100:
                print("❌ ERRO: A cobertura de testes está abaixo de 100%!")
                exit(1)

    print("✅ Sucesso: Cobertura total alcançada (100%)!")

if __name__ == "__main__":
    check_coverage()
