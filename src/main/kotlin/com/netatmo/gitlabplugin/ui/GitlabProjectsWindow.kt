package com.netatmo.gitlabplugin.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.PlatformIcons
import com.netatmo.gitlabplugin.model.CompositeProject
import com.netatmo.gitlabplugin.model.GitlabProject
import com.netatmo.gitlabplugin.model.Group
import com.netatmo.gitlabplugin.model.ProjectRelease
import com.netatmo.gitlabplugin.viewmodel.MainWindowViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode


class GitlabProjectsWindow : ToolWindowFactory {

    private val contentPanel = JPanel()
    private val listContent = JPanel().apply { layout = BoxLayout(this, BoxLayout.Y_AXIS) }
    private val selectorContent = JPanel()
    private val detailContent = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        preferredSize = Dimension(200, 400) // Set preferred height

    }
    private var listScrollPane = JScrollPane(listContent)
    private val viewModel = MainWindowViewModel()

    init {
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
            viewModel.selectedRelease.collectLatest {
                updateDetailPanel(it)
            }
        }
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val content: Content = ContentFactory.getInstance().createContent(createContentPanel(), "", false)
        toolWindow.contentManager.addContent(content)
    }

    private fun createContentPanel(): JPanel {
        contentPanel.layout = BorderLayout()
        setupToolbar(contentPanel)
        val mainPanel = JPanel()
        mainPanel.layout = BoxLayout(mainPanel, BoxLayout.Y_AXIS)
        mainPanel.add(listScrollPane)
        mainPanel.add(detailContent)
        contentPanel.add(mainPanel, BorderLayout.CENTER)
        return contentPanel
    }


    private fun setupToolbar(contentJPanel: JPanel): JToolBar {
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


        val searchField = JTextField(15)

        val bottomLine = JPanel(FlowLayout(FlowLayout.LEFT))
        bottomLine.add(JLabel("Group: "))
        bottomLine.add(selectorContent)
        bottomLine.add(JLabel("Search: "))
        bottomLine.add(searchField)
        bottomLine.add(JLabel(AllIcons.Actions.Search).apply {
            this.addMouseListener(object : MouseListener {
                override fun mouseClicked(e: MouseEvent?) {
                    viewModel.searchProjectInGroup(searchField.text)
                }

                override fun mousePressed(e: MouseEvent?) {
                    // do nothing
                }

                override fun mouseReleased(e: MouseEvent?) {
                    // do nothing
                }

                override fun mouseEntered(e: MouseEvent?) {
                    // do nothing
                }

                override fun mouseExited(e: MouseEvent?) {
                    // do nothing
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
                    this.add(DefaultMutableTreeNode(it.name))
                }
            })
        }

        // Create the JTree with the root node
        val tree = Tree(rootNode).apply {
            addTreeSelectionListener {
                val node = this.lastSelectedPathComponent as DefaultMutableTreeNode
                val projectNode = node.parent as DefaultMutableTreeNode
                projectNode.userObject.apply {
                    if (this is GitlabProject) {
                        val projectId = this.id
                        viewModel.selectRelease(projectId, node.userObject as String)
                    }
                }
            }
        }


        val scrollPane = JScrollPane(tree)

        listContent.add(scrollPane)

        contentPanel.validate()
        contentPanel.repaint()
    }

    private fun updateDetailPanel(projectRelease: ProjectRelease?) {
        detailContent.removeAll()
        projectRelease?.let {
            val label = JTextArea(it.description)
            val jScrollPane = JScrollPane(label)
            detailContent.add(jScrollPane)
        }
        contentPanel.validate()
        contentPanel.repaint()
    }
}