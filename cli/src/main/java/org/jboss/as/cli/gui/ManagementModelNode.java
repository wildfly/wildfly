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

import java.util.ArrayList;
import java.util.List;
import javax.swing.tree.DefaultMutableTreeNode;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * A node in the management tree.  Non-leaves are addressable entities in a DMR command.  Leaves are attributes.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2012 Red Hat Inc.
 */
public class ManagementModelNode extends DefaultMutableTreeNode {

    private CliGuiContext cliGuiCtx;
    private CommandExecutor executor;
    private boolean isLeaf = false;
    private boolean isGeneric = false;

    /**
     * Constructor for root node only.
     */
    public ManagementModelNode(CliGuiContext cliGuiCtx) {
        this.cliGuiCtx = cliGuiCtx;
        this.executor = cliGuiCtx.getExecutor();
        this.isLeaf = false;
        setUserObject(new UserObject());
    }

    private ManagementModelNode(CliGuiContext cliGuiCtx, UserObject userObject) {
        this.cliGuiCtx = cliGuiCtx;
        this.executor = cliGuiCtx.getExecutor();
        this.isLeaf = userObject.isLeaf;
        this.isGeneric = userObject.isGeneric;
        if (isGeneric) setAllowsChildren(false);
        setUserObject(userObject);
    }

    /**
     * Refresh children using read-resource operation.
     */
    public void explore() {
        if (isLeaf) return;
        if (isGeneric) return;
        removeAllChildren();

        try {
            String addressPath = addressPath();
            ModelNode response = executor.doCommand(addressPath + ":read-resource");
            ModelNode result = response.get("result");
            if (!result.isDefined()) return;

            List<String> childrenTypes = getChildrenTypes(addressPath);
            for (ModelNode node : result.asList()) {
                Property prop = node.asProperty();
                if (childrenTypes.contains(prop.getName())) { // resource node
                    if (hasGenericOperations(addressPath, prop.getName())) {
                        add(new ManagementModelNode(cliGuiCtx, new UserObject(node, prop.getName())));
                    }
                    if (prop.getValue().isDefined()) {
                        for (ModelNode innerNode : prop.getValue().asList()) {
                            UserObject usrObj = new UserObject(innerNode, prop.getName(), innerNode.asProperty().getName(), false);
                            add(new ManagementModelNode(cliGuiCtx, usrObj));
                        }
                    }
                } else { // attribute node
                    UserObject usrObj = new UserObject(node, prop.getName(), prop.getValue().asString(), true);
                    add(new ManagementModelNode(cliGuiCtx, usrObj));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean hasGenericOperations(String addressPath, String resourceName) throws Exception {
        ModelNode response = executor.doCommand(addressPath + resourceName + "=*/:read-operation-names");
        if (response.get("outcome").asString().equals("failed")) return false;

        for (ModelNode node : response.get("result").asList()) {
            if (node.asString().equals("add")) return true;
        }

        return false;
    }

    private List<String> getChildrenTypes(String addressPath) throws Exception {
        List<String> childrenTypes = new ArrayList<String>();
        ModelNode readChildrenTypes = executor.doCommand(addressPath + ":read-children-types");
        for (ModelNode type : readChildrenTypes.get("result").asList()) {
            childrenTypes.add(type.asString());
        }
        return childrenTypes;
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

    public boolean isGeneric() {
        return this.isGeneric;
    }

    public static String escapeAddressElement(String element) {
        element = element.replace(":", "\\:");
        element = element.replace("/", "\\/");
        element = element.replace("=", "\\=");
        return element;
    }

    /**
     * Encapsulates name/value pair.  Also encapsulates escaping of the value.
     */
    class UserObject {
        private ModelNode backingNode;
        private String name;
        private String value;
        private boolean isLeaf;
        private boolean isGeneric = false;
        private String separator;

        /**
         * Constructor for the root node.
         */
        public UserObject() {
            this.backingNode = new ModelNode();
            this.name = "/";
            this.value = "";
            this.isLeaf = false;
            this.separator = "";
        }
        /**
         * Constructor for generic folder where resource=*.
         *
         * @param name The name of the resource.
         */
        public UserObject(ModelNode backingNode, String name) {
            this.backingNode = backingNode;
            this.name = name;
            this.value = "*";
            this.isLeaf = false;
            this.isGeneric = true;
            this.separator = "=";
        }

        public UserObject(ModelNode backingNode, String name, String value, boolean isLeaf) {
            this.backingNode = backingNode;
            this.name = name;
            this.value = value;
            this.isLeaf = isLeaf;
            if (isLeaf) {
                this.separator = " => ";
            } else {
                this.separator = "=";
            }
        }

        public ModelNode getBackingNode() {
            return this.backingNode;
        }

        public String getName() {
            return this.name;
        }

        public String getValue() {
            return this.value;
        }

        public String getEscapedValue() {
            return ManagementModelNode.escapeAddressElement(this.value);
        }

        public boolean isLeaf() {
            return this.isLeaf;
        }

        public boolean isGeneric() {
            return this.isGeneric;
        }

        @Override
        public String toString() {
            return this.name + this.separator + this.value;
        }
    }

}