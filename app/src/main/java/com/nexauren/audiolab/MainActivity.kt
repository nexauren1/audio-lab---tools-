package com.nexauren.audiolab

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Equalizer
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Merge
import androidx.compose.material.icons.outlined.MicOff
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Piano
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.SlowMotionVideo
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.TheaterComedy
import androidx.compose.material.icons.outlined.Timelapse
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Waves
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sin

private val Ink = Color(0xFF080B11)
private val SurfaceOne = Color(0xFF111722)
private val SurfaceTwo = Color(0xFF17202C)
private val Stroke = Color(0xFF263242)
private val PrimaryMint = Color(0xFF79F2D0)
private val Lavender = Color(0xFFA7A6FF)
private val Sky = Color(0xFF88CEFF)
private val Amber = Color(0xFFFFC878)
private val Rose = Color(0xFFFF91BE)
private val MainText = Color(0xFFF3F6FB)
private val MutedText = Color(0xFF9BA8B9)

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Ink.toArgb()
        window.navigationBarColor = Ink.toArgb()
        setContent {
            AudioLabTheme {
                AudioLabApp()
            }
        }
    }
}

@Composable
private fun AudioLabTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = PrimaryMint,
            onPrimary = Ink,
            secondary = Lavender,
            background = Ink,
            surface = SurfaceOne,
            onBackground = MainText,
            onSurface = MainText,
            outline = Stroke
        ),
        content = content
    )
}

private enum class ToolCategory(val labelRes: Int, val accent: Color) {
    ALL(R.string.category_all, PrimaryMint),
    EFFECTS(R.string.category_effects, PrimaryMint),
    VOICE_AI(R.string.category_voice_ai, Lavender),
    EDIT_CONVERT(R.string.category_edit_convert, Sky),
    CREATE_ANALYZE(R.string.category_create_analyze, Amber),
    LAB(R.string.category_lab, Rose)
}

private enum class NavItem(val labelRes: Int, val icon: ImageVector) {
    EXPLORE(R.string.nav_explore, Icons.Outlined.Explore),
    SAVED(R.string.nav_saved, Icons.Outlined.FavoriteBorder),
    SETTINGS(R.string.nav_settings, Icons.Outlined.Settings)
}

private data class AudioTool(
    val id: String,
    val nameRes: Int,
    val descriptionRes: Int,
    val category: ToolCategory,
    val icon: ImageVector
)

