# Guía de presentación en vídeo

Checklist ordenado por bloque de evaluación. Márcalo antes de grabar.

---

## Preparación previa al vídeo

- [ ] Servidor arrancado (`./gradlew :server:run`)
- [ ] Al menos dos instancias del cliente abiertas para PVP
- [ ] `wordle-logs.txt` limpio (botón "Limpiar" en la ventana de logs)
- [ ] `server/records.json` con algún récord previo (para mostrar la pantalla)
- [ ] Ventana del servidor y ventanas de clientes visibles y ordenadas

---

## 1. Funcionalidad (40%)

### 1.1 El juego funciona correctamente
- [ ] Conectar un cliente al servidor → menú principal visible
- [ ] Introducir una palabra → ver colores por letra (verde / amarillo / gris)
- [ ] Ganar una ronda y ver el mensaje de victoria con puntuación
- [ ] Perder una ronda y ver la palabra correcta revelada

### 1.2 Modo PVE
- [ ] Seleccionar modo PVE en el menú
- [ ] Mostrar la pantalla de selección de modo (Clásico / Personalizado)
- [ ] En modo Personalizado: ajustar longitud de palabra, rondas, intentos, guardar récords
- [ ] Mostrar pista y explicar la penalización de puntuación
- [ ] Completar una partida multi-ronda y ver el marcador final

### 1.3 Modo PVP
- [ ] Cliente A crea una sala → se muestra el ID de sala
- [ ] Cliente B se une con el ID → aparece en el lobby de ambos
- [ ] El host inicia la partida
- [ ] Ambos clientes juegan la misma palabra simultáneamente
- [ ] Mostrar que se ven los intentos del rival en tiempo real (o al terminar la ronda)
- [ ] Finalizar la ronda y ver el marcador comparativo

### 1.4 Sistema de récords
- [ ] Completar una partida PVE con récords activados
- [ ] Navegar a la pantalla de Records → ver la puntuación registrada
- [ ] Mostrar que se separan por longitud de palabra y modo (PVE / PVP)

### 1.5 Configuración desde archivos
- [ ] Abrir `server/server.properties` y señalar puerto, maxClients
- [ ] Mostrar que el servidor arranca leyendo esa configuración en los logs

---

## 2. Comunicación en Red (30%)

### 2.1 Protocolo bien diseñado

**Cómo funciona (para defenderlo):**

Todo mensaje que viaja por la red tiene siempre la misma forma:
```json
{ "type": "GUESS", "payload": "{\"word\":\"perro\",\"attemptNumber\":1}" }
```
- `type` → identifica la acción (string)
- `payload` → datos concretos serializados como JSON dentro del JSON

**Flujo completo de un mensaje (ejemplo: el jugador envía "PERRO"):**
1. `GameViewModel` llama a `networkClient.send("GUESS", GuessRequest("perro", 1))`
2. `NetworkClient` serializa el `Message` y lo escribe en el socket TCP (una línea de texto)
3. `ClientHandler` en el servidor lee la línea, deserializa el `Message` y hace `when(msg.type)`
4. Deserializa el payload como `GuessRequest` y llama a `currentGame.handleGuess()`
5. El servidor construye un `GuessResult` y lo envía de vuelta con `send("GUESS_RESULT", ...)`
6. El cliente lo recibe a través de un `SharedFlow` (flujo reactivo): la UI se actualiza automáticamente

**Por qué está bien diseñado:**
- **Separación de capas**: el socket no sabe qué significa un `GUESS`; el `ClientHandler` no sabe cómo pintar la pantalla
- **Extensible**: añadir un nuevo tipo de mensaje solo requiere una data class en `shared` + un `when` en servidor + reacción en ViewModel; el transporte no cambia
- **Tipo-seguro**: `@Serializable` hace que Kotlin genere el código JSON automáticamente; imposible desincronizar cliente y servidor porque comparten los mismos tipos en `shared`
- **Reactivo, no polling**: el servidor empuja mensajes cuando ocurre algo; el cliente no pregunta cada X segundos

- [ ] Abrir `shared/src/.../protocol/Messages.kt` → mostrar los tipos de mensaje
- [ ] Señalar la estructura `Message(type, payload)` y que el payload es JSON tipado
- [ ] Mencionar que cliente y servidor comparten los mismos modelos (módulo `shared`)
- [ ] Señalar `NetworkClient.messagesFlow` (SharedFlow) como mecanismo reactivo de recepción

