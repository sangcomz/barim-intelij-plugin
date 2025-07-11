package io.github.sangcomz.barim

import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.jetbrains.plugins.github.authentication.accounts.GHAccountManager

class IssueToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        DumbService.getInstance(project).runWhenSmart {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
            val mainPanel = MainToolWindowPanel(project, scope, toolWindow.disposable)

            Disposer.register(toolWindow.disposable, Disposable {
                scope.cancel()
                mainPanel.dispose()
            })

            val contentFactory = ContentFactory.getInstance()
            val content = contentFactory.createContent(mainPanel, "", false)
            toolWindow.contentManager.addContent(content)

            val addTaskAction = object : AnAction("Add Task") {
                override fun actionPerformed(e: AnActionEvent) = mainPanel.showAddTaskDialog()
            }
            val addNoteAction = object : AnAction("Add Note") {
                override fun actionPerformed(e: AnActionEvent) = mainPanel.showAddNoteDialog()
            }
            val addProjectAction = object : AnAction("Add Project") {
                override fun actionPerformed(e: AnActionEvent) = mainPanel.showAddProjectDialog()
            }

            val goToRepoAction = object : AnAction("Go to GitHub Repository", null, AllIcons.Vcs.Vendors.Github) {
                override fun actionPerformed(e: AnActionEvent) {
                    BrowserUtil.browse("https://github.com/sangcomz/barim-intelij-plugin")
                }
            }

            val addActionGroup = DefaultActionGroup("Add", true).apply {
                templatePresentation.icon = AllIcons.General.Add
                add(addProjectAction)
                add(Separator.getInstance())
                add(addTaskAction)
                add(addNoteAction)
            }

            val refreshAction = object : AnAction("Refresh", "Refresh list", AllIcons.Actions.Refresh) {
                override fun actionPerformed(e: AnActionEvent) = mainPanel.forceRefresh()
            }

            val logoutAction = object : AnAction("Logout", "Logout from GitHub", AllIcons.Actions.Exit) {
                override fun actionPerformed(e: AnActionEvent) = mainPanel.logout()
            }

            val accountManager = service<GHAccountManager>()
            accountManager.accountsState
                .onEach { accounts ->
                    if (accounts.isNotEmpty()) {
                        toolWindow.setTitleActions(listOf(addActionGroup, refreshAction, goToRepoAction, logoutAction))
                    } else {
                        toolWindow.setTitleActions(listOf(goToRepoAction))
                    }
                }
                .launchIn(scope)

        }
    }
}