private val audioTools = listOf(
    AudioTool("audio_eq", R.string.audio_eq, R.string.audio_eq_desc, ToolCategory.EFFECTS, Icons.Outlined.Equalizer),
    AudioTool("audio_8d", R.string.audio_8d, R.string.audio_8d_desc, ToolCategory.EFFECTS, Icons.Outlined.Headphones),
    AudioTool("add_reverb", R.string.add_reverb, R.string.add_reverb_desc, ToolCategory.EFFECTS, Icons.Outlined.Waves),
    AudioTool("bass_boost", R.string.bass_boost, R.string.bass_boost_desc, ToolCategory.EFFECTS, Icons.Outlined.GraphicEq),
    AudioTool("nightcore", R.string.nightcore, R.string.nightcore_desc, ToolCategory.EFFECTS, Icons.Outlined.Speed),
    AudioTool("daycore", R.string.daycore, R.string.daycore_desc, ToolCategory.EFFECTS, Icons.Outlined.Timelapse),
    AudioTool("slowed_reverb", R.string.slowed_reverb, R.string.slowed_reverb_desc, ToolCategory.EFFECTS, Icons.Outlined.SlowMotionVideo),
    AudioTool("sped_up", R.string.sped_up, R.string.sped_up_desc, ToolCategory.EFFECTS, Icons.Outlined.FastForward),
    AudioTool("tiktok_slowed", R.string.tiktok_slowed, R.string.tiktok_slowed_desc, ToolCategory.EFFECTS, Icons.Outlined.PlayCircleOutline),
    AudioTool("vaporwave", R.string.vaporwave, R.string.vaporwave_desc, ToolCategory.EFFECTS, Icons.Outlined.AutoAwesome),
    AudioTool("volume_boost", R.string.volume_boost, R.string.volume_boost_desc, ToolCategory.EFFECTS, Icons.Outlined.VolumeUp),

    AudioTool("voice_cloning", R.string.voice_cloning, R.string.voice_cloning_desc, ToolCategory.VOICE_AI, Icons.Outlined.RecordVoiceOver),
    AudioTool("vocal_tuner", R.string.vocal_tuner, R.string.vocal_tuner_desc, ToolCategory.VOICE_AI, Icons.Outlined.Tune),
    AudioTool("voice_to_instrument", R.string.voice_to_instrument, R.string.voice_to_instrument_desc, ToolCategory.VOICE_AI, Icons.Outlined.Piano),
    AudioTool("noise_remover", R.string.noise_remover, R.string.noise_remover_desc, ToolCategory.VOICE_AI, Icons.Outlined.FilterAlt),
    AudioTool("stem_splitter", R.string.stem_splitter, R.string.stem_splitter_desc, ToolCategory.VOICE_AI, Icons.Outlined.LibraryMusic),
    AudioTool("text_to_speech", R.string.text_to_speech, R.string.text_to_speech_desc, ToolCategory.VOICE_AI, Icons.Outlined.TextFields),
    AudioTool("speech_to_text", R.string.speech_to_text, R.string.speech_to_text_desc, ToolCategory.VOICE_AI, Icons.Outlined.Subtitles),
    AudioTool("speech_to_song", R.string.speech_to_song, R.string.speech_to_song_desc, ToolCategory.VOICE_AI, Icons.Outlined.MusicNote),
    AudioTool("vocal_remover", R.string.vocal_remover, R.string.vocal_remover_desc, ToolCategory.VOICE_AI, Icons.Outlined.MicOff),
    AudioTool("voice_changer", R.string.voice_changer, R.string.voice_changer_desc, ToolCategory.VOICE_AI, Icons.Outlined.TheaterComedy),

    AudioTool("audio_converter", R.string.audio_converter, R.string.audio_converter_desc, ToolCategory.EDIT_CONVERT, Icons.Outlined.AudioFile),
    AudioTool("audio_joiner", R.string.audio_joiner, R.string.audio_joiner_desc, ToolCategory.EDIT_CONVERT, Icons.Outlined.Merge),
    AudioTool("audio_trimmer", R.string.audio_trimmer, R.string.audio_trimmer_desc, ToolCategory.EDIT_CONVERT, Icons.Outlined.ContentCut),
    AudioTool("flac_cover_art", R.string.flac_cover_art, R.string.flac_cover_art_desc, ToolCategory.EDIT_CONVERT, Icons.Outlined.Image),
    AudioTool("metadata_editor", R.string.metadata_editor, R.string.metadata_editor_desc, ToolCategory.EDIT_CONVERT, Icons.Outlined.Sell),
    AudioTool("pitch_shifter", R.string.pitch_shifter, R.string.pitch_shifter_desc, ToolCategory.EDIT_CONVERT, Icons.Outlined.GraphicEq),
    AudioTool("reverse_audio", R.string.reverse_audio, R.string.reverse_audio_desc, ToolCategory.EDIT_CONVERT, Icons.Outlined.Replay),
    AudioTool("music_speed", R.string.music_speed, R.string.music_speed_desc, ToolCategory.EDIT_CONVERT, Icons.Outlined.Speed),
    AudioTool("video_to_audio", R.string.video_to_audio, R.string.video_to_audio_desc, ToolCategory.EDIT_CONVERT, Icons.Outlined.VideoLibrary),

    AudioTool("beat_maker", R.string.beat_maker, R.string.beat_maker_desc, ToolCategory.CREATE_ANALYZE, Icons.Outlined.Piano),
    AudioTool("bpm_detector", R.string.bpm_detector, R.string.bpm_detector_desc, ToolCategory.CREATE_ANALYZE, Icons.Outlined.Insights),
    AudioTool("granular_synth", R.string.granular_synth, R.string.granular_synth_desc, ToolCategory.CREATE_ANALYZE, Icons.Outlined.Waves),
    AudioTool("tone_generator", R.string.tone_generator, R.string.tone_generator_desc, ToolCategory.CREATE_ANALYZE, Icons.Outlined.MusicNote),
    AudioTool("music_visualizer", R.string.music_visualizer, R.string.music_visualizer_desc, ToolCategory.CREATE_ANALYZE, Icons.Outlined.Equalizer),
    AudioTool("soundboard", R.string.soundboard, R.string.soundboard_desc, ToolCategory.CREATE_ANALYZE, Icons.Outlined.Widgets),

    AudioTool("extreme_time_stretch", R.string.extreme_time_stretch, R.string.extreme_time_stretch_desc, ToolCategory.LAB, Icons.Outlined.Timelapse),
    AudioTool("noise_music_generator", R.string.noise_music_generator, R.string.noise_music_generator_desc, ToolCategory.LAB, Icons.Outlined.AutoAwesome)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AudioLabApp() {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("audio_lab_settings", Context.MODE_PRIVATE) }
    val savedTools = remember {
        mutableStateListOf<String>().apply {
            addAll(preferences.getStringSet("saved_tools", emptySet()).orEmpty())
        }
    }
    var query by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf(ToolCategory.ALL) }
    var selectedTab by rememberSaveable { mutableStateOf(NavItem.EXPLORE) }
    var activeToolId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedFileName by rememberSaveable { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val isPortuguese = currentLanguageIsPortuguese()
    val activeTool = audioTools.firstOrNull { it.id == activeToolId }

    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) {
                // A file picker URI can still be used during this session.
            }
            selectedFileName = getDisplayName(context, uri)
            coroutineScope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.audio_loaded, selectedFileName.orEmpty()))
            }
        }
    }

    fun toggleSaved(toolId: String) {
        if (savedTools.contains(toolId)) savedTools.remove(toolId) else savedTools.add(toolId)
        preferences.edit().putStringSet("saved_tools", savedTools.toSet()).apply()
    }

    fun chooseLanguage(tag: String) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
    }

    fun toggleLanguage() {
        chooseLanguage(if (isPortuguese) "en" else "pt")
    }

    Scaffold(
        containerColor = Ink,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(containerColor = SurfaceOne, contentColor = MainText) {
                NavItem.values().forEach { item ->
                    NavigationBarItem(
                        selected = selectedTab == item,
                        onClick = {
                            selectedTab = item
                            if (item != NavItem.SETTINGS) {
                                query = ""
                                selectedCategory = ToolCategory.ALL
                            }
                        },
                        icon = { Icon(imageVector = item.icon, contentDescription = stringResource(item.labelRes)) },
                        label = { Text(stringResource(item.labelRes), fontSize = 11.sp) },
                        alwaysShowLabel = true
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (selectedTab == NavItem.SETTINGS) {
                SettingsScreen(
                    isPortuguese = isPortuguese,
                    onLanguageSelected = ::chooseLanguage,
                    onLanguageToggle = ::toggleLanguage
                )
            } else {
                MainBrowseScreen(
                    selectedTab = selectedTab,
                    query = query,
                    onQueryChange = { query = it },
                    selectedCategory = selectedCategory,
                    onCategoryChange = { selectedCategory = it },
                    savedToolIds = savedTools.toSet(),
                    isPortuguese = isPortuguese,
                    onLanguageToggle = ::toggleLanguage,
                    onToolClick = { activeToolId = it.id },
                    onToggleSaved = ::toggleSaved,
                    onImportAudio = { audioPicker.launch(arrayOf("audio/*", "video/*")) }
                )
            }
        }
    }

    if (activeTool != null) {
        AudioToolWorkspace(
            tool = activeTool,
            fileName = selectedFileName,
            onChooseFile = { audioPicker.launch(arrayOf("audio/*", "video/*")) },
            onDismiss = { activeToolId = null }
        )
    }
}

private fun currentLanguageIsPortuguese(): Boolean {
    val appTag = AppCompatDelegate.getApplicationLocales().toLanguageTags()
    return if (appTag.isBlank()) {
        java.util.Locale.getDefault().language.equals("pt", ignoreCase = true)
    } else {
        appTag.startsWith("pt", ignoreCase = true)
    }
}

private fun getDisplayName(context: Context, uri: Uri): String {
    var cursor: Cursor? = null
    return try {
        cursor = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        if (cursor != null && cursor.moveToFirst()) {
            cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
        } else {
            uri.lastPathSegment ?: context.getString(R.string.picked_file)
        }
    } catch (_: Exception) {
        uri.lastPathSegment ?: context.getString(R.string.picked_file)
    } finally {
        cursor?.close()
    }
}

@Composable
private fun MainBrowseScreen(
    selectedTab: NavItem,
    query: String,
    onQueryChange: (String) -> Unit,
    selectedCategory: ToolCategory,
    onCategoryChange: (ToolCategory) -> Unit,
    savedToolIds: Set<String>,
    isPortuguese: Boolean,
    onLanguageToggle: () -> Unit,
    onToolClick: (AudioTool) -> Unit,
    onToggleSaved: (String) -> Unit,
    onImportAudio: () -> Unit
) {
    val context = LocalContext.current
    val isExplore = selectedTab == NavItem.EXPLORE
    val isSaved = selectedTab == NavItem.SAVED
    val filteredTools = audioTools.filter { tool ->
        (selectedCategory == ToolCategory.ALL || tool.category == selectedCategory) &&
            (!isSaved || tool.id in savedToolIds) &&
            (
                query.isBlank() ||
                    context.getString(tool.nameRes).contains(query, ignoreCase = true) ||
                    context.getString(tool.descriptionRes).contains(query, ignoreCase = true)
                )
    }
    val showGroupedHome = isExplore && query.isBlank() && selectedCategory == ToolCategory.ALL

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, top = 12.dp, end = 18.dp, bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { BrandHeader(isPortuguese = isPortuguese, onLanguageToggle = onLanguageToggle) }
        item { SearchBar(query = query, onQueryChange = onQueryChange) }

        if (showGroupedHome) {
            item { HeroCard(onImportAudio = onImportAudio) }
            item { CategoryStrip(selectedCategory = selectedCategory, onCategoryChange = onCategoryChange) }

            ToolCategory.values().filter { it != ToolCategory.ALL }.forEach { category ->
                val categoryTools = audioTools.filter { it.category == category }
                item(key = "section_" + category.name) {
                    CategorySectionHeader(
                        category = category,
                        count = categoryTools.size,
                        onViewAll = { onCategoryChange(category) }
                    )
                }
                items(
                    items = categoryTools.take(4).chunked(2),
                    key = { row -> row.joinToString("_") { it.id } }
                ) { row ->
                    ToolRow(
                        tools = row,
                        savedToolIds = savedToolIds,
                        onToolClick = onToolClick,
                        onToggleSaved = onToggleSaved
                    )
                }
            }
        } else {
            item {
                BrandPageTitle(
                    title = if (isSaved) stringResource(R.string.saved_tools) else stringResource(R.string.section_results),
                    subtitle = stringResource(R.string.tool_count, filteredTools.size)
                )
            }
            item { CategoryStrip(selectedCategory = selectedCategory, onCategoryChange = onCategoryChange) }

            if (filteredTools.isEmpty()) {
                item {
                    EmptyState(
                        title = if (isSaved) stringResource(R.string.no_saved_title) else stringResource(R.string.no_results_title),
                        body = if (isSaved) stringResource(R.string.no_saved_body) else stringResource(R.string.no_results_body),
                        actionLabel = if (isSaved) null else stringResource(R.string.clear_search),
                        onAction = if (isSaved) null else {
                            {
                                onQueryChange("")
                                onCategoryChange(ToolCategory.ALL)
                            }
                        }
                    )
                }
            } else {
                items(
                    items = filteredTools.chunked(2),
                    key = { row -> row.joinToString("_") { it.id } }
                ) { row ->
                    ToolRow(
                        tools = row,
                        savedToolIds = savedToolIds,
                        onToolClick = onToolClick,
                        onToggleSaved = onToggleSaved
                    )
                }
            }
        }
    }
}

