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
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2012 Red Hat Inc.
 */
public class ManagementModelNode extends DefaultMutableTreeNode {

    private CommandExecutor executor;
    private boolean isLeaf = false;

    public ManagementModelNode(CommandExecutor executor, String label, boolean isLeaf) {
        this.executor = executor;
        this.isLeaf = isLeaf;
        setUserObject(label);
    }

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
                        String label = prop.getName() + "=" + innerNode.asProperty().getName() + "/";
                        add(new ManagementModelNode(executor, label, false));
                    }
                } else {
                    add(new ManagementModelNode(executor, node.asString(), true));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String addressPath() {
        Object[] path;
        if (isLeaf) {
            ManagementModelNode parent = (ManagementModelNode)getParent();
            path = parent.getUserObjectPath();
        } else {
            path = getUserObjectPath();
        }

        StringBuilder builder = new StringBuilder();
        for (Object pathElement : path) {
            builder.append(pathElement.toString());
        }
        return builder.toString();
    }

    @Override
    public boolean isLeaf() {
        return this.isLeaf;
    }

}
