package info.novatec.testit.livingdoc.intellij.gui.toolwindows;


import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.Presentation;
import info.novatec.testit.livingdoc.intellij.common.Icons;
import info.novatec.testit.livingdoc.intellij.common.NodeType;
import info.novatec.testit.livingdoc.intellij.domain.ModuleNode;
import info.novatec.testit.livingdoc.intellij.domain.Node;
import info.novatec.testit.livingdoc.intellij.domain.RepositoryNode;
import info.novatec.testit.livingdoc.intellij.domain.SpecificationNode;
import info.novatec.testit.livingdoc.server.domain.DocumentNode;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.swing.tree.DefaultMutableTreeNode;

public class RepositoryViewUtilsTest {

    private boolean hasError;
    private DocumentNode documentNode;
    private Node node;
    private SpecificationNode specificationNode;


    @Before
    public void setUp() {
        documentNode = new DocumentNode("title");
        specificationNode = new SpecificationNode(documentNode, node);
    }

    @Test
    public void getModuleNode() {

        Node childNode;
        ModuleNode parentNode;

        parentNode = new ModuleNode("grandpa", "module");
        childNode = new Node("child", Icons.ERROR, NodeType.ERROR, parentNode);
        node = new Node("grandchild", Icons.ERROR, NodeType.ERROR, childNode);

        Assert.assertNotNull(RepositoryViewUtils.getModuleNode(node));
    }

    @Test
    public void getErrorNode() {
        Assert.assertNotNull(RepositoryViewUtils.getErrorNode("Error message"));
    }

    @Test
    public void getRepositoryNode() {

        RepositoryNode rn1;

        String repoNodeName = "repoNode";
        rn1 = new RepositoryNode(repoNodeName, null);
        SpecificationNode parentSN = new SpecificationNode(documentNode, rn1);
        SpecificationNode childSN = new SpecificationNode(documentNode, parentSN);

        RepositoryNode result = RepositoryViewUtils.getRepositoryNode(childSN);
        Assert.assertNotNull(result);
        Assert.assertEquals(repoNodeName, result.getName());
    }

    @Test
    public void getResultIconWorkingError() {

        hasError = true;
        specificationNode.setUsingCurrentVersion(true);

        Assert.assertEquals(Icons.ERROR_WORKING, RepositoryViewUtils.getResultIcon(hasError, specificationNode));
    }

    @Test
    public void getResultIconWorkingSuccess() {

        hasError = false;
        specificationNode.setUsingCurrentVersion(true);

        Assert.assertEquals(Icons.SUCCESS_WORKING, RepositoryViewUtils.getResultIcon(hasError, specificationNode));
    }

    @Test
    public void getResultIconDiffError() {

        hasError = true;
        specificationNode.setCanBeImplemented(true);

        Assert.assertEquals(Icons.ERROR_DIFF, RepositoryViewUtils.getResultIcon(hasError, specificationNode));
    }

    @Test
    public void getResultIconDiffSuccess() {

        hasError = false;
        specificationNode.setCanBeImplemented(true);

        Assert.assertEquals(Icons.SUCCESS_DIFF, RepositoryViewUtils.getResultIcon(hasError, specificationNode));
    }


    @Test
    public void getNodeIconExeWork() {

        specificationNode.setExecutable(true);
        specificationNode.setUsingCurrentVersion(true);

        Assert.assertEquals(Icons.EXE_WORKING, RepositoryViewUtils.getNodeIcon(specificationNode));
    }

    @Test
    public void getNodeIconExeDiff() {

        specificationNode.setExecutable(true);
        specificationNode.setCanBeImplemented(true);

        Assert.assertEquals(Icons.EXE_DIFF, RepositoryViewUtils.getNodeIcon(specificationNode));
    }

    @Test
    public void getNodeIconFolder() {

        specificationNode.setExecutable(false);

        Assert.assertEquals(AllIcons.Nodes.Folder, RepositoryViewUtils.getNodeIcon(specificationNode));
    }

    @Test
    public void setEnabledForNodeVersion() {

        DefaultMutableTreeNode defaultMutableTreeNode = new DefaultMutableTreeNode(specificationNode);

        DefaultMutableTreeNode[] selectedNodes = new DefaultMutableTreeNode[]{defaultMutableTreeNode};
        DefaultMutableTreeNode[] selectedNodesEmpty = new DefaultMutableTreeNode[]{};

        Presentation presentation = new Presentation("Presentation text");
        boolean toCurrentVersion = true;

        presentation.setEnabled(true);
        specificationNode.setExecutable(true);
        specificationNode.setCanBeImplemented(true);

        RepositoryViewUtils.setEnabledForNodeVersion(selectedNodes, presentation, toCurrentVersion);
        Assert.assertTrue(presentation.isEnabled());

        RepositoryViewUtils.setEnabledForNodeVersion(selectedNodesEmpty, presentation, toCurrentVersion);
        Assert.assertFalse(presentation.isEnabled());

        // Coverage another similar method without boolean parameter
        RepositoryViewUtils.setEnabledForSpecificationNode(selectedNodes, presentation);
        Assert.assertTrue(presentation.isEnabled());
    }
}
