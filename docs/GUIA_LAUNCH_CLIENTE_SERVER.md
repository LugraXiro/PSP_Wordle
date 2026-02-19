# Guia de lanzamiento - Wordle Multiplatform

## Servidor

### Comprobar si el puerto esta en uso

El servidor usa el puerto configurado en `server/server.properties` (por defecto `5678`).

**Windows (PowerShell o CMD):**
```bash
netstat -ano | findstr ":5678"
```

**Linux/Mac:**
```bash
lsof -i :5678
```

Si aparece una linea con `LISTENING`, el puerto esta ocupado. El numero al final de la linea es el PID del proceso.

### Identificar y matar el proceso que ocupa el puerto

**Windows:**
```bash
# Ver que proceso usa el puerto (el PID aparece en la ultima columna)
netstat -ano | findstr ":5678"

# Matar el proceso por PID (sustituir 12345 por el PID real)
taskkill /PID 12345 /F
```

**Linux/Mac:**
```bash
# Ver que proceso usa el puerto
lsof -i :5678

# Matar el proceso por PID
kill -9 12345
```

### Levantar el servidor

Desde la raiz del proyecto:

```bash
./gradlew :server:run
```

Esto compila y arranca el servidor. Se abrira automaticamente la ventana de logs en tiempo real. El servidor queda escuchando en `localhost:5678`.

Para verificar que arranca correctamente, la ventana de logs debe mostrar las lineas de inicio con `[SERVER]` en verde.

---

## Cliente

### Lanzar un cliente

Desde la raiz del proyecto:

```bash
./gradlew :composeApp:run
```

Cada cliente que se lance tendra un ID unico de 4 caracteres (ej: `CLIENT-a1b2`) que aparecera en la ventana de logs del servidor para diferenciarlo de otros clientes.

### Lanzar multiples clientes

Simplemente abre varias terminales y ejecuta el mismo comando en cada una:

```bash
# Terminal 1
./gradlew :composeApp:run

# Terminal 2
./gradlew :composeApp:run

# Terminal 3
./gradlew :composeApp:run
```

Cada instancia se conectara al servidor con su propio ID y aparecera con un color distinto en los logs.

---

## Orden de lanzamiento

1. Primero levantar el servidor (`./gradlew :server:run`)
2. Esperar a que la ventana de logs muestre "SERVIDOR OPERATIVO"
3. Lanzar los clientes que se necesiten (`./gradlew :composeApp:run`)

---

## Parar los servicios

### Parar el cliente

El cliente tiene ventana grafica: simplemente **cierra la ventana** o pulsa `Ctrl+C` en la terminal donde se lanzo.

### Parar el servidor

El servidor es mas complejo porque aunque abre una ventana de logs, **cerrar esa ventana NO detiene el proceso**: el servidor sigue ejecutandose en background y el puerto sigue ocupado.

Hay tres formas de pararlo:

#### Opcion 1 (recomendada): Ctrl+C en la terminal

Pulsa `Ctrl+C` en la terminal donde ejecutaste `./gradlew :server:run`. Esto envia una senal de interrupcion al proceso de Gradle y detiene el servidor limpiamente.

#### Opcion 2: Matar por PID (si la terminal esta cerrada)

Si ya no tienes la terminal, identifica el proceso que ocupa el puerto y matalo:

**Windows (PowerShell o CMD):**
```bash
# 1. Ver el PID del proceso que ocupa el puerto
netstat -ano | findstr ":5678"

# 2. Matar ese proceso (sustituir 12345 por el PID real)
taskkill /PID 12345 /F
```

**Linux/Mac:**
```bash
# 1. Ver el PID
lsof -i :5678

# 2. Matar el proceso
kill -9 12345
```

#### Opcion 3: Matar todos los procesos Java (nuclear)

Si no encuentras el PID o hay varios procesos Gradle colgados:

**Windows:**
```bash
taskkill /IM java.exe /F
```

**Linux/Mac:**
```bash
pkill -f java
```

> **Atencion:** esta opcion mata *todos* los procesos Java en ejecucion, no solo el servidor. Usa solo si las otras opciones fallan.

---

## Resumen rapido

| Accion | Comando |
|---|---|
| Comprobar puerto (Windows) | `netstat -ano \| findstr ":5678"` |
| Matar proceso (Windows) | `taskkill /PID <PID> /F` |
| Levantar servidor | `./gradlew :server:run` |
| Levantar cliente | `./gradlew :composeApp:run` |
| **Parar servidor** | `Ctrl+C` en la terminal |
| **Parar servidor (forzado)** | `taskkill /PID <PID> /F` |
