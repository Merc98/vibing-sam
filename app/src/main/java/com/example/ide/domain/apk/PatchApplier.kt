package com.example.ide.domain.apk

import org.w3c.dom.Document
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

class PatchApplier {

    fun apply(workspace: ApkWorkspace, plan: ApkTransformationPlan): PatchApplyResult {
        val results = plan.operations.map { op ->
            when (op) {
                is PatchOperation.ReplaceText -> applyReplace(workspace, op)
                is PatchOperation.UpdateResourceValue -> applyResourceXml(workspace, op)
                is PatchOperation.UpdateXmlAttribute -> applyXmlAttribute(workspace, op)
                is PatchOperation.AddFile -> applyAddFile(workspace, op)
            }
        }
        return PatchApplyResult(
            success = results.all { it.success },
            results = results
        )
    }

    private fun applyReplace(ws: ApkWorkspace, op: PatchOperation.ReplaceText): PatchOperationResult {
        val file = File(ws.decodedDir, op.path)
        if (!file.exists()) return PatchOperationResult("replace_text", op.path, false, "Target file not found")
        val content = file.readText()
        if (!content.contains(op.before)) return PatchOperationResult("replace_text", op.path, false, "Target text not found")
        file.writeText(content.replace(op.before, op.after))
        return PatchOperationResult("replace_text", op.path, true, "Replacement applied")
    }

    private fun applyResourceXml(ws: ApkWorkspace, op: PatchOperation.UpdateResourceValue): PatchOperationResult {
        val file = File(ws.decodedDir, op.path)
        if (!file.exists()) return PatchOperationResult("update_resource_value", op.path, false, "Resource file not found")
        val doc = parseXml(file) ?: return PatchOperationResult("update_resource_value", op.path, false, "Invalid XML file")
        val tagName = when {
            op.path.endsWith("colors.xml") -> "color"
            op.path.endsWith("strings.xml") -> "string"
            op.path.endsWith("styles.xml") -> "item"
            else -> "string"
        }
        val nodes = doc.getElementsByTagName(tagName)
        var updated = false
        for (i in 0 until nodes.length) {
            val node = nodes.item(i)
            val nameAttr = node.attributes?.getNamedItem("name")?.nodeValue
            if (nameAttr == op.key) {
                node.textContent = op.value
                updated = true
            }
        }
        if (updated) {
            writeXml(doc, file)
            return PatchOperationResult("update_resource_value", op.path, true, "Updated key ${op.key}")
        }
        return PatchOperationResult("update_resource_value", op.path, false, "Key ${op.key} not found")
    }

    private fun applyXmlAttribute(ws: ApkWorkspace, op: PatchOperation.UpdateXmlAttribute): PatchOperationResult {
        val file = File(ws.decodedDir, op.path)
        if (!file.exists()) return PatchOperationResult("update_xml_attribute", op.path, false, "XML file not found")
        val doc = parseXml(file) ?: return PatchOperationResult("update_xml_attribute", op.path, false, "Invalid XML file")
        val nodes = doc.getElementsByTagName(op.selector)
        var updated = false
        for (i in 0 until nodes.length) {
            val node = nodes.item(i)
            val attr = node.attributes?.getNamedItem(op.attribute)
            if (attr != null) {
                attr.nodeValue = op.value
                updated = true
            }
        }
        if (updated) {
            writeXml(doc, file)
            return PatchOperationResult("update_xml_attribute", op.path, true, "Updated ${op.selector}@${op.attribute}")
        }
        return PatchOperationResult("update_xml_attribute", op.path, false, "No matching nodes/attribute")
    }

    private fun applyAddFile(ws: ApkWorkspace, op: PatchOperation.AddFile): PatchOperationResult {
        val file = File(ws.decodedDir, op.path)
        file.parentFile?.mkdirs()
        file.writeText(op.content)
        return PatchOperationResult("add_file", op.path, true, "File created")
    }

    private fun parseXml(file: File): Document? {
        return try {
            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            builder.parse(file)
        } catch (_: Exception) {
            null
        }
    }

    private fun writeXml(doc: Document, file: File) {
        val transformer = TransformerFactory.newInstance().newTransformer().apply {
            setOutputProperty(OutputKeys.INDENT, "yes")
        }
        transformer.transform(DOMSource(doc), StreamResult(file))
    }
}
