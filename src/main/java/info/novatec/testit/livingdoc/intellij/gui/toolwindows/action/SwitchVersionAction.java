package info.novatec.testit.livingdoc.intellij.gui.toolwindows.action;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.ui.treeStructure.SimpleTree;
import info.novatec.testit.livingdoc.intellij.domain.Node;
import info.novatec.testit.livingdoc.intellij.domain.NodeType;
import info.novatec.testit.livingdoc.intellij.domain.SpecificationNode;
import info.novatec.testit.livingdoc.intellij.gui.toolwindows.RepositoryViewUtils;
import info.novatec.testit.livingdoc.intellij.util.I18nSupport;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 * Version switcher between current and implemented version.<br>
 * Updates the selected node attributes.
 * See {@link #update(AnActionEvent)} for the display restrictions.
 *
 * @see SpecificationNode
 */
public class SwitchVersionAction extends AnAction {

    private final SimpleTree repositoryTree;
    private final boolean toCurrentVersion;

    /**
     * Creates the action with its text, description and icon.
     *
     * @param tree             LivingDoc repository tree.
     * @param toWorkingVersion true for current/working version. False otherwise.
     */
    public SwitchVersionAction(final SimpleTree tree, final boolean toWorkingVersion) {

        super();

        this.repositoryTree = tree;
        this.toCurrentVersion = toWorkingVersion;

        String text;
        Icon icon;

        if (toCurrentVersion) {
            text = I18nSupport.getValue("repository.view.action.switch.working.tooltip");
            icon = AllIcons.Actions.NewFolder;

        } else {
            text = I18nSupport.getValue("repository.view.action.switch.implemented.tooltip");
            icon = AllIcons.Actions.Module;
        }
        Presentation presentation = getTemplatePresentation();
        presentation.setText(text);
        presentation.setDescription(text);
        presentation.setIcon(icon);
    }

    /**
     * Action handler.
     *
     * @param actionEvent Carries information on the invocation place
     */
    @Override
    public void actionPerformed(AnActionEvent actionEvent) {

        DefaultMutableTreeNode[] nodes = repositoryTree.getSelectedNodes(DefaultMutableTreeNode.class, null);

        Object userObject = nodes[0].getUserObject();

        if (((Node) userObject).getType() == NodeType.SPECIFICATION) {

            SpecificationNode specificationNode = (SpecificationNode) userObject;
            specificationNode.setUsingCurrentVersion(toCurrentVersion);
            specificationNode.setIcon(RepositoryViewUtils.getNodeIcon(specificationNode));
            repositoryTree.getSelectionModel().clearSelection();
        }
    }

    /**
     * This action will be enabled only for executable nodes that can be implemented.
     *
     * @param actionEvent Carries information on the invocation place
     */
    @Override
    public void update(AnActionEvent actionEvent) {

        super.update(actionEvent);

        DefaultMutableTreeNode[] selectedNodes = repositoryTree.getSelectedNodes(DefaultMutableTreeNode.class, null);
        RepositoryViewUtils.setEnabledForNodeVersion(selectedNodes, actionEvent.getPresentation(), toCurrentVersion);
    }
}
