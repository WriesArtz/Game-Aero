package app.gamenative.ui.screen.library.components

import android.view.KeyEvent
import androidx.compose.animation.core.Spring
import androidx.compose.ui.unit.Dp
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.layout.widthIn
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.gamenative.PrefManager
import app.gamenative.ui.theme.AeroStyle
import app.gamenative.R
import app.gamenative.ui.component.LocalBarBlur
import app.gamenative.ui.component.focusRing
import app.gamenative.ui.component.themedBar
import app.gamenative.ui.component.themedHeaderOrNull
import app.gamenative.emulator.EmulatorLibrary
import app.gamenative.emulator.EmulatorSelection
import app.gamenative.ui.enums.LibraryTab
import app.gamenative.ui.screen.support.SupportReplyWatcher
import app.gamenative.ui.theme.PluviaTheme
import app.gamenative.ui.theme.scaleAlpha
import app.gamenative.ui.theme.slidingFill
import app.gamenative.ui.util.WindowWidthClass
import app.gamenative.ui.util.rememberWindowWidthClass

/**
 * Tab bar for library navigation with sliding pill indicator.
 * Adapts to screen width
 */
@Composable
fun LibraryTabBar(
    currentTab: LibraryTab,
    tabs: List<LibraryTab>,
    tabCounts: Map<LibraryTab, Int>,
    onTabSelected: (LibraryTab) -> Unit,
    onOptionsClick: () -> Unit,
    onSearchClick: () -> Unit,
    onAddGameClick: () -> Unit,
    onMenuClick: () -> Unit,
    onNavigateDownToGrid: () -> Unit,
    onPreviousTab: () -> Unit = {},
    onNextTab: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val widthClass = rememberWindowWidthClass()

    val today = System.currentTimeMillis() / (24L * 60 * 60 * 1000)
    var recommendedSeenDay by remember { mutableLongStateOf(PrefManager.recommendedTabSeenDay) }
    LaunchedEffect(currentTab) {
        recommendedSeenDay = PrefManager.recommendedTabSeenDay
    }
    val showRecommendedDot = recommendedSeenDay != today

    when (widthClass) {
        WindowWidthClass.COMPACT -> CompactLibraryTabBar(
            currentTab = currentTab,
            tabs = tabs,
            tabCounts = tabCounts,
            showRecommendedDot = showRecommendedDot,
            onTabSelected = onTabSelected,
            onOptionsClick = onOptionsClick,
            onSearchClick = onSearchClick,
            onAddGameClick = onAddGameClick,
            onMenuClick = onMenuClick,
            onNavigateDownToGrid = onNavigateDownToGrid,
            onPreviousTab = onPreviousTab,
            onNextTab = onNextTab,
            modifier = modifier,
        )

        else -> ExpandedLibraryTabBar(
            currentTab = currentTab,
            tabs = tabs,
            tabCounts = tabCounts,
            showRecommendedDot = showRecommendedDot,
            onTabSelected = onTabSelected,
            onOptionsClick = onOptionsClick,
            onSearchClick = onSearchClick,
            onAddGameClick = onAddGameClick,
            onMenuClick = onMenuClick,
            onNavigateDownToGrid = onNavigateDownToGrid,
            onPreviousTab = onPreviousTab,
            onNextTab = onNextTab,
            modifier = modifier,
        )
    }
}

/**
 * Compact tab bar for narrow screens.
 * Centered tabs with action buttons for Options, Search, Add Game, and Menu.
 */
