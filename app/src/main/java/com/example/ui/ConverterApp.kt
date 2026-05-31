package com.example.ui

import android.app.Application
import android.net.Uri
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.TextStyle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.database.ConversionRecord
import com.example.util.IntentUtils
import com.example.util.LocalFileConverters
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SymmetricGeometricTransformerHeader(
    sourceExt: String,
    targetExt: String
) {
    val displaySrc = if (sourceExt.isNotEmpty()) sourceExt.uppercase() else "MHT"
    val displayDst = if (targetExt.isNotEmpty()) targetExt.uppercase() else "PDF"

    val srcIcon = when (sourceExt.lowercase()) {
        "mht", "mhtml", "html" -> Icons.Default.Code
        "png", "jpg", "jpeg", "webp", "bmp" -> Icons.Default.Image
        "csv" -> Icons.Default.List
        "json", "xml" -> Icons.Default.Code
        else -> Icons.Default.Description
    }

    val dstIcon = when (targetExt.lowercase()) {
        "pdf" -> Icons.Default.PictureAsPdf
        "png", "jpg", "jpeg", "webp" -> Icons.Default.Image
        "txt", "md" -> Icons.Default.Description
        "csv", "xls" -> Icons.Default.List
        "html" -> Icons.Default.Html
        else -> Icons.Default.Autorenew
    }

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("geometric_symmetry_header")
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Source box
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .border(BorderStroke(1.dp, Color(0xFFD0BCFF)), shape = RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = srcIcon,
                            contentDescription = "Source Format",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (displaySrc.startsWith(".")) displaySrc else ".$displaySrc",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "SOURCE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            letterSpacing = 1.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Action Pill / Arrow
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "to",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Target box
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .border(BorderStroke(1.dp, Color(0xFFD0BCFF)), shape = RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = dstIcon,
                            contentDescription = "Target Format",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (displayDst.startsWith(".")) displayDst else ".$displayDst",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "TARGET",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConverterApp(
    viewModel: ConversionViewModel = viewModel(
        factory = ConversionViewModelFactory(LocalContext.current.applicationContext as Application)
    )
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // UI state flows
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedFileUri by viewModel.selectedFileUri.collectAsStateWithLifecycle()
    val fileName by viewModel.fileName.collectAsStateWithLifecycle()
    val fileSize by viewModel.fileSize.collectAsStateWithLifecycle()
    val fileExtension by viewModel.fileExtension.collectAsStateWithLifecycle()
    val targetExtension by viewModel.targetExtension.collectAsStateWithLifecycle()
    val useAI by viewModel.useAI.collectAsStateWithLifecycle()
    val aiAdditionalPrompt by viewModel.aiAdditionalPrompt.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val possibleOutputs by viewModel.possibleOutputs.collectAsStateWithLifecycle()
    val filteredRecords by viewModel.filteredRecords.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) } // 0 = Convertitore, 1 = Cronologia

    // File selection launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        viewModel.selectFile(uri, context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Convertitore Universale",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            fontFamily = FontFamily.SansSerif
                        )
                        Text(
                            text = "Converti MHT a PDF, Immagini, CSV e testo con AI",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    if (activeTab == 1 && filteredRecords.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearHistory() }) {
                            Icon(
                                imageVector = Icons.Default.DeleteForever,
                                contentDescription = "Cancella cronologia",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                windowInsets = WindowInsets.navigationBars
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Icon(Icons.Default.Autorenew, contentDescription = "Convertitore") },
                    label = { Text("Convertitore") }
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = { Icon(Icons.Default.History, contentDescription = "Cronologia") },
                    label = { Text("Cronologia") }
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (activeTab) {
                0 -> {
                    // Converter Dashboard Screen
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Geometric symmetry header
                        item {
                            SymmetricGeometricTransformerHeader(
                                sourceExt = fileExtension,
                                targetExt = targetExtension
                            )
                        }

                        // SECTION 1: File selection box
                        item {
                            FileDropZone(
                                selectedUri = selectedFileUri,
                                fileName = fileName,
                                fileSize = fileSize,
                                fileExtension = fileExtension,
                                onSelectClick = { filePickerLauncher.launch("*/*") },
                                onClearClick = { viewModel.selectFile(null, context) }
                            )
                        }

                        // Config settings if file is selected
                        if (selectedFileUri != null) {
                            item {
                                Card(
                                    shape = RoundedCornerShape(24.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "Opzioni di Conversione",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(bottom = 12.dp)
                                        )

                                        // AI vs Local toggle selector
                                        ConversionEngineSelector(
                                            useAI = useAI,
                                            sourceExtension = fileExtension,
                                            onEngineChange = { viewModel.setUseAI(it) }
                                        )

                                        Spacer(modifier = Modifier.height(16.dp))

                                        // Target extension choice
                                        Text(
                                            text = if (useAI) "Estensione di destinazione (AI)" else "Estensione di destinazione (Locale)",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )

                                        TargetExtensionRow(
                                            possibleOutputs = possibleOutputs,
                                            selectedOutput = targetExtension,
                                            onSelectOutput = { viewModel.setTargetExtension(it) }
                                        )

                                        // Additional prompt textbox for AI
                                        if (useAI) {
                                            Spacer(modifier = Modifier.height(16.dp))
                                            OutlinedTextField(
                                                value = aiAdditionalPrompt,
                                                onValueChange = { viewModel.setAiAdditionalPrompt(it) },
                                                label = { Text("Istruzioni aggiuntive per l'AI (Opzionale)") },
                                                placeholder = { Text("Es: Traduci i testi in inglese, formatta come tabella...") },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("ai_custom_prompt"),
                                                textStyle = TextStyle(fontSize = 14.sp),
                                                maxLines = 3,
                                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                                keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                                                colors = TextFieldDefaults.colors(
                                                    focusedContainerColor = Color.Transparent,
                                                    unfocusedContainerColor = Color.Transparent
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            // Run process button state
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { 
                                        keyboardController?.hide()
                                        viewModel.executeConversion(context) 
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .testTag("submit_button"),
                                    enabled = uiState !is ConversionUiState.Converting && targetExtension.isNotEmpty(),
                                    shape = RoundedCornerShape(28.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(Icons.Default.Autorenew, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Avvia Conversione",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                    }
                                }
                            }
                        }

                        // SECTION 3: Progress status display cards
                        item {
                            AnimatedVisibility(
                                visible = uiState !is ConversionUiState.Idle,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                StatusSection(
                                    uiState = uiState,
                                    onDismiss = { viewModel.resetUiState() }
                                )
                            }
                        }
                    }
                }

                1 -> {
                    // History Screen Dashboard
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Search bar
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("Cerca file, estensioni...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_history_bar"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (filteredRecords.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FolderOpen,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                        modifier = Modifier.size(72.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = if (searchQuery.isNotEmpty()) "Nessun risultato per la ricerca." else "Ancora nessuna conversione effettuata.",
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = if (searchQuery.isNotEmpty()) "Prova a digitare un altro nome o estensione." else "I file convertiti appariranno qui di seguito.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.outline,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(bottom = 16.dp)
                            ) {
                                items(filteredRecords, key = { it.id }) { record ->
                                    HistoryItemCard(
                                        record = record,
                                        onOpen = { IntentUtils.openFile(context, it) },
                                        onShare = { IntentUtils.shareFile(context, it) },
                                        onDelete = { viewModel.deleteRecord(record.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FileDropZone(
    selectedUri: Uri?,
    fileName: String,
    fileSize: Long,
    fileExtension: String,
    onSelectClick: () -> Unit,
    onClearClick: () -> Unit
) {
    val context = LocalContext.current
    
    if (selectedUri == null) {
        // Dotted dashboard file drop picker
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable { onSelectClick() }
                .background(Color.White.copy(alpha = 0.5f))
                .border(BorderStroke(1.5.dp, Color(0xFF938F99)), shape = RoundedCornerShape(16.dp))
                .testTag("file_picker_zone"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CloudUpload,
                    contentDescription = "Carica file",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Seleziona un file da convertire",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Supporta MHT (MHTML), HTML, Immagini, CSV, TXT, MD",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        // Render selected file capsule
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val fileIcon = when (fileExtension) {
                    "mht", "mhtml", "html" -> Icons.Default.Code
                    "png", "jpg", "jpeg", "webp", "bmp" -> Icons.Default.Image
                    "csv" -> Icons.Default.List
                    "json", "xml" -> Icons.Default.Code
                    else -> Icons.Default.Description
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = fileIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = fileName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = Formatter.formatShortFileSize(context, fileSize),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = fileExtension.uppercase(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onClearClick,
                    modifier = Modifier.testTag("clear_file_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Rimuovi file",
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Composable
fun ConversionEngineSelector(
    useAI: Boolean,
    sourceExtension: String,
    onEngineChange: (Boolean) -> Unit
) {
    val isLocalConvertible = LocalFileConverters.SUPPORTED_INPUTS.contains(sourceExtension.lowercase())

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        // Local engine card
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .clickable(enabled = isLocalConvertible) { onEngineChange(false) }
                .background(
                    if (!useAI && isLocalConvertible) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    else Color.White.copy(alpha = 0.5f)
                )
                .border(
                    BorderStroke(
                        1.dp,
                        if (!useAI && isLocalConvertible) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = if (!useAI && isLocalConvertible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Conversione Locale",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = if (!useAI && isLocalConvertible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
                Text(
                    text = "Alta Fedeltà / Istantaneo",
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center
                )
            }
        }

        // AI Engine card
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .clickable { onEngineChange(true) }
                .background(
                    if (useAI) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
                    else Color.White.copy(alpha = 0.5f)
                )
                .border(
                    BorderStroke(
                        1.dp,
                        if (useAI) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.outlineVariant
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = if (useAI) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Conversione AI",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = if (useAI) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline
                )
                Text(
                    text = "Gestita da Gemini Flash",
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TargetExtensionRow(
    possibleOutputs: List<String>,
    selectedOutput: String,
    onSelectOutput: (String) -> Unit
) {
    if (possibleOutputs.isEmpty()) {
        Text(
            text = "Nessun formato di output disponibile per questo file.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(vertical = 4.dp)
        )
    } else {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            possibleOutputs.forEach { ext ->
                val isSelected = ext == selectedOutput
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectOutput(ext) },
                    label = { 
                        Text(
                            text = ext.uppercase(), 
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.sp
                        ) 
                    },
                    modifier = Modifier.testTag("target_chip_${ext}")
                )
            }
        }
    }
}

@Composable
fun StatusSection(
    uiState: ConversionUiState,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    when (uiState) {
        is ConversionUiState.Converting -> {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth().testTag("converting_state")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 3.dp
                    )
                    Column {
                        Text(
                            text = "Conversione in corso...",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Stiamo rielaborando file e impaginando la struttura...",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        is ConversionUiState.Success -> {
            val record = uiState.record
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE8F5E9) // soft light green background
                ),
                border = BorderStroke(1.dp, Color(0xFFC8E6C9)),
                modifier = Modifier.fillMaxWidth().testTag("success_state")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = Color(0xFF2E7D32)
                        )
                        Text(
                            text = "Conversione completata!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF2E7D32)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "File d'uscita salvato nella cartella privata dell'app:",
                        fontSize = 11.sp,
                        color = Color(0xFF33691E)
                    )
                    Text(
                        text = "${record.fileName.substringBeforeLast(".")}.${record.destExtension}",
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = Color(0xFF1B5E20)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { record.outputFilePath?.let { IntentUtils.openFile(context, it) } },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2E7D32)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("open_file_btn")
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Apri file", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { record.outputFilePath?.let { IntentUtils.shareFile(context, it) } },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF2E7D32)
                            ),
                            border = BorderStroke(1.dp, Color(0xFF2E7D32)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("share_file_btn")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Condividi", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        is ConversionUiState.Error -> {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFEBEE) // light red
                ),
                border = BorderStroke(1.dp, Color(0xFFFFCDD2)),
                modifier = Modifier.fillMaxWidth().testTag("error_state")
            ) {
                Column(modifier = Modifier.padding(15.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Error",
                            tint = Color(0xFFC62828)
                        )
                        Text(
                            text = "Errore della conversione",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFFC62828)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = uiState.message,
                        fontSize = 12.sp,
                        color = Color(0xFFB71C1C)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFC62828)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.align(Alignment.End).testTag("error_dismiss_btn")
                    ) {
                        Text("Riprova", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        else -> {}
    }
}

private fun size(dp: Int) = Modifier.size(dp.dp)

@Composable
fun HistoryItemCard(
    record: ConversionRecord,
    onOpen: (String) -> Unit,
    onShare: (String) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().testTag("history_record_${record.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                val statusIcon = if (record.isSuccess) Icons.Default.CheckCircle else Icons.Default.Cancel
                val statusColor = if (record.isSuccess) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error

                Icon(
                    imageVector = statusIcon,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = record.fileName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp).testTag("delete_record_${record.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Rimuovi scheda",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Conversion path display
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = record.sourceExtension.uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.Monospace
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(10.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = record.destExtension.uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Date formatting stamp
                val dateStr = try {
                    val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
                    sdf.format(Date(record.timestamp))
                } catch (e: Exception) {
                    ""
                }

                Text(
                    text = dateStr,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // Expanded detail / actions if succeeded
            if (record.isSuccess && record.outputFilePath != null) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(
                        onClick = { onOpen(record.outputFilePath) },
                        modifier = Modifier.weight(1f).height(32.dp).testTag("history_open_${record.id}")
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Apri", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    TextButton(
                        onClick = { onShare(record.outputFilePath) },
                        modifier = Modifier.weight(1f).height(32.dp).testTag("history_share_${record.id}")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Condividi", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else if (!record.isSuccess && record.errorMessage != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "Errore: ${record.errorMessage}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
