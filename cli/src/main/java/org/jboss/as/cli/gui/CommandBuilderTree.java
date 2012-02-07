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

import java.awt.event.MouseEvent;
import javax.swing.JTree;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import org.jboss.as.cli.gui.ManagementModelNode.UserObject;
import org.jboss.dmr.ModelNode;

/**
 * JTree that knows how to find context-sensitive help and display as ToolTip for
 * each node.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2012 Red Hat Inc.
 */
public class CommandBuilderTree extends JTree {
    private ManagementModelNode currentNode = null;
    private String currentDescription = null;

    public CommandBuilderTree(TreeModel model) {
        super(model);
        setToolTipText(""); // enables toolTip system for this tree
    }

    @Override
    public synchronized String getToolTipText(MouseEvent me) {
        if (getRowForLocation(me.getX(), me.getY()) == -1) {
            currentNode = null;
            currentDescription = null;
            return null;
        }

        TreePath treePath = getPathForLocation(me.getX(), me.getY());
        ManagementModelNode node = (ManagementModelNode)treePath.getLastPathComponent();

        // don't read description again when mouse is moved within the same node
        if (node == currentNode) return currentDescription;

        currentNode = node;
        currentDescription = null;

        try {
            ModelNode readResource = GuiMain.getExecutor().doCommand(node.addressPath() + ":read-resource-description");
            UserObject usrObj = (UserObject)node.getUserObject();
            if (node.isGeneric()) {
                currentDescription = "Used for generic operations on " + usrObj.getName() + ", such as 'add'";
            } else if (!node.isLeaf()) {
                currentDescription = readResource.get("result", "description").asString();
            } else {
                ModelNode description = readResource.get("result", "attributes", usrObj.getName(), "description");
                if (description.isDefined()) {
                    currentDescription = description.asString();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return currentDescription;
    }

}
