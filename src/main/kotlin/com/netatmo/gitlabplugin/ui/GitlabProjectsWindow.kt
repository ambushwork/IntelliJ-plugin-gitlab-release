package com.netatmo.gitlabplugin.ui

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory
import com.netatmo.gitlabplugin.action.LoadAction
import com.netatmo.gitlabplugin.model.GitlabProject
import com.netatmo.gitlabplugin.model.ProjectNamespace
import com.netatmo.gitlabplugin.viewmodel.MainWindowViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.awt.Dimension
import javax.swing.BoxLayout
import javax.swing.JPanel
import javax.swing.JScrollPane


class GitlabProjectsWindow : ToolWindowFactory {

    private val contentPanel = JPanel()
    private val listContent = JPanel().apply { layout = BoxLayout(this, BoxLayout.Y_AXIS) }
    private val selectorContent = JPanel()
    private var listScrollPane = JScrollPane(listContent)
    private val viewModel = MainWindowViewModel()


    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val content: Content = ContentFactory.getInstance().createContent(createContentPanel(), "", false)
        toolWindow.contentManager.addContent(content)

        CoroutineScope(Dispatchers.Default).launch {
            viewModel.projectsStateFlow.collectLatest {
                updateView(it)
            }
        }
        CoroutineScope(Dispatchers.Default).launch {
            viewModel.groupStateFlow.collectLatest {
                updateSelector(it)
            }
        }
    }

    private fun createContentPanel(): JPanel {
        contentPanel.layout = BoxLayout(contentPanel, BoxLayout.Y_AXIS)

        val actions = object : ActionGroup() {
            override fun getChildren(e: AnActionEvent?): Array<AnAction> {
                return arrayOf(LoadAction {
                    viewModel.requestProjects()
                })
            }

        }
        val actionToolbar = ActionToolbarImpl(ActionPlaces.TOOLBAR, actions, true)

        actionToolbar.targetComponent = contentPanel
        contentPanel.add(actionToolbar)
        contentPanel.add(selectorContent)
        contentPanel.add(listScrollPane)
        return contentPanel
    }

    private fun updateSelector(groups: Set<ProjectNamespace>) {
        selectorContent.removeAll()
        val comboBox = GroupSelector(groups.map { it.name }.toTypedArray()) {
            viewModel.changeGroup(it)
        }
        // Set the preferred height of the JComboBox
        comboBox.preferredSize = Dimension(200, 30)

        selectorContent.add(comboBox)
    }

    private fun updateView(projects: List<GitlabProject>) {

        listContent.removeAll()

        for (project in projects) {
            listContent.add(getProjectItemView(project))
        }
    }
}