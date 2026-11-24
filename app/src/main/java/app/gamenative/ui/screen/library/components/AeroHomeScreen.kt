package app.gamenative.ui.screen.library.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import app.gamenative.data.FavoritesManager
import app.gamenative.data.GameCompatibilityStatus
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

/** A selected cover is this much wider and taller than the others; the mockup's is wider than it is taller. */
private const val SELECTED_W = 1.4f
private const val SELECTED_H = 1.17f
private const val SELECTED_BANNER_W = 1.25f
private const val COVER_RATIO = 0.775f
private const val LIBRARY_KEY = "library"
private const val SHELF_LIMIT = 30
private const val LIGHT_BYTES = 5L * 1024 * 1024 * 1024
private val FrameCyan = Color(0xFF5FE6FF)

/** One row of the dashboard: a title and the games under it. */
private class Shelf(
    val key: String,
    val title: String,
    val icon: ImageVector,
    val items: List<LibraryItem>,
    /** Banners (the store's wide art) instead of tall covers. */
    val wide: Boolean = false,
)

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

    val favorites by FavoritesManager.favorites.collectAsStateWithLifecycle()
    val titleRecent = stringResource(R.string.shelf_recent)
    val titleFavorites = stringResource(R.string.shelf_favorites)
    val titleInstalled = stringResource(R.string.shelf_installed)
    val titleUnplayed = stringResource(R.string.shelf_unplayed)
    val titleRecommended = stringResource(R.string.shelf_recommended)
    val titleToInstall = stringResource(R.string.shelf_toinstall)
    val titleLight = stringResource(R.string.shelf_light)
    val titleFamily = stringResource(R.string.shelf_family)
    val titleCustom = stringResource(R.string.shelf_custom)
    val titleLibrary = stringResource(R.string.shelf_library)

    // the category shelves; the library shelf after them holds everything in list order
    val shelves = remember(
        items, history, favorites, titleRecent, titleFavorites, titleInstalled, titleUnplayed, titleRecommended,
        titleToInstall, titleLight, titleFamily, titleCustom, titleLibrary,
    ) {
        val real = items.filter { !it.isRecTeaser }
        val played = { item: LibraryItem -> history[item.appId] ?: 0L }
        buildList {
            // a category that holds every game would only repeat the library shelf
            fun category(
                key: String,
                title: String,
                icon: ImageVector,
                list: List<LibraryItem>,
                always: Boolean = false,
                wide: Boolean = false,
            ) {
                val shown = list.take(SHELF_LIMIT)
                if (shown.isNotEmpty() && (always || list.size < real.size || real.size < 2)) {
                    add(Shelf(key, title, icon, shown, wide))
                }
            }
            // the two main shelves are always there; with no play history yet, "recent" shows the installed games
            val playedList = real.filter { played(it) > 0L }.sortedByDescending { played(it) }
            category(
                "recent", titleRecent, Icons.Default.History,
                playedList.ifEmpty { real.filter { it.isInstalled }.ifEmpty { real } },
                always = true,
            )
            category("unplayed", titleUnplayed, Icons.Default.SportsEsports, real.filter { played(it) == 0L }, always = true)
            category("favorites", titleFavorites, Icons.Default.Favorite, real.filter { it.appId in favorites })
            // the installed games as banners, for a mix of tall covers and wide art
            category(
                "installed", titleInstalled, Icons.Default.CheckCircle,
                real.filter { it.isInstalled && it.headerImageUrl.isNotEmpty() },
                always = true, wide = true,
            )
            category(
                "recommended", titleRecommended, Icons.Default.Star,
                real.filter { it.compatibilityStatus == GameCompatibilityStatus.RECOMMENDED },
            )
            category("toinstall", titleToInstall, Icons.Default.Download, real.filter { !it.isInstalled })
            category(
                "light", titleLight, Icons.Default.Speed,
                real.filter { it.sizeBytes in 1L..LIGHT_BYTES }.sortedBy { it.sizeBytes },
            )
            category("family", titleFamily, Icons.Default.Group, real.filter { it.isShared })
            val bySource = real.groupBy { it.gameSource }
            if (bySource.size > 1) {
                GameSource.entries.forEach { source ->
                    val list = bySource[source].orEmpty()
                    val title = when (source) {
                        GameSource.STEAM -> "Steam"
                        GameSource.GOG -> "GOG"
                        GameSource.EPIC -> "Epic"
                        GameSource.AMAZON -> "Amazon"
                        GameSource.CUSTOM_GAME -> titleCustom
                    }
                    category(
                        source.name, title,
                        if (source == GameSource.CUSTOM_GAME) Icons.Default.Storage else Icons.Default.Cloud,
                        list,
                    )
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
        // every measure follows the screen's height, so the proportions hold from a phone to a television
        val footer = (screenH * 0.12f).coerceIn(40.dp, 64.dp)
        val shelvesTop = screenH * 0.197f
        val tileH = (screenH * 0.25f).coerceAtLeast(72.dp)
        val tileW = tileH * COVER_RATIO
        val bannerH = tileH * 0.78f
        val bannerW = bannerH * 2.14f
        val gap = tileW * 0.115f
        val rowHeight = tileH * SELECTED_H + 10.dp
        val pillHeight = (screenH * 0.075f).coerceIn(26.dp, 40.dp)
        val chevron = (screenH * 0.075f).coerceIn(28.dp, 40.dp)
        val logoHeight = (screenH * 0.15f).coerceIn(56.dp, 112.dp)
        val density = LocalDensity.current
        val tilePx = with(density) { tileW.toPx() }
        val selectedPx = with(density) { (tileW * SELECTED_W).toPx() }
        val bannerSelectedPx = with(density) { (bannerW * SELECTED_BANNER_W).toPx() }
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

        // the shelves, one under the other; the page scrolls to keep the focused one in view
        Column(
            modifier = Modifier
                .offset(y = shelvesTop)
                .fillMaxWidth()
                .height(screenH - shelvesTop)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            shelves.forEach { shelf ->
                val aligned = shelf.key == LIBRARY_KEY
                val rowState = if (aligned) listState else rememberLazyListState()
                ShelfFrame(
                    title = shelf.title,
                    icon = shelf.icon,
                    rowHeight = rowHeight,
                    gap = gap,
                    pillHeight = pillHeight,
                    chevron = chevron,
                    rowState = rowState,
                    scrollStepPx = ((if (shelf.wide) with(density) { bannerW.toPx() } else tilePx) + with(density) { gap.toPx() }) * 4f,
                ) {
                    itemsIndexed(shelf.items, key = { _, item -> "${shelf.key}_${item.appId}" }) { _, item ->
                        val isCurrent = focusedShelf == shelf.key && item.appId == selectedId
                        AeroTile(
                            item = item,
                            width = if (shelf.wide) bannerW else tileW,
                            height = if (shelf.wide) bannerH else tileH,
                            wide = shelf.wide,
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
                            Box(modifier = Modifier.size(tileH), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Color.White)
                            }
                        }
                    }
                }
                // keep the current cover at the middle of the shelf that has the focus
                LaunchedEffect(focusedShelf, selectedId, shelf.items.size, selectedPx, bannerSelectedPx, viewportPx) {
                    if (focusedShelf == shelf.key) {
                        val at = shelf.items.indexOfFirst { it.appId == selectedId }
                        if (at >= 0) rowState.animateScrollToItem(at, -((viewportPx - (if (shelf.wide) bannerSelectedPx else selectedPx)) / 2f).toInt())
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
                    pillHeight = pillHeight,
                    chevron = chevron,
                    rowState = rememberLazyListState(),
                    scrollStepPx = (tilePx + with(density) { gap.toPx() }) * 4f,
                ) {
                    itemsIndexed(list, key = { _, game -> "emu_${game.uri}" }) { _, game ->
                        Box(modifier = Modifier.height(rowHeight), contentAlignment = Alignment.Center) {
                            EmulatorTile(game, tileW, tileH, false)
                        }
                    }
                }
            }
            // room under the last shelf so it can scroll clear of the hint bar
            Spacer(modifier = Modifier.height(footer + 16.dp))
        }

        // the current game's logo floats over the top of the shelves, as on the console's dashboard; drawn after them
        // so it stays on top
        val plateItem = items.firstOrNull { it.appId == selectedId }
            ?: shelves.firstOrNull()?.items?.firstOrNull()
        if (plateItem != null) {
            val plateTitle = if (plateItem.isRecTeaser) stringResource(R.string.rec_teaser_title) else plateItem.name
            val plateLogo = plateItem
                .takeIf { it.gameSource == GameSource.STEAM && !it.isRecTeaser }
                ?.let { "https://shared.steamstatic.com/store_item_assets/steam/apps/${it.gameId}/logo.png" }
            val altLogo by produceState<String?>(null, plateItem.appId) { value = CoverArt.logoUrl(appContext, plateItem) }
            Box(modifier = Modifier.fillMaxWidth().offset(y = screenH * 0.115f)) {
                AeroTitlePlate(title = plateTitle, logoUrl = plateLogo, altLogoUrl = altLogo, logoHeight = logoHeight)
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
    pillHeight: Dp,
    chevron: Dp,
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
                    .height(pillHeight)
                    .clip(CircleShape)
                    .background(style.fill())
                    .border(BorderStroke(1.dp, style.rim()), CircleShape)
                    .padding(start = pillHeight * 0.22f, end = pillHeight * 0.5f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = style.textColor(Color.White),
                    modifier = Modifier.size(pillHeight * 0.62f),
                )
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
                    size = chevron,
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp),
                    onClick = { scope.launch { rowState.animateScrollBy(-scrollStepPx) } },
                )
            }
            if (rowState.canScrollForward) {
                ScrollButton(
                    icon = Icons.Default.ChevronRight,
                    size = chevron,
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 4.dp),
                    onClick = { scope.launch { rowState.animateScrollBy(scrollStepPx) } },
                )
            }
        }
        // a faint reflection of the row on the glossy floor
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.12f), Color.Transparent))),
        )
    }
}

