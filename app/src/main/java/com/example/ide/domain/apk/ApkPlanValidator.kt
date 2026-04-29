package com.example.ide.domain.apk

import java.io.File

class ApkPlanValidator {

    fun validate(workspace: ApkWorkspace, plan: ApkTransformationPlan): ValidationResult {
        if (plan.riskLevel == RiskLevel.BLOCKED) {
            return ValidationResult(false, "Plan blocked by planner")
        }
        if (plan.operations.isEmpty()) {
            return ValidationResult(false, "Plan has no operations")
        }

        val root = File(workspace.decodedDir).canonicalFile
        for (op in plan.operations) {
            val pathCheck = validateRelativePath(root, op.path)
            if (!pathCheck.valid) return pathCheck

            when (op) {
                is PatchOperation.ReplaceText -> {
                    if (op.before.isBlank()) return ValidationResult(false, "replace_text before cannot be blank")
                    if (op.before.length > 20_000 || op.after.length > 20_000) {
                        return ValidationResult(false, "replace_text payload too large")
                    }
                }
                is PatchOperation.UpdateResourceValue -> {
                    if (op.key.isBlank() || op.value.isBlank()) {
                        return ValidationResult(false, "resource key/value cannot be blank")
                    }
                }
                is PatchOperation.UpdateXmlAttribute -> {
                    if (op.attribute.isBlank() || op.value.isBlank()) {
                        return ValidationResult(false, "xml attribute/value cannot be blank")
                    }
                }
                is PatchOperation.AddFile -> {
                    if (op.content.length > 500_000) return ValidationResult(false, "added file too large")
                    if (!op.path.startsWith("res/") && !op.path.startsWith("assets/")) {
                        return ValidationResult(false, "add_file only allowed under res/ or assets/")
                    }
                }
            }
        }

        return ValidationResult(true, "OK")
    }

    private fun validateRelativePath(root: File, relativePath: String): ValidationResult {
        if (relativePath.isBlank()) return ValidationResult(false, "Blank path")
        if (relativePath.startsWith("/") || relativePath.startsWith("\\")) {
            return ValidationResult(false, "Absolute paths are not allowed: $relativePath")
        }
        if (relativePath.contains("..")) {
            return ValidationResult(false, "Parent traversal is not allowed: $relativePath")
        }
        val target = File(root, relativePath).canonicalFile
        if (!target.path.startsWith(root.path)) {
            return ValidationResult(false, "Path escapes workspace: $relativePath")
        }
        val allowed = relativePath.startsWith("res/") ||
            relativePath.startsWith("assets/") ||
            relativePath.startsWith("smali") ||
            relativePath == "AndroidManifest.xml"
        if (!allowed) return ValidationResult(false, "Path not editable: $relativePath")
        return ValidationResult(true, "OK")
    }
}

data class ValidationResult(
    val valid: Boolean,
    val message: String
)