@Composable
private fun CompactLibraryTabBar(
    currentTab: LibraryTab,
    tabs: List<LibraryTab>,
    tabCounts: Map<LibraryTab, Int>,
    showRecommendedDot: Boolean,
    onTabSelected: (LibraryTab) -> Unit,
    onOptionsClick: () -> Unit,
    onSearchClick: () -> Unit,
    onAddGameClick: () -> Unit,
    onMenuClick: () -> Unit,
    onNavigateDownToGrid: () -> Unit,
    onPreviousTab: () -> Unit,
    onNextTab: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentIndex = tabs.indexOf(currentTab)
    val scrollState = rememberScrollState()
    val tabPositions = remember { mutableStateMapOf<Int, Float>() }
    val tabWidths = remember { mutableStateMapOf<Int, Float>() }

    LaunchedEffect(currentTab) {
        val pos = tabPositions[currentIndex] ?: return@LaunchedEffect
        val width = tabWidths[currentIndex] ?: return@LaunchedEffect
        val targetCenter = (pos + width / 2).toInt()
        val viewportCenter = scrollState.viewportSize / 2
        scrollState.animateScrollTo((targetCenter - viewportCenter).coerceAtLeast(0))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                themedHeaderOrNull()?.let { Modifier.themedBar(it, contentBelow = true, blurState = LocalBarBlur.current) }
                    ?: Modifier.background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                PluviaTheme.colors.bar.scaleAlpha(0.95f),
                                PluviaTheme.colors.bar.scaleAlpha(0.8f),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )
            .padding(top = 8.dp, bottom = 12.dp, start = 8.dp, end = 8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .focusGroup()
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                        when (keyEvent.nativeKeyEvent.keyCode) {
                            KeyEvent.KEYCODE_DPAD_DOWN -> {
                                onNavigateDownToGrid()
                                true
                            }
                            KeyEvent.KEYCODE_BUTTON_L1 -> {
                                onPreviousTab()
                                true
                            }
                            KeyEvent.KEYCODE_BUTTON_R1 -> {
                                onNextTab()
                                true
                            }
                            else -> false
                        }
                    } else {
                        false
                    }
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            CompactIconButton(
                icon = Icons.Default.Tune,
                contentDescription = stringResource(R.string.options),
                onClick = onOptionsClick,
            )

            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(tabPillShape(20.dp))
                    .then(
                        if (themedHeaderOrNull() != null) {
                            Modifier
                        } else {
                            (if (PluviaTheme.isThemed) Modifier.background(PluviaTheme.barBacking) else Modifier)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        },
                    )
                    .horizontalScroll(scrollState)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEachIndexed { index, tab ->
                    val isSelected = tab == currentTab
                    val tabInteractionSource = remember { MutableInteractionSource() }
                    val isTabFocused by tabInteractionSource.collectIsFocusedAsState()
                    Box(
                        modifier = Modifier
                            .onGloballyPositioned { coordinates ->
                                tabPositions[index] = coordinates.positionInParent().x
                                tabWidths[index] = coordinates.size.width.toFloat()
                            }
                            .clip(tabPillShape(16.dp))
                            .then(
                                PluviaTheme.accentGradientBrush?.takeIf { isSelected && PluviaTheme.selectedTabFill == null }
                                    ?.let { Modifier.slidingFill(it, PluviaTheme.fillSlideSec) }
                                    ?: Modifier.background(
                                        when {
                                            isSelected -> PluviaTheme.selectedTabFill ?: MaterialTheme.colorScheme.primary
                                            isTabFocused -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                            else -> Color.Transparent
                                        },
                                    ),
                            )
                            .focusRing(tabInteractionSource, tabPillShape(16.dp), width = 2.dp)
                            .selectable(
                                selected = isSelected,
                                interactionSource = tabInteractionSource,
                                indication = null,
                                onClick = { onTabSelected(tab) },
                            )
                            .padding(horizontal = if (tab.icon != null) 8.dp else 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        val count = if (PluviaTheme.hideTabCounts) null else tabCounts[tab]
                        val tabColor = when {
                            isSelected -> selectedTabContent()
                            isTabFocused -> MaterialTheme.colorScheme.primary
                            else -> barContentColor(0.7f)
                        }
                        if (tab.icon != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TabIcon(
                                    tab = tab,
                                    isSelected = isSelected,
                                    showDot = showRecommendedDot,
                                    tint = tabColor,
                                    size = 18.dp,
                                )
                                if (count != null && count > 0) {
                                    Text(
                                        text = "($count)",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) PluviaTheme.selectedWeight(FontWeight.Bold) else FontWeight.Medium,
                                        maxLines = 1,
                                        color = tabColor,
                                        modifier = Modifier.padding(start = 4.dp),
                                    )
                                }
                            }
                        } else {
                            val label = if (count != null && count > 0) {
                                stringResource(R.string.library_tab_with_count, stringResource(tab.labelResId), count)
                            } else {
                                stringResource(tab.labelResId)
                            }
                            Text(
                                text = PluviaTheme.tabText(label),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) PluviaTheme.selectedWeight(FontWeight.Bold) else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = tabColor,
                            )
                        }
                    }
                }
            }

            CompactIconButton(
                icon = Icons.Default.Search,
                contentDescription = stringResource(R.string.search),
                onClick = onSearchClick,
            )
            CompactIconButton(
                icon = Icons.Default.Add,
                contentDescription = stringResource(R.string.action_add_game),
                onClick = onAddGameClick,
            )
            Box {
                CompactIconButton(
                    icon = Icons.Default.Menu,
                    contentDescription = stringResource(R.string.menu),
                    onClick = onMenuClick,
                )
                SupportUnreadDot(modifier = Modifier.align(Alignment.TopEnd))
            }
        }
    }
}

