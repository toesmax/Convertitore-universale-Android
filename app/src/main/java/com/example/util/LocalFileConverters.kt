package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader

object LocalFileConverters {
    private const val TAG = "LocalFileConverters"

    // Supported input types
    val SUPPORTED_INPUTS = listOf(
        "mht", "mhtml", "html", "htm", 
        "txt", "md", "csv", 
        "png", "jpg", "jpeg", "webp", "bmp"
    )

    // Map of input types to their possible outputs
    fun getPossibleOutputs(inputExtension: String): List<String> {
        val ext = inputExtension.lowercase()
        return when (ext) {
            "mht", "mhtml" -> listOf("pdf", "html")
            "html", "htm" -> listOf("pdf")
            "txt", "md" -> listOf("pdf", "html")
            "csv" -> listOf("pdf", "html", "json", "xml")
            "png", "jpg", "jpeg", "webp", "bmp" -> listOf("pdf", "png", "jpg", "webp", "bmp")
            else -> emptyList()
        }
    }

    suspend fun convertFile(
        context: Context,
        inputFile: File,
        outputFile: File,
        targetExtension: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val sourceExt = inputFile.extension.lowercase()
        val destExt = targetExtension.lowercase()

        try {
            when {
                // 1. MHT/HTML to PDF
                (sourceExt == "mht" || sourceExt == "mhtml" || sourceExt == "html" || sourceExt == "htm") && destExt == "pdf" -> {
                    WebViewToPdfConverter.convert(context, inputFile, outputFile, onComplete)
                }

                // 2. MHT/MHTML to HTML (essentially extracts HTML or renders it to HTML)
                // In simpler terms, we can write MHT's body or copy it. But wait, we can load it in WebView and save it,
                // or just do a simple text-based extract, or save direct. Let's do a simple file copy for ease if not complex.
                (sourceExt == "mht" || sourceExt == "mhtml") && destExt == "html" -> {
                    withContext(Dispatchers.IO) {
                        try {
                            // Extract basic HTML body from MHTML (which is MIME-encoded, usually boundary based)
                            val mhtmlContent = inputFile.readText()
                            // Simple heuristic to extract the main HTML part:
                            var htmlPart = ""
                            if (mhtmlContent.contains("Content-Type: text/html")) {
                                val parts = mhtmlContent.split("Content-Type: text/html")
                                if (parts.size > 1) {
                                    val subParts = parts[1].split("------=")
                                    if (subParts.isNotEmpty()) {
                                        htmlPart = subParts[0].trim()
                                        // clean headers
                                        if (htmlPart.contains("\r\n\r\n")) {
                                            val index = htmlPart.indexOf("\r\n\r\n")
                                            htmlPart = htmlPart.substring(index + 4)
                                        } else if (htmlPart.contains("\n\n")) {
                                            val index = htmlPart.indexOf("\n\n")
                                            htmlPart = htmlPart.substring(index + 2)
                                        }
                                    }
                                }
                            }
                            
                            if (htmlPart.isEmpty()) {
                                // fallback: copy entire file as html as WebView might parse it
                                inputFile.copyTo(outputFile, overwrite = true)
                            } else {
                                outputFile.writeText(htmlPart)
                            }
                            onComplete(true, null)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error extracting HTML from MHTML", e)
                            onComplete(false, "MHT to HTML Extraction failed: ${e.message}")
                        }
                    }
                }

                // 3. Text/Markdown to PDF
                (sourceExt == "txt" || sourceExt == "md") && destExt == "pdf" -> {
                    withContext(Dispatchers.IO) {
                        val text = inputFile.readText()
                        val htmlFile = File(context.cacheDir, "temp_conversion.html")
                        val styledHtml = wrapTextInElegantHtml(text, isMarkdown = sourceExt == "md")
                        htmlFile.writeText(styledHtml)
                        
                        // Hand off to WebView PDF conversion
                        WebViewToPdfConverter.convert(context, htmlFile, outputFile) { success, err ->
                            htmlFile.delete() // cleanup cache
                            onComplete(success, err)
                        }
                    }
                }

                // 4. Text/Markdown to HTML
                (sourceExt == "txt" || sourceExt == "md") && destExt == "html" -> {
                    withContext(Dispatchers.IO) {
                        try {
                            val text = inputFile.readText()
                            val styledHtml = wrapTextInElegantHtml(text, isMarkdown = sourceExt == "md")
                            outputFile.writeText(styledHtml)
                            onComplete(true, null)
                        } catch (e: Exception) {
                            onComplete(false, e.message)
                        }
                    }
                }

                // 5. CSV to HTML
                sourceExt == "csv" && destExt == "html" -> {
                    withContext(Dispatchers.IO) {
                        try {
                            val html = convertCsvToHtmlString(inputFile)
                            outputFile.writeText(html)
                            onComplete(true, null)
                        } catch (e: Exception) {
                            onComplete(false, "CSV conversion failed: ${e.message}")
                        }
                    }
                }

                // 6. CSV to PDF
                sourceExt == "csv" && destExt == "pdf" -> {
                    withContext(Dispatchers.IO) {
                        try {
                            val htmlContext = convertCsvToHtmlString(inputFile)
                            val htmlFile = File(context.cacheDir, "temp_csv_conversion.html")
                            htmlFile.writeText(htmlContext)

                            WebViewToPdfConverter.convert(context, htmlFile, outputFile) { success, err ->
                                htmlFile.delete() // cleanup cache
                                onComplete(success, err)
                            }
                        } catch (e: Exception) {
                            onComplete(false, "CSV to PDF process failed: ${e.message}")
                        }
                    }
                }

                // 7. CSV to JSON
                sourceExt == "csv" && destExt == "json" -> {
                    withContext(Dispatchers.IO) {
                        try {
                            val jsonString = convertCsvToJsonString(inputFile)
                            outputFile.writeText(jsonString)
                            onComplete(true, null)
                        } catch (e: Exception) {
                            onComplete(false, "CSV to JSON failed: ${e.message}")
                        }
                    }
                }

                // 8. CSV to XML
                sourceExt == "csv" && destExt == "xml" -> {
                    withContext(Dispatchers.IO) {
                        try {
                            val xmlString = convertCsvToXmlString(inputFile)
                            outputFile.writeText(xmlString)
                            onComplete(true, null)
                        } catch (e: Exception) {
                            onComplete(false, "CSV to XML failed: ${e.message}")
                        }
                    }
                }

                // 9. Image (PNG/JPG/WEBP/BMP) to Image (any other format)
                isImage(sourceExt) && isImage(destExt) -> {
                    withContext(Dispatchers.IO) {
                        try {
                            val bitmap = BitmapFactory.decodeFile(inputFile.absolutePath)
                            if (bitmap == null) {
                                onComplete(false, "Impossibile decodificare l'immagine di input.")
                                return@withContext
                            }

                            val format = when (destExt) {
                                "png" -> Bitmap.CompressFormat.PNG
                                "jpg", "jpeg" -> Bitmap.CompressFormat.JPEG
                                "webp" -> {
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                                        Bitmap.CompressFormat.WEBP_LOSSLESS
                                    } else {
                                        @Suppress("DEPRECATION")
                                        Bitmap.CompressFormat.WEBP
                                    }
                                }
                                else -> Bitmap.CompressFormat.PNG
                            }

                            FileOutputStream(outputFile).use { out ->
                                bitmap.compress(format, 95, out)
                            }
                            bitmap.recycle()
                            onComplete(true, null)
                        } catch (e: Exception) {
                            onComplete(false, "Compressione immagine fallita: ${e.message}")
                        }
                    }
                }

                // 10. Image to PDF
                isImage(sourceExt) && destExt == "pdf" -> {
                    withContext(Dispatchers.IO) {
                        try {
                            val bitmap = BitmapFactory.decodeFile(inputFile.absolutePath)
                            if (bitmap == null) {
                                onComplete(false, "Impossibile decodificare l'immagine.")
                                return@withContext
                            }

                            val pdfDocument = PdfDocument()
                            
                            // Scale design to fit within Standard A4 (595 x 842 pt)
                            val a4Width = 595
                            val a4Height = 842
                            
                            val pageInfo = PdfDocument.PageInfo.Builder(a4Width, a4Height, 1).create()
                            val page = pdfDocument.startPage(pageInfo)
                            val canvas = page.canvas

                            // Calculate scale factor to match design aspect ratio
                            val scaleX = a4Width.toFloat() / bitmap.width
                            val scaleY = a4Height.toFloat() / bitmap.height
                            val scale = Math.min(scaleX, scaleY) * 0.9f // scale slightly smaller to give 10% margins

                            val destWidth = (bitmap.width * scale).toInt()
                            val destHeight = (bitmap.height * scale).toInt()
                            
                            val startX = (a4Width - destWidth) / 2f
                            val startY = (a4Height - destHeight) / 2f

                            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, destWidth, destHeight, true)
                            canvas.drawBitmap(scaledBitmap, startX, startY, null)
                            
                            pdfDocument.finishPage(page)

                            FileOutputStream(outputFile).use { out ->
                                pdfDocument.writeTo(out)
                            }
                            
                            pdfDocument.close()
                            bitmap.recycle()
                            scaledBitmap.recycle()
                            
                            onComplete(true, null)
                        } catch (e: Exception) {
                            onComplete(false, "Errore salvataggio PDF da immagine: ${e.message}")
                        }
                    }
                }

                else -> {
                    onComplete(false, "Conversione da .$sourceExt a .$destExt non supportata localmente.")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "General runtime conversion exception", e)
            onComplete(false, "Inaspettato errore di runtime: ${e.message}")
        }
    }

    private fun isImage(ext: String): Boolean {
        return listOf("png", "jpg", "jpeg", "webp", "bmp").contains(ext)
    }

    // Wrap plain text/markdown inside stylized, elegant, and readable HTML layout
    private fun wrapTextInElegantHtml(text: String, isMarkdown: Boolean): String {
        // Basic Markdown parser helper (transforms headers, list items, bold etc.)
        var formattedText = text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")

        if (isMarkdown) {
            val lines = formattedText.split("\n").map { line ->
                var trimmed = line.trim()
                when {
                    trimmed.startsWith("### ") -> "<h3>${trimmed.substring(4)}</h3>"
                    trimmed.startsWith("## ") -> "<h2>${trimmed.substring(3)}</h2>"
                    trimmed.startsWith("# ") -> "<h1>${trimmed.substring(2)}</h1>"
                    trimmed.startsWith("- ") || trimmed.startsWith("* ") -> "<li>${trimmed.substring(2)}</li>"
                    trimmed.isEmpty() -> "<br/>"
                    else -> "<p>$line</p>"
                }
            }
            formattedText = lines.joinToString("\n")
                .replace(Regex("\\*\\*(.*?)\\*\\*"), "<strong>$1</strong>") // bold
                .replace(Regex("\\*(.*?)\\*"), "<em>$1</em>") // italics
                .replace(Regex("`(.*?)`"), "<code style='background:#f4f4f4; padding:2px 4px; border-radius:3px;'>$1</code>") // inline code
        } else {
            // simple text paragraphs or code
            formattedText = "<pre style='font-family: monospace; white-space: pre-wrap; font-size: 13px;'>$formattedText</pre>"
        }

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                        color: #1e293b;
                        background-color: #ffffff;
                        line-height: 1.6;
                        padding: 40px 50px;
                        margin: 0;
                    }
                    h1 {
                        font-size: 26px;
                        color: #0f172a;
                        margin-bottom: 20px;
                        border-bottom: 2px solid #e2e8f0;
                        padding-bottom: 8px;
                    }
                    h2 {
                        font-size: 20px;
                        color: #1e293b;
                        margin-top: 30px;
                        margin-bottom: 15px;
                    }
                    h3 {
                        font-size: 16px;
                        color: #334155;
                        margin-top: 25px;
                    }
                    p {
                        font-size: 14px;
                        margin-bottom: 16px;
                    }
                    li {
                        font-size: 14px;
                        margin-bottom: 8px;
                    }
                    pre {
                        background-color: #f8fafc;
                        border: 1px solid #e2e8f0;
                        border-radius: 8px;
                        padding: 16px;
                        overflow-x: auto;
                        line-height: 1.5;
                    }
                </style>
            </head>
            <body>
                $formattedText
            </body>
            </html>
        """.trimIndent()
    }

    // CSV parsing helper routines
    private fun parseCsvRows(csvFile: File): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        BufferedReader(FileReader(csvFile)).use { reader ->
            var line: String? = reader.readLine()
            while (line != null) {
                // Split row, supporting basic escaping or simple comma separation
                val row = parseCsvLine(line)
                rows.add(row)
                line = reader.readLine()
            }
        }
        return rows
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '\"') {
                inQuotes = !inQuotes
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString().trim())
                current.setLength(0)
            } else {
                current.append(c)
            }
            i++
        }
        result.add(current.toString().trim())
        return result
    }

    private fun convertCsvToHtmlString(csvFile: File): String {
        val rows = parseCsvRows(csvFile)
        val sb = StringBuilder()
        sb.append("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <style>
                    body {
                        font-family: sans-serif;
                        color: #334155;
                        padding: 30px;
                        background: #ffffff;
                    }
                    h2 {
                        color: #0f172a;
                        margin-bottom: 20px;
                    }
                    table {
                        width: 100%;
                        border-collapse: collapse;
                        margin-top: 10px;
                        font-size: 13px;
                    }
                    th {
                        background-color: #f1f5f9;
                        color: #0f172a;
                        text-align: left;
                        padding: 12px;
                        border-bottom: 2px solid #cbd5e1;
                        font-weight: 600;
                    }
                    td {
                        padding: 10px 12px;
                        border-bottom: 1px solid #e2e8f0;
                    }
                    tr:nth-child(even) {
                        background-color: #fafbfc;
                    }
                </style>
            </head>
            <body>
                <h2>Foglio Dati Convertito (${csvFile.name})</h2>
                <table>
        """.trimIndent())

        if (rows.isNotEmpty()) {
            // Header row
            sb.append("<thead><tr>")
            for (col in rows[0]) {
                sb.append("<th>").append(escapeHtml(col)).append("</th>")
            }
            sb.append("</tr></thead><tbody>")

            // Data rows
            for (i in 1 until rows.size) {
                sb.append("<tr>")
                val row = rows[i]
                for (j in 0 until rows[0].size) {
                    val cellValue = if (j < row.size) row[j] else ""
                    sb.append("td>").append(escapeHtml(cellValue)).append("</td>")
                }
                sb.append("</tr>")
            }
            sb.append("</tbody>")
        } else {
            sb.append("<tr><td>Nessun dato CSV rilevato</td></tr>")
        }

        sb.append("</table></body></html>")
        return sb.toString().replace("td>", "<td>") // resolve template replacement safely
    }

    private fun convertCsvToJsonString(csvFile: File): String {
        val rows = parseCsvRows(csvFile)
        val jsonArray = JSONArray()
        if (rows.size > 1) {
            val headers = rows[0]
            for (i in 1 until rows.size) {
                val row = rows[i]
                val jsonObject = JSONObject()
                for (j in headers.indices) {
                    val key = headers[j]
                    val value = if (j < row.size) row[j] else ""
                    jsonObject.put(key, value)
                }
                jsonArray.put(jsonObject)
            }
        }
        return jsonArray.toString(4)
    }

    private fun convertCsvToXmlString(csvFile: File): String {
        val rows = parseCsvRows(csvFile)
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<dataset>\n")
        if (rows.size > 1) {
            val headers = rows[0].map { cleanXmlTag(it) }
            for (i in 1 until rows.size) {
                val row = rows[i]
                sb.append("  <record>\n")
                for (j in headers.indices) {
                    val tag = headers[j]
                    val value = if (j < row.size) row[j] else ""
                    sb.append("    <").append(tag).append(">")
                      .append(escapeXml(value))
                      .append("</").append(tag).append(">\n")
                }
                sb.append("  </record>\n")
            }
        }
        sb.append("</dataset>")
        return sb.toString()
    }

    private fun escapeHtml(str: String): String {
        return str.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
    }

    private fun cleanXmlTag(str: String): String {
        val clean = str.replace(Regex("[^a-zA-Z0-9_]"), "_")
        return if (clean.isEmpty() || !clean[0].isLetter()) "item_$clean" else clean
    }

    private fun escapeXml(str: String): String {
        return str.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
