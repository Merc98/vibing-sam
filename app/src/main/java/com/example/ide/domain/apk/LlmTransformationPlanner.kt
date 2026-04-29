package com.example.ide.domain.apk

import com.example.ide.data.model.ChatMessage
import com.example.ide.data.repository.AIRepository
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser

class LlmTransformationPlanner(
    private val aiRepository: AIRepository,
    private val gson: Gson = Gson()
) {

    suspend fun plan(request: ApkTransformationRequest, context: ApkContext): ApkTransformationPlan {
        val response = aiRepository.sendMessage(
            request.model,
            request.apiKey,
            listOf(ChatMessage("user", buildPrompt(request, context)))
        )

        val raw = response.getOrElse {
            return blocked("The selected model failed: ${it.message}")
        }

        return parse(raw) ?: blocked("The model did not return a valid transformation plan.")
    }

    private fun buildPrompt(request: ApkTransformationRequest, context: ApkContext): String {
        return """
You are the APK transformation planner for VibeCode Mobile.
Return ONLY strict JSON. Do not include markdown.

Allowed operations:
- replace_text: {"type":"replace_text","path":"res/values/colors.xml","before":"#FFFFFF","after":"#0000FF"}
- update_resource_value: {"type":"update_resource_value","path":"res/values/colors.xml","key":"colorPrimary","value":"#0000FF"}
- update_xml_attribute: {"type":"update_xml_attribute","path":"res/layout/chat.xml","selector":"TextView","attribute":"android:textColor","value":"#0000FF"}
- add_file: {"type":"add_file","path":"res/values/vibecode_patch.xml","content":"<resources/>"}

Safety rules:
- Refuse malware, credential theft, spyware, evasion, payment bypass, account abuse, persistence, stealth, or data exfiltration.
- UI/resource transformations, labels, strings, layouts, themes, colors, accessibility and benign test changes are allowed.
- Prefer resource/XML changes over smali.

Required JSON schema:
{
  "summary": "short user-readable summary",
  "riskLevel": "LOW|MEDIUM|HIGH|BLOCKED",
  "requiresSmali": false,
  "targetFiles": ["path"],
  "operations": [ ... ]
}

User goal:
${request.userGoal}

AndroidManifest snippet:
${context.manifestSnippet.take(2500)}

Resource/code tree candidates:
${context.resourceTree.take(120).joinToString("\n")}

Candidate file snippets:
${context.candidateFiles.joinToString("\n\n") { "FILE: ${it.path}\n${it.contentSnippet.take(1200)}" }}
        """.trimIndent()
    }

    fun parse(raw: String): ApkTransformationPlan? {
        val json = extractJson(raw) ?: return null
        val obj = JsonParser.parseString(json).asJsonObject
        val risk = runCatching { RiskLevel.valueOf(obj.get("riskLevel").asString.uppercase()) }.getOrDefault(RiskLevel.MEDIUM)
        val operations = obj.getAsJsonArray("operations")?.mapNotNull { parseOperation(it.asJsonObject) }.orEmpty()
        return ApkTransformationPlan(
            summary = obj.get("summary")?.asString ?: "APK transformation plan",
            targetFiles = obj.getAsJsonArray("targetFiles")?.map { it.asString }.orEmpty(),
            operations = operations,
            riskLevel = risk,
            requiresSmali = obj.get("requiresSmali")?.asBoolean ?: false
        )
    }

    private fun parseOperation(obj: JsonObject): PatchOperation? {
        val type = obj.get("type")?.asString ?: return null
        val path = obj.get("path")?.asString ?: return null
        return when (type) {
            "replace_text" -> PatchOperation.ReplaceText(
                path = path,
                before = obj.get("before")?.asString ?: return null,
                after = obj.get("after")?.asString ?: return null
            )
            "update_resource_value" -> PatchOperation.UpdateResourceValue(
                path = path,
                key = obj.get("key")?.asString ?: return null,
                value = obj.get("value")?.asString ?: return null
            )
            "update_xml_attribute" -> PatchOperation.UpdateXmlAttribute(
                path = path,
                selector = obj.get("selector")?.asString ?: "",
                attribute = obj.get("attribute")?.asString ?: return null,
                value = obj.get("value")?.asString ?: return null
            )
            "add_file" -> PatchOperation.AddFile(
                path = path,
                content = obj.get("content")?.asString ?: ""
            )
            else -> null
        }
    }

    private fun extractJson(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) return trimmed
        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        return if (start >= 0 && end > start) trimmed.substring(start, end + 1) else null
    }

    private fun blocked(reason: String) = ApkTransformationPlan(
        summary = reason,
        targetFiles = emptyList(),
        operations = emptyList(),
        riskLevel = RiskLevel.BLOCKED,
        requiresSmali = false
    )
}
