package app.gamenative.ui.screen.library.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.gamenative.R
import app.gamenative.data.GameSource
import app.gamenative.data.LibraryItem
import app.gamenative.data.PlayHistoryStore
import app.gamenative.emulator.EmulatorLibrary
import app.gamenative.ui.data.LibraryState
import app.gamenative.ui.theme.AeroStyle
import app.gamenative.utils.CoverArt
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil.CoilImage
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

private const val FOCUSED_SCALE = 1.3f
private const val LIBRARY_KEY = "library"

/** One row of the dashboard: a title and the games under it. */
private class Shelf(val key: String, val title: String, val icon: ImageVector, val items: List<LibraryItem>)

/**
 * The library's home as a console dashboard, drawn on its own instead of through the theme engine: the current
 * game's logo floating at the top and, under it, shelves of glass covers by category (recent, installed, unplayed,
 * one per store, one per emulator console and the whole library), with the focused cover larger in its shelf. The
 * top bar and the hint bar belong to the screen around it; this fills the space between them.
 *
 * Focus, paging and opening a game work as in [LibraryCarouselPane]: [firstItemFocusRequester] goes to the cover of
 * the game at [focusTargetListIndex], [onFocusedIndexChanged] reports the library index of the cover that takes
 * focus and [onNavigate] opens a game. The last shelf holds every loaded game in list order and is the one tied to
 * [listState] and to paging.
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
    val appContext = LocalContext.current
    LaunchedEffect(Unit) { EmulatorLibrary.ensureLoaded(appContext) }
    val emulatorEntries by EmulatorLibrary.entries.collectAsStateWithLifecycle()
    val emulatorGames by EmulatorLibrary.games.collectAsStateWithLifecycle()
    val history by PlayHistoryStore.lastPlayed.collectAsStateWithLifecycle()

    var selectedId by remember { mutableStateOf<String?>(null) }
    var focusedShelf by remember { mutableStateOf<String?>(null) }

    val titleRecent = stringResource(R.string.shelf_recent)
    val titleInstalled = stringResource(R.string.shelf_installed)
    val titleUnplayed = stringResource(R.string.shelf_unplayed)
    val titleCustom = stringResource(R.string.shelf_custom)
    val titleLibrary = stringResource(R.string.shelf_library)

    // the category shelves; the library shelf after them holds everything in list order
    val shelves = remember(items, history, titleRecent, titleInstalled, titleUnplayed, titleCustom, titleLibrary) {
        val real = items.filter { !it.isRecTeaser }
        val played = { item: LibraryItem -> history[item.appId] ?: 0L }
        buildList {
            val recent = real.filter { played(it) > 0L }.sortedByDescending { played(it) }.take(20)
            if (recent.isNotEmpty()) add(Shelf("recent", titleRecent, Icons.Default.History, recent))
            val installed = real.filter { it.isInstalled }.take(30)
            if (installed.isNotEmpty()) add(Shelf("installed", titleInstalled, Icons.Default.CheckCircle, installed))
            val unplayed = real.filter { played(it) == 0L }.take(30)
            if (unplayed.isNotEmpty()) add(Shelf("unplayed", titleUnplayed, Icons.Default.SportsEsports, unplayed))
            val bySource = real.groupBy { it.gameSource }
            if (bySource.size > 1) {
                GameSource.entries.forEach { source ->
                    val list = bySource[source].orEmpty().take(40)
                    if (list.isEmpty()) return@forEach
                    val title = when (source) {
                        GameSource.STEAM -> "Steam"
                        GameSource.GOG -> "GOG"
                        GameSource.EPIC -> "Epic"
                        GameSource.AMAZON -> "Amazon"
                        GameSource.CUSTOM_GAME -> titleCustom
                    }
                    add(Shelf(source.name, title, if (source == GameSource.CUSTOM_GAME) Icons.Default.Storage else Icons.Default.Cloud, list))
                }
            }
            add(Shelf(LIBRARY_KEY, titleLibrary, Icons.Default.LibraryBooks, items))
        }
    }

    val firstShelfKey = shelves.firstOrNull()?.key
    // where the focus requester sits: the first tile of the first shelf until something has focus, then the first
    // shelf that holds the game at the target index
    val targetId = if (selectedId == null) {
        shelves.firstOrNull()?.items?.firstOrNull()?.appId
    } else {
        items.getOrNull(focusTargetListIndex ?: 0)?.appId
    }
    val targetShelfKey = if (selectedId == null) {
        firstShelfKey
    } else {
        shelves.firstOrNull { shelf -> shelf.items.any { it.appId == targetId } }?.key ?: LIBRARY_KEY
    }

    // load the next page when the library shelf reaches its end
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
        val tile: Dp = maxOf(72.dp, minOf(screenH * 0.2f, screenW / 8f))
        val rowHeight = tile * FOCUSED_SCALE + 12.dp
        val gap = tile * 0.16f
        val plateHeight = 108.dp
        val scrollTop = topBar + plateHeight
        val density = LocalDensity.current
        val tilePx = with(density) { tile.toPx() }
        val viewportPx = with(density) { screenW.toPx() }

        if (items.isEmpty() && emulatorGames.isEmpty()) {
            if (!state.isLoading) {
                Text(
                    text = stringResource(R.string.library_no_items),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                )
            }
            return@BoxWithConstraints
        }

        // the current game's logo, or its name, floating at the top
        val plateItem = items.firstOrNull { it.appId == selectedId }
            ?: shelves.firstOrNull()?.items?.firstOrNull()
        if (plateItem != null) {
            val plateTitle = if (plateItem.isRecTeaser) stringResource(R.string.rec_teaser_title) else plateItem.name
            val plateLogo = plateItem
                .takeIf { it.gameSource == GameSource.STEAM && !it.isRecTeaser }
                ?.let { "https://shared.steamstatic.com/store_item_assets/steam/apps/${it.gameId}/logo.png" }
            Box(modifier = Modifier.fillMaxWidth().offset(y = topBar + 6.dp)) {
                AeroTitlePlate(title = plateTitle, logoUrl = plateLogo)
            }
        }

        // the shelves, one under the other; the page scrolls to keep the focused one in view
        Column(
            modifier = Modifier
                .offset(y = scrollTop)
                .fillMaxWidth()
                .height(screenH - scrollTop - footer)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            shelves.forEach { shelf ->
                val aligned = shelf.key == LIBRARY_KEY
                val rowState = if (aligned) listState else rememberLazyListState()
                ShelfFrame(
                    title = shelf.title,
                    icon = shelf.icon,
                    rowHeight = rowHeight,
                    gap = gap,
                    rowState = rowState,
                    scrollStepPx = (tilePx + with(density) { gap.toPx() }) * 4f,
                ) {
                    itemsIndexed(shelf.items, key = { _, item -> "${shelf.key}_${item.appId}" }) { _, item ->
                        val isCurrent = focusedShelf == shelf.key && item.appId == selectedId
                        AeroTile(
                            item = item,
                            size = tile,
                            current = isCurrent,
                            focusRequester = if (firstItemFocusRequester != null && shelf.key == targetShelfKey && item.appId == targetId) {
                                firstItemFocusRequester
                            } else {
                                null
                            },
                            onFocus = {
                                selectedId = item.appId
                                focusedShelf = shelf.key
                                val realIndex = items.indexOfFirst { it.appId == item.appId }
                                if (realIndex >= 0) onFocusedIndexChanged(realIndex)
                            },
                            onClick = {
                                selectedId = item.appId
                                focusedShelf = shelf.key
                                val realIndex = items.indexOfFirst { it.appId == item.appId }
                                if (realIndex >= 0) onFocusedIndexChanged(realIndex)
                                onNavigate(item.appId)
                            },
                        )
                    }
                    if (aligned && items.size < state.totalAppsInFilter) {
                        item {
                            Box(modifier = Modifier.size(tile), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Color.White)
                            }
                        }
                    }
                }
                // keep the current cover at the middle of the shelf that has the focus
                LaunchedEffect(focusedShelf, selectedId, shelf.items.size, tilePx, viewportPx) {
                    if (focusedShelf == shelf.key) {
                        val at = shelf.items.indexOfFirst { it.appId == selectedId }
                        if (at >= 0) rowState.animateScrollToItem(at, -((viewportPx - tilePx) / 2f).toInt())
                    }
                }
            }

            // one shelf per emulator console, with its games
            emulatorEntries.forEach { entry ->
                val list = emulatorGames.filter { it.entryId == entry.id }
                if (list.isEmpty()) return@forEach
                ShelfFrame(
                    title = entry.shortLabel,
                    icon = Icons.Default.SportsEsports,
                    rowHeight = rowHeight,
                    gap = gap,
                    rowState = rememberLazyListState(),
                    scrollStepPx = (tilePx + with(density) { gap.toPx() }) * 4f,
                ) {
                    itemsIndexed(list, key = { _, game -> "emu_${game.uri}" }) { _, game ->
                        Box(modifier = Modifier.height(rowHeight), contentAlignment = Alignment.Center) {
                            EmulatorTile(game, tile * 0.78f, tile, false)
                        }
                    }
                }
            }
        }
    }
}

/** A shelf's title pill and rule, its row of covers with a scroll button at each end, and a faint reflection. */
@Composable
private fun ShelfFrame(
    title: String,
    icon: ImageVector,
    rowHeight: Dp,
    gap: Dp,
    rowState: LazyListState,
    scrollStepPx: Float,
    content: LazyListScope.() -> Unit,
) {
    val style by AeroStyle.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 28.dp, end = 28.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(style.fill())
                    .border(BorderStroke(1.dp, style.rim()), CircleShape)
                    .padding(start = 10.dp, end = 18.dp, top = 5.dp, bottom = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = style.textColor(Color.White), modifier = Modifier.size(22.dp))
                Text(
                    text = title,
                    color = style.textColor(Color.White),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            Box(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .weight(1f)
                    .height(1.dp)
                    .background(Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.55f), Color.Transparent))),
            )
        }
        Box(modifier = Modifier.fillMaxWidth().height(rowHeight)) {
            LazyRow(
                state = rowState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(gap),
                verticalAlignment = Alignment.CenterVertically,
                content = content,
            )
            if (rowState.canScrollBackward) {
                ScrollButton(
                    icon = Icons.Default.ChevronLeft,
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp),
                    onClick = { scope.launch { rowState.animateScrollBy(-scrollStepPx) } },
                )
            }
            if (rowState.canScrollForward) {
                ScrollButton(
                    icon = Icons.Default.ChevronRight,
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 4.dp),
                    onClick = { scope.launch { rowState.animateScrollBy(scrollStepPx) } },
                )
            }
        }
        // a faint reflection of the row on the glossy floor
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.14f), Color.Transparent))),
        )
    }
}

