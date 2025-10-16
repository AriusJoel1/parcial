#!/bin/bash

# Script de demostración completa del Sistema Bancario Distribuido
# Este script automatiza la ejecución de una demostración completa

echo "==============================================="
echo "  Sistema Bancario Distribuido - DEMO"
echo "==============================================="
echo ""

# Colores para mejor visualización
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Función para imprimir con color
print_step() {
    echo -e "${BLUE}[PASO $1]${NC} $2"
}

print_success() {
    echo -e "${GREEN}✓${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
}

# Función para limpiar procesos al salir
cleanup() {
    echo ""
    echo -e "${YELLOW}Limpiando procesos...${NC}"
    if [ ! -z "$SERVER_PID" ]; then
        kill $SERVER_PID 2>/dev/null
        print_success "Servidor Central detenido"
    fi

    pkill -f "worker_node.py" 2>/dev/null
    pkill -f "worker_go" 2>/dev/null
    print_success "Workers detenidos"

    echo ""
    echo -e "${GREEN}Demo finalizada. ¡Gracias!${NC}"
    exit 0
}

# Capturar Ctrl+C para limpiar
trap cleanup SIGINT SIGTERM

# ============================================
# PASO 1: Verificar requisitos
# ============================================
print_step "1" "Verificando requisitos..."
echo ""

# Verificar Java
if ! command -v java &> /dev/null; then
    print_error "Java no está instalado"
    exit 1
fi
print_success "Java: $(java -version 2>&1 | head -n 1)"

# Verificar Python
if ! command -v python &> /dev/null && ! command -v python3 &> /dev/null; then
    print_error "Python no está instalado"
    exit 1
fi
PYTHON_CMD=$(command -v python3 || command -v python)
print_success "Python: $($PYTHON_CMD --version)"

# Verificar Go
if ! command -v go &> /dev/null; then
    print_warning "Go no está instalado (opcional)"
    HAS_GO=false
else
    print_success "Go: $(go version)"
    HAS_GO=true
fi

echo ""

# ============================================
# PASO 2: Compilar componentes
# ============================================
print_step "2" "Compilando componentes..."
echo ""

# Compilar Java
echo "Compilando Java..."
javac CentralServer.java ChatClient.java 2>/dev/null
if [ $? -eq 0 ]; then
    print_success "CentralServer.java y ChatClient.java compilados"
else
    print_error "Error compilando Java"
    exit 1
fi

# Compilar Go (si está disponible)
if [ "$HAS_GO" = true ]; then
    echo "Compilando Go..."
    go build -o worker_go worker_node.go 2>/dev/null
    if [ $? -eq 0 ]; then
        print_success "worker_go compilado"
    fi

    go build -o bank_client_go bank_client.go 2>/dev/null
    if [ $? -eq 0 ]; then
        print_success "bank_client_go compilado"
    fi
fi

echo ""

# ============================================
# PASO 3: Limpiar datos previos
# ============================================
print_step "3" "Limpiando datos previos..."
rm -f worker_*_data.json 2>/dev/null
print_success "Archivos de datos limpiados"
echo ""

# ============================================
# PASO 4: Iniciar Servidor Central
# ============================================
print_step "4" "Iniciando Servidor Central en puerto 9000..."
java CentralServer > server.log 2>&1 &
SERVER_PID=$!
sleep 2

# Verificar que el servidor esté corriendo
if ps -p $SERVER_PID > /dev/null; then
    print_success "Servidor Central iniciado (PID: $SERVER_PID)"
else
    print_error "Error al iniciar el servidor"
    cat server.log
    exit 1
fi
echo ""

# ============================================
# PASO 5: Iniciar Workers
# ============================================
print_step "5" "Iniciando Workers..."
echo ""

# Workers Python
$PYTHON_CMD worker_node.py w0 > worker_w0.log 2>&1 &
sleep 1
print_success "Worker Python w0 iniciado"

$PYTHON_CMD worker_node.py w1 > worker_w1.log 2>&1 &
sleep 1
print_success "Worker Python w1 iniciado"

$PYTHON_CMD worker_node.py w2 > worker_w2.log 2>&1 &
sleep 1
print_success "Worker Python w2 iniciado"

# Workers Go (si está disponible)
if [ "$HAS_GO" = true ] && [ -f "worker_go" ]; then
    ./worker_go w_go1 > worker_go1.log 2>&1 &
    sleep 1
    print_success "Worker Go w_go1 iniciado"

    ./worker_go w_go2 > worker_go2.log 2>&1 &
    sleep 1
    print_success "Worker Go w_go2 iniciado"
fi

echo ""
print_warning "Esperando 3 segundos para que los workers se conecten..."
sleep 3
echo ""

# ============================================
# PASO 6: Crear cuentas
# ============================================
print_step "6" "Creando cuentas de prueba..."
echo ""

if [ "$HAS_GO" = true ] && [ -f "bank_client_go" ]; then
    print_warning "Usando cliente Go (más rápido)..."
    ./bank_client_go create
else
    print_warning "Usando cliente Python..."
    $PYTHON_CMD bank_client.py create
fi

echo ""
sleep 2

# ============================================
# PASO 7: Lanzar interfaz gráfica (ChatClient)
# ============================================
print_step "7" "Lanzando interfaz gráfica (ChatClient)..."
echo ""

print_warning "Se abrirá una ventana del ChatClient para consultas visuales"
print_warning "Puedes probar consultando cuentas: 1, 2, 100, 500, etc."
echo ""

java ChatClient > chatclient.log 2>&1 &
CHATCLIENT_PID=$!

print_success "ChatClient UI iniciado (PID: $CHATCLIENT_PID)"
echo ""

