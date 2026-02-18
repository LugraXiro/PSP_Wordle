# 🎮 Wordle Multiplatform

Proyecto de Wordle con Kotlin Multiplatform y Compose Desktop. Modo PVE totalmente funcional.

## 🚀 Ejecución Rápida

### 1. Ejecutar el Servidor
```bash
./gradlew :server:run
```

### 2. Ejecutar el Cliente (en otra terminal)
```bash
./gradlew :composeApp:run
```

## 📋 Requisitos
- JDK 17 o superior
- Gradle (incluido con wrapper)

## 🎯 Cómo Jugar

1. **Conectar**: localhost:5678
2. **Jugar PVE**: 5 rondas, 6 intentos por palabra
3. **Colores**:
   - 🟩 Verde: Letra correcta en posición correcta
   - 🟨 Amarillo: Letra existe en otra posición
   - ⬜ Gris: Letra no existe

## 📊 Puntuación
```
Puntos = (7 - intentos) × 1000 - tiempo_segundos
```

Ejemplo: 3 intentos en 45s = (7-3)×1000-45 = 3955 puntos

## 📁 Estructura
```
wordle-multiplatform/
├── server/              # Servidor TCP
├── shared/              # Protocolo compartido
├── composeApp/          # Cliente Desktop (MVVM)
├── palabras.json        # Diccionario 50 palabras
└── server.properties    # Configuración
```

## ⚙️ Configuración

**server.properties**:
```properties
server.host=localhost
server.port=5678
max.clients=10
```

## 🐛 Solución de Problemas

**Error: "Address already in use"**
→ Cambia el puerto en server.properties

**Error: "palabras.json not found"**
→ Verifica que palabras.json esté en la raíz del proyecto

**Cliente no conecta**
→ Verifica que el servidor esté ejecutándose

## 📚 Documentación Completa
Ver `docs/ARQUITECTURA.md` para detalles técnicos.

## 🔮 Roadmap
- ✅ PVE funcional
- 🚧 PVP (próximamente)
- Sistema de pistas
- Estadísticas avanzadas
- Soporte Android/iOS

---
**Versión:** 1.0 | **Enero 2026**
