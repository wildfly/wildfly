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

import org.jboss.dmr.ModelNode;

/**
 * Class that contains the static application client server configuration
 *
 * @author Stuart Douglas
 */
class AppClientServerConfiguration {

    private AppClientServerConfiguration() {
    }

    public static List<ModelNode> serverConfiguration(final String filePath, final String deploymentName, final String additionalClassPath, final List<String> parameters) {
        List<ModelNode> ret = new ArrayList<ModelNode>();
        appclient(ret, filePath, deploymentName, additionalClassPath, parameters);
        interfaces(ret);
        transactionSocketBindings(ret);
        transactions(ret);
        naming(ret);
        ee(ret);
        ejb3(ret);
        security(ret);

        return ret;
    }

    private static void appclient(List<ModelNode> nodes, final String filePath, final String deploymentName, final String additionalClassPath, final List<String> parameters) {
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
        if(additionalClassPath != null) {
            add.get(Constants.ADDITIONAL_CLASS_PATH).set(additionalClassPath);
        }
        nodes.add(add);
    }

    private static void naming(List<ModelNode> nodes) {
        loadExtension(nodes, "org.jboss.as.naming");
        ModelNode add = new ModelNode();
        add.get(OP_ADDR).set(new ModelNode().setEmptyList()).add(SUBSYSTEM, "naming");
        add.get(OP).set(ADD);
        nodes.add(add);
    }

    private static void ee(List<ModelNode> nodes) {
        loadExtension(nodes, "org.jboss.as.ee");
        ModelNode add = new ModelNode();
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

    private static void transactions(List<ModelNode> nodes) {
        loadExtension(nodes, "org.jboss.as.transactions");
        ModelNode add = new ModelNode();
        add.get(OP_ADDR).set(new ModelNode().setEmptyList()).add(SUBSYSTEM, "transactions");
        add.get(OP).set(ADD);
        add.get("coordinator-environment", "default-timeout").set(300);
        add.get("core-environment", "process-id", "uuid").set("uuid");
        add.get("recovery-environment", "socket-binding").set("txn-recovery-environment");
        add.get("recovery-environment", "status-socket-binding").set("txn-status-manager");
        nodes.add(add);
    }

    private static void transactionSocketBindings(List<ModelNode> nodes) {
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