/** Text and icons on the top bar: the theme top bar color, else onSurface, at [alpha]. */
@Composable
@ReadOnlyComposable
private fun barContentColor(alpha: Float): Color {
    val header = themedHeaderOrNull()
    val base = if (header == null) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
    } else {
        header.contentColor.scaleAlpha(alpha)
    }
    return if (PrefManager.aeroHome) AeroStyle.state.value.textColor(base) else base
}

/** Text on the selected tab: readable on the chosen selection color when there is one. */
@Composable
private fun selectedTabContent(): Color =
    if (PrefManager.aeroHome && AeroStyle.state.value.selectionColor != null) {
        AeroStyle.state.value.onSelection(PluviaTheme.onSelectedTab)
    } else {
        PluviaTheme.onSelectedTab
    }

/** Tabs and the tab strip are pills; a theme corner radius keeps them fully round. */
@Composable
@ReadOnlyComposable
private fun tabPillShape(default: Dp): Shape =
    if (PluviaTheme.cornerRadius == null) PluviaTheme.shape(default) else PluviaTheme.pillShape

@Composable
private fun SupportUnreadDot(modifier: Modifier = Modifier) {
    if (SupportReplyWatcher.unread.value.isEmpty()) return
    Box(
        modifier = modifier
            .size(10.dp)
            .background(MaterialTheme.colorScheme.primary, CircleShape),
    )
}

/**
 * Simple icon button for compact tab bar.
 */
@Composable
private fun CompactIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Box(
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(
                if (isFocused) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                },
            )
            .focusRing(interactionSource, CircleShape, width = 2.dp)
            .selectable(
                selected = false,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isFocused) {
                MaterialTheme.colorScheme.primary
            } else {
                barContentColor(0.8f)
            },
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * Expanded tab bar for wide screens (landscape phone, tablet).
 */
