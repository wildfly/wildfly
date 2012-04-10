/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.cli.gui;

import org.jboss.as.cli.gui.charts.ChartMenu;
import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import org.jboss.as.cli.gui.ManagementModelNode.AttributeProps;
import org.jboss.as.cli.gui.ManagementModelNode.UserObject;

/**
 * This class contains a JTree view of the management model that allows you to build commands by
 * clicking nodes and operations.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2012 Red Hat Inc.
 */
public class ManagementModel extends JPanel {

    private CliGuiContext cliGuiCtx;

    public ManagementModel(CliGuiContext cliGuiCtx) {
        this.cliGuiCtx = cliGuiCtx;
        setLayout(new BorderLayout(10,10));
        add(new JLabel("Right-click a node to choose an operation.  Close/Open a folder to refresh.  Hover for help."), BorderLayout.NORTH);
        add(makeTree(), BorderLayout.CENTER);
    }

    private JTree makeTree() {
        ManagementModelNode root = new ManagementModelNode(cliGuiCtx);
        root.explore();
        JTree tree = new CommandBuilderTree(cliGuiCtx, new DefaultTreeModel(root));
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.addTreeExpansionListener(new ManagementTreeExpansionListener((DefaultTreeModel) tree.getModel()));
        tree.addTreeSelectionListener(new ManagementTreeSelectionListener());
        tree.addMouseListener(new ManagementTreeMouseListener(tree));
        return tree;
    }

    /**
     * Listener that populates (or refreshes) the children when a node is expanded.
     */
    private class ManagementTreeExpansionListener implements TreeExpansionListener {
        private DefaultTreeModel treeModel;

        public ManagementTreeExpansionListener(DefaultTreeModel treeModel) {
            this.treeModel = treeModel;
        }

        public void treeCollapsed(TreeExpansionEvent tee) {
            // do nothing
        }

        public void treeExpanded(TreeExpansionEvent tee) {
            ManagementModelNode node = (ManagementModelNode) tee.getPath().getLastPathComponent();
            node.explore();
            treeModel.nodeStructureChanged(node);
        }
    }

    /**
     * Listener that populates the command line with the address of the selected node.
     */
    private class ManagementTreeSelectionListener implements TreeSelectionListener {

        public void valueChanged(TreeSelectionEvent tse) {
            ManagementModelNode selected = (ManagementModelNode) tse.getPath().getLastPathComponent();
            cliGuiCtx.getCommandLine().getCmdText().setText(selected.addressPath());
        }
    }

    /**
     * Listener that triggers the operationMenu menu containing operations.
     */
    private class ManagementTreeMouseListener extends MouseAdapter {

        private JTree tree;
        private OperationMenu operationMenu;
        private ChartMenu graphingMenu;

        public ManagementTreeMouseListener(JTree tree) {
            this.tree = tree;
            this.operationMenu = new OperationMenu(cliGuiCtx, tree);
            this.graphingMenu = new ChartMenu(cliGuiCtx, tree);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (!e.isPopupTrigger()) return;
            showPopup(e);
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (!e.isPopupTrigger()) return;
            showPopup(e);
        }

        private void showPopup(MouseEvent e) {
            int selRow = tree.getRowForLocation(e.getX(), e.getY());
            if (selRow == -1) return;

            TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
            tree.setSelectionPath(selPath);

            ManagementModelNode node = (ManagementModelNode)selPath.getLastPathComponent();

            UserObject usrObj = (UserObject)node.getUserObject();
            AttributeProps attrDesc = usrObj.getAttributeProps();
            if ((attrDesc != null) && attrDesc.isGraphable()) {
                graphingMenu.show(node, e.getX(), e.getY());
            } else {
                operationMenu.show(node, e.getX(), e.getY());
            }
        }
    }
}
