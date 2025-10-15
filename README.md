# Parcial CC4P1 - Sistema Bancario Distribuido

## Contenido:

### Servidor Central
- **CentralServer.java**: Servidor central (Java) - Coordina todos los workers y clientes

### Workers (3 lenguajes diferentes)
- **worker_node.py**: Nodo trabajador (Python)
- **worker_node.go**: Nodo trabajador (Go) - Nuevo!

### Clientes
- **ChatClient.java**: Cliente chat con UI (Java Swing)
- **bank_client.py**: Cliente bancario (Python, terminal)
- **bank_client.go**: Cliente bancario (Go, terminal) - Nuevo!

### Scripts
- **compile_all.sh**: Script para compilar todos los componentes
- **clean.sh**: Script para limpiar builds y archivos temporales
- **run_workers.sh**: Script para iniciar varios workers

## Arquitectura

### Servidor Central (Java)
- Puerto: 9000
- Acepta conexiones de: WORKER, CLIENT_CHAT, CLIENT_BANK
- Coordina particionamiento por módulo: `accountId % numWorkers`
- Protocolo: líneas de texto con campos separados por '|'
- **Nuevas funcionalidades:**
  - Sistema completo de préstamos (CRUD)
  - Función de arqueo para auditoría
  - Bug de replicación corregido

### Workers (Python y Go)
- Almacenan particiones de datos en archivos JSON
- Operaciones: CREATE_ACCOUNT, CONSULTAR_CUENTA, DEBIT, CREDIT, RECORD_TX
- **Nuevas operaciones:**
  - CREAR_PRESTAMO
  - PAGAR_PRESTAMO
  - ESTADO_PAGO_PRESTAMO
  - ARQUEO

### Clientes
- **CLIENT_CHAT**: Interfaz gráfica (Java Swing) para consultar saldos
- **CLIENT_BANK**: Terminal (Python/Go) para operaciones bancarias completas

## Requisitos

- **Java 8+** (JDK 8 o superior)
- **Python 3.8+**
- **Go 1.18+** (para workers y cliente Go)
- Ejecutar en localhost para la demo

## Instrucciones de Instalación y Ejecución

### 1. Compilar componentes

#### Compilar todo automáticamente:
```bash
bash compile_all.sh
```

#### O compilar manualmente:

**Java:**
```bash
javac CentralServer.java ChatClient.java
```

**Go:**
```bash
go build -o worker_go worker_node.go
go build -o bank_client_go bank_client.go
```

### 1.1. Limpiar builds (opcional)

Para eliminar todos los archivos compilados y datos temporales:
```bash
bash clean.sh
```

Esto eliminará:
- Archivos `.class` de Java
- Binarios `worker_go` y `bank_client_go`
- Archivos de datos `worker_*_data.json`
- Cache de Python (`__pycache__`, `*.pyc`)

### 2. Iniciar el servidor central
```bash
java CentralServer
```

### 3. Iniciar workers (en nuevas terminales)

#### Workers Python:
```bash
python worker_node.py w0
python worker_node.py w1
python worker_node.py w2
```

#### Workers Go:
```bash
./worker_go w_go1
./worker_go w_go2
```

#### O usar script (background):
```bash
bash run_workers.sh
```

### 4. Crear 10,000 cuentas

#### Con Python:
```bash
python bank_client.py create
```

#### Con Go:
```bash
./bank_client_go create
```

Esto creará **10,000 cuentas** con saldo inicial 100.0.

### 5. Operaciones disponibles

#### Consultar saldo (ChatClient UI):
```bash
java ChatClient
```
En la UI, ingresar ID de cuenta y presionar "Consultar Saldo".

#### Consultar saldo (Terminal):
```bash
# Python
python bank_client.py consult <id>

# Go
./bank_client_go consult <id>
```

#### Transferencias:
```bash
# Python
python bank_client.py transfer 1 2 25.5

# Go
./bank_client_go transfer 1 2 25.5
```

#### Crear préstamo:
```bash
# Python
python bank_client.py create_loan 1 5000.0

# Go
./bank_client_go create_loan 1 5000.0
```