@Composable
private fun ExpandedLibraryTabBar(
    currentTab: LibraryTab,
    tabs: List<LibraryTab>,
    tabCounts: Map<LibraryTab, Int>,
    showRecommendedDot: Boolean,
    onTabSelected: (LibraryTab) -> Unit,
    onOptionsClick: () -> Unit,
    onSearchClick: () -> Unit,
    onAddGameClick: () -> Unit,
    onMenuClick: () -> Unit,
    onNavigateDownToGrid: () -> Unit,
    onPreviousTab: () -> Unit,
    onNextTab: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentIndex = tabs.indexOf(currentTab)
    val scrollState = rememberScrollState()

    val tabPositions = remember { mutableStateMapOf<Int, Float>() }
    val tabWidths = remember { mutableStateMapOf<Int, Float>() }

    val density = LocalDensity.current

    val indicatorOffset by animateDpAsState(
        targetValue = with(density) { (tabPositions[currentIndex] ?: 0f).toDp() },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "indicatorOffset",
    )

    val indicatorWidth by animateDpAsState(
        targetValue = with(density) { (tabWidths[currentIndex] ?: 80f).toDp() },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "indicatorWidth",
    )

    LaunchedEffect(currentTab) {
        val pos = tabPositions[currentIndex] ?: return@LaunchedEffect
        val width = tabWidths[currentIndex] ?: return@LaunchedEffect
        val targetCenter = (pos + width / 2).toInt()
        val viewportCenter = scrollState.viewportSize / 2
        scrollState.animateScrollTo((targetCenter - viewportCenter).coerceAtLeast(0))
    }

    Box(
        modifier = modifier
            .then(
                themedHeaderOrNull()?.let { Modifier.themedBar(it, contentBelow = true, blurState = LocalBarBlur.current) }
                    ?: Modifier.background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                PluviaTheme.colors.bar.scaleAlpha(0.98f),
                                PluviaTheme.colors.bar.scaleAlpha(0.85f),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )
            .padding(top = 8.dp, bottom = 20.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .focusGroup()
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                        when (keyEvent.nativeKeyEvent.keyCode) {
                            KeyEvent.KEYCODE_DPAD_DOWN -> {
                                onNavigateDownToGrid()
                                true
                            }
                            KeyEvent.KEYCODE_BUTTON_L1 -> {
                                onPreviousTab()
                                true
                            }
                            KeyEvent.KEYCODE_BUTTON_R1 -> {
                                onNextTab()
                                true
                            }
                            else -> false
                        }
                    } else {
                        false
                    }
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconActionButton(
                icon = Icons.Default.Tune,
                contentDescription = stringResource(R.string.options),
                onClick = onOptionsClick,
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(tabPillShape(24.dp))
                    .then(
                        if (themedHeaderOrNull() != null) {
                            Modifier
                        } else {
                            (if (PluviaTheme.isThemed) Modifier.background(PluviaTheme.barBacking) else Modifier)
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        ),
                                    ),
                                )
                        },
                    )
                    .horizontalScroll(scrollState)
                    .padding(4.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                // Sliding pill indicator (rendered behind tabs)
                Box(
                    modifier = Modifier
                        .offset { IntOffset(indicatorOffset.roundToPx(), 0) }
                        .width(indicatorWidth)
                        .height(40.dp)
                        .clip(tabPillShape(20.dp))
                        .then(
                            PluviaTheme.selectedTabFill?.let { Modifier.background(it) }
                                ?: PluviaTheme.accentGradientBrush?.let { Modifier.slidingFill(it, PluviaTheme.fillSlideSec) }
                                ?: Modifier.background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                                        ),
                                    ),
                                ),
                        ),
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    tabs.forEachIndexed { index, tab ->
                        TabItem(
                            tab = tab,
                            count = if (PluviaTheme.hideTabCounts) null else tabCounts[tab],
                            isSelected = tab == currentTab,
                            showDot = showRecommendedDot,
                            onClick = { onTabSelected(tab) },
                            onPositioned = { position, width ->
                                tabPositions[index] = position
                                tabWidths[index] = width
                            },
                        )
                    }
                }
            }

            IconActionButton(
                icon = Icons.Default.Search,
                contentDescription = stringResource(R.string.search),
                onClick = onSearchClick,
            )

            IconActionButton(
                icon = Icons.Default.Add,
                contentDescription = stringResource(R.string.action_add_game),
                onClick = onAddGameClick,
            )

            Box {
                IconActionButton(
                    icon = Icons.Default.Menu,
                    contentDescription = stringResource(R.string.menu),
                    onClick = onMenuClick,
                )
                SupportUnreadDot(modifier = Modifier.align(Alignment.TopEnd))
            }
        }
    }
}

