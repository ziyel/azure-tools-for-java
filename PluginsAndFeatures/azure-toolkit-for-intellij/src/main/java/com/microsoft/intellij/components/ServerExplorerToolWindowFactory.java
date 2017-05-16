/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.microsoft.intellij.components;

import com.intellij.ide.util.treeView.NodeRenderer;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import com.microsoft.azure.hdinsight.common.HDInsightUtil;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.ijidea.actions.AzureSignInAction;
import com.microsoft.azuretools.ijidea.actions.SelectSubscriptionsAction;
import com.microsoft.intellij.AzurePlugin;
import com.microsoft.intellij.helpers.UIHelperImpl;
import com.microsoft.intellij.serviceexplorer.azure.AzureModuleImpl;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.collections.ListChangeListener;
import com.microsoft.tooling.msservices.helpers.collections.ListChangedEvent;
import com.microsoft.tooling.msservices.helpers.collections.ObservableList;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeAction;
import com.microsoft.tooling.msservices.serviceexplorer.RefreshableNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureModule;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ServerExplorerToolWindowFactory implements ToolWindowFactory, PropertyChangeListener {
    public static final String EXPLORER_WINDOW = "Azure Explorer";

    private Map<Project, DefaultTreeModel> treeModelMap = new HashMap<>();

    @Override
    public void createToolWindowContent(@NotNull final Project project, @NotNull final ToolWindow toolWindow) {
        // initialize azure service module
        AzureModule azureModule = new AzureModuleImpl(project);

        HDInsightUtil.setHDInsightRootModule(azureModule);

        // initialize with all the service modules
        DefaultTreeModel treeModel = new DefaultTreeModel(initRoot(project, azureModule));
        treeModelMap.put(project, treeModel);

        // initialize tree
        final JTree tree = new Tree(treeModel);
        tree.setRootVisible(false);
        tree.setCellRenderer(new NodeTreeCellRenderer());
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        // add a click handler for the tree
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                treeMousePressed(e, tree);
            }
        });

        // add the tree to the window
        toolWindow.getComponent().add(new JBScrollPane(tree));

        // setup toolbar icons
        addToolbarItems(toolWindow, azureModule);

