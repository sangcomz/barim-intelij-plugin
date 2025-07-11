package io.github.sangcomz.barim.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.wm.ToolWindowManager
import io.github.sangcomz.barim.MainToolWindowPanel

class AddNoteAction : AnAction(), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
        getBarimPanel(e)?.showAddNoteDialog()
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = getBarimPanel(e) != null
    }

    private fun getBarimPanel(e: AnActionEvent): MainToolWindowPanel? {
        val project = e.project ?: return null
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Barim") ?: return null

        if (!toolWindow.isVisible) {
            toolWindow.show(null)
        }

        return toolWindow.contentManager.contents.firstOrNull()?.component as? MainToolWindowPanel
    }
}