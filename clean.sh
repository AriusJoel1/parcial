#!/bin/bash

echo "============================================"
echo "Limpiando builds del Sistema Bancario"
echo "============================================"
echo ""

# Limpiar archivos .class de Java
echo "[1/4] Limpiando archivos .class de Java..."
find . -name "*.class" -type f -delete
if [ $? -eq 0 ]; then
    echo "✓ Archivos .class eliminados"
else
    echo "⚠ No se encontraron archivos .class"
fi
echo ""

# Limpiar binarios de Go
echo "[2/4] Limpiando binarios de Go..."
if [ -f "worker_go" ]; then
    rm -f worker_go
    echo "✓ worker_go eliminado"
fi
if [ -f "bank_client_go" ]; then
    rm -f bank_client_go
    echo "✓ bank_client_go eliminado"
fi
echo ""

# Limpiar archivos de datos de workers
echo "[3/4] Limpiando archivos de datos de workers..."
find . -name "worker_*_data.json" -type f -delete
if [ $? -eq 0 ]; then
    echo "✓ Archivos de datos de workers eliminados"
else
    echo "⚠ No se encontraron archivos de datos"
fi
echo ""

# Limpiar archivos temporales de Python
echo "[4/4] Limpiando archivos temporales de Python..."
find . -name "*.pyc" -type f -delete
find . -name "__pycache__" -type d -exec rm -rf {} + 2>/dev/null
if [ $? -eq 0 ]; then
    echo "✓ Archivos .pyc y __pycache__ eliminados"
else
    echo "⚠ No se encontraron archivos temporales de Python"
fi
echo ""

echo "============================================"
echo "Limpieza completada!"
echo "============================================"
echo ""
echo "Para recompilar todo, ejecuta:"
echo "  bash compile_all.sh"
echo ""