@Composable
private fun ScrollButton(icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    val style by AeroStyle.state.collectAsStateWithLifecycle()
    Box(
        modifier = modifier
            .zIndex(2f)
            .size(34.dp)
            .clip(CircleShape)
            .background(style.fill())
            .border(BorderStroke(1.dp, style.rim()), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = style.textColor(Color.White), modifier = Modifier.size(22.dp))
    }
}

/**
 * One glass cover: a frosted frame with a bright cyan edge and a cool tint over the art. The current game's cover is
 * larger and glows; the focused one has a thick bright ring.
 */
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
    val shape = RoundedCornerShape(22.dp)
    val glow = style.selectionColor ?: Color(0xFF5FE6FF)
    // square art from SteamGridDB when the person has a key, else the store's cover cropped to the frame
    val tileContext = LocalContext.current
    val squareUrl by produceState<String?>(null, item.appId) { value = CoverArt.squareUrl(tileContext, item) }
    val imageUrl = squareUrl ?: item.capsuleImageUrl.ifEmpty { item.headerImageUrl }
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
            // the glow around the current cover, outside its edge
            .drawBehind {
                if (current || focused) {
                    val spread = 10.dp.toPx()
                    drawRoundRect(
                        color = glow.copy(alpha = if (focused) 0.5f else 0.3f),
                        topLeft = androidx.compose.ui.geometry.Offset(-spread / 2f, -spread / 2f),
                        size = androidx.compose.ui.geometry.Size(this.size.width + spread, this.size.height + spread),
                        cornerRadius = CornerRadius(22.dp.toPx() + spread / 2f),
                    )
                }
            }
            .clip(shape)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { if (it.isFocused) onFocus() }
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .border(
                BorderStroke(
                    if (focused) 3.dp else 2.dp,
                    if (focused) {
                        Brush.verticalGradient(listOf(Color.White, glow))
                    } else {
                        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.85f), glow.copy(alpha = 0.55f)))
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
        // a cool, glassy wash over the art and a clear sheen along the top edge
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.28f),
                            glow.copy(alpha = 0.08f),
                            glow.copy(alpha = 0.16f),
                        ),
                    ),
                ),
        )
        if (current) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.55f)),
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
