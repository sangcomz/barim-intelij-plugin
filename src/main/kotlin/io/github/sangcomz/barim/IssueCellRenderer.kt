package io.github.sangcomz.barim

import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import java.awt.*
import javax.swing.*
import javax.swing.border.MatteBorder

class IssueCellRenderer : JPanel(BorderLayout(0, V_GAP)), ListCellRenderer<Issue> {

    private val titleLabel = JLabel()
    private val bodyArea = JTextArea()
    private val statusLabel = JBLabel().apply {
        isOpaque = true
        border = JBUI.Borders.empty(0, 6)
        font = font.deriveFont(Font.BOLD, 10f)
    }
    private val titlePanel = JPanel(BorderLayout(0, 0))

    init {
        titleLabel.font = titleLabel.font.deriveFont(Font.BOLD)

        bodyArea.isOpaque = false
        bodyArea.isEditable = false
        bodyArea.wrapStyleWord = true
        bodyArea.lineWrap = true

        titlePanel.isOpaque = false
        titlePanel.add(titleLabel, BorderLayout.CENTER)
        titlePanel.add(statusLabel, BorderLayout.EAST)

        add(titlePanel, BorderLayout.NORTH)
        add(bodyArea, BorderLayout.CENTER)

        border = BorderFactory.createCompoundBorder(
            MatteBorder(0, 0, BOTTOM_BORDER, 0, JBColor.DARK_GRAY),
            JBUI.Borders.empty(BORDER_OFFSET)
        )
    }

    override fun getListCellRendererComponent(
        list: JList<out Issue>?,
        value: Issue?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        value?.let {
            titleLabel.text = it.title
            bodyArea.text = it.body

            val status = findStatus(it)
            if (status != null) {
                statusLabel.text = status.name
                statusLabel.background = Color.decode("#${status.color}")
                statusLabel.foreground = if (isColorDark(statusLabel.background)) JBColor.WHITE else JBColor.BLACK
                statusLabel.isVisible = true
            } else {
                statusLabel.isVisible = false
            }

            val availableWidth = list?.width?.takeIf { width -> width > 0 } ?: -1

            bodyArea.size = Dimension(availableWidth - BORDER_OFFSET * 2, 1)

            val bodyPrefSize = bodyArea.preferredSize

            val titlePrefSize = titlePanel.preferredSize
            val totalHeight = titlePrefSize.height + bodyPrefSize.height + BORDER_OFFSET * 2 + V_GAP + BOTTOM_BORDER

            preferredSize = Dimension(availableWidth, totalHeight)
        }

        if (isSelected) {
            background = list?.selectionBackground
            titleLabel.foreground = list?.selectionForeground
            bodyArea.foreground = list?.selectionForeground
        } else {
            background = list?.background
            titleLabel.foreground = list?.foreground
            bodyArea.foreground = list?.foreground
        }

        return this
    }

    private fun findStatus(issue: Issue): Label? {
        val statusNames = setOf("TODO", "IN PROGRESS", "DONE", "PENDING")
        return issue.labels.find { it.name in statusNames }
    }

    // Simple function to determine if a color is dark
    private fun isColorDark(color: Color): Boolean {
        val darkness = 1 - (0.299 * color.red + 0.587 * color.green + 0.114 * color.blue) / 255
        return darkness > 0.5
    }

    companion object {
        private const val BORDER_OFFSET = 10
        private const val V_GAP = 5
        private const val BOTTOM_BORDER = 2
    }
}