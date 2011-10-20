/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.appclient.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CRITERIA;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.ee.subsystem.GlobalModulesDefinition;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.ModuleIdentifier;

/**
 * Class that contains the static application client server configuration
 *
 * @author Stuart Douglas
 */
class AppClientServerConfiguration {

    private AppClientServerConfiguration() {
    }

    public static List<ModelNode> serverConfiguration(final String filePath, final String deploymentName, final String globalModules, final String hostUrl, final List<String> parameters) {
        List<ModelNode> ret = new ArrayList<ModelNode>();
        appclient(ret, filePath, deploymentName, hostUrl, parameters);
        interfaces(ret);
        socketBindings(ret);
        transactions(ret);
        naming(ret);
        ee(ret, globalModules);
        ejb3(ret);
        security(ret);
        jacorb(ret);

        return ret;
    }

    private static void appclient(List<ModelNode> nodes, final String filePath, final String deploymentName, final String hostUrl, final List<String> parameters) {
        loadExtension(nodes, "org.jboss.as.appclient");
        ModelNode add = new ModelNode();
        add.get(OP_ADDR).set(new ModelNode().setEmptyList()).add(SUBSYSTEM, "appclient");
        add.get(OP).set(ADD);
        add.get(Constants.FILE).set(filePath);
        if (deploymentName != null) {
            add.get(Constants.DEPLOYMENT).set(deploymentName);
        }
        if (parameters.isEmpty()) {
            add.get(Constants.PARAMETERS).addEmptyList();
        } else {
            for (String param : parameters) {
                add.get(Constants.PARAMETERS).add(param);
            }
        }
        add.get(Constants.HOST_URL).set(hostUrl);
        nodes.add(add);
    }

    private static void naming(List<ModelNode> nodes) {
        loadExtension(nodes, "org.jboss.as.naming");
        ModelNode add = new ModelNode();
        add.get(OP_ADDR).set(new ModelNode().setEmptyList()).add(SUBSYSTEM, "naming");
        add.get(OP).set(ADD);
        nodes.add(add);
    }

    private static void ee(List<ModelNode> nodes, final String globalModules) {
        ModelNode add = new ModelNode();
        final List<ModuleIdentifier> identifiers = new ArrayList<ModuleIdentifier>();
        if (globalModules != null && !globalModules.isEmpty()) {

            ModelNode globalModulesNode = new ModelNode();
            final String[] modules = globalModules.split(",");
            for (final String module : modules) {
                final ModuleIdentifier identifier = ModuleIdentifier.fromString(module);
                final ModelNode node = new ModelNode();
                node.get(GlobalModulesDefinition.NAME).set(identifier.getName());
                node.get(GlobalModulesDefinition.SLOT).set(identifier.getSlot());
                globalModulesNode.add(node);
            }
            add.get(GlobalModulesDefinition.GLOBAL_MODULES).set(globalModulesNode);
        }

        loadExtension(nodes, "org.jboss.as.ee");
        add.get(OP_ADDR).set(new ModelNode().setEmptyList()).add(SUBSYSTEM, "ee");
        add.get(OP).set(ADD);
        add.get("appclient").set(true);
        nodes.add(add);
    }

    private static void ejb3(List<ModelNode> nodes) {
        loadExtension(nodes, "org.jboss.as.ejb3");
        ModelNode add = new ModelNode();
        add.get(OP_ADDR).set(new ModelNode().setEmptyList()).add(SUBSYSTEM, "ejb3");
        add.get(OP).set(ADD);
        add.get("appclient").set(true);
        nodes.add(add);
    }

    private static void security(List<ModelNode> nodes) {
        loadExtension(nodes, "org.jboss.as.security");
        ModelNode add = new ModelNode();
        add.get(OP_ADDR).set(new ModelNode().setEmptyList()).add(SUBSYSTEM, "security");
        add.get(OP).set(ADD);
        nodes.add(add);
    }

    private static void jacorb(List<ModelNode> nodes) {
        loadExtension(nodes, "org.jboss.as.jacorb");
        ModelNode add = new ModelNode();
        add.get(OP_ADDR).set(new ModelNode().setEmptyList()).add(SUBSYSTEM, "jacorb");
        add.get(OP).set(ADD);
        nodes.add(add);
    }

    private static void transactions(List<ModelNode> nodes) {
        loadExtension(nodes, "org.jboss.as.transactions");
        ModelNode add = new ModelNode();
        add.get(OP_ADDR).set(new ModelNode().setEmptyList()).add(SUBSYSTEM, "transactions");
        add.get(OP).set(ADD);
        add.get("coordinator-environment", "default-timeout").set(300);
        add.get("process-id-uuid").set(true);
        add.get("socket-binding").set("txn-recovery-environment");
        add.get("status-socket-binding").set("txn-status-manager");
        nodes.add(add);
    }

    private static void socketBindings(List<ModelNode> nodes) {
        ModelNode add = new ModelNode();
        add.get(OP_ADDR).set(new ModelNode().setEmptyList()).add(SOCKET_BINDING_GROUP, "standard-sockets");
        add.get(OP).set(ADD);
        add.get("default-interface").set("public");
        add.get("port-offset").set(0);
        nodes.add(add);
        add = new ModelNode();
        add.get(OP_ADDR).set(new ModelNode().setEmptyList()).add(SOCKET_BINDING_GROUP, "standard-sockets").add(SOCKET_BINDING, "txn-recovery-environment");
        add.get(OP).set(ADD);
        add.get("port").set(4712);
        nodes.add(add);
        add = new ModelNode();
        add.get(OP_ADDR).set(new ModelNode().setEmptyList()).add(SOCKET_BINDING_GROUP, "standard-sockets").add(SOCKET_BINDING, "txn-status-manager");
        add.get(OP).set(ADD);
        add.get("port").set(4713);
        nodes.add(add);
        add = new ModelNode();
        add.get(OP_ADDR).set(new ModelNode().setEmptyList()).add(SOCKET_BINDING_GROUP, "standard-sockets").add(SOCKET_BINDING, "jacorb");
        add.get(OP).set(ADD);
        add.get("port").set(3628);
        nodes.add(add);
        add = new ModelNode();
        add.get(OP_ADDR).set(new ModelNode().setEmptyList()).add(SOCKET_BINDING_GROUP, "standard-sockets").add(SOCKET_BINDING, "jacorb-ssl");
        add.get(OP).set(ADD);
        add.get("port").set(3629);
        nodes.add(add);
    }

    private static void interfaces(List<ModelNode> nodes) {
        ModelNode add = new ModelNode();
        add.get(OP_ADDR).set(new ModelNode().setEmptyList()).add(INTERFACE, "public");
        add.get(OP).set(ADD);
        add.get(CRITERIA).add().set("inet-address", "127.0.0.1");
        nodes.add(add);
    }

    private static void loadExtension(List<ModelNode> nodes, String moduleName) {
        final ModelNode add = new ModelNode();
        add.get(OP_ADDR).set(new ModelNode().setEmptyList()).add(EXTENSION, moduleName);
        add.get(OP).set(ADD);
        nodes.add(add);
    }

}