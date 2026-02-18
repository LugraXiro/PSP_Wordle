# Arquitectura Wordle Multiplatform

## Visión General
Wordle con Kotlin Multiplatform, Compose Desktop, TCP sockets y JSON.

## Estructura
```
server/         → Servidor TCP con lógica PVE
shared/         → Protocolo JSON compartido
composeApp/     → Cliente Desktop (MVVM)
```

## Tecnologías
- Kotlin 2.1.0
- Compose Multiplatform 1.7.1
- Coroutines para concurrencia
- kotlinx.serialization para JSON
- TCP Sockets

## Módulo Server

**Componentes principales**:
- `GameServer`: ServerSocket TCP
- `PVEGame`: Lógica de partida 1 jugador
- `ClientHandler`: Gestión por cliente
- `DictionaryManager`: Carga palabras.json
- `RecordsManager`: Persistencia records.json

**Flujo PVE**:
1. Cliente conecta
2. Servidor crea PVEGame
3. Selecciona palabra aleatoria
4. Valida intentos del jugador
5. Compara letras (CORRECT/PRESENT/ABSENT)
6. Calcula puntuación: `(7-intentos)×1000-tiempo`
7. Repite 5 rondas
8. Actualiza records si hay nuevo máximo

## Módulo Shared

**Protocolo JSON**:
- `StartGameRequest`: Iniciar partida
- `GuessRequest`: Enviar intento
- `GuessResult`: Resultado con colores
- `RoundEnd`: Fin de ronda con puntuación
- `GameEnd`: Fin de juego
- `RecordsResponse`: Records del servidor

## Módulo Cliente (MVVM)

**Capas**:
1. **Network**: NetworkClient (TCP + Flows)
2. **ViewModel**: GameViewModel (StateFlow)
3. **UI**: Compose components

**Componentes UI**:
- `WordGrid`: Grid 6×5 palabras
- `LetterCell`: Celda individual animada
- `VirtualKeyboard`: Teclado QWERTY
- `StatusPanel`: Intentos, tiempo, puntuación
- Screens: Menu, Game, Records

## Sistema de Puntuación
```kotlin
val roundScore = (7 - attempts) * 1000 - seconds
```

- Acertar rápido y en pocos intentos = más puntos
- No acertar o timeout = 0 puntos
- Puntuación final = suma de 5 rondas

## Configuración

**server.properties**:
```properties
server.host=localhost
server.port=5678
max.clients=10
```

**palabras.json**:
```json
{
  "palabras": [
    {"palabra": "PERRO", "pista": "..."},
    ...
  ]
}
```

**records.json**:
```json
{
  "pveMaxScore": 18500,
  "pvpMaxScore": 0
}
```

## Flujo de Datos

```
Usuario → UI → ViewModel → NetworkClient → TCP → GameServer → PVEGame
                     ↑                                            ↓
                     ←────── StateFlow ←──────────────────────────
```

## Validación de Palabras
1. Longitud correcta (5 letras)
2. Solo alfabéticos
3. Existe en diccionario
4. Servidor responde `ErrorMessage` si falla

## Comparación de Letras
```kotlin
// Primera pasada: CORRECT (posición exacta)
// Segunda pasada: PRESENT (existe pero mal posición)
// Resto: ABSENT
```

---
**Ver README.md para instrucciones de ejecución**