/**
 * The library's top bar as a console dashboard: three crystal pills, the actions on the left, the categories in the
 * middle and the clock, battery and menu on the right in a slanted piece. Same behavior as [LibraryTabBar].
 */
@Composable
fun AeroLibraryTabBar(
    currentTab: LibraryTab,
    tabs: List<LibraryTab>,
    tabCounts: Map<LibraryTab, Int>,
    onTabSelected: (LibraryTab) -> Unit,
    onOptionsClick: () -> Unit,
    onSearchClick: () -> Unit,
    onAddGameClick: () -> Unit,
    onMenuClick: () -> Unit,
    onNavigateDownToGrid: () -> Unit,
    onPreviousTab: () -> Unit = {},
    onNextTab: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val clock by produceState("") {
        while (true) {
            value = java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(java.util.Date())
            delay(20_000)
        }
    }
    val battery by produceState(-1) {
        while (true) {
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
            value = if (level >= 0 && scale > 0) level * 100 / scale else -1
            delay(60_000)
        }
    }
    val today = System.currentTimeMillis() / (24L * 60 * 60 * 1000)
    var recommendedSeenDay by remember { mutableLongStateOf(PrefManager.recommendedTabSeenDay) }
    LaunchedEffect(currentTab) { recommendedSeenDay = PrefManager.recommendedTabSeenDay }
    val showRecommendedDot = recommendedSeenDay != today

    val tabScroll = rememberScrollState()

    Row(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .focusGroup()
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_DOWN -> { onNavigateDownToGrid(); true }
                        KeyEvent.KEYCODE_BUTTON_L1 -> { onPreviousTab(); true }
                        KeyEvent.KEYCODE_BUTTON_R1 -> { onNextTab(); true }
                        else -> false
                    }
                } else {
                    false
                }
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // actions
        AeroPill(shape = RoundedCornerShape(50)) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (PrefManager.username.isNotBlank()) {
                    Text(
                        text = PrefManager.username,
                        style = MaterialTheme.typography.titleSmall,
                        color = barContentColor(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .widthIn(max = 120.dp)
                            .padding(start = 12.dp, end = 6.dp),
                    )
                }
                IconActionButton(Icons.Default.Tune, stringResource(R.string.options), onOptionsClick)
                IconActionButton(Icons.Default.Search, stringResource(R.string.search), onSearchClick)
                IconActionButton(Icons.Default.Add, stringResource(R.string.action_add_game), onAddGameClick)
            }
        }

        // categories
        AeroPill(shape = RoundedCornerShape(50), modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier
                    .horizontalScroll(tabScroll)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                LaunchedEffect(Unit) { EmulatorLibrary.ensureLoaded(context) }
                val emulatorEntries by EmulatorLibrary.entries.collectAsStateWithLifecycle()
                val emulatorGames by EmulatorLibrary.games.collectAsStateWithLifecycle()
                val consoleId by EmulatorSelection.id.collectAsStateWithLifecycle()
                tabs.forEach { tab ->
                    val selected = tab == currentTab && consoleId == null
                    Box(
                        modifier = Modifier
                            .clip(tabPillShape(20.dp))
                            .then(
                                if (selected) {
                                    AeroStyle.state.value.selectionColor?.let { Modifier.background(it) }
                                        ?: PluviaTheme.selectedTabFill?.let { Modifier.background(it) }
                                        ?: Modifier.background(MaterialTheme.colorScheme.primary)
                                } else {
                                    Modifier
                                },
                            ),
                    ) {
                        TabItem(
                            tab = tab,
                            count = if (PluviaTheme.hideTabCounts) null else tabCounts[tab],
                            isSelected = selected,
                            showDot = showRecommendedDot,
                            onClick = {
                                EmulatorSelection.select(null)
                                onTabSelected(tab)
                            },
                            onPositioned = { _, _ -> },
                        )
                    }
                }
                // the consoles of the emulators, next to Steam, GOG and the rest
                emulatorEntries.forEach { entry ->
                    val count = emulatorGames.count { it.entryId == entry.id }
                    if (count == 0) return@forEach
                    val selected = consoleId == entry.id
                    val interaction = remember { MutableInteractionSource() }
                    val focused by interaction.collectIsFocusedAsState()
                    Box(
                        modifier = Modifier
                            .heightIn(min = 40.dp)
                            .clip(tabPillShape(20.dp))
                            .then(
                                if (selected) {
                                    AeroStyle.state.value.selectionColor?.let { Modifier.background(it) }
                                        ?: PluviaTheme.selectedTabFill?.let { Modifier.background(it) }
                                        ?: Modifier.background(MaterialTheme.colorScheme.primary)
                                } else {
                                    Modifier
                                },
                            )
                            .focusRing(interaction, tabPillShape(20.dp), width = 2.dp)
                            .selectable(
                                selected = selected,
                                interactionSource = interaction,
                                indication = null,
                                role = Role.Tab,
                                onClick = { EmulatorSelection.select(entry.id) },
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (PluviaTheme.hideTabCounts) entry.shortLabel else "${entry.shortLabel} ($count)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = barContentColor(if (selected) 1f else if (focused) 0.9f else 0.6f),
                            maxLines = 1,
                        )
                    }
                }
            }
        }

        // clock, battery and menu in a slanted piece
        val slant = GenericShape { size: Size, _ ->
            val skew = size.height * 0.35f
            moveTo(skew, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width - skew, size.height)
            lineTo(0f, size.height)
            close()
        }
        AeroPill(shape = slant) {
            Row(
                modifier = Modifier.padding(start = 22.dp, end = 14.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (battery >= 0) {
                    Text(
                        text = "$battery%",
                        style = MaterialTheme.typography.labelLarge,
                        color = barContentColor(0.85f),
                        maxLines = 1,
                    )
                }
                Text(
                    text = clock,
                    style = MaterialTheme.typography.titleMedium,
                    color = barContentColor(1f),
                    maxLines = 1,
                )
                Box {
                    IconActionButton(Icons.Default.Menu, stringResource(R.string.menu), onMenuClick)
                    SupportUnreadDot(modifier = Modifier.align(Alignment.TopEnd))
                }
            }
        }
    }
}

