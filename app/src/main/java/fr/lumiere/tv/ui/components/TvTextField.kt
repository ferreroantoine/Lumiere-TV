package fr.lumiere.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
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
) {
    var focused by remember { mutableStateOf(false) }
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
            .fillMaxWidth()
            .background(LumiereColors.Ink800, LumiereShapes.Pill)
            .border(2.dp, if (focused) LumiereColors.Brand300 else LumiereColors.Ink600, LumiereShapes.Pill),
        decorationBox = { inner ->
            Box(Modifier.padding(horizontal = 28.dp, vertical = 16.dp)) {
                if (value.isEmpty()) Text(placeholder, style = style, color = LumiereColors.TextMuted)
                inner()
            }
        },
    )
}
