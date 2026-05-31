package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.File
import java.io.FileOutputStream

object WebViewToPdfConverter {
    private const val TAG = "WebViewToPdfConverter"

    fun convert(
        context: Context,
        inputFile: File,
        outputFile: File,
        onResult: (Boolean, String?) -> Unit
    ) {
        Handler(Looper.getMainLooper()).post {
            try {
                // Pre-enable slow whole document draw in case it wasn't done on startup
                try {
                    WebView.enableSlowWholeDocumentDraw()
                } catch (e: Exception) {
                    Log.d(TAG, "enableSlowWholeDocumentDraw called after webview creation", e)
                }

                val webView = WebView(context)
                
                // Configure WebView settings to process MHT and local HTML
                webView.settings.apply {
                    allowFileAccess = true
                    allowContentAccess = true
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                }

                val a4WidthPx = 794 // Standard A4 width pixel scale for layout

                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        
                        // Wait for webpage layout settles and executes any JS
                        Handler(Looper.getMainLooper()).postDelayed({
                            try {
                                renderWebViewToPdfFile(webView, a4WidthPx, outputFile) { success, error ->
                                    onResult(success, error)
                                    webView.destroy() // cleanup
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error rendering webview directly to PDF", e)
                                onResult(false, e.localizedMessage ?: "Renderer crash")
                                webView.destroy()
                            }
                        }, 1200)
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        errorCode: Int,
                        description: String?,
                        failingUrl: String?
                    ) {
                        super.onReceivedError(view, errorCode, description, failingUrl)
                        Log.e(TAG, "WebView layout error: $description")
                    }
                }

                val fileUrl = "file://" + inputFile.absolutePath
                webView.loadUrl(fileUrl)

            } catch (e: Exception) {
                Log.e(TAG, "Exception during WebView initialized", e)
                onResult(false, e.localizedMessage ?: "WebView setup failed")
            }
        }
    }

    private fun renderWebViewToPdfFile(
        webView: WebView,
        a4WidthPx: Int,
        outputFile: File,
        onComplete: (Boolean, String?) -> Unit
    ) {
        try {
            // Measure webview height for contents
            webView.measure(
                View.MeasureSpec.makeMeasureSpec(a4WidthPx, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            val computedHeight = if (webView.measuredHeight > 0) webView.measuredHeight else 1123
            webView.layout(0, 0, a4WidthPx, computedHeight)

            // Render full webpage layout onto a single high-def bitmap
            val bitmap = Bitmap.createBitmap(a4WidthPx, computedHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            webView.draw(canvas)

            // Prepare native PdfDocument
            val pdfDocument = PdfDocument()

            // Page dimensions in standard PDF points (A4 ratio: 595 pt x 842 pt)
            val pdfPageWidthPt = 595
            val pdfPageHeightPt = 842

            // Ratio is 1123 / 794 (approx 1.41)
            val pxSliceHeight = (a4WidthPx * (pdfPageHeightPt.toFloat() / pdfPageWidthPt.toFloat())).toInt() // 1123 px

            var numPages = (computedHeight / pxSliceHeight)
            if (computedHeight % pxSliceHeight > 0) {
                numPages += 1
            }

            // Loop and slice bitmap into separate A4 Pdf pages
            for (pageIdx in 0 until numPages) {
                val startY = pageIdx * pxSliceHeight
                val endY = Math.min(startY + pxSliceHeight, computedHeight)
                if (startY >= computedHeight) break

                val pageInfo = PdfDocument.PageInfo.Builder(pdfPageWidthPt, pdfPageHeightPt, pageIdx + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val pageCanvas = page.canvas

                val srcRect = Rect(0, startY, a4WidthPx, endY)
                val sliceHeightPx = endY - startY
                val destHeightPt = sliceHeightPx * (pdfPageWidthPt.toFloat() / a4WidthPx.toFloat())
                val destRect = RectF(0f, 0f, pdfPageWidthPt.toFloat(), destHeightPt)

                pageCanvas.drawBitmap(bitmap, srcRect, destRect, null)
                pdfDocument.finishPage(page)
            }

            // Write output file
            if (outputFile.exists()) {
                outputFile.delete()
            }
            outputFile.createNewFile()

            FileOutputStream(outputFile).use { fos ->
                pdfDocument.writeTo(fos)
            }

            pdfDocument.close()
            bitmap.recycle()

            onComplete(true, null)
        } catch (e: Exception) {
            Log.e(TAG, "Error writing canvas document", e)
            onComplete(false, "Slicing renderer exception: ${e.message}")
        }
    }
}
