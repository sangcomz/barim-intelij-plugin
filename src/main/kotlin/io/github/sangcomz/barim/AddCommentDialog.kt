package io.github.sangcomz.barim

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import java.awt.Dimension
import javax.swing.JComponent

class AddCommentDialog(project: Project?) : DialogWrapper(project) {
    private val commentArea = JBTextArea(5, 40)

    val commentBody: String
        get() = commentArea.text.trim()

    init {
        title = "Add Pending Reason" // Dialog title
        init()
    }

    override fun createCenterPanel(): JComponent {
        val scrollPane = JBScrollPane(commentArea).apply {
            preferredSize = Dimension(400, 100)
        }

        // Enable OK button only when comment content is not empty
        commentArea.document.addUndoableEditListener {
            isOKActionEnabled = commentBody.isNotBlank()
        }
        isOKActionEnabled = false // Initially disabled

        return scrollPane
    }

    override fun getPreferredFocusedComponent(): JComponent {
        return commentArea
    }
}