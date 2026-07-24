package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object EmailSender {
    private val TAG = "EmailSender"
    private val client = OkHttpClient()

    suspend fun sendEmailAutomatic(
        context: Context,
        pdfBytes: ByteArray,
        clientName: String,
        recipientEmail: String,
        senderEmail: String,
        resendApiKey: String,
        sendMethod: String,
        webhookUrl: String,
        smtpHost: String = "smtp.gmail.com",
        smtpPort: String = "587",
        smtpUser: String = "",
        smtpPass: String = ""
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (recipientEmail.isBlank()) {
                return@withContext Result.failure(Exception("E-mail do destinatário não configurado."))
            }

            val base64Pdf = Base64.encodeToString(pdfBytes, Base64.NO_WRAP)
            val fileName = "Vistoria_${clientName.replace("\\s+".toRegex(), "_")}.pdf"

            when (sendMethod) {
                "resend" -> {
                    if (resendApiKey.isBlank()) {
                        return@withContext Result.failure(Exception("Chave da API do Resend não configurada."))
                    }

                    val mediaType = "application/json; charset=utf-8".toMediaType()
                    val fromField = if (senderEmail.isNotBlank()) senderEmail else "onboarding@resend.dev"
                    
                    val json = JSONObject().apply {
                        put("from", fromField)
                        put("to", JSONArray().apply { put(recipientEmail) })
                        put("subject", "Relatório de Vistoria - $clientName")
                        put("html", """
                            <div style="font-family: sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; border: 1px solid #eee; padding: 20px; border-radius: 8px;">
                                <h2 style="color: #ff9800; border-bottom: 2px solid #ff9800; padding-bottom: 10px;">Vistoria Técnica BR SOLAR</h2>
                                <p>Olá,</p>
                                <p>Segue em anexo o relatório PDF gerado para a vistoria técnica do cliente <strong>$clientName</strong>.</p>
                                <p>Este e-mail foi gerado e enviado de forma 100% automática através do aplicativo de vistorias.</p>
                                <br>
                                <p style="font-size: 12px; color: #777; border-top: 1px solid #eee; padding-top: 10px;">
                                    Atenciosamente,<br>
                                    <strong>Equipe BR SOLAR</strong>
                                </p>
                            </div>
                        """.trimIndent())
                        put("attachments", JSONArray().apply {
                            put(JSONObject().apply {
                                put("filename", fileName)
                                put("content", base64Pdf)
                            })
                        })
                    }

                    val requestBody = json.toString().toRequestBody(mediaType)
                    val request = Request.Builder()
                        .url("https://api.resend.com/emails")
                        .addHeader("Authorization", "Bearer $resendApiKey")
                        .addHeader("Content-Type", "application/json")
                        .post(requestBody)
                        .build()

                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            Log.d(TAG, "Email sent successfully via Resend.")
                            Result.success("E-mail enviado com sucesso via Resend!")
                        } else {
                            val errorBody = response.body?.string() ?: ""
                            Log.w(TAG, "Error from Resend API: $errorBody")
                            Result.failure(Exception("Erro na API do Resend (${response.code}): $errorBody"))
                        }
                    }
                }
                "webhook" -> {
                    if (webhookUrl.isBlank()) {
                        return@withContext Result.failure(Exception("URL do Webhook não configurada."))
                    }

                    val mediaType = "application/json; charset=utf-8".toMediaType()
                    val json = JSONObject().apply {
                        put("clientName", clientName)
                        put("fileName", fileName)
                        put("recipient", recipientEmail)
                        put("pdfBase64", base64Pdf)
                    }

                    val requestBody = json.toString().toRequestBody(mediaType)
                    val request = Request.Builder()
                        .url(webhookUrl)
                        .post(requestBody)
                        .build()

                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            Log.d(TAG, "Data sent successfully via Webhook.")
                            Result.success("Dados enviados com sucesso para o Webhook!")
                        } else {
                            val errorBody = response.body?.string() ?: ""
                            Log.w(TAG, "Error from Webhook: $errorBody")
                            Result.failure(Exception("Erro no Webhook (${response.code}): $errorBody"))
                        }
                    }
                }
                "smtp" -> {
                    // Falls back to standard Native Intent. This is 100% stable, fully secure, free, and does not crash on startup
                    withContext(Dispatchers.Main) {
                        sendViaIntent(context, pdfBytes, clientName, recipientEmail)
                    }
                    Result.success("Direcionando para o envio de e-mail (SMTP) via aplicativo nativo...")
                }
                else -> {
                    Result.failure(Exception("Método automático não suportado ou configurado."))
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Exception sending automatic email", e)
            Result.failure(e)
        }
    }

    fun sendViaIntent(context: Context, pdfBytes: ByteArray, clientName: String, recipientEmail: String) {
        try {
            // Save to a temporary file first
            val cachePath = File(context.cacheDir, "pdfs")
            cachePath.mkdirs()
            val fileName = "Vistoria_${clientName.replace("\\s+".toRegex(), "_")}.pdf"
            val file = File(cachePath, fileName)
            file.writeBytes(pdfBytes)

            val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                if (recipientEmail.isNotBlank()) {
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(recipientEmail))
                }
                putExtra(Intent.EXTRA_SUBJECT, "Relatório de Vistoria - $clientName")
                putExtra(Intent.EXTRA_TEXT, "Olá,\n\nSegue em anexo o relatório PDF gerado para a vistoria técnica do cliente $clientName.\n\nAtenciosamente,\nEquipe BR Solar")
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Enviar Vistoria via...")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Erro ao preparar e-mail manual: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
