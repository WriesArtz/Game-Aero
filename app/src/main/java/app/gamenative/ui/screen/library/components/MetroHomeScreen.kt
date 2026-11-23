package app.gamenative.ui.screen.library.components

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.gamenative.R
import app.gamenative.data.LibraryItem
import app.gamenative.ui.data.LibraryState
import app.gamenative.ui.theme.AeroStyle
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil.CoilImage
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull

/** The flat colors of the Metro tiles, picked by game so that neighbours differ. */
private val MetroColors = listOf(
    Color(0xFF107C10), Color(0xFF1BA1E2), Color(0xFFD80073), Color(0xFFE3A21A), Color(0xFF6A00FF), Color(0xFF00ABA9),
)

private fun metroColorOf(item: LibraryItem): Color =
    MetroColors[(item.appId.hashCode() and 0x7fffffff) % MetroColors.size]

/**
 * The library's home as the Xbox 360 "Metro" dashboard: a big thin hub title, the current game as a large flat tile
 * and a panorama of square solid-color tiles beside it. No glass, no rounded corners. It takes the same arguments as
 * [AeroHomeScreen] and works the same way for focus, paging and opening a game.
 */
@Composable
internal fun MetroHomeScreen(
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
    val style by AeroStyle.state.collectAsStateWithLifecycle()
    var selected by remember { mutableIntStateOf(focusTargetListIndex ?: 0) }
    LaunchedEffect(focusTargetListIndex) {
        if (focusTargetListIndex != null) selected = focusTargetListIndex
    }
    val current = selected.coerceIn(0, (items.size - 1).coerceAtLeast(0))
    val accent = style.selectionColor ?: Color(0xFF107C10)

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
        val heroW: Dp = (screenW * 0.34f).coerceIn(180.dp, 520.dp)
        val heroH: Dp = heroW * 9f / 16f
        val titleSize = (screenH.value * 0.1f).coerceIn(32f, 64f)
        val heroTop = topBar + (titleSize * 1.25f).dp
        val tile: Dp = heroH * 0.62f
        val rowHeight = heroH
        val shelfTop = heroTop + heroH + 18.dp
        val shelfRoom = screenH - footer - shelfTop

        // keep the current tile at the left edge of the panorama
        LaunchedEffect(current, items.size) {
            if (items.isNotEmpty()) listState.animateScrollToItem(current)
        }

        Text(
            text = stringResource(R.string.metro_hub_games),
            color = style.textColor(Color.White),
            fontSize = titleSize.sp,
            fontWeight = FontWeight.Light,
            maxLines = 1,
            modifier = Modifier.offset(x = 28.dp, y = topBar - 6.dp),
        )

        if (items.isEmpty()) {
            if (!state.isLoading) {
                Text(
                    text = stringResource(R.string.library_no_items),
                    color = style.textColor(Color.White),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                )
            }
            return@BoxWithConstraints
        }

        // the current game, as a large flat tile
        val hero = items[current]
        Box(
            modifier = Modifier
                .offset(x = 28.dp, y = heroTop)
                .size(heroW, heroH)
                .background(metroColorOf(hero))
                .clickable { onNavigate(hero.appId) },
        ) {
            val heroUrl = hero.headerImageUrl.ifEmpty { hero.capsuleImageUrl }
            if (heroUrl.isNotEmpty()) {
                CoilImage(
                    imageModel = { heroUrl },
                    modifier = Modifier.fillMaxSize(),
                    imageOptions = ImageOptions(contentScale = ContentScale.Crop, contentDescription = hero.name),
                    loading = {},
                    failure = {},
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(accent.copy(alpha = 0.92f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(
                    text = if (hero.isRecTeaser) stringResource(R.string.rec_teaser_title) else hero.name,
                    color = style.onSelection(Color.White),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // the panorama of square tiles
        LazyRow(
            state = listState,
            modifier = Modifier
                .offset(x = 28.dp + heroW + 16.dp, y = heroTop)
                .width(screenW - (28.dp + heroW + 16.dp))
                .height(rowHeight),
            contentPadding = PaddingValues(end = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            itemsIndexed(items) { index, item ->
                MetroTile(
                    item = item,
                    size = tile,
                    current = index == current,
                    accent = accent,
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

        // room under the hero for the emulators, when the screen has it
        if (shelfRoom >= 96.dp) {
            Box(modifier = Modifier.fillMaxWidth().offset(y = shelfTop)) {
                EmulatorShelf(height = minOf(shelfRoom, 150.dp), metro = true)
            }
        }
    }
}

/** One flat square tile; the current game's tile has a white edge and the focused one a thick one. */
@Composable
private fun MetroTile(
    item: LibraryItem,
    size: Dp,
    current: Boolean,
    accent: Color,
    focusRequester: FocusRequester?,
    onFocus: () -> Unit,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val style by AeroStyle.state.collectAsStateWithLifecycle()
    val shape = RoundedCornerShape(2.dp)
    val imageUrl = item.capsuleImageUrl.ifEmpty { item.headerImageUrl }
    Box(
        modifier = Modifier
            .size(size)
            .clip(shape)
            .background(metroColorOf(item))
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { if (it.isFocused) onFocus() }
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .border(
                BorderStroke(
                    if (focused) 4.dp else if (current) 2.dp else 0.dp,
                    if (focused) Color.White else Color.White.copy(alpha = 0.7f),
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
                failure = {
                    Text(
                        text = item.name,
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(8.dp),
                    )
                },
            )
        } else {
            Text(
                text = item.name,
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(8.dp),
            )
        }
        if (current) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(accent.copy(alpha = 0.9f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    text = item.name,
                    color = style.onSelection(Color.White),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
