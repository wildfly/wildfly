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

    /**
     * Constructor for root node only.
     */
    public ManagementModelNode() {
        this.executor = GuiMain.getExecutor();
        this.isLeaf = false;
        setUserObject("/");
    }

    private ManagementModelNode(UserObject userObject) {
        this.executor = GuiMain.getExecutor();
        this.isLeaf = userObject.isLeaf;
        setUserObject(userObject);
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
                        UserObject usrObj = new UserObject(prop.getName(), innerNode.asProperty().getName(), false);
                        add(new ManagementModelNode(usrObj));
                    }
                } else {
                    UserObject usrObj = new UserObject(prop.getName(), prop.getValue().asString(), true);
                    add(new ManagementModelNode(usrObj));
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

        StringBuilder builder = new StringBuilder("/"); // start with root
        for (Object pathElement : getUserObjectPath()) {
            String pathElementStr = pathElement.toString();
            if (pathElementStr.equals("/")) continue; // don't want to escape root

            UserObject userObj = (UserObject)pathElement;
            builder.append(userObj.getName());
            builder.append("=");
            builder.append(userObj.getEscapedValue());
            builder.append("/");
        }

        return builder.toString();
    }

    @Override
    public boolean isLeaf() {
        return this.isLeaf;
    }

    /**
     * Encapsulates name/value pair.  Also encapsulates escaping of the value.
     */
    class UserObject {
        private String name;
        private String value;
        private boolean isLeaf;
        private String seperator;

        public UserObject(String name, String value, boolean isLeaf) {
            this.name = name;
            this.value = value;
            this.isLeaf = isLeaf;
            if (isLeaf) {
                this.seperator = " => ";
            } else {
                this.seperator = "=";
            }
        }

        public String getName() {
            return this.name;
        }

        public String getValue() {
            return this.value;
        }

        public String getEscapedValue() {
            String escapedVal = this.value;
            escapedVal = escapedVal.replace(":", "\\:");
            escapedVal = escapedVal.replace("/", "\\/");
            escapedVal = escapedVal.replace("=", "\\=");
            return escapedVal;
        }

        public boolean isLeaf() {
            return this.isLeaf;
        }

        @Override
        public String toString() {
            return this.name + this.seperator + this.value;
        }
    }

}