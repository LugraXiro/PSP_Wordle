# Solución de Problemas - Wordle Multiplatform

## Problemas Comunes del Servidor

### 1. Error: "Address already in use" o "Puerto 5678 ya está en uso"

**Causa:** El puerto 5678 está siendo utilizado por otra instancia del servidor u otro proceso.

**Soluciones:**

#### Windows:
1. **Verificar qué proceso usa el puerto:**
   ```cmd
   netstat -ano | findstr :5678
   ```

2. **O usa el script incluido:**
   ```cmd
   check-port.bat
   ```

3. **Cerrar el proceso:**
   ```cmd
   taskkill /PID <numero_de_PID> /F
   ```
   Reemplaza `<numero_de_PID>` con el número que aparece en la última columna del comando anterior.

#### Linux/Mac:
1. **Verificar qué proceso usa el puerto:**
   ```bash
   lsof -i :5678
   # o
   netstat -tulpn | grep 5678
   ```

2. **O usa el script incluido:**
   ```bash
   ./check-port.sh
   ```

3. **Cerrar el proceso:**
   ```bash
   kill -9 <PID>
   ```
   Reemplaza `<PID>` con el número de proceso.

#### Cambiar el puerto (alternativa):
Edita el archivo `server.properties` y cambia el puerto:
```properties
server.port=5679
```

---

### 2. Error: "Diccionario no disponible" o "Archivo de diccionario no encontrado"

**Causa:** El archivo `palabras.json` no se encuentra en la ubicación correcta.

**Solución:**
1. Verifica que el archivo `palabras.json` esté en la raíz del proyecto
2. Si no existe, cópialo desde la carpeta de recursos
3. Asegúrate de ejecutar el servidor desde el directorio raíz del proyecto

---

### 3. Error al parsear el diccionario

**Causa:** El archivo `palabras.json` está corrupto o tiene formato incorrecto.

**Solución:**
1. Abre `palabras.json` y verifica que el formato JSON sea válido
2. Debe tener esta estructura:
   ```json
   {
     "palabras": [
       {"palabra": "HOLA", "pista": "Saludo"},
       {"palabra": "MUNDO", "pista": "Tierra"}
     ]
   }
   ```

---

### 4. El servidor se cierra inmediatamente

**Posibles causas:**
- Puerto en uso (ver solución #1)
- Diccionario no encontrado (ver solución #2)
- Error de configuración

**Solución:**
1. Revisa el archivo `wordle-logs.txt` para ver el error exacto
2. Las últimas líneas del log mostrarán qué salió mal
3. Busca líneas que empiecen con `[ERROR]`

---

## Logs del Servidor

El servidor guarda todos los eventos en `wordle-logs.txt`. Si tienes problemas:

1. Abre `wordle-logs.txt`
2. Busca las líneas más recientes (al final del archivo)
3. Busca mensajes de ERROR (marcados con ❌)
4. El log te dirá exactamente qué salió mal

**Ver últimas líneas del log (Linux/Mac/Git Bash):**
```bash
tail -50 wordle-logs.txt
```

**Ver últimas líneas del log (PowerShell):**
```powershell
Get-Content wordle-logs.txt -Tail 50
```

---

## Verificación Rápida

Antes de arrancar el servidor, verifica:

✅ El archivo `palabras.json` existe en la raíz del proyecto
✅ El archivo `server.properties` existe (se crea automáticamente si no existe)
✅ El puerto 5678 está libre (usa `check-port.bat` o `check-port.sh`)
✅ Tienes permisos para abrir puertos de red

---

## Scripts de Utilidad

- **`check-port.bat`** (Windows): Verifica si el puerto 5678 está en uso
- **`check-port.sh`** (Linux/Mac): Verifica si el puerto 5678 está en uso

Estos scripts te dirán si el puerto está libre o en uso, y cómo cerrar el proceso si es necesario.
