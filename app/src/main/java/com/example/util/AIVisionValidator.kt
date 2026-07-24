package com.example.util

import android.graphics.Bitmap
import android.util.Log
import com.example.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AIVisionValidator {
    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY
        )
    }

    suspend fun verifyImageLegibility(bitmap: Bitmap): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            if (BuildConfig.GEMINI_API_KEY == "MY_GEMINI_API_KEY" || BuildConfig.GEMINI_API_KEY.isBlank()) {
                return@withContext Pair(true, "API Key do Gemini não configurada. Verificação ignorada.")
            }

            val prompt = "Verifique se esta foto está legível, focada e não está excessivamente embaçada. Responda apenas com 'OK' se estiver boa, ou 'EMBAÇADA' (seguida de uma breve explicação do porquê) se estiver ruim para uma inspeção ou registro."
            val response = generativeModel.generateContent(
                content {
                    image(bitmap)
                    text(prompt)
                }
            )

            val text = response.text ?: ""
            if (text.trim().uppercase().startsWith("OK")) {
                Pair(true, "A foto parece estar legível.")
            } else {
                Pair(false, text)
            }
        } catch (e: Throwable) {
            Log.e("AIVisionValidator", "Erro ao validar imagem: ${e.message}")
            Pair(true, "Erro na verificação AI, continuando mesmo assim.")
        }
    }
}
