/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.jboss.as.test.configadmin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.management.ManagementOperations;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.test.integration.management.util.ModelUtil;
import org.jboss.dmr.ModelNode;

/**
 * Config Admin management operations.
 *
 * @author David Bosschaert
 */
public class ConfigAdminManagementOperations {
    public static boolean addConfiguration(ModelControllerClient client, String pid, Map<String, String> entries) throws IOException, MgmtOperationException {
        ModelNode op = ModelUtil.createOpNode("subsystem=configadmin/configuration=" + pid, ModelDescriptionConstants.ADD);
        ModelNode en = new ModelNode();
        for (Map.Entry<String, String> entry : entries.entrySet()) {
            en.get(entry.getKey()).set(entry.getValue());
        }
        op.get("entries").set(en);
        ModelNode result = executeOperation(client, op, false);
        return ModelDescriptionConstants.SUCCESS.equals(result.get(ModelDescriptionConstants.OUTCOME).asString());
    }

    public static List<String> listConfigurations(ModelControllerClient client) throws IOException, MgmtOperationException {
        return listChildrenNames(client, "configuration");
    }

    public static Map<String, String> readConfiguration(ModelControllerClient client, String pid) throws IOException, MgmtOperationException {
        ModelNode op = ModelUtil.createOpNode("subsystem=configadmin/configuration=" + pid, ModelDescriptionConstants.READ_RESOURCE_OPERATION);
        ModelNode result = executeOperation(client, op, true);
        ModelNode entries = result.get("entries");

        Map<String, String> map = new HashMap<String, String>();
        for (String key : entries.keys()) {
            map.put(key, entries.get(key).asString());
        }
        return map;
    }

    public static boolean removeConfiguration(ModelControllerClient client, String pid) throws IOException, MgmtOperationException {
        return removeResource(client, "configuration", pid);
    }

    private static ModelNode executeOperation(final ModelControllerClient client, ModelNode op, boolean unwrapResult) throws IOException, MgmtOperationException {
        System.out.println(op);
        ModelNode result = unwrapResult ? ManagementOperations.executeOperation(client, op) : ManagementOperations.executeOperationRaw(client, op);
        System.out.println(result);
        return result;
    }

    private static List<String> listChildrenNames(ModelControllerClient client, String type) throws IOException, MgmtOperationException {
        ModelNode op = ModelUtil.createOpNode("subsystem=configadmin", ModelDescriptionConstants.READ_CHILDREN_NAMES_OPERATION);
        op.get(ModelDescriptionConstants.CHILD_TYPE).set(type);
        ModelNode result = executeOperation(client, op, true);

        List<String> names = new ArrayList<String>();
        for (ModelNode n : result.asList()) {
            names.add(n.asString());
        }
        return names;
    }

    private static boolean removeResource(ModelControllerClient client, String type, String name) throws IOException, MgmtOperationException {
        ModelNode op = ModelUtil.createOpNode("subsystem=configadmin/" + type + "=" + name, ModelDescriptionConstants.REMOVE);
        ModelNode result = executeOperation(client, op, false);
        return ModelDescriptionConstants.SUCCESS.equals(result.get(ModelDescriptionConstants.OUTCOME).asString());
    }
}