/** A crystal capsule: a clear gradient with a bright top edge and a thin glass rim, no blur haze. */
@Composable
private fun AeroPill(
    shape: Shape,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val style by AeroStyle.state.collectAsStateWithLifecycle()
    // the Metro dashboard draws flat, square pieces in a solid color instead of glass
    val drawn = if (style.metro) RoundedCornerShape(2.dp) else shape
    Box(
        modifier = modifier
            .clip(drawn)
            .then(
                if (style.metro) {
                    Modifier.background((style.selectionColor ?: Color(0xFF107C10)).copy(alpha = 0.9f))
                } else {
                    Modifier
                        .background(style.fill())
                        .border(BorderStroke(1.dp, style.rim()), drawn)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun IconActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.15f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "iconButtonScale",
    )

    val alpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0.7f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "iconButtonAlpha",
    )

    Box(
        modifier = modifier
            // Both the focus scale and alpha go in a SINGLE graphicsLayer above the ring. Previously
            // .scale (above) and .alpha (below) were two separate animating layers sandwiching the
            // focusRing draw node, which made the ring flicker off ~100ms after focus. One stable
            // layer wrapping clip/background/focusRing fixes it.
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .size(44.dp)
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = if (isFocused) {
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        )
                    } else {
                        listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                        )
                    },
                ),
            )
            .focusRing(interactionSource, CircleShape, width = 2.dp)
            .selectable(
                // These are actions, not toggles; feeding isFocused back as `selected` caused an
                // extra recomposition on every focus change.
                selected = false,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isFocused) {
                MaterialTheme.colorScheme.primary
            } else {
                barContentColor(0.8f)
            },
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun TabItem(
    tab: LibraryTab,
    count: Int?,
    isSelected: Boolean,
    showDot: Boolean,
    onClick: () -> Unit,
    onPositioned: (Float, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val textAlpha by animateFloatAsState(
        targetValue = when {
            isSelected -> 1f
            isFocused -> 0.9f
            else -> 0.6f
        },
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "textAlpha",
    )

    val label = if (count != null && count > 0) {
        stringResource(R.string.library_tab_with_count, stringResource(tab.labelResId), count)
    } else {
        stringResource(tab.labelResId)
    }

    Box(
        modifier = modifier
            // Min height = the sliding pill's height (40.dp) so the focus ring outlines the same
            // 40dp-tall, 20dp-radius capsule the user sees. Uses heightIn (not a fixed height) so the
            // label can grow at large accessibility font scales instead of being clipped.
            .heightIn(min = 40.dp)
            .clip(tabPillShape(20.dp))
            .focusRing(interactionSource, tabPillShape(20.dp), width = 2.dp)
            .onGloballyPositioned { coordinates ->
                onPositioned(
                    coordinates.positionInParent().x,
                    coordinates.size.width.toFloat(),
                )
            }
            .selectable(
                selected = isSelected,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = if (tab.icon != null) 10.dp else 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (tab.icon != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TabIcon(
                    tab = tab,
                    isSelected = isSelected,
                    showDot = showDot,
                    tint = when {
                        isSelected -> selectedTabContent()
                        else -> barContentColor(textAlpha)
                    },
                    size = 20.dp,
                )
                if (count != null && count > 0) {
                    Text(
                        text = "($count)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) PluviaTheme.selectedWeight(FontWeight.Bold) else FontWeight.Medium,
                        maxLines = 1,
                        color = when {
                            isSelected -> selectedTabContent()
                            else -> barContentColor(textAlpha)
                        },
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }
        } else {
            Text(
                text = PluviaTheme.tabText(label),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) PluviaTheme.selectedWeight(FontWeight.Bold) else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = when {
                    isSelected -> selectedTabContent()
                    else -> barContentColor(textAlpha)
                },
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun TabIcon(
    tab: LibraryTab,
    isSelected: Boolean,
    showDot: Boolean,
    tint: Color,
    size: Dp,
) {
    val icon = tab.icon ?: return
    Box {
        Icon(
            imageVector = icon,
            contentDescription = stringResource(tab.labelResId),
            tint = tint,
            modifier = Modifier.size(size),
        )
        if (tab == LibraryTab.RECOMMENDED && !isSelected && showDot) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 3.dp, y = (-2).dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiary),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1A1A)
@Composable
private fun Preview_LibraryTabBar() {
    PluviaTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            LibraryTabBar(
                currentTab = LibraryTab.ALL,
                tabs = LibraryTab.visibleEntries,
                tabCounts = mapOf(
                    LibraryTab.ALL to 42,
                    LibraryTab.STEAM to 30,
                    LibraryTab.GOG to 8,
                    LibraryTab.EPIC to 4,
                    LibraryTab.LOCAL to 3,
                ),
                onTabSelected = {},
                onOptionsClick = {},
                onSearchClick = {},
                onAddGameClick = {},
                onMenuClick = {},
                onNavigateDownToGrid = {},
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1A1A)
@Composable
private fun Preview_LibraryTabBar_Steam() {
    PluviaTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            LibraryTabBar(
                currentTab = LibraryTab.STEAM,
                tabs = LibraryTab.visibleEntries,
                tabCounts = mapOf(
                    LibraryTab.ALL to 42,
                    LibraryTab.STEAM to 30,
                    LibraryTab.GOG to 8,
                    LibraryTab.EPIC to 4,
                    LibraryTab.LOCAL to 3,
                ),
                onTabSelected = {},
                onOptionsClick = {},
                onSearchClick = {},
                onAddGameClick = {},
                onMenuClick = {},
                onNavigateDownToGrid = {},
            )
        }
    }
}
