package io.github.sangcomz.barim

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import java.awt.Component
import javax.swing.*

class AddProjectDialog(
    project: Project?,
    private val repositories: List<GithubRepo>
) : DialogWrapper(project) {

    private val tabbedPane = JBTabbedPane()
    private val repoSelector = JComboBox<GithubRepo>()
    private val manualInputField = JBTextField()

    val selectedProjectName: String?
        get() {
            return when (tabbedPane.selectedIndex) {
                0 -> (repoSelector.selectedItem as? GithubRepo)?.name
                1 -> manualInputField.text.trim().takeIf { it.isNotBlank() }
                else -> null
            }
        }

    init {
        title = "Add New Project"
        init()
        // Set initial OK button state
        updateOkButtonState()
    }

    override fun createCenterPanel(): JComponent {
        // Tab 1: Select from repository list
        val selectPanel = createSelectPanel()
        // Tab 2: Enter name manually
        val manualPanel = createManualPanel()

        tabbedPane.addTab("Select from Repositories", selectPanel)
        tabbedPane.addTab("Create Manually", manualPanel)

        // Update OK button state when tab changes
        tabbedPane.addChangeListener { updateOkButtonState() }

        return tabbedPane
    }

    private fun createSelectPanel(): JPanel {
        // Fill combobox with repository list
        repoSelector.model = DefaultComboBoxModel(repositories.toTypedArray())
        // Configure how each item appears in combobox (display repository's fullName)
        repoSelector.renderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?,
                value: Any?,
                index: Int,
                isSelected: Boolean,
                cellHasFocus: Boolean
            ): Component {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                if (value is GithubRepo) {
                    text = value.name
                }
                return this
            }
        }
        repoSelector.addActionListener { updateOkButtonState() }

        return FormBuilder.createFormBuilder()
            .addLabeledComponent("Select a repository:", repoSelector)
            .panel
    }

    private fun createManualPanel(): JPanel {
        manualInputField.document.addUndoableEditListener { updateOkButtonState() }
        return FormBuilder.createFormBuilder()
            .addLabeledComponent("Enter a new project name:", manualInputField)
            .panel
    }

    private fun updateOkButtonState() {
        isOKActionEnabled = !selectedProjectName.isNullOrBlank()
    }

    override fun getPreferredFocusedComponent(): JComponent {
        // Focus on the combobox of the first tab
        return repoSelector
    }
}