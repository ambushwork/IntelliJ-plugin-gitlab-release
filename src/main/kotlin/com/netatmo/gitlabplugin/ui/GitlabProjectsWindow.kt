package com.netatmo.gitlabplugin.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.PlatformIcons
import com.netatmo.gitlabplugin.callback.OnClickListener
import com.netatmo.gitlabplugin.model.CompositeProject
import com.netatmo.gitlabplugin.model.GitlabProject
import com.netatmo.gitlabplugin.model.Group
import com.netatmo.gitlabplugin.model.ProjectRelease
import com.netatmo.gitlabplugin.utils.applyFavoriteIcon
import com.netatmo.gitlabplugin.utils.getLoadingLabel
import com.netatmo.gitlabplugin.viewmodel.MainWindowViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.net.URI
import javax.swing.*
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants
import javax.swing.tree.DefaultMutableTreeNode


class GitlabProjectsWindow : ToolWindowFactory {


    private val toolbarTopLine = JPanel(FlowLayout(FlowLayout.LEFT))

    private val contentPanel = JPanel()
    private val listContent = JPanel().apply { layout = BoxLayout(this, BoxLayout.Y_AXIS) }
    private val pageIndicatorPanel = JPanel().apply {
        this.layout = FlowLayout(FlowLayout.LEFT)
    }
    private val selectorContent = JPanel()
    private val detailContent = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        preferredSize = Dimension(200, 400) // Set preferred height
    }
    private var listScrollPane = JScrollPane(listContent)
    private val viewModel = MainWindowViewModel()


    init {
        // TODO merge toolbar ui state.
        CoroutineScope(Dispatchers.Default).launch {
            viewModel.compositeProjectFlow.collectLatest {
                updateListView(it)
            }
        }
        CoroutineScope(Dispatchers.Default).launch {
            viewModel.groupStateFlow.collectLatest {
                updateSelector(it)
            }
        }
        CoroutineScope(Dispatchers.Default).launch {
            viewModel.detailState.collectLatest {
                updateDetailPanel(it)
            }
        }

        CoroutineScope(Dispatchers.Default).launch {
            viewModel.toolbarState.collectLatest {
                updateToolbarIcon(it)
            }
        }
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val content: Content = ContentFactory.getInstance().createContent(createContentPanel(), "", false)
        toolWindow.contentManager.addContent(content)
    }

    private fun createContentPanel(): JPanel {
        contentPanel.layout = BorderLayout()
        setupToolbar()
        val mainPanel = JPanel()
        mainPanel.layout = BoxLayout(mainPanel, BoxLayout.Y_AXIS)
        mainPanel.add(listScrollPane)
        mainPanel.add(detailContent)
        contentPanel.add(mainPanel, BorderLayout.CENTER)
        return contentPanel
    }

    private fun updateToolbarIcon(toolbarState: MainWindowViewModel.ToolbarState? = null) {
        // Add an icon to the toolbar
        toolbarTopLine.removeAll()
        val refreshButton = JLabel(PlatformIcons.SYNCHRONIZE_ICON).apply {
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent?) {
                    viewModel.requestCompositeProjects()
                }
            })
        }

        val favoriteButton: JLabel = JLabel().apply {
            this@GitlabProjectsWindow.applyFavoriteIcon(this, toolbarState?.favorite ?: false)
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent?) {
                    viewModel.toggleFavorite()
                }
            })
        }


        pageIndicatorPanel.removeAll()
        toolbarState?.pageInfo?.let {
            pageIndicatorPanel.add(JLabel(AllIcons.Actions.Back).apply {
                this.addMouseListener(object : OnClickListener {
                    override fun mouseClicked(e: MouseEvent?) {
                        viewModel.fetchLastPage()
                    }
                })
            })
            pageIndicatorPanel.add(JLabel("${it.current}/${it.total}"))
            pageIndicatorPanel.add(JLabel(AllIcons.Actions.Forward).apply {
                this.addMouseListener(object : OnClickListener {
                    override fun mouseClicked(e: MouseEvent?) {
                        viewModel.fetchNextPage()
                    }
                })
            })
        }
        pageIndicatorPanel.validate()
        pageIndicatorPanel.repaint()

        toolbarTopLine.add(refreshButton)
        toolbarTopLine.add(favoriteButton)
        toolbarTopLine.add(pageIndicatorPanel)
        if (toolbarState?.loading == true) {
            toolbarTopLine.add(getLoadingLabel())
        }
    }

    private fun setupTopLine() {

    }

    private fun setupToolbar(): JToolBar {
        // Create the toolbar
        val toolBar = JToolBar()
        setupTopLine()

        val searchField = JTextField(15)

        val bottomLine = JPanel(FlowLayout(FlowLayout.LEFT))
        bottomLine.add(JLabel("Group: "))
        bottomLine.add(selectorContent)
        bottomLine.add(JLabel("Search: "))
        bottomLine.add(searchField)
        bottomLine.add(JLabel(AllIcons.Actions.Search).apply {
            this.addMouseListener(object : OnClickListener {
                override fun mouseClicked(e: MouseEvent?) {
                    viewModel.searchProjectInGroup(searchField.text)
                }
            })
        })


        val toolbarPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
        }
        toolbarPanel.add(toolbarTopLine)
        toolbarPanel.add(bottomLine)

        contentPanel.add(toolbarPanel, BorderLayout.PAGE_START)

        return toolBar
    }

    private fun updateSelector(groups: List<Group>) {
        selectorContent.removeAll()
        val comboBox = GroupSelector(groups.toTypedArray()) {
            viewModel.changeGroup(it)
        }
        // Set the preferred height of the JComboBox
        comboBox.preferredSize = Dimension(200, 30)
        selectorContent.add(comboBox)
        contentPanel.validate()
        contentPanel.repaint()
    }

    private fun updateListView(projects: List<CompositeProject>) {
        listContent.removeAll()
        val rootNode = DefaultMutableTreeNode("Projects")
        // Create child nodes
        projects.forEach { compositeProject ->
            rootNode.add(DefaultMutableTreeNode(compositeProject.gitlabProject).apply {
                compositeProject.projectReleases.forEach {
                    this.add(DefaultMutableTreeNode(it))
                }
            })
        }

        // Create the JTree with the root node
        val tree = Tree(rootNode).apply {
            addTreeSelectionListener {
                val node = this.lastSelectedPathComponent as DefaultMutableTreeNode?
                when (val obj = node?.userObject) {
                    is ProjectRelease -> {
                        val projectNode = node.parent as DefaultMutableTreeNode
                        projectNode.userObject.apply {
                            if (this is GitlabProject) {
                                val projectId = this.id
                                viewModel.selectRelease(projectId, node.userObject as ProjectRelease)
                            }
                        }
                    }

                    is GitlabProject -> {
                        viewModel.selectProject(obj.id)
                    }
                }

            }
        }


        val scrollPane = JScrollPane(tree)

        listContent.add(scrollPane)

        contentPanel.validate()
        contentPanel.repaint()
    }

    private fun updateDetailPanel(detailState: MainWindowViewModel.DetailState?) {
        detailContent.removeAll()
        detailState?.let { state ->
            val descriptionContent = JPanel().apply {
                this.layout = BoxLayout(this, BoxLayout.Y_AXIS)
            }
            if (state.release != null) {
                val textPane = JTextPane()

                descriptionContent.add(textPane)


                // Create a simple attribute set for the title
                val titleAttributes = SimpleAttributeSet()
                StyleConstants.setFontFamily(titleAttributes, "Arial")
                StyleConstants.setFontSize(titleAttributes, 20)
                StyleConstants.setBold(titleAttributes, true)

                // Create a simple attribute set for the description
                val descriptionAttributes = SimpleAttributeSet()
                StyleConstants.setFontFamily(descriptionAttributes, "Times New Roman")
                StyleConstants.setFontSize(descriptionAttributes, 14)
                StyleConstants.setItalic(descriptionAttributes, true)

                // Set the text with different attributes

                // Set the text with different attributes
                textPane.text = "${state.release.name}\n \n ${state.release.description}"

                // Apply the attributes to specific text ranges
                textPane.styledDocument.setCharacterAttributes(0, 6, titleAttributes, true) // Apply to "Title:"

                textPane.styledDocument.setCharacterAttributes(
                    8,
                    textPane.text.length,
                    descriptionAttributes,
                    true
                ) // Apply to the description


            } else {
                state.gitlabProject?.apply {
                    val title = JLabel(this.name)
                    title.font = Font("Arial", Font.BOLD, 20)
                    title.border = BorderFactory.createEmptyBorder(10, 10, 10, 10) // Add padding
                    applyFavoriteIcon(title, detailState.favorite)
                    title.addMouseListener(object : MouseAdapter() {
                        override fun mouseClicked(e: MouseEvent?) {
                            viewModel.toggleProjectFav(this@apply.id)
                        }
                    })

                    descriptionContent.add(title)
                    val timestamp = JLabel("Updated at: ${this.updated_at}")
                    timestamp.border = BorderFactory.createEmptyBorder(10, 10, 10, 10); // Add padding
                    timestamp.alignmentX = Component.LEFT_ALIGNMENT
                    descriptionContent.add(timestamp)
                    descriptionContent.add(createWebLink("Open Repository", web_url))
                    /* descriptionContent.add(createWebLink("Browse Branches", this._links.repo_branches))
                     descriptionContent.add(createWebLink("Browse Merge requests", this._links.merge_requests))*/
                    descriptionContent.alignmentX = Component.LEFT_ALIGNMENT
                }

            }

            val jScrollPane = JScrollPane(descriptionContent)
            detailContent.add(jScrollPane)
        }
        contentPanel.validate()
        contentPanel.repaint()
    }

    private fun createWebLink(title: String, link: String): JLabel {
        return JLabel("<html><u>$title</u></html>").apply {
            foreground = Color(0, 128, 255)
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            border = BorderFactory.createEmptyBorder(10, 10, 10, 10); // Add padding
            alignmentX = Component.LEFT_ALIGNMENT
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent?) {
                    try {
                        Desktop.getDesktop().browse(URI(link))
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
            })
        }
    }
}