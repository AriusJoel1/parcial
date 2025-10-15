# Parcial CC4P1 

Contenido:
- CentralServer.java         : Servidor central (Java)
- ChatClient.java           : Cliente chat con UI (Java Swing)
- worker_node.py            : Nodo trabajador (Python)
- bank_client.py            : Cliente bancario (Python, terminal)
- run_workers.sh            : Script para iniciar varios workers 
- README.md                 : Este archivo

Diseño simplificado:
- El CentralServer (puerto 9000) acepta conexiones de WORKER, CLIENT_CHAT y CLIENT_BANK.
- WORKER: nodos Python que almacenan particiones locales de cuentas en un archivo JSON.
- CLIENT_CHAT: cliente Java con una UI simple para consultar saldo de cuenta.
- CLIENT_BANK: cliente Python (terminal) para crear cuentas, transferir y stress test.
- Particionamiento: accountId % numWorkers (distribución simple). Replicación no estricta en esta demo.
- Protocolo: líneas de texto con campos separados por '|'. Ejemplo: "CONSULTAR_CUENTA|101".

Requisitos:
- Java 8+ (jdk 8 o superior)
- Python 3.8+
- Ejecutar en la misma máquina (localhost) para la demo.

Instrucciones rápidas:
1. Compilar Java:
   ```
   javac CentralServer.java ChatClient.java
   ```
2. Iniciar el servidor central:
   ```
   java CentralServer
   ```
3. En nuevas terminales, iniciar 3 workers:
   ```
   python worker_node.py w0
   python worker_node.py w1
   python worker_node.py w2
   ```
   o sino usar bash run_workers.sh se iniciarán en background

4. Crear cuentas:
   ```
   python bank_client.py create
   ```
   Esto creará 1000 cuentas con saldo inicial 100.0.

5. Consultar saldo (desde ChatClient UI):
   - Ejecutar:
     ```
     java ChatClient
     ```
     En la UI, poner la cuenta ID (1,) y presionar "Consultar Saldo (cuenta)".

6. Transferencias:
   ```
   python bank_client.py transfer 1 2 25.5
   ```

7. Stress test:
   ```
   python bank_client.py stress 200 1000
   ```
   
   Recomendacion (mas rapido)
   ```
   python bank_client.py stress 200 100
   ```
