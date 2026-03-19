package com.modularerp.ai

import org.springframework.stereotype.Component

@Component
class AiPermissionChecker {

    /**
     * Filters tool definitions to only those the user has permission to use.
     * Permission format: "RESOURCE:ACTION" (e.g. "SALES:READ").
     * Wildcard "*" grants access to everything.
     */
    fun filterToolsByPermission(
        tools: List<ErpToolRegistry.ToolDefinition>,
        userPermissions: List<String>
    ): List<ErpToolRegistry.ToolDefinition> {
        return tools.filter { tool ->
            hasPermission(tool.requiredPermission, userPermissions)
        }
    }

    fun canExecuteTool(
        toolName: String,
        tools: List<ErpToolRegistry.ToolDefinition>,
        userPermissions: List<String>
    ): Boolean {
        val tool = tools.find { it.name == toolName } ?: return false
        return hasPermission(tool.requiredPermission, userPermissions)
    }

    private fun hasPermission(requiredPermission: String, userPermissions: List<String>): Boolean {
        val (resource, _) = requiredPermission.split(":")
        return userPermissions.any { perm ->
            perm == "*" ||
                perm == requiredPermission ||
                perm == "$resource:*" ||
                perm.startsWith("$resource:")
        }
    }
}