//        try {
//            azureModule.registerSubscriptionsChanged();
//        } catch (AzureCmdException ignored) {
//        }
    }

    private DefaultMutableTreeNode initRoot(Project project, AzureModule azureModule) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode();

        // add the azure service root service module
        root.add(createTreeNode(azureModule, project));

        // kick-off asynchronous load of child nodes on all the modules
        azureModule.load(false);

        return root;
    }

    private void treeMousePressed(MouseEvent e, JTree tree) {
        // get the tree node associated with this mouse click
        TreePath treePath = tree.getPathForLocation(e.getX(), e.getY());
        if (treePath == null)
            return;

        DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) treePath.getLastPathComponent();
        Node node = (Node) treeNode.getUserObject();

        // delegate click to the node's click action if this is a left button click
        if (SwingUtilities.isLeftMouseButton(e)) {
            // if the node in question is in a "loading" state then we
            // do not propagate the click event to it
            if (!node.isLoading()) {
                node.getClickAction().fireNodeActionEvent();
            }
        }
        // for right click show the context menu populated with all the
        // actions from the node
        else if (SwingUtilities.isRightMouseButton(e) || e.isPopupTrigger()) {
            if (node.hasNodeActions()) {
                // select the node which was right-clicked
                tree.getSelectionModel().setSelectionPath(treePath);

                JPopupMenu menu = createPopupMenuForNode(node);
                menu.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }

    private JPopupMenu createPopupMenuForNode(Node node) {
        JPopupMenu menu = new JPopupMenu();

        for (final NodeAction nodeAction : node.getNodeActions()) {
            JMenuItem menuItem = new JMenuItem(nodeAction.getName());
//            menuItem.setIconTextGap(16);
            menuItem.setEnabled(nodeAction.isEnabled());
            if (nodeAction.getIconPath() != null) {
                menuItem.setIcon(UIHelperImpl.loadIcon(nodeAction.getIconPath()));
            }
            // delegate the menu item click to the node action's listeners
            menuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    nodeAction.fireNodeActionEvent();
                }
            });

            menu.add(menuItem);
        }

        return menu;
    }

    private DefaultMutableTreeNode createTreeNode(Node node, Project project) {
        DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(node, true);

        // associate the DefaultMutableTreeNode with the Node via it's "viewData"
        // property; this allows us to quickly retrieve the DefaultMutableTreeNode
        // object associated with a Node
        node.setViewData(treeNode);

        // listen for property change events on the node
        node.addPropertyChangeListener(this);

        // listen for structure changes on the node, i.e. when child nodes are
        // added or removed
        node.getChildNodes().addChangeListener(new NodeListChangeListener(treeNode, project));

        // create child tree nodes for each child node
        if (node.hasChildNodes()) {
            for (Node childNode : node.getChildNodes()) {
                treeNode.add(createTreeNode(childNode, project));
            }
        }

        return treeNode;
    }

    private void removeEventHandlers(Node node) {
        node.removePropertyChangeListener(this);

        ObservableList<Node> childNodes = node.getChildNodes();
        childNodes.removeAllChangeListeners();

        if (node.hasChildNodes()) {
            // this remove call should cause the NodeListChangeListener object
            // registered on it's child nodes to fire which should recursively
            // clean up event handlers on it's children
            node.removeAllChildNodes();
        }
    }

    @Override
    public void propertyChange(final PropertyChangeEvent evt) {
        // if we are not running on the dispatch thread then switch
        // to dispatch thread
        if (!ApplicationManager.getApplication().isDispatchThread()) {
            ApplicationManager.getApplication().invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    propertyChange(evt);
                }
            }, ModalityState.any());

            return;
        }

        // this event is fired whenever a property on a node in the
        // model changes; we respond by triggering a node change
        // event in the tree's model
        Node node = (Node) evt.getSource();

        // the treeModel object can be null before it is initialized
        // from createToolWindowContent; we ignore property change
        // notifications till we have a valid model object
        DefaultTreeModel treeModel = treeModelMap.get(node.getProject());
        if (treeModel != null) {
            treeModel.nodeChanged((TreeNode) node.getViewData());
        }
    }

    private class NodeListChangeListener implements ListChangeListener {
        private DefaultMutableTreeNode treeNode;
        private Project project;

        NodeListChangeListener(DefaultMutableTreeNode treeNode, Project project) {
            this.treeNode = treeNode;
            this.project = project;
        }

        @Override
        public void listChanged(final ListChangedEvent e) {
            // if we are not running on the dispatch thread then switch
            // to dispatch thread
            if (!ApplicationManager.getApplication().isDispatchThread()) {
                ApplicationManager.getApplication().invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        listChanged(e);
                    }
                }, ModalityState.any());

                return;
            }

            switch (e.getAction()) {
                case add:
                    // create child tree nodes for the new nodes
                    for (Node childNode : (Collection<Node>) e.getNewItems()) {
                        treeNode.add(createTreeNode(childNode, project));
                    }
                    break;
                case remove:
                    // unregister all event handlers recursively and remove
                    // child nodes from the tree
                    for (Node childNode : (Collection<Node>) e.getOldItems()) {
                        removeEventHandlers(childNode);

                        // remove this node from the tree
                        treeNode.remove((MutableTreeNode) childNode.getViewData());
                    }
                    break;
            }
            if (treeModelMap.get(project)!= null) {
                treeModelMap.get(project).reload(treeNode);
            }
        }
    }

    private class NodeTreeCellRenderer extends NodeRenderer {
        @Override
        protected void doPaint(Graphics2D g) {
            super.doPaint(g);
            setOpaque(false);
        }

        @Override
        public void customizeCellRenderer(JTree jTree,
                                          Object value,
                                          boolean selected,
                                          boolean expanded,
                                          boolean isLeaf,
                                          int row,
                                          boolean focused) {
            super.customizeCellRenderer(jTree, value, selected, expanded, isLeaf, row, focused);

            // if the node has an icon set then we use that
            DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) value;
            Node node = (Node) treeNode.getUserObject();

            // "node" can be null if it's the root node which we keep hidden to simulate
            // a multi-root tree control
            if (node == null) {
                return;
            }

            String iconPath = node.getIconPath();
            if (iconPath != null && !iconPath.isEmpty()) {
                setIcon(UIHelperImpl.loadIcon(iconPath));
            }

            // setup a tooltip
            setToolTipText(node.getToolTip());
        }
    }

    private void addToolbarItems(ToolWindow toolWindow, final AzureModule azureModule) {
        if (toolWindow instanceof ToolWindowEx) {
            ToolWindowEx toolWindowEx = (ToolWindowEx) toolWindow;
            try {
                Runnable forceRefreshTitleActions = () -> {
                    try {
                        toolWindowEx.setTitleActions(
                            new AnAction("Refresh", "Refresh Azure Nodes List", null) {
                                @Override
                                public void actionPerformed(AnActionEvent event) {
                                    azureModule.load(true);
                                }

                                @Override
                                public void update(AnActionEvent e) {
                                    boolean isDarkTheme = DefaultLoader.getUIHelper().isDarkTheme();
                                    e.getPresentation().setIcon(UIHelperImpl.loadIcon(isDarkTheme ?
                                        RefreshableNode.REFRESH_ICON_DARK : RefreshableNode.REFRESH_ICON_LIGHT));
                                }
                            },
                            new AzureSignInAction(),
                            new SelectSubscriptionsAction());
                    } catch (Exception e) {
                        AzurePlugin.log(e.getMessage(), e);
                    }
                };
                AuthMethodManager.getInstance().addSignInEventListener(forceRefreshTitleActions);
                AuthMethodManager.getInstance().addSignOutEventListener(forceRefreshTitleActions);
                forceRefreshTitleActions.run();
            } catch (Exception e) {
                AzurePlugin.log(e.getMessage(), e);
            }
        }
    }
}