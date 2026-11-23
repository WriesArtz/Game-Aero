package app.gamenative.ui.screen.library.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.gamenative.R
import app.gamenative.emulator.EmulatorGame
import app.gamenative.emulator.EmulatorLauncher
import app.gamenative.emulator.EmulatorLibrary
import app.gamenative.ui.theme.AeroStyle
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil.CoilImage

/**
 * The games of the configured emulators in one row under the covers, grouped by emulator and system: a small label
 * starts each group. A tile starts its game straight in the emulator, without passing through the emulator's menus.
 * It draws nothing while no emulator has games.
 */
@Composable
internal fun EmulatorShelf(height: Dp, metro: Boolean = false, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { EmulatorLibrary.ensureLoaded(context) }
    val games by EmulatorLibrary.games.collectAsStateWithLifecycle()
    if (games.isEmpty()) return
    val style by AeroStyle.state.collectAsStateWithLifecycle()
    val groups = remember(games) { games.groupBy { it.groupLabel }.toList() }
    val labelHeight = 22.dp
    val tileHeight = (height - labelHeight - 8.dp).coerceAtLeast(64.dp)
    val tileWidth = tileHeight * 0.78f

    Column(modifier = modifier.fillMaxWidth()) {
        LazyRow(
            modifier = Modifier.fillMaxWidth().height(height),
            contentPadding = PaddingValues(horizontal = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            groups.forEach { (label, list) ->
                item(key = "label_$label") {
                    Column(modifier = Modifier.height(height), verticalArrangement = Arrangement.Bottom) {
                        Text(
                            text = label,
                            color = style.textColor(Color.White),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier
                                .height(labelHeight)
                                .padding(end = 8.dp),
                        )
                        Box(modifier = Modifier.height(tileHeight + 8.dp))
                    }
                }
                items(list.size, key = { "game_${list[it].uri}" }) { i ->
                    EmulatorTile(list[i], tileWidth, tileHeight, metro)
                }
            }
        }
    }
}

@Composable
internal fun EmulatorTile(game: EmulatorGame, width: Dp, height: Dp, metro: Boolean) {
    val context = LocalContext.current
    val style by AeroStyle.state.collectAsStateWithLifecycle()
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(if (metro) 2.dp else 16.dp)
    val ring = style.selectionColor ?: Color(0xFFE8FFFF)
    val failure = stringResource(R.string.emulator_launch_failed)
    val notInstalled = stringResource(R.string.emulator_not_installed)
    val noPath = stringResource(R.string.emulator_no_path)
    val noCore = stringResource(R.string.emulator_no_core)
    Box(
        modifier = Modifier
            .padding(bottom = 4.dp)
            .width(width)
            .height(height)
            .clip(shape)
            .background(if (metro) Color(0xFF1BA1E2) else Color.White.copy(alpha = 0.12f))
            .clickable(interactionSource = interaction, indication = null) {
                val message = when (val result = EmulatorLauncher.launch(context, game)) {
                    EmulatorLauncher.Result.Started -> null
                    is EmulatorLauncher.Result.Failed -> when (result.reason) {
                        EmulatorLauncher.Reason.NOT_INSTALLED -> notInstalled
                        EmulatorLauncher.Reason.NO_PATH -> noPath
                        EmulatorLauncher.Reason.NO_CORE -> noCore
                        EmulatorLauncher.Reason.NO_ENTRY -> failure
                    }
                }
                if (message != null) Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
            .border(
                BorderStroke(if (focused) 3.dp else 1.dp, if (focused) ring else Color.White.copy(alpha = 0.35f)),
                shape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        val nameText: @Composable () -> Unit = {
            Text(
                text = game.title,
                color = style.textColor(Color.White),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(8.dp),
            )
        }
        if (game.coverUrl.isNotEmpty()) {
            CoilImage(
                imageModel = { game.coverUrl },
                modifier = Modifier.fillMaxWidth().height(height),
                imageOptions = ImageOptions(contentScale = ContentScale.Crop, contentDescription = game.title),
                loading = {},
                failure = { nameText() },
            )
        } else {
            nameText()
        }
        if (!metro) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height)
                    .background(
                        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.2f), Color.Transparent, Color.Transparent)),
                    ),
            )
        }
    }
}