@Composable
private fun BrandHeader(isPortuguese: Boolean, onLanguageToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(16.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF17352F), Color(0xFF21223B))),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.GraphicEq, contentDescription = null, tint = PrimaryMint, modifier = Modifier.size(30.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("AUDIO LAB", color = MainText, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.1.sp)
            Text(stringResource(R.string.brand_caption), color = MutedText, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp)
        }
        Surface(
            modifier = Modifier.clickable(onClick = onLanguageToggle),
            shape = CircleShape,
            color = SurfaceTwo,
            border = BorderStroke(1.dp, Stroke)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Outlined.Language, contentDescription = null, tint = PrimaryMint, modifier = Modifier.size(15.dp))
                Text(if (isPortuguese) "PT" else "EN", color = MainText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(17.dp),
        placeholder = { Text(stringResource(R.string.search_hint), color = MutedText, fontSize = 14.sp) },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = MutedText) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(38.dp)) {
                    Text("×", color = MutedText, fontSize = 24.sp)
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search, keyboardType = KeyboardType.Text),
        keyboardActions = KeyboardActions(onSearch = {}),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = SurfaceOne,
            unfocusedContainerColor = SurfaceOne,
            focusedBorderColor = PrimaryMint,
            unfocusedBorderColor = Stroke,
            focusedTextColor = MainText,
            unfocusedTextColor = MainText,
            cursorColor = PrimaryMint
        )
    )
}

