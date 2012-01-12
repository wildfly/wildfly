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

import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.JTree;
import org.jboss.dmr.ModelNode;

/**
 * JPopupMenu that selects the available operations for a node address.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2012 Red Hat Inc.
 */
public class OperationMenu extends JPopupMenu {
    private static final String[] genericOps = {"add", "read-operation-description", "read-resource-description", "read-operation-names"};
    private static final List<String> genericOpList = Arrays.asList(genericOps);
    private CommandExecutor executor;
    private JTree invoker;
    private JTextField cmdText;

    public OperationMenu(CommandExecutor executor, JTree invoker, JTextField cmdText) {
        this.executor = executor;
        this.invoker = invoker;
        this.cmdText = cmdText;
        setLightWeightPopupEnabled(true);
        setOpaque(true);
    }

    /**
     * Show the OperationMenu based on the selected node.
     * @param node The selected node.
     * @param x The x position of the selection.
     * @param y The y position of the selection.
     */
    public void show(ManagementModelNode node, int x, int y) {
        removeAll();
        String addressPath = node.addressPath();
        try {
            ModelNode  opNames = executor.doCommand(addressPath + ":read-operation-names");
            if (opNames.get("outcome").asString().equals("failed")) return;

            for (ModelNode name : opNames.get("result").asList()) {
                String strName = name.asString();
                if (node.isGeneric() && !genericOpList.contains(strName)) continue;
                if (node.isLeaf() && !strName.equals("write-attribute")) continue;
                ModelNode opDescription = getResourceDescription(addressPath, strName);
                add(new OperationAction(node, strName, opDescription));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        super.show(invoker, x, y);
    }

    private ModelNode getResourceDescription(String addressPath, String name) {
        try {
            return executor.doCommand(addressPath + ":read-operation-description(name=\"" + name + "\")");
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Action for a menu selection.  For operations with params, display an Operation Dialog.  For operations
     * without params, just construct the operation and set the command line.
     */
    private class OperationAction extends AbstractAction {

        private ManagementModelNode node;
        private String opName;
        private String addressPath;
        private ModelNode opDescription;
        private String strDescription; // help text

        public OperationAction(ManagementModelNode node, String opName, ModelNode opDescription) {
            super(opName);
            this.node = node;
            this.opName = opName;
            this.addressPath = node.addressPath();
            this.opDescription = opDescription;

            if (opDescription != null) {
                strDescription = opDescription.get("result", "description").asString();
                putValue(Action.SHORT_DESCRIPTION, strDescription);
            }
        }

        public void actionPerformed(ActionEvent ae) {
            ModelNode requestProperties = opDescription.get("result", "request-properties");
            if ((requestProperties == null) || (!requestProperties.isDefined()) || requestProperties.asList().isEmpty()) {
                cmdText.setText(addressPath + ":" + opName);
                cmdText.requestFocus();
                return;
            }

            OperationDialog dialog = new OperationDialog(node, opName, strDescription, requestProperties);
            dialog.setLocationRelativeTo(GuiMain.getFrame());
            dialog.setVisible(true);
        }

    }
}
