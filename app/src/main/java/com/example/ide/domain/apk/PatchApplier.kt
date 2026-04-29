package com.example.ide.domain.apk

import java.io.File

class PatchApplier {

    fun apply(workspace: ApkWorkspace, plan: ApkTransformationPlan) {
        plan.operations.forEach { op ->
            when (op) {
                is PatchOperation.ReplaceText -> applyReplace(workspace, op)
                is PatchOperation.UpdateResourceValue -> applyResource(workspace, op)
                is PatchOperation.UpdateXmlAttribute -> applyXml(workspace, op)
                is PatchOperation.AddFile -> applyAddFile(workspace, op)
            }
        }
    }

    private fun applyReplace(ws: ApkWorkspace, op: PatchOperation.ReplaceText) {
        val file = File(ws.decodedDir, op.path)
        if (!file.exists()) return
        val content = file.readText()
        file.writeText(content.replace(op.before, op.after))
    }

    private fun applyResource(ws: ApkWorkspace, op: PatchOperation.UpdateResourceValue) {
        val file = File(ws.decodedDir, op.path)
        if (!file.exists()) return
        val updated = file.readText().replace(op.key, op.value)
        file.writeText(updated)
    }

    private fun applyXml(ws: ApkWorkspace, op: PatchOperation.UpdateXmlAttribute) {
        val file = File(ws.decodedDir, op.path)
        if (!file.exists()) return
        val content = file.readText()
        val updated = content.replace(op.attribute + "=\".*?\"".toRegex(), op.attribute + "=\"${op.value}\"")
        file.writeText(updated)
    }

    private fun applyAddFile(ws: ApkWorkspace, op: PatchOperation.AddFile) {
        val file = File(ws.decodedDir, op.path)
        file.parentFile?.mkdirs()
        file.writeText(op.content)
    }
}