#### Pagar préstamo:
```bash
# Python
python bank_client.py pay_loan 1 1 500.0

# Go
./bank_client_go pay_loan 1 1 500.0
```

#### Consultar estado de préstamos:
```bash
# Python
python bank_client.py loan_status 1

# Go
./bank_client_go loan_status 1
```

#### Arqueo del sistema:
```bash
# Python
python bank_client.py arqueo

# Go
./bank_client_go arqueo
```

#### Stress test:
```bash
# Python (200 transacciones, cuentas 1-10000)
python bank_client.py stress 200 10000

# Go (más rápido debido a goroutines)
./bank_client_go stress 200 10000

# Recomendación para prueba rápida:
python bank_client.py stress 200 100
./bank_client_go stress 200 100
```

## Nuevas Funcionalidades Implementadas

### ✅ Sistema Completo de Préstamos
- **CREAR_PRESTAMO**: Crea préstamos con monto total y monto pendiente
- **PAGAR_PRESTAMO**: Permite pagos parciales o totales
- **ESTADO_PAGO_PRESTAMO**: Consulta detallada de préstamos con:
  - ID del préstamo
  - Monto total
  - Monto pagado
  - Monto pendiente
  - Estado (Activo/Cancelado)

### ✅ Función de Arqueo
- Suma total de saldos en todos los workers
- Cuenta total de cuentas en el sistema
- Útil para auditoría y verificación de integridad

### ✅ Bug de Replicación Corregido
- **Problema anterior**: Las transferencias se replicaban en TODOS los workers
- **Solución**: Ahora usa particionamiento correcto con `accountId % numWorkers`
- Evita duplicación de operaciones

### ✅ Soporte Multi-lenguaje (Caso 3 del PDF)
- **Lenguaje 1 (LP1)**: Java - Servidor Central y Cliente Chat
- **Lenguaje 2 (LP2)**: Python - Workers y Cliente Bancario
- **Lenguaje 3 (LP3)**: Go - Workers y Cliente Bancario

### ✅ Capacidad Aumentada
- Sistema ahora soporta **10,000 cuentas** (requisito del PDF cumplido)
- Stress test configurado para 10,000 cuentas por defecto

## Estructura de Datos

### Cuentas
```json
{
  "id": 123,
  "id_cliente": 123,
  "balance": 1500.50,
  "loans": [1, 2]
}
```

### Préstamos
```json
{
  "id": 1,
  "id_cliente": 123,
  "monto": 5000.0,
  "monto_pendiente": 3500.0,
  "estado": "Activo",
  "fecha_solicitud": "2025-10-15 14:30:00"
}
```

### Transacciones
```json
{
  "type": "transfer_out",
  "to": "456",
  "amount": 250.0
}
```

## Queries Implementadas (según PDF)

### Query 1: CONSULTAR_CUENTA ✅
- Parámetros: ID_CUENTA
- Validación: Existencia de cuenta
- Salida: Detalles completos de la cuenta

### Query 2: TRANSFERIR_CUENTA ✅
- Parámetros: ID_ORIGEN, ID_DESTINO, MONTO
- Validación: Existencia de cuentas + Saldo suficiente
- Acciones: DEBIT + CREDIT + Registro de transacción
- Compensación automática en caso de fallo

### Query 3: ESTADO_PAGO_PRESTAMO ✅
- Parámetros: ID_CUENTA
- Validación: Existencia de cuenta y préstamos
- Salida: Listado detallado con montos y estados

## Protocolo de Comunicación

Todas las comunicaciones usan el formato:
```
OPERACION|PARAM1|PARAM2|...|PARAMN\n
```

Ejemplos:
```
CONSULTAR_CUENTA|123
TRANSFERIR_CUENTA|123|456|500.50
CREAR_PRESTAMO|123|5000.0|5000.0
PAGAR_PRESTAMO|123|1|500.0
ARQUEO
```

## Ventajas de la Implementación Go

- **Mayor rendimiento**: Compilado vs interpretado
- **Concurrencia real**: Goroutines vs GIL de Python
- **Type safety**: Errores detectados en compilación
- **Binarios standalone**: No requiere runtime instalado
- **Paralelismo verdadero**: Usa múltiples cores eficientemente
