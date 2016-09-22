package info.novatec.testit.livingdoc.intellij.gui.toolwindows;

import com.intellij.execution.testframework.ui.TestStatusLine;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.util.ColorProgressBar;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.ui.PopupHandler;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.SimpleTree;
import info.novatec.testit.livingdoc.intellij.common.I18nSupport;
import info.novatec.testit.livingdoc.intellij.core.ModuleSettings;
import info.novatec.testit.livingdoc.intellij.domain.*;
import info.novatec.testit.livingdoc.intellij.gui.toolwindows.action.ExecuteDocumentAction;
import info.novatec.testit.livingdoc.intellij.gui.toolwindows.action.OpenRemoteDocumentAction;
import info.novatec.testit.livingdoc.intellij.gui.toolwindows.action.SwitchVersionAction;
import info.novatec.testit.livingdoc.intellij.rpc.PluginLivingDocXmlRpcClient;
import info.novatec.testit.livingdoc.server.LivingDocServerException;
import info.novatec.testit.livingdoc.server.domain.DocumentNode;
import info.novatec.testit.livingdoc.server.domain.Repository;
import info.novatec.testit.livingdoc.server.domain.SystemUnderTest;
import org.apache.commons.lang3.StringUtils;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.util.Set;


/**
 * User interface for LivingDoc Repository View.<br>
 *
 * @see SimpleToolWindowPanel
 */
public class ToolWindowPanel extends SimpleToolWindowPanel {

    private static final Logger LOG = Logger.getInstance(ToolWindowPanel.class);

    private final transient Project project;


    private final JBPanel mainContent;
    private final DefaultMutableTreeNode rootNode;
    private DefaultTreeModel treeModel;
    private transient ActionToolbar toolBar;
    private transient DefaultActionGroup actionGroup;
    private SimpleTree tree;
    private TestStatusLine statusLine;


    public ToolWindowPanel(Project project) {
        super(false);

        this.project = project;

        mainContent = new JBPanel(new BorderLayout());
        mainContent.setAutoscrolls(true);
        setContent(mainContent);

        this.rootNode = new DefaultMutableTreeNode(getDefaultRootNode());

        createRepositoryTree();
        createActionToolBar();
        createStatusLine();

        configureActions();

        loadRepositories();
    }

    public SimpleTree getRepositoryTree() {
        return tree;
    }

    public TestStatusLine getStatusLine() {
        return statusLine;
    }

    private void createActionToolBar() {

        ActionManager actionManager = ActionManager.getInstance();
        actionGroup = new DefaultActionGroup(null, true);

        toolBar = actionManager.createActionToolbar("LivingDoc.RepositoryViewToolbar", actionGroup, false);
        toolBar.adjustTheSameSize(true);
        toolBar.setTargetComponent(tree);
        setToolbar(toolBar.getComponent());
    }

    private void createRepositoryTree() {

        tree = new SimpleTree();
        tree.setCellRenderer(new LDTreeCellRenderer());
        tree.setRootVisible(true);

        // Basic functionality with single selection, desired multiple selection.
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        treeModel = new DefaultTreeModel(rootNode, true);
        tree.setModel(treeModel);

        JBScrollPane scrollPane = new JBScrollPane(tree);
        mainContent.add(scrollPane, BorderLayout.CENTER);
    }

    private void createStatusLine() {

        statusLine = new TestStatusLine();
        statusLine.setPreferredSize(false);
        resetStatusLine();
        mainContent.add(statusLine, BorderLayout.NORTH);
    }

    private void resetStatusLine() {
        statusLine.setText("");
        statusLine.setStatusColor(ColorProgressBar.GREEN);
        statusLine.setFraction(0d);
    }

    private void resetTree(Node newRootNode) {
        rootNode.removeAllChildren();
        rootNode.setUserObject(newRootNode);

        resetStatusLine();
    }

    private void configureActions() {

        createExecuteDocumentAction();
        actionGroup.addSeparator();
        createVersionSwitcherAction();
        actionGroup.addSeparator();
        createOpenDocumentAction();
        actionGroup.addSeparator();
        createRefreshRepositoryAction();

        toolBar.updateActionsImmediately();

        // Context menu with the plugin actions.
        getRepositoryTree().addMouseListener(new PopupHandler() {

            @Override
            public void invokePopup(final Component comp, final int x, final int y) {

                ActionPopupMenu actionPopupMenu = ActionManager.getInstance().createActionPopupMenu("LivingDoc.RepositoryViewToolbar", actionGroup);
                actionPopupMenu.getComponent().show(comp, x, y);
            }
        });
    }

    private void createVersionSwitcherAction() {
        SwitchVersionAction implementedVersionAction = new SwitchVersionAction(tree, false);
        actionGroup.add(implementedVersionAction);

        // Current version
        SwitchVersionAction workingsVersionAction = new SwitchVersionAction(tree, true);
        actionGroup.add(workingsVersionAction);
    }

    private void createOpenDocumentAction() {
        OpenRemoteDocumentAction openRemoteDocumentAction = new OpenRemoteDocumentAction(tree);
        actionGroup.add(openRemoteDocumentAction);
    }

