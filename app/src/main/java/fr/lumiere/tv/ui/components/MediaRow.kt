package fr.lumiere.tv.ui.components

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Rangée titrée défilant horizontalement. */
@Composable
fun MediaRow(title: String, modifier: Modifier = Modifier, content: LazyListScope.() -> Unit) {
    Column(modifier) {
        SectionTitle(title, Modifier.padding(start = 4.dp, bottom = 4.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            // Marge pour la carte agrandie au focus et son halo.
            contentPadding = PaddingValues(vertical = 16.dp, horizontal = 14.dp),
            modifier = Modifier.leftExitsTo(LocalSidebarFocus.current).focusGroup(),
            content = content,
        )
    }
}
