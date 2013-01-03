/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.osgi;

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.osgi.framework.Version;

/**
 * Abstract OSGi management operations
 *
 * @author thomas.diesler@jboss.com
 * @author David Bosschaert
 * @since 06-Mar-2012
 */
public final class FrameworkManagement {

    // Hide ctor
    private FrameworkManagement() {
    }

    interface ModelConstants {
        String ACTIVATE = "activate";
        String ACTIVATION = "activation";
        String BUNDLE = "bundle";
        String CAPABILITY = "capability";
        String PROPERTY = "property";
        String START = "start";
        String STARTLEVEL = "startlevel";
        String STATE = "state";
        String STOP = "stop";
        String SYMBOLIC_NAME = "symbolic-name";
        String VALUE = "value";
        String VERSION = "version";
    }

    interface ModelDescriptionConstants {
        String ADD = "add";
        String CHILD_TYPE = "child-type";
        String FAILURE_DESCRIPTION = "failure-description";
        String INCLUDE_RUNTIME = "include-runtime";
        String NAME = "name";
        String OUTCOME = "outcome";
        String READ_ATTRIBUTE_OPERATION = "read-attribute";
        String READ_CHILDREN_NAMES_OPERATION = "read-children-names";
        String READ_RESOURCE_OPERATION = "read-resource";
        String RECURSIVE = "recursive";
        String REMOVE = "remove";
        String RESULT = "result";
        String SUCCESS = "success";
        String VALUE = "value";
        String WRITE_ATTRIBUTE_OPERATION = "write-attribute";
    }

    public static void activateFramework(ModelControllerClient client) throws Exception {
        ModelNode op = createOpNode("subsystem=osgi", ModelConstants.ACTIVATE);
        executeOperation(client, op);
    }

    public static String getActivationMode(ModelControllerClient client) throws Exception {
        return readAttribute(client, ModelConstants.ACTIVATION);
    }

    public static void setActivationMode(ModelControllerClient client, String mode) throws Exception {
        writeAttribute(client, ModelConstants.ACTIVATION, mode);
    }

    public static Integer getFrameworkStartLevel(ModelControllerClient client) throws Exception {
        String sl = readAttribute(client, ModelConstants.STARTLEVEL);
        if (sl.trim().length() == 0)
            return null;

        return Integer.parseInt(sl);
    }

    public static void setFrameworkStartLevel(ModelControllerClient client, int i) throws Exception {
        writeAttribute(client, ModelConstants.STARTLEVEL, "" + i);
    }

    public static void bundleStart(ModelControllerClient client, Object resId) throws Exception {
        ModelNode op = createOpNode("subsystem=osgi/bundle=" + resId, ModelConstants.START);
        executeOperation(client, op, true);
    }

    public static void bundleStop(ModelControllerClient client, Object resId) throws Exception {
        ModelNode op = createOpNode("subsystem=osgi/bundle=" + resId, ModelConstants.STOP);
        executeOperation(client, op, true);
    }

    public static List<Long> listBundleIDs(ModelControllerClient client) throws Exception {
        ModelNode op = createOpNode("subsystem=osgi", ModelDescriptionConstants.READ_CHILDREN_NAMES_OPERATION);
        op.get(ModelDescriptionConstants.CHILD_TYPE).set(ModelConstants.BUNDLE);
        ModelNode result = executeOperation(client, op, true);

        List<Long> ids = new ArrayList<Long>();
        for (ModelNode s : result.asList()) {
            ids.add(Long.parseLong(s.asString()));
        }
        return ids;
    }

    public static Long getBundleId(ModelControllerClient client, String symbolicName, Version version) throws Exception {
        Long result = new Long(-1);
        ModelNode op = createOpNode("subsystem=osgi", ModelDescriptionConstants.READ_RESOURCE_OPERATION);
        op.get(ModelDescriptionConstants.INCLUDE_RUNTIME).set("true");
        op.get(ModelDescriptionConstants.RECURSIVE).set("true");
        ModelNode bundleNode = executeOperation(client, op).get(ModelConstants.BUNDLE);
        for (ModelNode node : bundleNode.asList()) {
            Property propNode = node.asProperty();
            ModelNode valueNode = propNode.getValue();
            ModelNode symbolicNameNode = valueNode.get(ModelConstants.SYMBOLIC_NAME);
            if (symbolicNameNode.asString().equals(symbolicName)) {
                if (version == null) {
                    result = new Long(propNode.getName());
                    break;
                }
                ModelNode versionNode = valueNode.get(ModelConstants.VERSION);
                if (versionNode.isDefined()) {
                    Version auxver = Version.parseVersion(versionNode.asString());
                    if (version.equals(auxver)) {
                        result = new Long(propNode.getName());
                        break;
                    }
                }
            }
        }
        return result;
    }

