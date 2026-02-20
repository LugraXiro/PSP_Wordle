package view.componentes

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import protocol.LetterStatus

/**
 * Celda individual de una letra en el tablero de juego.
 *
 * El color de fondo refleja el [status]: verde (CORRECT), amarillo (PRESENT), gris (ABSENT).
 * Si [shouldAnimate] es `true`, ejecuta una animación de flip al revelar el resultado,
 * con retraso escalonado por posición configurable mediante [animationDelay].
 *
 * @param letter Letra a mostrar, o `null` para celda vacía.
 * @param status Estado de la letra tras la validación del servidor, o `null` si aún no se ha enviado.
 * @param shouldAnimate Si debe ejecutar la animación de flip al mostrar el resultado.
 * @param animationDelay Milisegundos de retraso para escalonar la animación entre celdas de la misma fila.
 */
@Composable
fun LetterCell(
    letter: Char?,
    status: LetterStatus?,
    modifier: Modifier = Modifier,
    shouldAnimate: Boolean = false,
    animationDelay: Int = 0
) {
    var animationPlayed by remember { mutableStateOf(false) }

    // Animación de flip
    val rotation by animateFloatAsState(
        targetValue = if (shouldAnimate && !animationPlayed) 360f else 0f,
        animationSpec = tween(
            durationMillis = 600,
            delayMillis = animationDelay,
            easing = FastOutSlowInEasing
        ),
        finishedListener = {
            if (shouldAnimate) animationPlayed = true
        }
    )

    // Resetear animación cuando status cambia
    LaunchedEffect(status) {
        if (status != null) {
            animationPlayed = false
        }
    }

    val backgroundColor by animateColorAsState(
        targetValue = when (status) {
            LetterStatus.CORRECT -> Color(0xFF6AAA64)  // Verde
            LetterStatus.PRESENT -> Color(0xFFC9B458)  // Amarillo
            LetterStatus.ABSENT -> Color(0xFF787C7E)   // Gris claro
            null -> Color.White
        },
        animationSpec = tween(300)
    )
    
    val textColor = if (status != null) Color.White else Color.Black
    val border = if (status == null && letter != null) {
        BorderStroke(2.dp, Color(0xFF878A8C))
    } else if (status == null) {
        BorderStroke(1.dp, Color(0xFFD3D6DA))
    } else {
        null
    }
    
    Card(
        modifier = modifier
            .size(62.dp)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            },
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = border
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (letter != null) {
                Text(
                    text = letter.toString(),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
        }
    }
}
