# Diccionarios Wordle (ES) — carpeta de datos

Esta carpeta contiene los ficheros que usa el Wordle en español (sin tildes), separados por finalidad:
- Un diccionario grande para validar lo que escribe el jugador.
- Un diccionario pequeño para elegir las palabras objetivo (soluciones) con pistas.
- Dos ficheros de referencia para poder regenerar todo desde cero si hiciera falta.

## Referencia (no se usa en el juego)
- 00_source_hunspell_es_ES.dic  
  Diccionario Hunspell original. Se conserva para rehacer el proceso desde la fuente.

- 01_palabras_clean.txt  
  Lista limpia intermedia generada desde el Hunspell (una palabra por línea). Se conserva para regenerar listas sin depender del .dic.

## Validación del input del jugador (diccionario completo por longitud)
Estos ficheros se usan para comprobar si la palabra introducida por el jugador es válida.
- 10_valid_4.txt
- 10_valid_5.txt
- 10_valid_6.txt
- 10_valid_7.txt

Características:
- Palabras sin tildes.
- Solo letras (a–z y ñ).
- Una palabra por línea.
- Listas amplias: aceptan muchas palabras (incluidas menos habituales) para evitar frustración al introducir intentos.

## Soluciones del juego (pool habitual + pistas)
Estos ficheros se usan para elegir la palabra objetivo del juego. Están acotados para que no salgan palabras raras.
- 20_solutions_4.json
- 20_solutions_5.json
- 20_solutions_6.json
- 20_solutions_7.json

Qué incluyen:
- Cada entrada tiene una palabra (word) y una pista corta (hint).
- Selección a partir del diccionario completo de esa longitud, priorizando palabras más frecuentes.
- Regla de acotación:
  - Se toma el 30% más frecuente y, de ahí, como máximo 500 palabras.
  - Se intenta variedad (mezcla por letra inicial).
  - Se excluyen pronombres/determinantes/artículos, preposiciones y conjunciones.
- Pistas: definiciones cortas estilo Wordle, sin etiquetas gramaticales y sin indicar infinitivos.

## Normalización esperada en la app
La app debe normalizar la entrada del jugador igual que estas listas:
- minúsculas
- sin tildes
- conservar ñ como letra distinta
- validar longitud exacta
