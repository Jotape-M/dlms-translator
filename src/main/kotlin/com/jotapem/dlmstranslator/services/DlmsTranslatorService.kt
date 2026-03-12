package com.jotapem.dlmstranslator.services

import com.jotapem.dlmstranslator.MyBundle
import gurux.dlms.GXDLMSTranslator
import gurux.dlms.enums.TranslatorOutputType
import gurux.dlms.internal.GXCommon
import java.io.StringReader
import java.io.StringWriter
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource

object DlmsTranslatorService {

    private val translator = GXDLMSTranslator(TranslatorOutputType.SIMPLE_XML).apply {
        setComments(true)
        setHex(true)
    }

    fun translate(input: String, useHex: Boolean = true): String {
        return processTranslation(input, useHex) { translator, bytes ->
            translator.pduToXml(bytes)
        }
    }

    fun translateMessage(input: String, useHex: Boolean = true): String {
        return processTranslation(input, useHex) { translator, bytes ->
            translator.messageToXml(bytes)
        }
    }

    private fun processTranslation(input: String, useHex: Boolean, translateFunc: (GXDLMSTranslator, ByteArray) -> String): String {
        val cleanInput = input.replace("\\s+".toRegex(), "")

        if (cleanInput.isBlank()) {
            return MyBundle.message("error.noInput")
        }

        return try {
            val bytes = GXCommon.hexToBytes(cleanInput)
            translator.setHex(useHex)
            val xmlResult = translateFunc(translator, bytes)

            // Aqui passamos o resultado pela nossa nova função de formatação!
            formatXml(xmlResult)

        } catch (e: Exception) {
            MyBundle.message("error.invalidFrame", e.message ?: "")
        }
    }

    /**
     * Função auxiliar que pega num texto XML e aplica identação profissional.
     */
    private fun formatXml(xml: String): String {
        return try {
            // 1. O SEGREDO: Esmagar o XML numa linha só.
            // Substitui qualquer espaço/quebra de linha entre tags por apenas "><"
            val singleLineXml = xml.replace(">\\s+<".toRegex(), "><").trim()

            // 2. Configurar o formatador
            val transformer = TransformerFactory.newInstance().newTransformer()
            transformer.setOutputProperty(OutputKeys.INDENT, "yes")
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4") // 4 espaços
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")

            // 3. Executar a formatação
            val result = StreamResult(StringWriter())
            val source = StreamSource(StringReader(singleLineXml))
            transformer.transform(source, result)

            // 4. Limpeza final: por precaução, removemos linhas completamente vazias
            val finalXml = result.writer.toString().replace("(?m)^[ \t]*\r?\n".toRegex(), "")

            finalXml.trim()
        } catch (_: Exception) {
            xml
        }
    }
}