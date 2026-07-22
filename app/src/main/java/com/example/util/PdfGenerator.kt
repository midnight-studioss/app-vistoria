package com.example.util
import androidx.core.net.toUri

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.example.data.Inspection
import java.io.ByteArrayOutputStream
import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

object PdfGenerator {
    
    fun generatePdfBytes(context: Context, inspection: Inspection, companyName: String = "BR SOLAR"): ByteArray {
        val pdfDocument = PdfDocument()
        
        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            isAntiAlias = true
        }
        val labelPaint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val headerTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 15f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val sectionPaint = Paint().apply {
            color = Color.rgb(13, 71, 161) // primary blue
            textSize = 14f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }
        
        // PAGE 1: HEADER & CLIENT DATA & TECHNICAL SPECS
        var pageNum = 1
        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNum++).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        
        // Draw Header
        canvas.drawRect(RectF(30f, 30f, 565f, 80f), Paint().apply { color = Color.rgb(255, 145, 0) }) // solar orange
        canvas.drawText("${companyName.uppercase()} - RELATÓRIO DE VISTORIA TÉCNICA", 45f, 61f, headerTextPaint)
        
        var y = 110f
        
        // Client Data
        canvas.drawText("1. Dados do Cliente", 30f, y, sectionPaint)
        y += 5f
        canvas.drawLine(30f, y, 565f, y, linePaint)
        y += 20f
        
        val fullName = "${inspection.clientFirstName} ${inspection.clientLastName}".trim()
        canvas.drawText("Nome Completo: $fullName", 40f, y, textPaint)
        y += 20f
        canvas.drawText("Identificador (ID): ${inspection.clientIdString}", 40f, y, textPaint)
        y += 20f
        canvas.drawText("Endereço: ${inspection.address}", 40f, y, textPaint)
        y += 30f
        
        // Location Data
        canvas.drawText("2. Dados de Localização", 30f, y, sectionPaint)
        y += 5f
        canvas.drawLine(30f, y, 565f, y, linePaint)
        y += 20f
        canvas.drawText("Latitude: ${inspection.latitude ?: "Não informada"}", 40f, y, textPaint)
        y += 20f
        canvas.drawText("Longitude: ${inspection.longitude ?: "Não informada"}", 40f, y, textPaint)
        y += 20f
        canvas.drawText("Precisão do GPS: ${inspection.gpsAccuracy?.let { "${it}m" } ?: "Não informada"}", 40f, y, textPaint)
        y += 30f
        
        // Technical Specs
        canvas.drawText("3. Especificações Técnicas", 30f, y, sectionPaint)
        y += 5f
        canvas.drawLine(30f, y, 565f, y, linePaint)
        y += 20f
        canvas.drawText("Tipo de Conexão: ${inspection.connectionType}", 40f, y, textPaint)
        y += 20f
        canvas.drawText("Disjuntor Geral: ${inspection.mainBreaker}", 40f, y, textPaint)
        y += 20f
        canvas.drawText("Tensão: ${inspection.voltage}", 40f, y, textPaint)
        y += 30f
        
        // Checklist
        canvas.drawText("4. Checklist de Instalação", 30f, y, sectionPaint)
        y += 5f
        canvas.drawLine(30f, y, 565f, y, linePaint)
        y += 20f
        
        fun formatCheck(v: Boolean?): String = when (v) {
            true -> "Sim"
            false -> "Não"
            else -> "Não respondido"
        }
        
        canvas.drawText("Possui aterramento? ${formatCheck(inspection.hasGrounding)}", 40f, y, textPaint)
        y += 18f
        canvas.drawText("Necessita de andaime? ${formatCheck(inspection.needsScaffold)}", 40f, y, textPaint)
        y += 18f
        canvas.drawText("Necessita de poda? ${formatCheck(inspection.needsPruning)}", 40f, y, textPaint)
        y += 18f
        canvas.drawText("Necessita de obras civis? ${formatCheck(inspection.needsConstruction)}", 40f, y, textPaint)
        y += 18f
        canvas.drawText("Possui área de sombreamento? ${formatCheck(inspection.hasShadowArea)}", 40f, y, textPaint)
        y += 18f
        canvas.drawText("Possui WiFi disponível? ${formatCheck(inspection.hasWifi)}", 40f, y, textPaint)
        y += 18f
        canvas.drawText("Possui telhas sobressalentes? ${formatCheck(inspection.hasSpareTiles)}", 40f, y, textPaint)
        y += 18f
        canvas.drawText("Possui infiltrações? ${formatCheck(inspection.hasInfiltration)}", 40f, y, textPaint)
        
        pdfDocument.finishPage(page)
        
        // PAGE 2: ROOF & OBSERVATIONS & SIGNATURES
        pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNum++).create()
        page = pdfDocument.startPage(pageInfo)
        canvas = page.canvas
        y = 40f
        
        // Roof Information
        canvas.drawText("5. Informações do Telhado", 30f, y, sectionPaint)
        y += 5f
        canvas.drawLine(30f, y, 565f, y, linePaint)
        y += 20f
        canvas.drawText("Tipo de Telhado: ${inspection.roofType}", 40f, y, textPaint)
        y += 20f
        canvas.drawText("Inclinação do Telhado: ${inspection.roofInclination}", 40f, y, textPaint)
        y += 20f
        canvas.drawText("Arranjo dos Módulos: ${inspection.arrayArrangement}", 40f, y, textPaint)
        y += 20f
        canvas.drawText("Local de Instalação do Inversor: ${inspection.inverterLocation}", 40f, y, textPaint)
        y += 30f
        
        // Observations
        canvas.drawText("6. Observações Gerais", 30f, y, sectionPaint)
        y += 5f
        canvas.drawLine(30f, y, 565f, y, linePaint)
        y += 20f
        
        val obsLines = inspection.observations.chunked(70)
        if (obsLines.isEmpty()) {
            canvas.drawText("Nenhuma observação informada.", 40f, y, textPaint)
            y += 20f
        } else {
            for (line in obsLines) {
                canvas.drawText(line, 40f, y, textPaint)
                y += 18f
            }
        }
        y += 30f
        
        // Signatures Section
        canvas.drawText("7. Assinaturas", 30f, y, sectionPaint)
        y += 5f
        canvas.drawLine(30f, y, 565f, y, linePaint)
        y += 30f
        
        // Tech Signature box
        canvas.drawText("Vistoriador: ${inspection.techName}", 40f, y, textPaint)
        val techSig = loadUriBitmap(context, inspection.techSignatureUri)
        if (techSig != null) {
            drawScaledBitmap(canvas, techSig, 40f, y + 10f, 200f, 80f)
        } else {
            val italicPaint = Paint().apply {
                color = Color.GRAY
                textSize = 11f
                isAntiAlias = true
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            }
            canvas.drawText("[Assinatura pendente]", 40f, y + 30f, italicPaint)
        }
        
        // Client Signature box
        canvas.drawText("Responsável: ${inspection.clientRepName}", 320f, y, textPaint)
        val clientSig = loadUriBitmap(context, inspection.clientSignatureUri)
        if (clientSig != null) {
            drawScaledBitmap(canvas, clientSig, 320f, y + 10f, 200f, 80f)
        } else {
            val italicPaint = Paint().apply {
                color = Color.GRAY
                textSize = 11f
                isAntiAlias = true
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            }
            canvas.drawText("[Assinatura pendente]", 320f, y + 30f, italicPaint)
        }
        
        pdfDocument.finishPage(page)
        
        // PAGE 3: PHOTOS
        val photos = listOf(
            "Foto do Padrão" to inspection.photoMeterUri,
            "Foto do Disjuntor" to inspection.photoBreakerUri,
            "Foto do Quadro" to inspection.photoPanelUri,
            "Foto do Telhado" to inspection.photoRoofUri,
            "Foto Geral" to inspection.photoGeneralUri
        ).filter { it.second != null }
        
        if (photos.isNotEmpty()) {
            pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNum++).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            y = 40f
            
            canvas.drawText("8. Anexos Fotográficos", 30f, y, sectionPaint)
            y += 5f
            canvas.drawLine(30f, y, 565f, y, linePaint)
            y += 30f
            
            // Draw photos in a 2x3 grid
            var col = 0
            var rowY = y
            val colWidth = 240f
            val colHeight = 180f
            
            for ((label, uriStr) in photos) {
                val bmp = loadUriBitmap(context, uriStr)
                if (bmp != null) {
                    val startX = if (col == 0) 40f else 310f
                    canvas.drawText(label, startX, rowY - 10f, labelPaint)
                    drawScaledBitmap(canvas, bmp, startX, rowY, colWidth, colHeight)
                    
                    col++
                    if (col > 1) {
                        col = 0
                        rowY += colHeight + 40f
                    }
                    
                    if (rowY + colHeight > 800f && label != photos.last().first) {
                        pdfDocument.finishPage(page)
                        pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNum++).create()
                        page = pdfDocument.startPage(pageInfo)
                        canvas = page.canvas
                        rowY = 60f
                        col = 0
                    }
                }
            }
            pdfDocument.finishPage(page)
        }
        
        val outputStream = ByteArrayOutputStream()
        pdfDocument.writeTo(outputStream)
        pdfDocument.close()
        return outputStream.toByteArray()
    }
    
    private fun loadUriBitmap(context: Context, uriString: String?): Bitmap? {
        if (uriString.isNullOrBlank()) return null
        return try {
            val uri = Uri.parse(uriString)
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun drawScaledBitmap(canvas: Canvas, bitmap: Bitmap, left: Float, top: Float, maxWidth: Float, maxHeight: Float) {
        val srcWidth = bitmap.width.toFloat()
        val srcHeight = bitmap.height.toFloat()
        val scale = Math.min(maxWidth / srcWidth, maxHeight / srcHeight)
        val destWidth = srcWidth * scale
        val destHeight = srcHeight * scale
        
        val destRect = RectF(left, top, left + destWidth, top + destHeight)
        canvas.drawBitmap(bitmap, null, destRect, null)
    }

    fun savePdfDirectly(context: Context, inspection: Inspection, companyName: String = "BR SOLAR") {
        try {
            val bytes = generatePdfBytes(context, inspection, companyName)
            val namePart = "${inspection.clientFirstName}_${inspection.clientLastName}".trim()
            val baseName = if (namePart.isNotBlank()) namePart else "Cliente_Sem_Nome"
            val clientNameClean = baseName.replace(Regex("[^A-Za-z0-9 _-]"), "")
            val fileName = "Vistoria_${clientNameClean.replace("\\s+".toRegex(), "_")}.pdf"
            
            var outputStream: OutputStream? = null
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    outputStream = resolver.openOutputStream(uri)
                }
            } else {
                val targetDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!targetDir.exists()) {
                    targetDir.mkdirs()
                }
                val file = File(targetDir, fileName)
                outputStream = FileOutputStream(file)
            }
            
            outputStream?.use {
                it.write(bytes)
            }
            
            android.widget.Toast.makeText(context, "PDF salvo nos Downloads.", android.widget.Toast.LENGTH_SHORT).show()

            // Automatic / Fallback Email sending
            val userPrefs = com.example.data.UserPreferences(context)
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val recipient = userPrefs.emailRecipient.first() ?: ""
                    val method = userPrefs.sendMethod.first() ?: "manual"
                    val sender = userPrefs.emailSender.first() ?: ""
                    val resendKey = userPrefs.resendApiKey.first() ?: ""
                    val webhookUrl = userPrefs.webhookUrl.first() ?: ""
                    val smtpHost = userPrefs.smtpHost.first() ?: "smtp.gmail.com"
                    val smtpPort = userPrefs.smtpPort.first() ?: "587"
                    val smtpUser = userPrefs.smtpUsername.first() ?: ""
                    val smtpPass = userPrefs.smtpPassword.first() ?: ""

                    val clientFullName = "${inspection.clientFirstName} ${inspection.clientLastName}".trim()
                    val clientName = if (clientFullName.isNotBlank()) clientFullName else "Cliente Sem Nome"

                    if (method == "manual") {
                        // Launch standard email chooser with attachment
                        com.example.util.EmailSender.sendViaIntent(context, bytes, clientName, recipient)
                    } else {
                        if (recipient.isBlank()) {
                            android.widget.Toast.makeText(context, "Aviso: E-mail de destino não configurado nas opções de e-mail.", android.widget.Toast.LENGTH_LONG).show()
                            // If auto configured but empty recipient, fallback to standard share intent so they don't lose the flow
                            com.example.util.EmailSender.sendViaIntent(context, bytes, clientName, "")
                            return@launch
                        }

                        android.widget.Toast.makeText(context, "Enviando PDF automaticamente para: $recipient...", android.widget.Toast.LENGTH_SHORT).show()
                        val result = com.example.util.EmailSender.sendEmailAutomatic(
                            context = context,
                            pdfBytes = bytes,
                            clientName = clientName,
                            recipientEmail = recipient,
                            senderEmail = sender,
                            resendApiKey = resendKey,
                            sendMethod = method,
                            webhookUrl = webhookUrl,
                            smtpHost = smtpHost,
                            smtpPort = smtpPort,
                            smtpUser = smtpUser,
                            smtpPass = smtpPass
                        )
                        
                        if (result.isSuccess) {
                            android.widget.Toast.makeText(context, result.getOrNull() ?: "Enviado com sucesso!", android.widget.Toast.LENGTH_LONG).show()
                        } else {
                            val errMsg = result.exceptionOrNull()?.message ?: "Erro desconhecido"
                            android.widget.Toast.makeText(context, "Erro no envio automático: $errMsg", android.widget.Toast.LENGTH_LONG).show()
                            // Fallback to manual chooser on failure
                            android.widget.Toast.makeText(context, "Iniciando compartilhamento manual...", android.widget.Toast.LENGTH_SHORT).show()
                            com.example.util.EmailSender.sendViaIntent(context, bytes, clientName, recipient)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    android.widget.Toast.makeText(context, "Erro nas configurações de e-mail: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(context, "Erro ao salvar PDF: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
    }
}
