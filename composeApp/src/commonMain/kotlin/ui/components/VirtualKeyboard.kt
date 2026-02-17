package ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import protocol.LetterStatus

private val keyboardLayout = listOf(
    listOf('Q', 'W', 'E', 'R', 'T', 'Y', 'U', 'I', 'O', 'P'),
    listOf('A', 'S', 'D', 'F', 'G', 'H', 'J', 'K', 'L', 'Ñ'),
    listOf('Z', 'X', 'C', 'V', 'B', 'N', 'M')
)

@Composable
fun VirtualKeyboard(
    usedLetters: Map<Char, LetterStatus>,
    onLetterClick: (Char) -> Unit,
    onDeleteClick: () -> Unit,
    onEnterClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        keyboardLayout.forEachIndexed { rowIndex, row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botón ENTER solo en la última fila, al inicio
                if (rowIndex == 2) {
                    KeyboardButton(
                        text = "ENTER",
                        onClick = onEnterClick,
                        modifier = Modifier.width(65.dp).height(58.dp),
                        enabled = enabled
                    )
                }
                
                // Letras de la fila
                row.forEach { letter ->
                    val status = usedLetters[letter]
                    KeyboardButton(
                        text = letter.toString(),
                        onClick = { onLetterClick(letter) },
                        status = status,
                        modifier = Modifier.size(width = 43.dp, height = 58.dp),
                        enabled = enabled
                    )
                }
                
                // Botón DELETE solo en la última fila, al final
                if (rowIndex == 2) {
                    KeyboardButton(
                        text = "⌫",
                        onClick = onDeleteClick,
                        modifier = Modifier.width(65.dp).height(58.dp),
                        enabled = enabled
                    )
                }
            }
        }
    }
}

@Composable
private fun KeyboardButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    status: LetterStatus? = null,
    enabled: Boolean = true
) {
    val backgroundColor = when (status) {
        LetterStatus.CORRECT -> Color(0xFF6AAA64)
        LetterStatus.PRESENT -> Color(0xFFC9B458)
        LetterStatus.ABSENT -> Color(0xFF787C7E)
        null -> Color(0xFFD3D6DA)
    }
    
    val textColor = if (status != null) Color.White else Color.Black
    
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            disabledContainerColor = backgroundColor.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(4.dp),
        contentPadding = PaddingValues(0.dp),
        enabled = enabled
    ) {
        Text(
            text = text,
            fontSize = if (text.length == 1) 20.sp else 12.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}