@Composable
private fun HeroCard(onImportAudio: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF18372F), Color(0xFF192334), Color(0xFF211F39))))
            .drawBehind {
                drawRect(brush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.05f), Color.Transparent)))
            }
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(color = Color.White.copy(alpha = 0.08f), shape = RoundedCornerShape(8.dp)) {
            Text(
                stringResource(R.string.hero_eyebrow),
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                color = PrimaryMint, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.0.sp
            )
        }
        Text(stringResource(R.string.hero_title), color = MainText, fontSize = 28.sp, lineHeight = 32.sp, fontWeight = FontWeight.ExtraBold)
        Text(stringResource(R.string.hero_body), color = Color(0xFFC2CED9), fontSize = 13.sp, lineHeight = 19.sp)
        WaveformArtwork(modifier = Modifier.fillMaxWidth().height(42.dp))
        Button(
            onClick = onImportAudio,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryMint, contentColor = Ink),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 11.dp)
        ) {
            Icon(Icons.Outlined.AudioFile, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.import_audio), fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(6.dp))
            Icon(Icons.Outlined.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun WaveformArtwork(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val count = 58
        val center = size.height / 2f
        val step = size.width / count
        for (index in 0 until count) {
            val shape = abs(sin(index * 0.57)).toFloat()
            val rhythm = 0.18f + ((index % 7).toFloat() / 11f)
            val amplitude = (0.18f + shape * rhythm).coerceIn(0.12f, 0.92f) * size.height
            val x = step * index + step / 2f
            drawLine(
                color = if (index < 22) PrimaryMint else Lavender.copy(alpha = 0.55f),
                start = Offset(x, center - amplitude / 2f),
                end = Offset(x, center + amplitude / 2f),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun CategoryStrip(selectedCategory: ToolCategory, onCategoryChange: (ToolCategory) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.browse_categories), color = MainText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ToolCategory.values().forEach { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { onCategoryChange(category) },
                    label = { Text(stringResource(category.labelRes), fontSize = 12.sp, maxLines = 1) },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (selectedCategory == category) PrimaryMint else Stroke),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryMint,
                        selectedLabelColor = Ink,
                        containerColor = SurfaceOne,
                        labelColor = MutedText
                    )
                )
            }
        }
    }
}