    private void createExecuteDocumentAction() {
        ExecuteDocumentAction executeDocumentAction = new ExecuteDocumentAction(this, false);
        actionGroup.add(executeDocumentAction);

        // With debug mode
        ExecuteDocumentAction debugDocumentAction = new ExecuteDocumentAction(this, true);
        actionGroup.add(debugDocumentAction);
    }

    private void createRefreshRepositoryAction() {

        AnAction anAction = new AnAction() {

            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {

                resetTree(getDefaultRootNode());

                loadRepositories();
            }
        };
        anAction.getTemplatePresentation().setIcon(AllIcons.Actions.Refresh);
        anAction.getTemplatePresentation().setDescription(I18nSupport.getValue("toolwindows.action.refresh.tooltip"));
        anAction.getTemplatePresentation().setText(I18nSupport.getValue("toolwindows.action.refresh.tooltip"));
        actionGroup.add(anAction);
    }

    private void loadRepositories() {

        PluginLivingDocXmlRpcClient service = new PluginLivingDocXmlRpcClient(project);

        for (Module module : ModuleManager.getInstance(project).getModules()) {

            ModuleSettings moduleSettings = ModuleSettings.getInstance(module);
            if (moduleSettings.isLivingDocEnabled()) {

                ModuleNode moduleNode = new ModuleNode(
                        module.getName() + " [" + moduleSettings.getSud() + "]",
                        module.getName());
                DefaultMutableTreeNode moduleTreeNode = new DefaultMutableTreeNode(moduleNode);
                rootNode.add(moduleTreeNode);

                SystemUnderTest systemUnderTest = SystemUnderTest.newInstance(moduleSettings.getSud());
                systemUnderTest.setProject(info.novatec.testit.livingdoc.server.domain.Project.newInstance(moduleSettings.getProject()));

                try {
                    Set<Repository> repositories = service.getAllRepositoriesForSystemUnderTest(systemUnderTest);

                    for (Repository repository : repositories) {

                        RepositoryNode repositoryNode;

                        if (validateCredentials(repository, moduleSettings)) {
                            repositoryNode = new RepositoryNode(repository.getProject().getName(), moduleNode);
                            repositoryNode.setRepository(repository);
                            DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(repositoryNode);
                            moduleTreeNode.add(childNode);

                            DocumentNode documentNode = service.getSpecificationHierarchy(repository, systemUnderTest);
                            paintDocumentNode(documentNode.getChildren(), childNode);

                        } else {
                            moduleTreeNode.add(new DefaultMutableTreeNode(getErrorNode(I18nSupport.getValue("toolwindows.error.credentials"))));
                        }
                    }
                } catch (LivingDocServerException ldse) {
                    LOG.error(ldse);
                    resetTree(getErrorNode(I18nSupport.getValue("toolwindows.error.loading.repositories")
                            + ldse.getMessage()));
                }
            }
        }
        treeModel.reload();
    }

    private Node getErrorNode(final String descError) {
        return new Node(descError, AllIcons.Nodes.ErrorIntroduction, NodeType.ERROR, null);
    }

    private Node getDefaultRootNode() {
        return new Node(project.getName() /*+ " [" + ldProject.getSystemUnderTest().getName() + "]"*/,
                AllIcons.Nodes.Project, NodeType.PROJECT, null);
    }

    /**
     * @param childNode  {@link DocumentNode}
     * @param userObject {@link Node}
     * @return {@link SpecificationNode}
     */
    private SpecificationNode convertDocumentNodeToLDNode(final DocumentNode childNode, final Node userObject) {

        SpecificationNode specificationNode = new SpecificationNode(childNode, userObject);
        specificationNode.setIcon(RepositoryViewUtils.getNodeIcon(specificationNode));
        return specificationNode;
    }

    /**
     * This recursive method adds a node into the repository tree.<br>
     * Only the executable nodes or nodes with children will be painted.
     *
     * @param children   {@link java.util.List}
     * @param parentNode {@link DefaultMutableTreeNode} Parent node of children nodes indicated in the first parameter.
     * @see DocumentNode
     */
    private void paintDocumentNode(java.util.List<DocumentNode> children, DefaultMutableTreeNode parentNode) {

        children.stream().filter(child -> child.isExecutable() || (!child.isExecutable() && child.hasChildren())).forEach(child -> {

            SpecificationNode ldNode = convertDocumentNodeToLDNode(child, (Node) parentNode.getUserObject());
            DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(ldNode);
            parentNode.add(childNode);

            if (child.hasChildren()) {
                paintDocumentNode(child.getChildren(), childNode);
            }
        });
    }

    /**
     * Validates the LivingDoc user and password configured in IntelliJ to connect with LivingDoc repository.
     *
     * @param repository {@link Repository}
     * @return True if the credentials are valid. Otherwise, false.
     */
    private boolean validateCredentials(final Repository repository, final ModuleSettings moduleSettings) {

        boolean result = true;

        if (!StringUtils.equals(moduleSettings.getUser(), repository.getUsername())
                || !StringUtils.equals(moduleSettings.getPassword(), repository.getPassword())) {

            result = false;
        }
        return result;
    }
}
