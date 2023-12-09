package com.netatmo.gitlabplugin.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.PlatformIcons
import com.netatmo.gitlabplugin.model.CompositeProject
import com.netatmo.gitlabplugin.model.GitlabProject
import com.netatmo.gitlabplugin.model.ProjectNamespace
import com.netatmo.gitlabplugin.model.ProjectRelease
import com.netatmo.gitlabplugin.viewmodel.MainWindowViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import java.awt.Dimension
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
        val toolBar = JToolBar()
        contentJPanel.add(toolBar, BorderLayout.PAGE_START)
        // Add an icon to the toolbar
        val icon = PlatformIcons.SYNCHRONIZE_ICON // Replace with the actual path
        val iconButton = JButton(icon)
        iconButton.addActionListener {
            viewModel.requestCompositeProjects()
        }
        toolBar.add(iconButton)
        toolBar.add(selectorContent)
        return toolBar
    }

    private fun updateSelector(groups: Set<ProjectNamespace>) {
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