@Composable
private fun CategorySectionHeader(category: ToolCategory, count: Int, onViewAll: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(4.dp, 27.dp).clip(CircleShape).background(category.accent))
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(category.labelRes), color = MainText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.tool_count, count), color = MutedText, fontSize = 11.sp)
        }
        Box(modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(SurfaceTwo)) {
            TextButton(onClick = onViewAll, contentPadding = PaddingValues(horizontal = 11.dp, vertical = 4.dp)) {
                Text(stringResource(R.string.view_all), color = category.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun BrandPageTitle(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, color = MainText, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        Text(subtitle, color = MutedText, fontSize = 12.sp)
    }
}

@Composable
private fun ToolRow(
    tools: List<AudioTool>,
    savedToolIds: Set<String>,
    onToolClick: (AudioTool) -> Unit,
    onToggleSaved: (String) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        tools.forEach { tool ->
            ToolCard(
                tool = tool,
                isSaved = tool.id in savedToolIds,
                onClick = { onToolClick(tool) },
                onToggleSaved = { onToggleSaved(tool.id) },
                modifier = Modifier.weight(1f)
            )
        }
        if (tools.size == 1) Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun ToolCard(
    tool: AudioTool,
    isSaved: Boolean,
    onClick: () -> Unit,
    onToggleSaved: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = tool.category.accent
    Card(
        onClick = onClick,
        modifier = modifier.heightIn(min = 164.dp),
        shape = RoundedCornerShape(21.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceOne),
        border = BorderStroke(1.dp, Stroke)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(accent.copy(alpha = 0.13f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(tool.icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
                }
                IconButton(onClick = onToggleSaved, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = if (isSaved) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = stringResource(if (isSaved) R.string.remove_favorite else R.string.add_favorite),
                        tint = if (isSaved) Rose else MutedText,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
            Text(
                text = stringResource(tool.nameRes), color = MainText, fontSize = 14.sp, lineHeight = 18.sp,
                fontWeight = FontWeight.Bold, maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(tool.descriptionRes), color = MutedText, fontSize = 11.sp, lineHeight = 15.sp,
                maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(accent))
                Text(stringResource(tool.category.labelRes), color = accent, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun EmptyState(title: String, body: String, actionLabel: String?, onAction: (() -> Unit)?) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(SurfaceOne).padding(horizontal = 24.dp, vertical = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(Icons.Outlined.Search, contentDescription = null, tint = PrimaryMint, modifier = Modifier.size(36.dp))
        Text(title, color = MainText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(body, color = MutedText, fontSize = 13.sp, lineHeight = 18.sp)
        if (actionLabel != null && onAction != null) {
            OutlinedButton(onClick = onAction, shape = RoundedCornerShape(12.dp)) {
                Text(actionLabel, color = PrimaryMint)
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    isPortuguese: Boolean,
    onLanguageSelected: (String) -> Unit,
    onLanguageToggle: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, top = 16.dp, end = 18.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item { BrandHeader(isPortuguese = isPortuguese, onLanguageToggle = onLanguageToggle) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.settings_title), color = MainText, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
                Text(stringResource(R.string.settings_intro), color = MutedText, fontSize = 14.sp, lineHeight = 20.sp)
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.language_title), color = MainText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.language_help), color = MutedText, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    LanguageCard(
                        title = stringResource(R.string.language_english),
                        code = "EN",
                        selected = !isPortuguese,
                        onClick = { onLanguageSelected("en") },
                        modifier = Modifier.weight(1f)
                    )
                    LanguageCard(
                        title = stringResource(R.string.language_portuguese),
                        code = "PT",
                        selected = isPortuguese,
                        onClick = { onLanguageSelected("pt") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        item {
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(SurfaceOne).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Outlined.Info, contentDescription = null, tint = PrimaryMint)
                    Text(stringResource(R.string.about_title), color = MainText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Text(stringResource(R.string.about_body), color = MutedText, fontSize = 13.sp, lineHeight = 19.sp)
                Divider(color = Stroke)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(stringResource(R.string.status_label), color = MutedText, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text(stringResource(R.string.status_value), color = PrimaryMint, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Surface(color = PrimaryMint.copy(alpha = 0.12f), shape = CircleShape) {
                        Text("v0.1.0", color = PrimaryMint, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        item {
            Text(stringResource(R.string.privacy_note), color = MutedText, fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(horizontal = 2.dp))
        }
    }
}

@Composable
private fun LanguageCard(
    title: String,
    code: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.heightIn(min = 104.dp),
        colors = CardDefaults.cardColors(containerColor = if (selected) Color(0xFF16372F) else SurfaceOne),
        border = BorderStroke(1.dp, if (selected) PrimaryMint else Stroke),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(code, color = if (selected) PrimaryMint else MutedText, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                if (selected) Icon(Icons.Outlined.Language, contentDescription = null, tint = PrimaryMint, modifier = Modifier.size(17.dp))
            }
            Text(title, color = MainText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            if (selected) Text(stringResource(R.string.selected_language), color = PrimaryMint, fontSize = 10.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AudioToolWorkspace(
    tool: AudioTool,
    fileName: String?,
    onChooseFile: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SurfaceOne,
        contentColor = MainText,
        dragHandle = {
            Surface(modifier = Modifier.padding(top = 10.dp), shape = CircleShape, color = Stroke) {
                Spacer(Modifier.width(38.dp).height(4.dp))
            }
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier.size(54.dp).clip(RoundedCornerShape(17.dp)).background(tool.category.accent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(tool.icon, contentDescription = null, tint = tool.category.accent, modifier = Modifier.size(28.dp))
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(tool.nameRes), color = MainText, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                    Text(stringResource(tool.category.labelRes), color = tool.category.accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Text(stringResource(tool.descriptionRes), color = MutedText, fontSize = 14.sp, lineHeight = 20.sp)
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(SurfaceTwo).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Icon(Icons.Outlined.AudioFile, contentDescription = null, tint = PrimaryMint)
                    Text(
                        if (fileName == null) stringResource(R.string.no_file_yet) else stringResource(R.string.picked_file),
                        color = MainText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (fileName != null) {
                    Text(fileName, color = MutedText, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                } else {
                    Text(stringResource(R.string.workspace_intro), color = MutedText, fontSize = 12.sp, lineHeight = 17.sp)
                }
                Button(
                    onClick = onChooseFile,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(13.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryMint, contentColor = Ink)
                ) {
                    Icon(Icons.Outlined.AudioFile, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(if (fileName == null) R.string.choose_audio else R.string.change_audio), fontWeight = FontWeight.Bold)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF132821)).padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(Icons.Outlined.Info, contentDescription = null, tint = PrimaryMint, modifier = Modifier.size(20.dp))
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(stringResource(R.string.workspace_prototype_title), color = PrimaryMint, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.workspace_prototype_body), color = Color(0xFFB7CDC6), fontSize = 12.sp, lineHeight = 17.sp)
                }
            }
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(13.dp),
                border = BorderStroke(1.dp, Stroke)
            ) {
                Text(stringResource(R.string.close), color = MainText)
            }
        }
    }
}
