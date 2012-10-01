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

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;

/**
 * Config Admin management operations.
 *
 * @author David Bosschaert
 */
public class ConfigAdminManagement {

    interface ModelConstants {
        String CONFIGURATION = "configuration";
        String ENTRIES = "entries";
        String UPDATE = "update";
        String REMOVE = "remove";
    }

    public static void addConfiguration(ModelControllerClient client, String pid, Dictionary<String, String> entries) throws Exception {
        updateConfiguration(client, pid, entries, ModelDescriptionConstants.ADD);
    }

    public static List<String> listConfigurations(ModelControllerClient client) throws Exception {
        return listChildrenNames(client, "configuration");
    }

    public static Map<String, String> readConfiguration(ModelControllerClient client, String pid) throws Exception {
        ModelNode op = createOpNode("subsystem=configadmin/configuration=" + pid, ModelDescriptionConstants.READ_RESOURCE_OPERATION);
        ModelNode result = executeOperation(client, op, true);
        ModelNode entries = result.get("entries");
        Map<String, String> map = new HashMap<String, String>();
        for (String key : entries.keys()) {
            map.put(key, entries.get(key).asString());
        }
        return map;
    }

    public static void removeConfiguration(ModelControllerClient client, String pid) throws Exception {
        updateConfiguration(client, pid, null, ModelConstants.REMOVE);
    }

    public static void updateConfiguration(ModelControllerClient client, String pid, Dictionary<String, String> entries) throws Exception {
        updateConfiguration(client, pid, entries, ModelConstants.UPDATE);
    }

    private static void updateConfiguration(ModelControllerClient client, String pid, Dictionary<String, String> entries, String operation) throws Exception {
        ModelNode op = createOpNode("subsystem=configadmin/configuration=" + pid, operation);
        if (entries != null) {
            ModelNode en = new ModelNode();
            Enumeration<String> keys = entries.keys();
            while (keys.hasMoreElements()) {
                String key = keys.nextElement();
                en.get(key).set(entries.get(key));
            }
            op.get("entries").set(en);
        }
        executeOperation(client, op, true);
    }

    private static List<String> listChildrenNames(ModelControllerClient client, String type) throws Exception {
        ModelNode op = createOpNode("subsystem=configadmin", ModelDescriptionConstants.READ_CHILDREN_NAMES_OPERATION);
        op.get(ModelDescriptionConstants.CHILD_TYPE).set(type);
        ModelNode result = executeOperation(client, op, true);
        List<String> names = new ArrayList<String>();
        for (ModelNode n : result.asList()) {
            names.add(n.asString());
        }
        return names;
    }

    private static ModelNode createOpNode(String address, String operation) {
        ModelNode op = new ModelNode();
        ModelNode list = op.get("address").setEmptyList();
        if (address != null) {
            String[] pathSegments = address.split("/");
            for (String segment : pathSegments) {
                String[] elements = segment.split("=");
                list.add(elements[0], elements[1]);
            }
        }
        op.get("operation").set(operation);
        return op;
    }

    private static ModelNode executeOperation(final ModelControllerClient client, ModelNode op, boolean unwrapResult) throws Exception {
        ModelNode ret = client.execute(op);
        if (!unwrapResult) return ret;
        if (!ModelDescriptionConstants.SUCCESS.equals(ret.get(ModelDescriptionConstants.OUTCOME).asString())) {
            throw new IllegalStateException("Management operation failed: " + ret.get(ModelDescriptionConstants.FAILURE_DESCRIPTION));
        }
        return ret.get(ModelDescriptionConstants.RESULT);
    }
}
