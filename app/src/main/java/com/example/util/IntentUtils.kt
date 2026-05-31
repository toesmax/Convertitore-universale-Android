package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

object IntentUtils {
    
    fun shareFile(context: Context, filePath: String) {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                Toast.makeText(context, "Il file non esiste più.", Toast.LENGTH_SHORT).show()
                return
            }
            
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            val mimeType = context.contentResolver.getType(uri) ?: "*/*"
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            context.startActivity(Intent.createChooser(intent, "Invia file tramite..."))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Impossibile condividere il file: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    fun openFile(context: Context, filePath: String) {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                Toast.makeText(context, "Il file non esiste più.", Toast.LENGTH_SHORT).show()
                return
            }
            
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            val mimeType = context.contentResolver.getType(uri) ?: "*/*"
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            // If opening fail (e.g. no app can view this mime type), suggest sharing
            Toast.makeText(context, "File salvato. Nessuna applicazione installata per aprire questo formato. Condividilo per esportarlo.", Toast.LENGTH_LONG).show()
            shareFile(context, filePath)
        }
    }
}
