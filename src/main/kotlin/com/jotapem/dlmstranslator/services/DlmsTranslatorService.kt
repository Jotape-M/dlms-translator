package com.jotapem.dlmstranslator.services

import com.jotapem.dlmstranslator.MyBundle
import gurux.dlms.GXDLMSTranslator
import gurux.dlms.enums.TranslatorOutputType
import gurux.dlms.internal.GXCommon
import java.io.StringReader
import java.io.StringWriter
import java.util.Base64
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource

object DlmsTranslatorService {

    enum class InputType {
        HEX,
        BASE64
    }

    private val translator = GXDLMSTranslator(TranslatorOutputType.SIMPLE_XML).apply {
        setComments(true)
        setHex(true)
    }

    fun translate(input: String, useHex: Boolean = true, inputType: InputType = InputType.HEX): String {
        return processTranslation(input, useHex, inputType) { translator, bytes ->
            translator.pduToXml(bytes)
        }
    }

    private fun processTranslation(
        input: String,
        useHex: Boolean,
        inputType: InputType,
        translateFunc: (GXDLMSTranslator, ByteArray) -> String
    ): String {
        val cleanInput = input.replace("\\s+".toRegex(), "")

        if (cleanInput.isBlank()) {
            return MyBundle.message("error.noInput")
        }

        return try {
            val bytes = when (inputType) {
                InputType.HEX -> GXCommon.hexToBytes(cleanInput)
                InputType.BASE64 -> try {
                    Base64.getDecoder().decode(cleanInput)
                } catch (_: IllegalArgumentException) {
                    return MyBundle.message("error.invalidBase64")
                }
            }
            translator.setHex(useHex)
            val xmlResult = translateFunc(translator, bytes)

            formatXml(xmlResult)

        } catch (e: Exception) {
            MyBundle.message("error.invalidFrame", e.message ?: "")
        }
    }

    private fun formatXml(xml: String): String {
        return try {
            val singleLineXml = xml.replace(">\\s+<".toRegex(), "><").trim()

            val transformer = TransformerFactory.newInstance().newTransformer()
            transformer.setOutputProperty(OutputKeys.INDENT, "yes")
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4") // 4 espaços
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")

            val result = StreamResult(StringWriter())
            val source = StreamSource(StringReader(singleLineXml))
            transformer.transform(source, result)

            val finalXml = result.writer.toString().replace("(?m)^[ \t]*\r?\n".toRegex(), "")

            finalXml.trim()
        } catch (_: Exception) {
            xml
        }
    }
}