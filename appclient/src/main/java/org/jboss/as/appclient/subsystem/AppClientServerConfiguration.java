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

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

/**
 * Class that contains the static application client server configuration
 *
 * @author Stuart Douglas
 */
class AppClientServerConfiguration {

    private AppClientServerConfiguration() {
    }

    public static List<ModelNode> serverConfiguration(final String filePath, final String deploymentName, final String hostUrl, final String propertiesFileUrl, final List<String> parameters, List<ModelNode> xmlNodes) {
        List<ModelNode> ret = new ArrayList<ModelNode>();

        for (final ModelNode node : xmlNodes) {
            ret.add(node);
        }
        appclient(ret, filePath, deploymentName, hostUrl, propertiesFileUrl, parameters);

        return ret;
    }

    private static void appclient(List<ModelNode> nodes, final String filePath, final String deploymentName, final String hostUrl, final String propertiesFileUrl, final List<String> parameters) {
        loadExtension(nodes, "org.jboss.as.appclient");
        ModelNode add = Util.createAddOperation(PathAddress.pathAddress(AppClientExtension.SUBSYSTEM_PATH));
        add.get(Constants.FILE).set(filePath);
        if (deploymentName != null) {
            add.get(Constants.DEPLOYMENT).set(deploymentName);
        }
        if (parameters.isEmpty()) {
            add.get(Constants.PARAMETERS).setEmptyList();
        } else {
            for (String param : parameters) {
                add.get(Constants.PARAMETERS).add(param);
            }
        }
        if(hostUrl != null) {
            add.get(Constants.HOST_URL).set(hostUrl);
        }
        if(propertiesFileUrl != null) {
            add.get(Constants.CONNECTION_PROPERTIES_URL).set(propertiesFileUrl);
        }
        nodes.add(add);
    }

    private static void loadExtension(List<ModelNode> nodes, String moduleName) {
        final ModelNode add = new ModelNode();
        add.get(OP_ADDR).set(new ModelNode().setEmptyList()).add(EXTENSION, moduleName);
        add.get(OP).set(ADD);
        nodes.add(add);
    }

}
