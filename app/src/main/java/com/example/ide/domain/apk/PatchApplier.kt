package com.example.ide.domain.apk

import org.w3c.dom.Document
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

class PatchApplier {

    fun apply(workspace: ApkWorkspace, plan: ApkTransformationPlan) {
        plan.operations.forEach { op ->
            when (op) {
                is PatchOperation.ReplaceText -> applyReplace(workspace, op)
                is PatchOperation.UpdateResourceValue -> applyResourceXml(workspace, op)
                is PatchOperation.UpdateXmlAttribute -> applyXmlAttribute(workspace, op)
                is PatchOperation.AddFile -> applyAddFile(workspace, op)
            }
        }
    }

    private fun applyReplace(ws: ApkWorkspace, op: PatchOperation.ReplaceText) {
        val file = File(ws.decodedDir, op.path)
        if (!file.exists()) return
        val content = file.readText()
        if (!content.contains(op.before)) return
        file.writeText(content.replace(op.before, op.after))
    }

    private fun applyResourceXml(ws: ApkWorkspace, op: PatchOperation.UpdateResourceValue) {
        val file = File(ws.decodedDir, op.path)
        if (!file.exists()) return
        val doc = parseXml(file) ?: return
        val nodes = doc.getElementsByTagName("color")
        var updated = false
        for (i in 0 until nodes.length) {
            val node = nodes.item(i)
            val nameAttr = node.attributes?.getNamedItem("name")?.nodeValue
            if (nameAttr == op.key) {
                node.textContent = op.value
                updated = true
            }
        }
        if (updated) writeXml(doc, file)
    }

    private fun applyXmlAttribute(ws: ApkWorkspace, op: PatchOperation.UpdateXmlAttribute) {
        val file = File(ws.decodedDir, op.path)
        if (!file.exists()) return
        val doc = parseXml(file) ?: return
        val nodes = doc.getElementsByTagName(op.selector)
        for (i in 0 until nodes.length) {
            val node = nodes.item(i)
            val attr = node.attributes?.getNamedItem(op.attribute)
            if (attr != null) {
                attr.nodeValue = op.value
            }
        }
        writeXml(doc, file)
    }

    private fun applyAddFile(ws: ApkWorkspace, op: PatchOperation.AddFile) {
        val file = File(ws.decodedDir, op.path)
        file.parentFile?.mkdirs()
        file.writeText(op.content)
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
