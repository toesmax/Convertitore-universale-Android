package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.ConversionRecord
import com.example.data.repository.ConversionRepository
import com.example.util.GeminiFileConverter
import com.example.util.LocalFileConverters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

sealed interface ConversionUiState {
    object Idle : ConversionUiState
    object Converting : ConversionUiState
    data class Success(val record: ConversionRecord) : ConversionUiState
    data class Error(val message: String) : ConversionUiState
}

class ConversionViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "ConversionViewModel"
    private val repository: ConversionRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = ConversionRepository(database.conversionDao())
    }

    // List of history records
    val allRecords: StateFlow<List<ConversionRecord>> = repository.allRecords
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Current State Flow for ongoing file conversion
    private val _uiState = MutableStateFlow<ConversionUiState>(ConversionUiState.Idle)
    val uiState: StateFlow<ConversionUiState> = _uiState.asStateFlow()

    // Configuration states
    private val _selectedFileUri = MutableStateFlow<Uri?>(null)
    val selectedFileUri = _selectedFileUri.asStateFlow()

    private val _fileName = MutableStateFlow("")
    val fileName = _fileName.asStateFlow()

    private val _fileSize = MutableStateFlow(0L)
    val fileSize = _fileSize.asStateFlow()

    private val _fileExtension = MutableStateFlow("")
    val fileExtension = _fileExtension.asStateFlow()

    private val _targetExtension = MutableStateFlow("")
    val targetExtension = _targetExtension.asStateFlow()

    private val _useAI = MutableStateFlow(false)
    val useAI = _useAI.asStateFlow()

    private val _aiAdditionalPrompt = MutableStateFlow("")
    val aiAdditionalPrompt = _aiAdditionalPrompt.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // Computed list of supported output extensions for detected source
    val possibleOutputs: StateFlow<List<String>> = _fileExtension
        .combine(_useAI) { ext, ai ->
            if (ai) {
                // If using AI, let them choose any typical text/data extensions
                listOf("txt", "html", "md", "json", "xml", "csv", "yaml", "py", "java", "kt", "js", "css", "sql")
            } else {
                LocalFileConverters.getPossibleOutputs(ext)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered records in history based on search query
    val filteredRecords: StateFlow<List<ConversionRecord>> = allRecords
        .combine(_searchQuery) { records, query ->
            if (query.isBlank()) {
                records
            } else {
                records.filter {
                    it.fileName.contains(query, ignoreCase = true) ||
                    it.sourceExtension.contains(query, ignoreCase = true) ||
                    it.destExtension.contains(query, ignoreCase = true)
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectFile(uri: Uri?, context: Context) {
        if (uri == null) {
            _selectedFileUri.value = null
            _fileName.value = ""
            _fileSize.value = 0L
            _fileExtension.value = ""
            _targetExtension.value = ""
            return
        }

        _selectedFileUri.value = uri
        
        // Query original file descriptor name, size, type
        var name = "file"
        var size = 0L
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) name = cursor.getString(nameIndex)
                    if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving selected file URI metadata", e)
        }

        _fileName.value = name
        _fileSize.value = size
        
        val ext = File(name).extension.lowercase()
        _fileExtension.value = ext

        // Default the first available output
        val localOutputs = LocalFileConverters.getPossibleOutputs(ext)
        if (localOutputs.isNotEmpty()) {
            _targetExtension.value = localOutputs[0]
            _useAI.value = false
        } else {
            // Suggest AI conversion
            _useAI.value = true
            _targetExtension.value = "txt"
        }
    }

    fun setTargetExtension(ext: String) {
        _targetExtension.value = ext
    }

    fun setUseAI(value: Boolean) {
        _useAI.value = value
        // Reset target extension appropriately
        viewModelScope.launch {
            val outputs = possibleOutputs.value
            if (outputs.isNotEmpty()) {
                _targetExtension.value = outputs[0]
            }
        }
    }

    fun setAiAdditionalPrompt(prompt: String) {
        _aiAdditionalPrompt.value = prompt
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun resetUiState() {
        _uiState.value = ConversionUiState.Idle
    }

    fun executeConversion(context: Context) {
        val uri = _selectedFileUri.value
        val sourceExt = _fileExtension.value
        val destExt = _targetExtension.value
        val originalName = _fileName.value
        val isAiEnabled = _useAI.value
        val aiPrompt = _aiAdditionalPrompt.value

        if (uri == null || sourceExt.isEmpty() || destExt.isEmpty()) {
            _uiState.value = ConversionUiState.Error("Si prega di selezionare un file e un'estensione valida.")
            return
        }

        _uiState.value = ConversionUiState.Converting

        viewModelScope.launch {
            var tempInFile: File? = null
            try {
                // 1. Copy uri stream contents to local temp file to work with files API
                tempInFile = copyUriToTempFile(context, uri, originalName)
                if (tempInFile == null || !tempInFile.exists()) {
                    _uiState.value = ConversionUiState.Error("Errore nel caricare il file contrassegnato in memoria.")
                    return@launch
                }

                // 2. Prep output directory files
                val outDir = getOutputDirectory(context)
                val baseOutputName = originalName.substringBeforeLast(".")
                val outFileName = "$baseOutputName.$destExt"
                val outputFile = File(outDir, outFileName)
                if (outputFile.exists()) {
                    outputFile.delete()
                }

                if (isAiEnabled) {
                    // Perform AI-powered conversion
                    val aiResult = GeminiFileConverter.convertFileWithAI(tempInFile, outputFile, destExt, aiPrompt)
                    aiResult.fold(
                        onSuccess = {
                            saveAndRecordSuccess(originalName, sourceExt, destExt, outputFile)
                        },
                        onFailure = { error ->
                            saveAndRecordError(originalName, sourceExt, destExt, error.localizedMessage ?: "AI Conversion failed.")
                        }
                    )
                } else {
                    // Perform local high-fidelity conversion
                    LocalFileConverters.convertFile(context, tempInFile, outputFile, destExt) { success, err ->
                        viewModelScope.launch {
                            if (success) {
                                saveAndRecordSuccess(originalName, sourceExt, destExt, outputFile)
                            } else {
                                saveAndRecordError(originalName, sourceExt, destExt, err ?: "Conversione locale fallita.")
                            }
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Unexpected crash during conversion action flow", e)
                saveAndRecordError(originalName, sourceExt, destExt, e.localizedMessage ?: "Eccezione inattesa.")
            } finally {
                // Delete temp input file asynchronously to conserve space
                withContext(Dispatchers.IO) {
                    try {
                        tempInFile?.delete()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private suspend fun saveAndRecordSuccess(
        fileName: String,
        sourceExt: String,
        destExt: String,
        outputFile: File
    ) {
        val record = ConversionRecord(
            fileName = fileName,
            sourceExtension = sourceExt,
            destExtension = destExt,
            isSuccess = true,
            outputFilePath = outputFile.absolutePath
        )
        val id = repository.insert(record)
        val insertedRecord = record.copy(id = id.toInt())
        _uiState.value = ConversionUiState.Success(insertedRecord)
    }

    private suspend fun saveAndRecordError(
        fileName: String,
        sourceExt: String,
        destExt: String,
        errorMsg: String
    ) {
        val record = ConversionRecord(
            fileName = fileName,
            sourceExtension = sourceExt,
            destExtension = destExt,
            isSuccess = false,
            errorMessage = errorMsg
        )
        repository.insert(record)
        _uiState.value = ConversionUiState.Error(errorMsg)
    }

    fun deleteRecord(id: Int) {
        viewModelScope.launch {
            // Attempt to delete output file from storage if present
            allRecords.value.find { it.id == id }?.outputFilePath?.let { path ->
                try {
                    val file = File(path)
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            repository.deleteById(id)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            // Delete actual files in external storage folder as well
            for (record in allRecords.value) {
                record.outputFilePath?.let { path ->
                    try {
                        val file = File(path)
                        if (file.exists()) {
                            file.delete()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            repository.clearAll()
        }
    }

    // Helper to open content stream and copy to local temp file in context directory
    private suspend fun copyUriToTempFile(context: Context, uri: Uri, originalName: String): File? = 
        withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
                
                // Sanitize the temp file name to prevent spaces/special characters/parentheses from breaking WebView loadUrl
                val ext = File(originalName).extension.lowercase()
                val base = originalName.substringBeforeLast(".")
                var sanitizedBase = base.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                if (sanitizedBase.trim().replace("_", "").isEmpty()) {
                    sanitizedBase = "file_" + System.currentTimeMillis()
                }
                val safeName = "source_temp_" + sanitizedBase + if (ext.isNotEmpty()) "." + ext else ""
                
                val tempFile = File(context.cacheDir, safeName)
                if (tempFile.exists()) {
                    tempFile.delete()
                }
                tempFile.createNewFile()
                FileOutputStream(tempFile).use { outStream ->
                    inputStream.copyTo(outStream)
                }
                tempFile
            } catch (e: Exception) {
                Log.e(TAG, "Error copying Content resolver URI stream to temp file system", e)
                null
            }
        }

    fun getOutputDirectory(context: Context): File {
        val dir = File(context.getExternalFilesDir(null), "ConvertedFiles")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }
}

class ConversionViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ConversionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ConversionViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