    public static String getBundleState(ModelControllerClient client, Object resId) throws Exception {
        ModelNode op = createOpNode("subsystem=osgi/bundle=" + resId, ModelDescriptionConstants.READ_RESOURCE_OPERATION);
        op.get(ModelDescriptionConstants.INCLUDE_RUNTIME).set("true");
        op.get(ModelDescriptionConstants.RECURSIVE).set("true");
        ModelNode result = executeOperation(client, op);
        return result.get(ModelConstants.STATE).asString();
    }

    public static ModelNode getBundleInfo(ModelControllerClient client, Object resId) throws Exception {
        ModelNode op = createOpNode("subsystem=osgi/bundle=" + resId, ModelDescriptionConstants.READ_RESOURCE_OPERATION);
        op.get(ModelDescriptionConstants.INCLUDE_RUNTIME).set("true");
        op.get(ModelDescriptionConstants.RECURSIVE).set("true");
        return executeOperation(client, op);
    }

    public static void addCapability(ModelControllerClient client, String name, Integer startLevel) throws Exception {
        ModelNode op = createOpNode("subsystem=osgi/capability=" + name, ModelDescriptionConstants.ADD);
        op.get(ModelConstants.STARTLEVEL).set(startLevel.toString());
        executeOperation(client, op, true);
    }

    public static List<String> listCapabilities(ModelControllerClient client) throws Exception {
        return listChildrenNames(client, ModelConstants.CAPABILITY);
    }

    public static void removeCapability(ModelControllerClient client, String name) throws Exception {
        removeResource(client, ModelConstants.CAPABILITY, name);
    }

    public static void addProperty(ModelControllerClient client, String name, String value) throws Exception {
        ModelNode op = createOpNode("subsystem=osgi/property=" + name, ModelDescriptionConstants.ADD);
        op.get(ModelDescriptionConstants.VALUE).set(value);
        executeOperation(client, op, true);
    }

    public static List<String> listProperties(ModelControllerClient client) throws Exception {
        return listChildrenNames(client, ModelConstants.PROPERTY);
    }

    public static String readProperty(ModelControllerClient client, String name) throws Exception {
        ModelNode op = createOpNode("subsystem=osgi/property=" + name, ModelDescriptionConstants.READ_RESOURCE_OPERATION);
        ModelNode result = executeOperation(client, op);
        return result.get(ModelConstants.VALUE).asString();
    }

    public static void removeProperty(ModelControllerClient client, String name) throws Exception {
        removeResource(client, ModelConstants.PROPERTY, name);
    }

    private static List<String> listChildrenNames(ModelControllerClient client, String type) throws Exception {
        ModelNode op = createOpNode("subsystem=osgi", ModelDescriptionConstants.READ_CHILDREN_NAMES_OPERATION);
        op.get(ModelDescriptionConstants.CHILD_TYPE).set(type);
        ModelNode result = executeOperation(client, op);

        List<String> names = new ArrayList<String>();
        for (ModelNode n : result.asList()) {
            names.add(n.asString());
        }
        return names;
    }

    private static void removeResource(ModelControllerClient client, String type, String name) throws Exception {
        ModelNode op = createOpNode("subsystem=osgi/" + type + "=" + name, ModelDescriptionConstants.REMOVE);
        executeOperation(client, op, true);
    }

    private static String readAttribute(ModelControllerClient client, String attributeName) throws Exception {
        ModelNode op = createOpNode("subsystem=osgi", ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION);
        op.get(ModelDescriptionConstants.NAME).set(attributeName);
        ModelNode result = executeOperation(client, op);
        return result.asString();
    }

    private static void writeAttribute(ModelControllerClient client, String attributeName, String value)  throws Exception {
        ModelNode op = createOpNode("subsystem=osgi", ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION);
        op.get(ModelDescriptionConstants.NAME).set(attributeName);
        op.get(ModelDescriptionConstants.VALUE).set(value);
        executeOperation(client, op, true);
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

    private static ModelNode executeOperation(final ModelControllerClient client, ModelNode op) throws Exception {
        return executeOperation(client, op, true);
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
