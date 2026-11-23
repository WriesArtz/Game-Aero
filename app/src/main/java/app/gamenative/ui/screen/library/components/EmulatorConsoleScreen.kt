package app.gamenative.ui.screen.library.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.gamenative.R
import app.gamenative.emulator.EmulatorLibrary
import app.gamenative.ui.theme.AeroStyle

/**
 * The page of one console, shown when its pill is chosen in the top bar: its name as a title and all of its games as
 * covers in a grid. A cover starts its game straight in the emulator.
 */
@Composable
internal fun EmulatorConsoleScreen(entryId: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { EmulatorLibrary.ensureLoaded(context) }
    val entries by EmulatorLibrary.entries.collectAsStateWithLifecycle()
    val games by EmulatorLibrary.games.collectAsStateWithLifecycle()
    val style by AeroStyle.state.collectAsStateWithLifecycle()
    val entry = entries.firstOrNull { it.id == entryId }
    val list = remember(games, entryId) { games.filter { it.entryId == entryId } }
    val metro = style.metro

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val topBar = 76.dp
        val footer = 64.dp
        val titleSize = (maxHeight.value * 0.09f).coerceIn(28f, 56f)
        val gridTop = topBar + (titleSize * 1.4f).dp
        Text(
            text = (entry?.shortLabel ?: "").let { if (metro) it.lowercase() else it },
            color = style.textColor(Color.White),
            fontSize = titleSize.sp,
            fontWeight = if (metro) FontWeight.Light else FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier.padding(start = 28.dp, top = topBar - 6.dp),
        )
        if (list.isEmpty()) {
            Text(
                text = stringResource(R.string.library_no_items),
                color = style.textColor(Color.White),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
            )
            return@BoxWithConstraints
        }
        val tileHeight = (maxHeight * 0.34f).coerceIn(120.dp, 240.dp)
        val tileWidth = tileHeight * 0.78f
        LazyVerticalGrid(
            columns = GridCells.Adaptive(tileWidth + 12.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 28.dp, end = 28.dp, top = gridTop, bottom = footer + 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(list, key = { it.uri }) { game ->
                Box(contentAlignment = Alignment.Center) {
                    EmulatorTile(game, tileWidth, tileHeight, metro)
                }
            }
        }
    }
}
