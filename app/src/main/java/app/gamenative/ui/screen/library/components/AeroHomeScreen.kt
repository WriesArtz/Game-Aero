package app.gamenative.ui.screen.library.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import app.gamenative.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.gamenative.data.GameSource
import app.gamenative.data.LibraryItem
import app.gamenative.emulator.EmulatorLibrary
import app.gamenative.ui.data.LibraryState
import app.gamenative.ui.theme.AeroStyle
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil.CoilImage
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull

private const val FOCUSED_SCALE = 1.32f

/**
 * The library's home as a console dashboard, drawn on its own instead of through the theme engine: the current
 * game's logo on a glass plate, a row of glass covers with the current one larger, and wide cards under them.
 * The top bar and the hint bar belong to the screen around it; this fills the space between them.
 *
 * Focus, paging and opening a game work as in [LibraryCarouselPane]: [firstItemFocusRequester] goes to the cover at
 * [focusTargetListIndex], [onFocusedIndexChanged] reports the cover that takes focus and [onNavigate] opens a game.
 */
@Composable
internal fun AeroHomeScreen(
    state: LibraryState,
    listState: LazyListState,
    onPageChange: (Int) -> Unit,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    firstItemFocusRequester: FocusRequester? = null,
    focusTargetListIndex: Int? = null,
    onFocusedIndexChanged: (Int) -> Unit = {},
) {
    val items = state.appInfoList
    val density = LocalDensity.current
    val appContext = LocalContext.current
    LaunchedEffect(Unit) { EmulatorLibrary.ensureLoaded(appContext) }
    val emulatorGames by EmulatorLibrary.games.collectAsStateWithLifecycle()
    var selected by remember { mutableIntStateOf(focusTargetListIndex ?: 0) }
    LaunchedEffect(focusTargetListIndex) {
        if (focusTargetListIndex != null) selected = focusTargetListIndex
    }
    val current = selected.coerceIn(0, (items.size - 1).coerceAtLeast(0))

    // load the next page when the row reaches its end
    LaunchedEffect(listState, items.size, state.totalAppsInFilter) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { lastVisible ->
                if (lastVisible >= items.lastIndex && items.size < state.totalAppsInFilter) onPageChange(1)
            }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenW = maxWidth
        val screenH = maxHeight
        val topBar = 76.dp
        val footer = 64.dp
        val tile: Dp = maxOf(72.dp, minOf(screenH * 0.2f, screenW / 7.5f))
        val rowHeight = tile * FOCUSED_SCALE + 14.dp
        val plateTop = topBar + 6.dp
        val rowTop = maxOf(plateTop + 110.dp, screenH * 0.45f)
        val cardsTop = rowTop + rowHeight + 8.dp
        val cardsRoom = screenH - footer - cardsTop

        // keep the current cover at the middle of the row
        val tilePx = with(density) { tile.toPx() }
        val viewportPx = with(density) { screenW.toPx() }
        LaunchedEffect(current, items.size, tilePx, viewportPx) {
            if (items.isNotEmpty()) {
                listState.animateScrollToItem(current, -((viewportPx - tilePx) / 2f).toInt())
            }
        }

        if (items.isEmpty()) {
            if (!state.isLoading) {
                Text(
                    text = stringResource(R.string.library_no_items),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                )
            }
            return@BoxWithConstraints
        }

        // the current game's logo, or its name, on a glass plate
        val plateItem = items[current]
        val plateTitle = if (plateItem.isRecTeaser) stringResource(R.string.rec_teaser_title) else plateItem.name
        val plateLogo = plateItem
            .takeIf { it.gameSource == GameSource.STEAM && !it.isRecTeaser }
            ?.let { "https://shared.steamstatic.com/store_item_assets/steam/apps/${it.gameId}/logo.png" }
        Box(modifier = Modifier.fillMaxWidth().offset(y = plateTop)) {
            AeroTitlePlate(title = plateTitle, logoUrl = plateLogo)
        }

        // the covers
        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .height(rowHeight)
                .offset(y = rowTop),
            contentPadding = PaddingValues(horizontal = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(tile * 0.2f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            itemsIndexed(items) { index, item ->
                val isCurrent = index == current
                AeroTile(
                    item = item,
                    size = tile,
                    current = isCurrent,
                    focusRequester = if (firstItemFocusRequester != null && index == (focusTargetListIndex ?: 0)) {
                        firstItemFocusRequester
                    } else {
                        null
                    },
                    onFocus = {
                        selected = index
                        onFocusedIndexChanged(index)
                    },
                    onClick = {
                        selected = index
                        onFocusedIndexChanged(index)
                        onNavigate(item.appId)
                    },
                )
            }
            if (items.size < state.totalAppsInFilter) {
                item {
                    Box(modifier = Modifier.size(tile), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
            }
        }

        // under the covers, when the screen has the room: the emulators' games if there are any, else wide cards
        if (cardsRoom >= 104.dp) {
            Box(modifier = Modifier.fillMaxWidth().offset(y = cardsTop)) {
                if (emulatorGames.isNotEmpty()) {
                    EmulatorShelf(height = minOf(cardsRoom, 150.dp))
                } else {
                    AeroCardsRow(
                        items = items.filter { !it.isRecTeaser && it.headerImageUrl.isNotEmpty() }.take(4),
                        onNavigate = onNavigate,
                    )
                }
            }
        }
    }
}

/** One glass cover: it grows when it is the current game, and a bright ring marks the focused one. */
@Composable
private fun AeroTile(
    item: LibraryItem,
    size: Dp,
    current: Boolean,
    focusRequester: FocusRequester?,
    onFocus: () -> Unit,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val style by AeroStyle.state.collectAsStateWithLifecycle()
    val scale by animateFloatAsState(
        targetValue = if (current) FOCUSED_SCALE else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "aeroTileScale",
    )
    val shape = RoundedCornerShape(20.dp)
    val imageUrl = item.capsuleImageUrl.ifEmpty { item.headerImageUrl }
    val nameText: @Composable () -> Unit = {
        Text(
            text = item.name,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = style.textColor(Color.White),
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(8.dp),
        )
    }
    Box(
        modifier = Modifier
            .zIndex(if (current) 1f else 0f)
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { if (it.isFocused) onFocus() }
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .border(
                BorderStroke(
                    if (focused) 3.dp else 1.dp,
                    if (focused) {
                        val ring = style.selectionColor
                        if (ring != null) {
                            Brush.verticalGradient(listOf(ring, ring))
                        } else {
                            Brush.verticalGradient(listOf(Color(0xFFE8FFFF), Color(0xFF7FE6F5)))
                        }
                    } else {
                        style.rim()
                    },
                ),
                shape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (imageUrl.isNotEmpty()) {
            CoilImage(
                imageModel = { imageUrl },
                modifier = Modifier.fillMaxSize(),
                imageOptions = ImageOptions(contentScale = ContentScale.Crop, contentDescription = item.name),
                loading = {},
                failure = { nameText() },
            )
        } else {
            nameText()
        }
        // clear sheen along the top edge, like glass catching light
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.22f), Color.Transparent, Color.Transparent),
                    ),
                ),
        )
        if (current) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.6f)),
                        ),
                    ),
            )
            Text(
                text = item.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = style.textColor(Color.White),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            )
        }
    }
}