### 2.2 Gestión de múltiples clientes
- [ ] Tener dos clientes conectados al mismo tiempo en la ventana de logs
- [ ] Mostrar que cada cliente tiene un ID único (`[a1b2c3d4]`) en los logs
- [ ] Señalar el `ClientHandler` — una instancia por cliente en su propia corrutina

### 2.3 Manejo de errores y desconexiones
- [ ] Con un cliente en partida PVE: cerrar la ventana del cliente
- [ ] Mostrar en logs que el servidor detecta la desconexión y guarda la sesión
- [ ] Volver a conectar con el mismo nombre → aparece el diálogo de reanudación
- [ ] Reanudar la partida desde donde se dejó
- [ ] Mencionar el `soTimeout = 30s` que evita bloqueos si el servidor deja de responder
- [ ] Mencionar el sistema de reconexión automática del cliente

### 2.4 Validación de datos en el servidor
- [ ] En los logs, mostrar que un guess con longitud incorrecta es rechazado (`INVALID_LENGTH`)
- [ ] Mostrar que palabras no válidas son rechazadas (`INVALID_WORD`)
- [ ] Mencionar la validación de `playerName` (no vacío, máximo 20 caracteres)

---

## 3. Interfaz de Usuario (15%)

### 3.1 UI clara e intuitiva
- [ ] Recorrer el flujo completo: conexión → nombre → menú → selección de modo → partida → records
- [ ] Mostrar que cada pantalla tiene un propósito claro y botón de volver

### 3.2 Feedback visual apropiado
- [ ] Mostrar la animación de revelación de colores tras enviar una palabra
- [ ] Mostrar el cambio de color del teclado visual según el estado de las letras
- [ ] Mostrar el feedback de la pista (letra revelada con penalización visible)
- [ ] Mostrar mensajes de victoria/derrota con la puntuación

### 3.3 Interfaz optimizada para Desktop Windows
- [ ] Redimensionar la ventana a tamaño pequeño → el grid de letras escala automáticamente
- [ ] Redimensionar a tamaño grande → las tarjetas de menú no se expanden infinitamente (máximo definido)
- [ ] Mostrar que la ventana no puede reducirse por debajo del tamaño mínimo (600×700)

### 3.4 Buena experiencia de usuario
- [ ] Mostrar el diálogo de reanudación de sesión con los datos de la partida guardada
- [ ] Mostrar el lobby PVP con la lista de salas disponibles
- [ ] Mostrar la pantalla de records con separación por longitud y modo

---

## 4. Código y Arquitectura (15%)

### 4.1 Código limpio y organizado
- [ ] Mostrar la estructura de paquetes del proyecto en el árbol de directorios
  - `composeApp/` → cliente (UI + ViewModel + NetworkClient)
  - `server/` → servidor (GameServer, ClientHandler, GameOrchestrator, managers)
  - `shared/` → protocolo compartido (Messages, Models, FileLogger)
- [ ] Abrir una clase con KDoc (`ClientHandler`, `RecordsManager`) y señalar la documentación

### 4.2 Uso apropiado de corrutinas
- [ ] Abrir `ClientHandler.kt` → señalar `coroutineScope`, `Dispatchers.IO`, `withContext`
- [ ] Abrir `GameViewModel.kt` → señalar `StateFlow`, `SharedFlow`, colector de conexión
- [ ] Mencionar que el servidor maneja cada cliente en su propia corrutina sin bloquear el hilo principal

### 4.3 Compartición de código KMP
- [ ] Abrir el módulo `shared/` → señalar `protocol/Messages.kt` y `protocol/Models.kt`
- [ ] Abrir `FileLogger` → señalar el patrón `expect/actual` (interfaz común, implementación JVM)
- [ ] Destacar que cliente y servidor usan exactamente los mismos tipos de mensaje

### 4.4 Gestión de estados
- [ ] Abrir `GameViewModel.kt` → señalar `GameUiState` y cómo cada campo de la UI proviene del estado
- [ ] Mostrar que la UI reacciona automáticamente a cambios (Compose + StateFlow)
- [ ] Señalar la gestión de estado por cliente en el servidor (`currentGame`, `currentPVPGame`, `currentRoomId`)

---

## Cierre del vídeo

- [ ] Mostrar la ventana de logs con líneas de servidor y cliente mezcladas con colores distintos
- [ ] Usar el botón "Apagar servidor" para un cierre limpio
- [ ] Mencionar brevemente el `wordle-logs.txt` como herramienta de diagnóstico
