package fr.lumiere.tv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

/** Rangée titrée défilant horizontalement. */
@Composable
fun MediaRow(title: String, modifier: Modifier = Modifier, content: LazyListScope.() -> Unit) {
    Column(modifier) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 14.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(22.dp),
            // Marge pour la carte agrandie au focus et son halo.
            contentPadding = PaddingValues(vertical = 14.dp, horizontal = 14.dp),
            content = content,
        )
    }
}