@Composable
private fun ScrollButton(icon: ImageVector, size: Dp, modifier: Modifier, onClick: () -> Unit) {
    val style by AeroStyle.state.collectAsStateWithLifecycle()
    Box(
        modifier = modifier
            .zIndex(2f)
            .size(size)
            .clip(CircleShape)
            .background(style.fill())
            .border(BorderStroke(1.dp, style.rim()), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = style.textColor(Color.White), modifier = Modifier.size(size * 0.7f))
    }
}

/**
 * One glass cover, slightly taller than wide: a frosted frame with a white-to-cyan edge, the art inset in it under a
 * cool frosted wash, and a soft halo. The current game's cover is larger with a stronger glow; the focused one has a
 * thicker edge.
 */
@Composable
private fun AeroTile(
    item: LibraryItem,
    width: Dp,
    height: Dp,
    wide: Boolean,
    current: Boolean,
    focusRequester: FocusRequester?,
    onFocus: () -> Unit,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val style by AeroStyle.state.collectAsStateWithLifecycle()
    val sizeSpec = spring<Dp>(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
    val w by animateDpAsState(if (current) width * (if (wide) SELECTED_BANNER_W else SELECTED_W) else width, sizeSpec, label = "aeroTileW")
    val h by animateDpAsState(if (current) height * SELECTED_H else height, sizeSpec, label = "aeroTileH")
    val radius = minOf(w, h) * 0.14f
    val shape = RoundedCornerShape(radius)
    val frame = 3.dp
    val glow = if (current) (style.selectionColor ?: FrameCyan) else FrameCyan
    // square art from SteamGridDB when the person has a key, else the store's cover cropped to the frame
    val tileContext = LocalContext.current
    val squareUrl by produceState<String?>(null, item.appId, wide) { value = if (wide) null else CoverArt.squareUrl(tileContext, item) }
    val imageUrl = if (wide) {
        item.headerImageUrl.ifEmpty { item.capsuleImageUrl }
    } else {
        squareUrl ?: item.capsuleImageUrl.ifEmpty { item.headerImageUrl }
    }
    val nameText: @Composable () -> Unit = {
        Text(
            text = item.name,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = style.textColor(Color.White),
            textAlign = TextAlign.Center,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(8.dp),
        )
    }
    Box(
        modifier = Modifier
            .zIndex(if (current) 1f else 0f)
            .width(w)
            .height(h)
            // the halo around the cover, outside its edge: faint on all of them, strong on the current one
            .drawBehind {
                val spread = (if (current) 14.dp else 6.dp).toPx()
                drawRoundRect(
                    color = glow.copy(alpha = if (focused) 0.55f else if (current) 0.4f else 0.16f),
                    topLeft = Offset(-spread / 2f, -spread / 2f),
                    size = Size(this.size.width + spread, this.size.height + spread),
                    cornerRadius = CornerRadius(radius.toPx() + spread / 2f),
                )
            }
            .clip(shape)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { if (it.isFocused) onFocus() }
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            // the frosted frame the art sits in
            .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.55f), FrameCyan.copy(alpha = 0.35f))))
            .border(
                BorderStroke(
                    if (focused) 3.dp else 1.5.dp,
                    Brush.verticalGradient(listOf(Color.White, FrameCyan.copy(alpha = if (current) 1f else 0.7f))),
                ),
                shape,
            )
            .padding(frame),
        contentAlignment = Alignment.Center,
    ) {
        val inner = RoundedCornerShape((radius - frame).coerceAtLeast(2.dp))
        Box(modifier = Modifier.fillMaxSize().clip(inner), contentAlignment = Alignment.Center) {
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
            // a frosted, cool wash over the art: bright at the top like a pane of glass, cyan at the bottom
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.34f),
                                Color.White.copy(alpha = 0.06f),
                                FrameCyan.copy(alpha = 0.22f),
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
                                listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.5f)),
                            ),
                        ),
                )
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = style.textColor(Color.White),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                )
            }
        }
    }
}
