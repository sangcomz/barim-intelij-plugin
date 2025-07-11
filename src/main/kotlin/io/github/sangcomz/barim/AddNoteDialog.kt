package io.github.sangcomz.barim

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent

class AddNoteDialog(
    project: Project?,
    private val projectName: String, // Parameter to receive project name
    private val issueToEdit: Issue? = null
) : DialogWrapper(project) {
    private val titleField = JBTextField()
    private val bodyArea = JBTextArea(5, 40).apply {
        lineWrap = true
        wrapStyleWord = true
    }
    private val tagsField = JBTextField()

    val noteTitle: String
        get() = titleField.text.trim()

    val noteBody: String
        get() = bodyArea.text.trim()

    val noteTags: List<String>
        get() = tagsField.text.split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    init {
        title = if (issueToEdit != null) "Edit Note" else "Add New Note"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val bodyScrollPane = JBScrollPane(bodyArea).apply {
            verticalScrollBarPolicy = JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
            horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        }

        // Read-only field to display project name
        val projectDisplayField = JBTextField(projectName).apply { isEnabled = false }

        if (issueToEdit != null) {
            titleField.text = issueToEdit.title
            bodyArea.text = issueToEdit.body
            tagsField.text = issueToEdit.labels
                .filter { it.name.startsWith("tag:") }
                .joinToString(", ") { it.name.removePrefix("tag:") }
        }

        // Add project name field to FormBuilder
        val panel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Project:"), projectDisplayField, true)
            .addLabeledComponent(JBLabel("Title:"), titleField, true)
            .addLabeledComponent(JBLabel("Body:"), bodyScrollPane, true)
            .addLabeledComponent(JBLabel("Tags (comma-separated):"), tagsField, true)
            .panel

        titleField.document.addUndoableEditListener {
            isOKActionEnabled = titleField.text.isNotBlank()
        }
        isOKActionEnabled = issueToEdit != null || titleField.text.isNotBlank()

        return panel
    }

    override fun getPreferredFocusedComponent(): JComponent {
        return titleField
    }
}