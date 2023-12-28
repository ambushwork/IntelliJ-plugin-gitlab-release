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
import com.netatmo.gitlabplugin.model.*
import com.netatmo.gitlabplugin.utils.applyFavoriteIcon
import com.netatmo.gitlabplugin.utils.applyNetworkErrorIcon
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

    private val iconsGroupPanel = JPanel().apply {
        layout = FlowLayout(FlowLayout.LEFT)
        preferredSize = Dimension(preferredSize.width, 20)
    }

    private val pageIndicatorPanel = JPanel().apply {
        layout = FlowLayout(FlowLayout.LEFT)
        preferredSize = Dimension(80, 42)
    }

    private val contentPanel = JPanel()
    private val listContent = JPanel().apply { layout = BoxLayout(this, BoxLayout.Y_AXIS) }

    private val groupMenuContent = JPanel()
    private val detailContent = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        preferredSize = Dimension(200, 400) // Set preferred height
    }
    private var listScrollPane = JScrollPane(listContent)
    private val viewModel = MainWindowViewModel()

    init {
        CoroutineScope(Dispatchers.Default).launch {
            viewModel.dataStateFlow.collectLatest {
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
        iconsGroupPanel.removeAll()

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
        val pageIndicatorPanel = JPanel().apply {
            this.layout = FlowLayout(FlowLayout.LEFT)
        }
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
        iconsGroupPanel.add(refreshButton)
        iconsGroupPanel.add(favoriteButton)
        if (toolbarState?.loading == true) {
            iconsGroupPanel.add(getLoadingLabel())
        }

        this.pageIndicatorPanel.removeAll()
        this.pageIndicatorPanel.add(pageIndicatorPanel)
        iconsGroupPanel.validate()
        iconsGroupPanel.repaint()
        this.pageIndicatorPanel.validate()
        this.pageIndicatorPanel.repaint()
    }

    private fun setupToolbar(): JToolBar {
        // Create the toolbar
        val toolBar = JToolBar()
        val searchField = JTextField(10)
        val bottomLine = JPanel(FlowLayout(FlowLayout.LEFT))
        bottomLine.add(JLabel("Group: "))
        bottomLine.add(groupMenuContent)
        bottomLine.add(JLabel("Search: "))
        bottomLine.add(searchField)
        bottomLine.add(JLabel(AllIcons.Actions.Search).apply {
            this.addMouseListener(object : OnClickListener {
                override fun mouseClicked(e: MouseEvent?) {
                    viewModel.searchProjectInGroup(searchField.text)
                }
            })
        })
        bottomLine.add(pageIndicatorPanel)

        val toolbarPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
        }
        toolbarPanel.add(iconsGroupPanel)
        toolbarPanel.add(bottomLine)

        contentPanel.add(toolbarPanel, BorderLayout.PAGE_START)

        return toolBar
    }

    private fun updateSelector(groups: List<Group>) {
        groupMenuContent.removeAll()
        val comboBox = GroupSelector(groups.toTypedArray()) {
            viewModel.changeGroup(it)
        }
        // Set the preferred height of the JComboBox
        comboBox.preferredSize = Dimension(160, 30)
        groupMenuContent.add(comboBox)
        contentPanel.validate()
        contentPanel.repaint()
    }

    private fun updateListView(dataState: DataState<ProjectListResponse>) {
        listContent.removeAll()

        val panel = when (dataState) {
            is DataState.Error -> createErrorPanel()
            DataState.Idle -> createIdlePanel()
            is DataState.Success -> createProjectListPanel(dataState.data.projects)
        }

        listContent.add(panel)
        contentPanel.validate()
        contentPanel.repaint()
    }

    private fun createIdlePanel(): JPanel {
        return JPanel()
    }

    private fun createErrorPanel(): JPanel {
        val constraints = GridBagConstraints()
        constraints.gridx = 0
        constraints.gridy = 0
        constraints.weightx = 1.0
        constraints.weighty = 1.0
        constraints.anchor = GridBagConstraints.CENTER
        val jPanel = JPanel().apply {
            layout = GridBagLayout()
        }
        jPanel.add(JLabel("Network error, check your internet or gitlab access.").apply {
            this@GitlabProjectsWindow.applyNetworkErrorIcon(this)
        }, constraints)
        return jPanel
    }

    private fun createProjectListPanel(projects: List<CompositeProject>): JScrollPane {
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
        return JScrollPane(tree)
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