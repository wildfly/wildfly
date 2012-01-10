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

import javax.swing.tree.DefaultMutableTreeNode;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;

/**
 * A node in the management tree.  Non-leaves are addressable entities in a DMR command.  Leaves are attributes.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2012 Red Hat Inc.
 */
public class ManagementModelNode extends DefaultMutableTreeNode {

    private CommandExecutor executor;
    private boolean isLeaf = false;

    public ManagementModelNode(String label, boolean isLeaf) {
        this.executor = GuiMain.getExecutor();
        this.isLeaf = isLeaf;
        setUserObject(label);
    }

    /**
     * Refresh children using read-resource operation.
     */
    public void explore() {
        if (isLeaf) return;
        removeAllChildren();

        try {
            ModelNode result = executor.doCommand(addressPath() + ":read-resource");
            for (ModelNode node : result.get("result").asList()) {
                Property prop = node.asProperty();
                ModelType valueType = prop.getValue().getType();
                if (valueType == ModelType.OBJECT) {
                    for (ModelNode innerNode : prop.getValue().asList()) {
                        String label = prop.getName() + "=" + innerNode.asProperty().getName();
                        add(new ManagementModelNode(label, false));
                    }
                } else {
                    add(new ManagementModelNode(prop.getName() + " => " + prop.getValue().asString(), true));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Get the DMR path for this node.  For leaves, the DMR path is the path of its parent.
     * @return The DMR path for this node.
     */
    public String addressPath() {
        if (isLeaf) {
            ManagementModelNode parent = (ManagementModelNode)getParent();
            return parent.addressPath();
        }

        Object[] path = getUserObjectPath();
        StringBuilder builder = new StringBuilder("/"); // start with root
        for (Object pathElement : path) {
            String pathElementStr = pathElement.toString();
            if (pathElementStr.equals("/")) continue; // don't want to escape root

            String leftSide = pathElementStr.substring(0, pathElementStr.indexOf('=') + 1);
            String rightSide = pathElementStr.substring(pathElementStr.indexOf('=') + 1);

            // Colon, forward slash, & equals mess up parser.  Make them literal.
            rightSide = rightSide.replace(":", "\\:");
            rightSide = rightSide.replace("/", "\\/");
            rightSide = rightSide.replace("=", "\\=");

            builder.append(leftSide);
            builder.append(rightSide);
            builder.append("/");
        }

        return builder.toString();
    }

    @Override
    public boolean isLeaf() {
        return this.isLeaf;
    }

}