# Actualizar cleanup para incluir ChatClient
cleanup() {
    echo ""
    echo -e "${YELLOW}Limpiando procesos...${NC}"
    if [ ! -z "$SERVER_PID" ]; then
        kill $SERVER_PID 2>/dev/null
        print_success "Servidor Central detenido"
    fi

    if [ ! -z "$CHATCLIENT_PID" ]; then
        kill $CHATCLIENT_PID 2>/dev/null
        print_success "ChatClient detenido"
    fi

    pkill -f "worker_node.py" 2>/dev/null
    pkill -f "worker_go" 2>/dev/null
    pkill -f "ChatClient" 2>/dev/null
    print_success "Workers detenidos"

    echo ""
    echo -e "${GREEN}Demo finalizada. ¡Gracias!${NC}"
    exit 0
}

print_warning "Esperando 3 segundos para que explores la UI..."
sleep 3
echo ""

# ============================================
# PASO 8: Demostración de operaciones en terminal
# ============================================
print_step "8" "Ejecutando operaciones de demostración en terminal..."
echo ""

CLIENT_CMD="$PYTHON_CMD bank_client.py"
if [ "$HAS_GO" = true ] && [ -f "bank_client_go" ]; then
    CLIENT_CMD="./bank_client_go"
    print_warning "Usando cliente Go para la demo"
else
    print_warning "Usando cliente Python para la demo"
fi

echo ""
echo -e "${BLUE}══════════════════════════════════════════${NC}"
echo -e "${BLUE}  Operación 1: Consultar saldo de cuenta 1${NC}"
echo -e "${BLUE}══════════════════════════════════════════${NC}"
$CLIENT_CMD consult 1
echo ""
sleep 2

echo -e "${BLUE}══════════════════════════════════════════${NC}"
echo -e "${BLUE}  Operación 2: Transferir 50.0 de cuenta 1 a cuenta 2${NC}"
echo -e "${BLUE}══════════════════════════════════════════${NC}"
$CLIENT_CMD transfer 1 2 50.0
echo ""
sleep 2

echo -e "${BLUE}══════════════════════════════════════════${NC}"
echo -e "${BLUE}  Operación 3: Verificar saldo después de transferencia${NC}"
echo -e "${BLUE}══════════════════════════════════════════${NC}"
echo "Cuenta 1 (origen):"
$CLIENT_CMD consult 1
echo ""
echo "Cuenta 2 (destino):"
$CLIENT_CMD consult 2
echo ""
sleep 2

echo -e "${BLUE}══════════════════════════════════════════${NC}"
echo -e "${BLUE}  Operación 4: Crear préstamo de 5000.0 para cuenta 1${NC}"
echo -e "${BLUE}══════════════════════════════════════════${NC}"
$CLIENT_CMD create_loan 1 5000.0
echo ""
sleep 2

echo -e "${BLUE}══════════════════════════════════════════${NC}"
echo -e "${BLUE}  Operación 5: Consultar estado de préstamos de cuenta 1${NC}"
echo -e "${BLUE}══════════════════════════════════════════${NC}"
$CLIENT_CMD loan_status 1
echo ""
sleep 2

echo -e "${BLUE}══════════════════════════════════════════${NC}"
echo -e "${BLUE}  Operación 6: Pagar 1500.0 del préstamo 1${NC}"
echo -e "${BLUE}══════════════════════════════════════════${NC}"
$CLIENT_CMD pay_loan 1 1 1500.0
echo ""
sleep 2

echo -e "${BLUE}══════════════════════════════════════════${NC}"
echo -e "${BLUE}  Operación 7: Verificar préstamo después del pago${NC}"
echo -e "${BLUE}══════════════════════════════════════════${NC}"
$CLIENT_CMD loan_status 1
echo ""
sleep 2

echo -e "${BLUE}══════════════════════════════════════════${NC}"
echo -e "${BLUE}  Operación 8: Arqueo del sistema${NC}"
echo -e "${BLUE}══════════════════════════════════════════${NC}"
$CLIENT_CMD arqueo
echo ""
sleep 2

echo -e "${BLUE}══════════════════════════════════════════${NC}"
echo -e "${BLUE}  Operación 9: Prueba de estrés (50 transacciones)${NC}"
echo -e "${BLUE}══════════════════════════════════════════${NC}"
$CLIENT_CMD stress 50 100
echo ""
sleep 2

echo -e "${BLUE}══════════════════════════════════════════${NC}"
echo -e "${BLUE}  Operación 10: Arqueo final del sistema${NC}"
echo -e "${BLUE}══════════════════════════════════════════${NC}"
$CLIENT_CMD arqueo
echo ""

# ============================================
# PASO 9: Finalización
# ============================================
echo ""
echo -e "${GREEN}===============================================${NC}"
echo -e "${GREEN}  Demostración completada exitosamente!${NC}"
echo -e "${GREEN}===============================================${NC}"
echo ""

print_warning "Información adicional:"
echo "  • Logs del servidor: server.log"
echo "  • Logs de workers: worker_*.log"
echo "  • Logs del ChatClient: chatclient.log"
echo "  • Datos persistentes: worker_*_data.json"
echo ""

print_warning "La interfaz gráfica (ChatClient) sigue abierta para que puedas probarla"
echo "  • Puedes consultar cualquier cuenta (1-10000)"
echo "  • Ingresa el ID y presiona 'Consultar Saldo'"
echo ""

echo -e "${YELLOW}Presiona Ctrl+C para detener el sistema...${NC}"
echo ""

# Mantener el script corriendo hasta que el usuario presione Ctrl+C
wait $SERVER_PID
