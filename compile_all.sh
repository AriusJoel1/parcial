#!/bin/bash

echo "============================================"
echo "Compilando Sistema Bancario Distribuido"
echo "============================================"
echo ""

# Compilar Java
echo "[1/2] Compilando componentes Java..."
javac CentralServer.java ChatClient.java
if [ $? -eq 0 ]; then
    echo "✓ Java compilado exitosamente"
else
    echo "✗ Error compilando Java"
    exit 1
fi
echo ""

# Compilar Go
echo "[2/2] Compilando componentes Go..."
if command -v go &> /dev/null; then
    go build -o worker_go worker_node.go
    if [ $? -eq 0 ]; then
        echo "✓ worker_node.go compilado exitosamente"
    else
        echo "✗ Error compilando worker_node.go"
        exit 1
    fi

    go build -o bank_client_go bank_client.go
    if [ $? -eq 0 ]; then
        echo "✓ bank_client.go compilado exitosamente"
    else
        echo "✗ Error compilando bank_client.go"
        exit 1
    fi
else
    echo "⚠ Go no está instalado. Saltando compilación de Go..."
    echo "  Instala Go desde: https://golang.org/dl/"
fi

echo ""
echo "============================================"
echo "Compilación completada!"
echo "============================================"
echo ""
echo "Para iniciar el sistema:"
echo "  1. Terminal 1: java CentralServer"
echo "  2. Terminal 2: python worker_node.py w0"
echo "  3. Terminal 3: python worker_node.py w1"
echo "  4. Terminal 4: ./worker_go w_go1"
echo "  5. Terminal 5: python bank_client.py create"
echo ""
