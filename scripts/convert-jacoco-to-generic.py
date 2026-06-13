#!/usr/bin/env python3
import sys
import os

if len(sys.argv) < 2:
    print("Usage: convert-jacoco-to-generic.py <jacoco.xml>")
    sys.exit(1)

xml_path = sys.argv[1]
if not os.path.exists(xml_path):
    print(f"Error: {xml_path} not found")
    sys.exit(1)

import xml.etree.ElementTree as ET

tree = ET.parse(xml_path)
root = tree.getroot()

src_base = os.path.join(os.getcwd(), 'src/main/java') + os.sep
gen_root = ET.Element('coverage', version='1')
files = 0

for package in root.findall('package'):
    pkg_name = package.get('name', '').replace('/', os.sep)
    for sourcefile in package.findall('sourcefile'):
        fn = sourcefile.get('name', '')
        lang = sourcefile.get('lang', '')
        
        class_name = ET.SubElement(gen_root, 'file', path=f'src/main/java/{pkg_name}/{fn}')
        
        for line in sourcefile.findall('line'):
            nr = line.get('nr', '')
            ci = line.get('ci', '0')
            covered = 'true' if int(ci) > 0 else 'false'
            ET.SubElement(class_name, 'lineToCover', lineNumber=nr, covered=covered)
        
        files += 1

out_path = xml_path.replace('.xml', '.generic.xml')
ET.ElementTree(gen_root).write(out_path, encoding='utf-8', xml_declaration=True)
print(f"Converted {files} files to generic format: {out_path}")
