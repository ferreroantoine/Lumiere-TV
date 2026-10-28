package fr.lumiere.tv.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.tv.material3.Icon
import fr.lumiere.tv.ui.theme.GlassVariant
import fr.lumiere.tv.ui.theme.glass
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import fr.lumiere.tv.ui.theme.LumiereColors
import fr.lumiere.tv.ui.theme.LumiereShapes

/** Champ de saisie pour la télécommande (tv-material n'en fournit pas). */
@Composable
fun TvTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Done,
    onSubmit: () -> Unit = {},
    leadingIcon: ImageVector? = null,
) {
    var focused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val style = MaterialTheme.typography.titleMedium.copy(color = LumiereColors.Text)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = style,
        cursorBrush = SolidColor(LumiereColors.Brand400),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction, autoCorrectEnabled = false),
        keyboardActions = KeyboardActions(onDone = { onSubmit() }, onSearch = { onSubmit() }, onGo = { onSubmit() }),
        modifier = modifier
            .onFocusChanged { focused = it.isFocused }
            // Le champ garderait Haut/Bas : on les rend à la navigation au pavé directionnel.
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionDown -> focusManager.moveFocus(FocusDirection.Down)
                    Key.DirectionUp -> focusManager.moveFocus(FocusDirection.Up)
                    else -> false
                }
            }
            .fillMaxWidth()
            // Champ en verre de Lumière ; au focus, anneau ambré (`.glass-field:focus-within`).
            .then(if (focused) Modifier.border(3.dp, LumiereColors.Brand400.copy(alpha = 0.45f), LumiereShapes.Pill) else Modifier)
            .glass(LumiereShapes.Pill, GlassVariant.Regular, elevation = 0.dp)
            .then(if (focused) Modifier.border(1.5.dp, LumiereColors.Brand300, LumiereShapes.Pill) else Modifier),
        decorationBox = { inner ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = if (leadingIcon != null) 22.dp else 28.dp, end = 28.dp, top = 15.dp, bottom = 15.dp),
            ) {
                if (leadingIcon != null) {
                    Icon(leadingIcon, contentDescription = null, tint = LumiereColors.Zinc300, modifier = Modifier.size(20.dp))
                }
                Box {
                    if (value.isEmpty()) Text(placeholder, style = style, color = LumiereColors.TextMuted)
                    inner()
                }
            }
        },
    )
}
