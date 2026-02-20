# Wordle Multiplatform

Implementación del juego Wordle con arquitectura cliente-servidor. El servidor gestiona la lógica de juego y los clientes se conectan vía TCP para jugar en modo individual (PVE) o multijugador (PVP).

Desarrollado con **Kotlin Multiplatform**, **Compose Desktop** y un servidor **TCP en Kotlin/JVM**.

**Incluye:**
- Modo PVE con configuración clásica y personalizada
- Modo PVP con salas de espera y partidas sincronizadas en tiempo real
- Sistema de puntuación y récords globales por longitud de palabra
- Estadísticas por jugador
- Sistema de pistas con penalización
- Diccionario con palabras de 4 a 7 letras

---

## Video de demostración

> _Enlace al vídeo de YouTube_ <!-- TODO: añadir enlace -->

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
5. Al finalizar todas las rondas se muestra la puntuación total.

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

El archivo `server.properties` en la raíz del proyecto controla los parámetros del servidor:

```properties
server.host=localhost
server.port=5678
max.clients=10
```

El diccionario de palabras se encuentra en `palabras.json`. Cada entrada incluye la palabra y una pista opcional.

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

### Tipos de mensaje principales

| Dirección | Tipo | Descripción |
|-----------|------|-------------|
| Cliente → Servidor | `START_GAME` | Solicitar inicio de partida PVE |
| Cliente → Servidor | `GUESS` | Enviar intento de palabra |
| Cliente → Servidor | `CREATE_ROOM` | Crear sala PVP |
| Cliente → Servidor | `JOIN_ROOM` | Unirse a una sala PVP |
| Cliente → Servidor | `REQUEST_HINT` | Solicitar pista (con penalización) |
| Servidor → Cliente | `GAME_STARTED` | Inicio de ronda |
| Servidor → Cliente | `GUESS_RESULT` | Resultado del intento con colores |
| Servidor → Cliente | `ROUND_END` | Fin de ronda PVE |
| Servidor → Cliente | `PVP_ROUND_END` | Fin de ronda PVP con ranking |
| Servidor → Cliente | `ERROR` | Error con código y mensaje |

### Códigos de error

| Código | Causa |
|--------|-------|
| `INVALID_WORD` | La palabra no existe en el diccionario |
| `INVALID_LENGTH` | La palabra no tiene la longitud correcta |
| `ROUND_NOT_ACTIVE` | Intento enviado fuera de una ronda activa |
| `HINT_ALREADY_USED` | Ya se usó la pista en esta ronda |
| `ROOM_NOT_FOUND` | La sala indicada no existe |

---

## Roadmap

- [x] Modo PVE con configuración clásica y personalizada
- [x] Modo PVP con salas, sincronización en tiempo real y ranking
- [x] Sistema de pistas con penalización
- [x] Récords globales por modo y longitud de palabra
- [x] Estadísticas por jugador
- [ ] Soporte Android / iOS
- [ ] Interfaz web

---

## Licencia y autores

<!-- TODO: añadir licencia y autores -->
