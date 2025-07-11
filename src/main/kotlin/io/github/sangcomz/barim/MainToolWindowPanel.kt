package io.github.sangcomz.barim

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.*
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.jetbrains.plugins.github.authentication.GHAccountsUtil
import org.jetbrains.plugins.github.authentication.accounts.GHAccountManager
import java.awt.BorderLayout
import java.awt.Component
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.ItemEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

class MainToolWindowPanel(
    private val project: Project,
    private val scope: CoroutineScope,
    private val parentDisposable: Disposable
) : JPanel(BorderLayout()) {

    private val accountManager = service<GHAccountManager>()
    private val apiClient = ApiClient()
    private val mainPanel = JBPanel<JBPanel<*>>(BorderLayout())
    private var currentToken: String? = null

    private var projectSelector: JComboBox<ProjectLabel>? = null
    private val listModels = mutableMapOf<String, DefaultListModel<Issue>>()
    private val loadingPanels = mutableMapOf<String, JBLoadingPanel>()
    private val statusOrder = listOf("TODO", "IN PROGRESS", "DONE", "PENDING")
    private var tasksTabbedPane: JBTabbedPane? = null
    private var projectSelectorListener: java.awt.event.ItemListener? = null

    init {
        observeAccountChanges()
    }

    private fun checkRepositoryAndInitialize(token: String) {
        this.currentToken = token
        drawMainUI()
        setBusy(true)

        scope.launch(Dispatchers.IO) {
            try {
                val result = apiClient.hasRepository(token, "barim-data").repository.name
                val repoExists = result == "barim-data"

                withContext(Dispatchers.Main) {
                    if (repoExists) {
                        loadProjects()
                    } else {
                        showCreateRepoDialog()
                        setBusy(false)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showCreateRepoDialog()
                    setBusy(false)
                }
            }
        }
    }

    private fun showCreateRepoDialog() {
        val result = Messages.showOkCancelDialog(
            project,
            "Could not find the 'barim-data' repository.\n" +
                    "This could be because it doesn't exist, or it's a private repository and the token lacks 'repo' scope permission.\n\n" +
                    "Please read the setup guide to create the repository and set token permissions.",
            "Repository Not Found or Inaccessible",
            "View Setup Guide",
            "Cancel",
            Messages.getQuestionIcon()
        )

        if (result == Messages.OK) {
            val url = "https://github.com/sangcomz/barim-intelij-plugin#:~:text=Notes-,Prerequisites,-Before%20you%20start"
            BrowserUtil.browse(url)
        }

        val model = projectSelector?.model as? DefaultComboBoxModel<ProjectLabel>
        model?.removeAllElements()
        val placeholder = ProjectLabel(
            name = "No projects found.",
            label = "",
            color = "",
            issueCount = 0
        )
        model?.addElement(placeholder)
        projectSelector?.isEnabled = false
    }

    private fun observeAccountChanges() {
        accountManager.accountsState.onEach { accounts ->
            when {
                accounts.isEmpty() -> showLoginPanel()
                else -> {
                    val account = accounts.first()
                    scope.launch {
                        var token: String? = null
                        for (i in 1..5) {
                            token = accountManager.findCredentials(account)
                            if (token != null) break
                            delay(500)
                        }

                        if (token != null) {
                            checkRepositoryAndInitialize(token)
                        } else {
                            withContext(Dispatchers.Main) {
                                showError("Failed to get token after retries.")
                            }
                        }
                    }
                }
            }
        }.launchIn(scope)
    }

    private fun showLoginPanel() {
        UIUtil.invokeLaterIfNeeded {
            removeAll()
            val loginPanel = JPanel(BorderLayout())
            val loginButton = JButton("Login with GitHub")
            loginButton.addActionListener {
                GHAccountsUtil.requestNewAccount(project = project, parentComponent = this)
            }
            loginPanel.add(loginButton, BorderLayout.CENTER)
            add(loginPanel, BorderLayout.CENTER)
            revalidateAndRepaint()
        }
    }

    private fun showMainContent(token: String) {
        this.currentToken = token
        scope.launch(Dispatchers.Main) {
            drawMainUI()
            loadProjects()
        }
    }

    private fun drawMainUI() {
        removeAll()
        mainPanel.removeAll()

        projectSelector = JComboBox<ProjectLabel>()
        projectSelector?.renderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?,
                value: Any?,
                index: Int,
                isSelected: Boolean,
                cellHasFocus: Boolean
            ): Component {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                if (value is ProjectLabel) {
                    text = "${value.name} (${value.issueCount})"
                } else if (value != null) {
                    text = value.toString()
                }
                return this
            }
        }

        val topPanel = JPanel(BorderLayout(JBUI.scale(5), 0)).apply {
            border = JBUI.Borders.empty(5, 5, 0, 5)
            add(JBLabel("Project: "), BorderLayout.WEST)
            add(projectSelector, BorderLayout.CENTER)
        }

        val mainTabbedPane = JBTabbedPane()

        tasksTabbedPane = JBTabbedPane()
        statusOrder.forEach { status ->
            val listModel = DefaultListModel<Issue>()
            val list = object : JBList<Issue>(listModel) {
                override fun getScrollableTracksViewportWidth(): Boolean {
                    return true
                }
            }.apply {
                cellRenderer = IssueCellRenderer()
                setupContextMenu(this)
            }
            val loadingPanel = JBLoadingPanel(BorderLayout(), parentDisposable).apply {
                add(JBScrollPane(list), BorderLayout.CENTER)
            }
            loadingPanel.addComponentListener(object : ComponentAdapter() {
                override fun componentResized(e: ComponentEvent?) {
                    list.fixedCellHeight = 0
                    list.fixedCellHeight = -1
                }
            })
            tasksTabbedPane?.addTab(status, loadingPanel)
            listModels[status] = listModel
            loadingPanels[status] = loadingPanel
        }

        val noteListModel = DefaultListModel<Issue>()
        val noteList = object : JBList<Issue>(noteListModel) {
            override fun getScrollableTracksViewportWidth(): Boolean {
                return true
            }
        }.apply {
            cellRenderer = IssueCellRenderer()
            setupContextMenu(this)
        }
        val noteLoadingPanel = JBLoadingPanel(BorderLayout(), parentDisposable).apply {
            add(JBScrollPane(noteList), BorderLayout.CENTER)
        }
        noteLoadingPanel.addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent?) {
                noteList.fixedCellHeight = 0
                noteList.fixedCellHeight = -1
            }
        })
        listModels["Notes"] = noteListModel
        loadingPanels["Notes"] = noteLoadingPanel

        mainTabbedPane.addTab("Tasks", tasksTabbedPane)
        mainTabbedPane.addTab("Notes", noteLoadingPanel)

        mainPanel.add(topPanel, BorderLayout.NORTH)
        mainPanel.add(mainTabbedPane, BorderLayout.CENTER)
        add(mainPanel, BorderLayout.CENTER)
        revalidateAndRepaint()
    }

    fun logout() {
        scope.launch {
            accountManager.accountsState.value.forEach { account ->
                accountManager.removeAccount(account)
            }
        }
    }

    fun forceRefresh() {
        val currentSelectedLabel = (projectSelector?.selectedItem as? ProjectLabel)?.name
        scope.launch(Dispatchers.Main) {
            loadProjects(selectProjectLabel = currentSelectedLabel)
        }
    }

    fun showAddProjectDialog() {
        val token = currentToken ?: return

        setBusy(true)
        scope.launch(Dispatchers.IO) {
            try {
                val repositories = apiClient.getUserRepositories(token).repositories

                withContext(Dispatchers.Main) {
                    setBusy(false)
                    val dialog = AddProjectDialog(project, repositories)
                    if (dialog.showAndGet()) {
                        dialog.selectedProjectName?.let { projectName ->
                            if (projectName.isNotBlank()) {
                                createProject(projectName)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    setBusy(false)
                    showError("Failed to load repositories: ${e.message}")
                }
            }
        }
    }

    private fun createProject(projectName: String) {
        val token = currentToken ?: return
        val projectData = ProjectCreateData(projectName = projectName)

        setBusy(true)
        scope.launch(Dispatchers.IO) {
            try {
                val newProject = apiClient.createProject(token, projectData)
                withContext(Dispatchers.Main) {
                    loadProjects(selectProjectLabel = newProject.name)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showError("Failed to create project: ${e.message}")
                }
            } finally {
                withContext(Dispatchers.Main) {
                    setBusy(false)
                }
            }
        }
    }

    fun showAddTaskDialog() {
        val selectedProject = projectSelector?.selectedItem as? ProjectLabel ?: return
        val dialog = AddTaskDialog(project, selectedProject.name)
        if (dialog.showAndGet()) {
            createTask(dialog.taskTitle, dialog.taskBody)
        }
    }

    private fun createTask(title: String, body: String) {
        val selectedProject = projectSelector?.selectedItem as? ProjectLabel ?: return
        val issueData = IssueCreateData(
            title = title,
            body = body,
            issueType = "Task",
            repo = selectedProject.name
        )
        createIssueInternal(issueData)
    }

    fun showAddNoteDialog() {
        val selectedProject = projectSelector?.selectedItem as? ProjectLabel ?: return
        val dialog = AddNoteDialog(project, selectedProject.name)
        if (dialog.showAndGet()) {
            createNote(
                title = dialog.noteTitle,
                body = dialog.noteBody,
                tags = dialog.noteTags
            )
        }
    }

    private fun createNote(title: String, body: String, tags: List<String>) {
        val selectedProject = projectSelector?.selectedItem as? ProjectLabel ?: return
        val issueData = IssueCreateData(
            title = title,
            body = body,
            issueType = "Note",
            repo = selectedProject.name,
            tags = tags.ifEmpty { null }
        )
        createIssueInternal(issueData)
    }

    private fun createIssueInternal(issueData: IssueCreateData) {
        val token = currentToken ?: return
        setBusy(true)
        scope.launch(Dispatchers.IO) {
            try {
                val newIssue = apiClient.createIssue(token, issueData)

                withContext(Dispatchers.Main) {
                    if (issueData.issueType == "Task") {
                        listModels["TODO"]?.add(0, newIssue)

                        val todoIndex = statusOrder.indexOf("TODO")
                        if (todoIndex != -1) {
                            val currentCount = listModels["TODO"]?.size ?: 0
                            tasksTabbedPane?.setTitleAt(todoIndex, "TODO ($currentCount)")
                        }
                    } else {
                        listModels["Notes"]?.add(0, newIssue)
                    }
                    revalidateAndRepaint()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) { showError("Failed to create item: ${e.message}") }
            } finally {
                withContext(Dispatchers.Main) { setBusy(false) }
            }
        }
    }

    private fun editIssue(issue: Issue) {
        val selectedProject = projectSelector?.selectedItem as? ProjectLabel ?: return
        val isTask = issue.labels.any { it.name == "Task" }
        if (isTask) {
            val dialog = AddTaskDialog(project, selectedProject.name, issue)
            if (dialog.showAndGet()) {
                val updateData = IssueUpdateData(title = dialog.taskTitle, body = dialog.taskBody)
                updateIssueDetails(issue.number, updateData)
            }
        } else {
            val dialog = AddNoteDialog(project, selectedProject.name, issue)
            if (dialog.showAndGet()) {
                val nonTagLabels = issue.labels
                    .filter { !it.name.startsWith("tag:") }
                    .map { it.name }
                val newTagLabels = dialog.noteTags.map { "tag:$it" }
                val allLabels = (nonTagLabels + newTagLabels).distinct()

                val updateData = IssueUpdateData(
                    title = dialog.noteTitle,
                    body = dialog.noteBody,
                    labels = allLabels
                )
                updateIssueDetails(issue.number, updateData)
            }
        }
    }

    private fun updateIssueDetails(issueNumber: Int, updateData: IssueUpdateData) {
        val token = currentToken ?: return
        val selectedProject = projectSelector?.selectedItem as? ProjectLabel ?: return
        setBusy(true)
        scope.launch(Dispatchers.IO) {
            try {
                apiClient.updateIssue(token, issueNumber, updateData)
                loadAndDisplayIssues(token, selectedProject.name)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { showError("Failed to update issue: ${e.message}") }
            } finally {
                withContext(Dispatchers.Main) { setBusy(false) }
            }
        }
    }

    private fun loadProjects(selectProjectLabel: String? = null) {
        val token = currentToken ?: return
        setBusy(true)
        projectSelector?.isEnabled = false

        scope.launch(Dispatchers.IO) {
            try {
                val response = apiClient.getProjects(token)
                withContext(Dispatchers.Main) {
                    val model = projectSelector?.model as? DefaultComboBoxModel<ProjectLabel>
                    model?.removeAllElements()

                    if (response.projects.isNotEmpty()) {
                        response.projects.forEachIndexed { index, project  -> model?.insertElementAt(project, index) }

                        projectSelectorListener?.let { listener ->
                            projectSelector?.removeItemListener(listener)
                        }

                        projectSelectorListener = java.awt.event.ItemListener { event ->
                            if (event.stateChange == ItemEvent.SELECTED) {
                                val selectedProject = event.item as? ProjectLabel
                                selectedProject?.name?.let { projectName ->
                                    if (projectName.isNotBlank()) {
                                        loadAndDisplayIssues(token, projectName)
                                    }
                                }
                            }
                        }

                        projectSelector?.addItemListener(projectSelectorListener)

                        val itemToSelect = if (selectProjectLabel != null) {
                            response.projects.find { it.name == selectProjectLabel }
                        } else null

                        projectSelector?.selectedItem = itemToSelect ?: if((projectSelector?.itemCount ?: 0) > 0) projectSelector?.getItemAt(0) else null

                        if (projectSelector?.selectedItem == null && projectSelector?.itemCount ?: 0 > 0) {
                            projectSelector?.selectedIndex = 0
                        }

                        val project = itemToSelect ?: projectSelector?.selectedItem as? ProjectLabel
                        if (project != null && project.name.isNotBlank()) {
                            loadAndDisplayIssues(token, project.name)
                        }

                    } else {
                        val placeholder = ProjectLabel(
                            name = "No projects found. Add one!",
                            label = "",
                            color = "",
                            issueCount = 0
                        )
                        model?.addElement(placeholder)
                        projectSelector?.isEnabled = false
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showError("Failed to load projects: ${e.message}")
                }
            } finally {
                withContext(Dispatchers.Main) {
                    setBusy(false)
                    if ((projectSelector?.itemCount
                            ?: 0) > 0 && projectSelector?.getItemAt(0)?.name?.isNotBlank() == true
                    ) {
                        projectSelector?.isEnabled = true
                    }
                }
            }
        }
    }

    private fun loadAndDisplayIssues(token: String, repoName: String) {
        setBusy(true)
        scope.launch(Dispatchers.IO) {
            try {
                val response = apiClient.getIssues(token, repoName)
                val tasks = response.issues.filter { issue -> issue.labels.any { it.name == "Task" } }
                val notes = response.issues.filter { issue -> issue.labels.any { it.name == "Note" } }
                val tasksByStatus = tasks.groupBy { findStatus(it)?.name ?: "TODO" }

                withContext(Dispatchers.Main) {
                    statusOrder.forEachIndexed { index, status ->
                        val tasksInStatus = tasksByStatus[status] ?: emptyList()
                        listModels[status]?.apply {
                            removeAllElements()
                            tasksInStatus.forEach { addElement(it) }
                        }
                        tasksTabbedPane?.setTitleAt(index, "$status (${tasksInStatus.size})")
                    }
                    listModels["Notes"]?.apply {
                        removeAllElements()
                        notes.forEach { addElement(it) }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { showError("Failed to load issues: ${e.message}") }
            } finally {
                withContext(Dispatchers.Main) { setBusy(false) }
            }
        }
    }

    private fun setupContextMenu(list: JList<Issue>) {
        list.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    val index = list.locationToIndex(e.point)
                    if (index != -1 && list.getCellBounds(index, index).contains(e.point)) {
                        list.selectedIndex = index
                        val issue = list.model.getElementAt(index)
                        createContextMenu(issue).show(list, e.x, e.y)
                    }
                }
            }
        })
    }

    private fun createContextMenu(issue: Issue): JPopupMenu {
        val popupMenu = JPopupMenu()

        if (issue.labels.any { it.name == "Task" }) {
            val currentStatus = findStatus(issue)?.name
            when (currentStatus) {
                "TODO" -> popupMenu.add(createMenuItem("Start Task") { updateTaskStatus(issue, "IN PROGRESS") })
                "IN PROGRESS" -> {
                    popupMenu.add(createMenuItem("DONE") { updateTaskStatus(issue, "DONE", true, "completed") })
                    popupMenu.add(createMenuItem("PENDING") { pendTaskWithComment(issue) })
                }
                "PENDING" -> {
                    popupMenu.add(createMenuItem("Resume Task") { updateTaskStatus(issue, "IN PROGRESS") })
                    popupMenu.add(createMenuItem("Close") { updateTaskStatus(issue, null, true, "not_planned") })
                }
            }
            popupMenu.addSeparator()
        }

        popupMenu.add(createMenuItem("Edit") {
            editIssue(issue)
        })

        if (projectSelector?.selectedItem != null) {
            popupMenu.addSeparator()
            popupMenu.add(createMenuItem("Go to GitHub") {
                val owner = issue.user.login
                val actualUrl = "https://github.com/$owner/barim-data/issues/${issue.number}"
                BrowserUtil.browse(actualUrl)
            })
        }

        return popupMenu
    }

    private fun pendTaskWithComment(issue: Issue) {
        val dialog = AddCommentDialog(project)
        if (dialog.showAndGet()) {
            val commentBody = dialog.commentBody
            val token = currentToken ?: return
            val selectedProject = projectSelector?.selectedItem as? ProjectLabel ?: return

            setBusy(true)
            scope.launch(Dispatchers.IO) {
                try {
                    val commentData =
                        CommentCreateData(body = "Status changed to PENDING with reason: \n> ${commentBody}")
                    apiClient.addCommentToIssue(token, issue.number, commentData)

                    val newLabels = issue.labels.map { it.name }.toMutableList()
                    findStatus(issue)?.name?.let { newLabels.remove(it) }
                    "PENDING".let { newLabels.add(it) }
                    val updateData = IssueUpdateData(labels = newLabels.distinct())

                    apiClient.updateIssue(token, issue.number, updateData)

                    loadAndDisplayIssues(token, selectedProject.name)
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) { showError("Failed to update task: ${e.message}") }
                } finally {
                    withContext(Dispatchers.Main) { setBusy(false) }
                }
            }
        }
    }

    private fun updateTaskStatus(issue: Issue, newStatus: String?, close: Boolean = false, reason: String? = null) {
        val token = currentToken ?: return
        val selectedProject = projectSelector?.selectedItem as? ProjectLabel ?: return

        val newLabels = issue.labels.map { it.name }.toMutableList()
        findStatus(issue)?.name?.let { newLabels.remove(it) }
        newStatus?.let { newLabels.add(it) }

        val updateData = IssueUpdateData(
            labels = newLabels.distinct(),
            state = if (close) "closed" else null,
            stateReason = reason
        )
        updateIssueDetails(issue.number, updateData)
    }

    private fun createMenuItem(text: String, action: () -> Unit): JMenuItem {
        return JMenuItem(text).apply {
            addActionListener { action() }
        }
    }

    private fun findStatus(issue: Issue): Label? {
        val statusNames = setOf("TODO", "IN PROGRESS", "DONE", "PENDING")
        return issue.labels.find { it.name in statusNames }
    }

    private fun setBusy(isBusy: Boolean) {
        mainPanel.isEnabled = !isBusy
        loadingPanels.values.forEach { panel ->
            if (isBusy) panel.startLoading() else panel.stopLoading()
        }
    }

    private fun showError(message: String) {
        UIUtil.invokeLaterIfNeeded {
            removeAll()
            val topPanel = JPanel(BorderLayout()).apply {
                add(JBLabel("Error:"), BorderLayout.WEST)
                add(JLabel(message), BorderLayout.CENTER)
                border = JBUI.Borders.empty(5)
            }
            add(topPanel, BorderLayout.CENTER)
            revalidateAndRepaint()
        }
    }

    private fun revalidateAndRepaint() {
        revalidate()
        repaint()
    }

    fun dispose() {
        projectSelectorListener?.let { listener ->
            projectSelector?.removeItemListener(listener)
        }
        projectSelectorListener = null
    }
}