#!/bin/bash

echo "Corrigiendo imports en archivos Kotlin..."

# Función para agregar import si no existe
add_import() {
    local file=$1
    local import_line=$2
    
    if ! grep -q "$import_line" "$file"; then
        # Agregar después de la línea "package"
        sed -i "/^package/a $import_line" "$file"
        echo "  ✅ Agregado $import_line en $file"
    fi
}

# Corregir HorizontalDivider
for file in $(grep -rl "HorizontalDivider" app/src/main/java/); do
    add_import "$file" "import androidx.compose.material3.HorizontalDivider"
done

# Corregir Alignment
for file in $(grep -rl "Alignment\." app/src/main/java/); do
    add_import "$file" "import androidx.compose.ui.Alignment"
done

# Corregir dp
for file in $(grep -rl "\.dp" app/src/main/java/); do
    add_import "$file" "import androidx.compose.ui.unit.dp"
done

# Corregir background
for file in $(grep -rl "\.background(" app/src/main/java/); do
    add_import "$file" "import androidx.compose.foundation.background"
done

# Corregir clickable
for file in $(grep -rl "\.clickable" app/src/main/java/); do
    add_import "$file" "import androidx.compose.foundation.clickable"
done

echo "✅ Correcciones completadas"
