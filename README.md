# Wordle Multiplatform

Implementación del juego Wordle con arquitectura cliente-servidor. El servidor gestiona la lógica de juego y los clientes se conectan vía TCP para jugar en modo individual (PVE) o multijugador (PVP).

Desarrollado con **Kotlin Multiplatform**, **Compose Desktop** y un servidor **TCP en Kotlin/JVM**.

**Incluye:**
- Modo PVE con configuración clásica y personalizada
- Modo PVP con salas de espera y partidas sincronizadas en tiempo real
- Sistema de puntuación y récords globales por longitud de palabra
- Estadísticas por jugador
- Sistema de pistas con penalización
- Reanudación automática de partidas PVE interrumpidas
- Diccionario con palabras de 4 a 7 letras

---

## Video de demostración

[> _Enlace al vídeo de YouTube_ <!-- TODO: añadir enlace -->](https://youtu.be/fzKzljBOYiA)

---

## Para usuarios

### Colores

| Color | Significado |
|-------|-------------|
| Verde | La letra está en la posición correcta |
| Amarillo | La letra existe en la palabra pero en otra posición |
| Gris | La letra no está en la palabra |

### Cómo jugar PvE

1. Conecta al servidor introduciendo la dirección IP y el puerto.
2. Elige el modo **PVE** y selecciona la dificultad (clásico o configuración personalizada).
3. Adivina la palabra oculta escribiendo o usando el teclado en pantalla y pulsando Enter.
4. Tienes un número limitado de intentos y 90 segundos por ronda.
5. Si la partida se interrumpe, al reconectarte se ofrecerá reanudarla desde la ronda en que quedó.
6. Al finalizar todas las rondas se muestra la puntuación total.

### Cómo jugar PvP

1. Conecta al servidor e introduce tu nombre de jugador.
2. Elige el modo **PVP** y crea una sala o únete a una existente.
3. El anfitrión inicia la partida cuando haya al menos 2 jugadores.
4. Todos los jugadores reciben la misma palabra y compiten en tiempo real.
5. Entre rondas se muestra el ranking parcial. Al finalizar se muestra el ranking final.

### Puntuación

```
Puntos por ronda = (7 - intentos_usados) × 1000 - segundos_transcurridos
```

- Usar la pista resta **1000 puntos** al resultado de la ronda.
- La puntuación mínima por ronda es 0.
- Los récords se guardan en un top 10 global separado por modo y longitud de palabra.

---

## Para desarrolladores

### Requisitos

- JDK 17 o superior
- Gradle (incluido mediante wrapper, no requiere instalación)
- Sistema operativo: Windows, macOS o Linux

### Compilación

```bash
./gradlew build
```

### Ejecución del servidor

```bash
./gradlew :server:run
```

El servidor abre una ventana de logs donde se puede monitorizar la actividad en tiempo real.

### Ejecución del cliente

```bash
./gradlew :composeApp:run
```

Se pueden lanzar múltiples instancias del cliente en la misma máquina.

### Configuración

El archivo `server/server.properties` controla los parámetros del servidor:

```properties
server.host=localhost
server.port=5678
max.clients=10
```

Los diccionarios de palabras se encuentran en `server/dictionaries/`. Hay un archivo por longitud de palabra (`20_solutions_4.json`, `20_solutions_5.json`, `20_solutions_6.json`, `20_solutions_7.json`). Cada entrada incluye la palabra y su pista.

---

## Arquitectura

### Componentes

```
wordle-multiplatformA/
├── composeApp/   # Cliente de escritorio (Kotlin Multiplatform + Compose Desktop)
├── server/       # Servidor TCP (Kotlin/JVM + Swing)
└── shared/       # Protocolo compartido (modelos y mensajes serializables)
```

### Responsabilidades

| Módulo | Responsabilidad |
|--------|-----------------|
| `composeApp` | Interfaz gráfica, entrada del usuario, comunicación con el servidor |
| `server` | Lógica de juego, gestión de partidas PVE/PVP, récords, estadísticas |
| `shared` | Definición de mensajes y modelos del protocolo, compartidos entre cliente y servidor |

### Patrón arquitectónico

Ambos módulos siguen **MVVM**:
- `server`: `model/` → `viewmodel/` → `view/` (Swing)
- `composeApp`: `model/` → `viewmodel/` → `view/` (Compose)

---

## Protocolo de comunicación

### Framing

La comunicación se realiza sobre **TCP**. Cada mensaje es una línea JSON terminada en `\n`.

```
{"type":"TIPO","payload":"..."}\n
```

### Tipos de mensaje

**Cliente → Servidor**

| Tipo | Descripción |
|------|-------------|
| `SET_PLAYER_NAME` | Establecer nombre del jugador |
| `START_GAME` | Solicitar inicio de partida PVE |
| `GUESS` | Enviar intento de palabra |
| `REQUEST_HINT` | Solicitar pista (con penalización) |
| `ABANDON_GAME` | Abandonar la partida activa |
| `GET_RECORDS` | Solicitar tabla de récords |
| `GET_STATS` | Solicitar estadísticas personales |
| `CREATE_ROOM` | Crear sala PVP |
| `JOIN_ROOM` | Unirse a una sala PVP |
| `LIST_ROOMS` | Solicitar lista de salas disponibles |
| `LEAVE_ROOM` | Salir de la sala actual |
| `START_PVP_GAME` | El host inicia la partida PVP |
| `READY_NEXT_ROUND` | Confirmar que el jugador está listo para la siguiente ronda PVP |

**Servidor → Cliente**

| Tipo | Descripción |
|------|-------------|
| `GAME_STARTED` | Confirmación de inicio de partida con su configuración |
| `GUESS_RESULT` | Resultado del intento con el estado de cada letra |
| `ROUND_END` | Fin de ronda PVE con puntuación |
| `GAME_END` | Fin de partida PVE con resultado global |
| `GAME_ABANDONED` | Confirmación de abandono de partida |
| `PVP_ROUND_END` | Fin de ronda PVP con ranking |
| `PVP_GAME_END` | Fin de partida PVP con ranking final |
| `OPPONENT_UPDATE` | Progreso del rival en la ronda PVP actual |
| `PLAYERS_STATUS_UPDATE` | Estado de todos los jugadores en la ronda PVP |
| `WAITING_FOR_PLAYERS` | Sala esperando a que todos confirmen estar listos |
| `ROOM_CREATED` | Confirmación de sala creada |
| `ROOM_JOINED` | Confirmación de unión a sala |
| `ROOM_UPDATE` | Actualización del estado de la sala (entradas/salidas) |
| `ROOM_LIST` | Lista de salas disponibles |
| `PLAYER_LEFT` | Notificación de que un jugador abandonó la sala |
| `RECORDS_RESPONSE` | Tabla de récords PVE y PVP |
| `STATS` | Estadísticas personales del jugador |
| `HINT_RESPONSE` | Texto de la pista solicitada |
| `SESSION_AVAILABLE` | Notificación de partida PVE pendiente de reanudar |
| `ERROR` | Error con código y mensaje descriptivo |

### Códigos de error

| Código | Causa |
|--------|-------|
| `INVALID_NAME` | Nombre de jugador vacío o superior a 20 caracteres |
| `INVALID_WORD` | La palabra no existe en el diccionario |
| `INVALID_LENGTH` | La palabra no tiene la longitud correcta |
| `ROUND_NOT_ACTIVE` | Intento o pista enviados fuera de una ronda activa |
| `HINT_ALREADY_USED` | Ya se usó la pista en esta ronda |
| `NO_ACTIVE_GAME` | Se intentó abandonar una partida sin tener ninguna activa |
| `ALREADY_IN_ROOM` | El jugador ya está en una sala PVP |
| `ROOM_NOT_FOUND` | La sala indicada no existe o está llena |
| `NOT_IN_ROOM` | Se envió un comando de sala sin estar en ninguna |
| `NOT_HOST` | Solo el creador de la sala puede iniciar la partida |

