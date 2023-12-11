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
import javax.swing.tree.DefaultMutableTreeNode


class GitlabProjectsWindow : ToolWindowFactory {

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
            viewModel.pageFlow.collectLatest {
                updatePageIndicator(it)
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


    private fun setupToolbar(): JToolBar {
        // Create the toolbar
        val toolBar = JToolBar().apply {

        }
        val topLine = JPanel(FlowLayout(FlowLayout.LEFT))

        // Add an icon to the toolbar
        val refreshButton = JButton(PlatformIcons.SYNCHRONIZE_ICON)
        refreshButton.addActionListener {
            viewModel.requestCompositeProjects()
        }
        val favoriteButton = JButton(AllIcons.Actions.AddToDictionary)
        topLine.add(refreshButton)
        topLine.add(favoriteButton)
        topLine.add(pageIndicatorPanel)

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
        toolbarPanel.add(topLine)
        toolbarPanel.add(bottomLine)

        contentPanel.add(toolbarPanel, BorderLayout.PAGE_START)

        return toolBar
    }

    private fun updatePageIndicator(pageInfo: PageInfo?) {
        pageIndicatorPanel.removeAll()
        pageInfo?.let {
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
                val node = this.lastSelectedPathComponent as DefaultMutableTreeNode
                when (val obj = node.userObject) {
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
                descriptionContent.add(JTextArea(state.release.description))
            } else {
                state.gitlabProject?.apply {

                    val imageIcon =
                        ImageIcon(this@GitlabProjectsWindow::class.java.getResource("/drawable/star.png"))
                    val image: Image = imageIcon.image // transform it
                    val newimg = image.getScaledInstance(20, 20, Image.SCALE_SMOOTH) // scale it the smooth way

                    val title = JLabel(this.name)
                    title.font = Font("Arial", Font.BOLD, 20)
                    title.border = BorderFactory.createEmptyBorder(10, 10, 10, 10) // Add padding
                    title.icon = ImageIcon(newimg)

                    descriptionContent.add(title)
                    val timestamp = JLabel("Updated at: ${this.updated_at}")
                    timestamp.border = BorderFactory.createEmptyBorder(10, 10, 10, 10); // Add padding
                    timestamp.alignmentX = Component.LEFT_ALIGNMENT
                    descriptionContent.add(timestamp)

                    val urlLabel = JLabel("<html><u>Open Repository</u></html>")
                    urlLabel.foreground = Color.BLUE
                    urlLabel.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                    urlLabel.addMouseListener(object : MouseAdapter() {
                        override fun mouseClicked(e: MouseEvent?) {
                            try {
                                Desktop.getDesktop().browse(URI(this@apply.web_url))
                            } catch (ex: Exception) {
                                ex.printStackTrace()
                            }
                        }
                    })
                    urlLabel.border = BorderFactory.createEmptyBorder(10, 10, 10, 10); // Add padding
                    urlLabel.alignmentX = Component.LEFT_ALIGNMENT
                    descriptionContent.add(urlLabel)
                    descriptionContent.alignmentX = Component.LEFT_ALIGNMENT
                }

            }

            val jScrollPane = JScrollPane(descriptionContent)
            detailContent.add(jScrollPane)
        }
        contentPanel.validate()
        contentPanel.repaint()
    }
}