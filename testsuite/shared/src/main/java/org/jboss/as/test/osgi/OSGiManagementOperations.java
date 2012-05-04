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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.osgi.parser.ModelConstants;
import org.jboss.as.test.integration.management.ManagementOperations;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.test.integration.management.util.ModelUtil;
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
public abstract class OSGiManagementOperations {

    public static void activateFramework(ModelControllerClient client) throws MgmtOperationException, IOException {
        ModelNode op = ModelUtil.createOpNode("subsystem=osgi", ModelConstants.ACTIVATE);
        executeOperation(client, op);
    }

    public static String getActivationMode(ModelControllerClient client) throws MgmtOperationException, IOException {
        return readAttribute(client, ModelConstants.ACTIVATION);
    }

    public static boolean setActivationMode(ModelControllerClient client, String mode) throws MgmtOperationException, IOException {
        return writeAttribute(client, ModelConstants.ACTIVATION, mode);
    }

    public static Integer getFrameworkStartLevel(ModelControllerClient client) throws MgmtOperationException, IOException {
        String sl = readAttribute(client, ModelConstants.STARTLEVEL);
        if (sl.trim().length() == 0)
            return null;

        return Integer.parseInt(sl);
    }

    public static boolean setFrameworkStartLevel(ModelControllerClient client, int i) throws MgmtOperationException, IOException {
        return writeAttribute(client, ModelConstants.STARTLEVEL, "" + i);
    }

    public static boolean bundleStart(ModelControllerClient client, Object resId) throws MgmtOperationException, IOException {
        ModelNode op = ModelUtil.createOpNode("subsystem=osgi/bundle=" + resId, ModelConstants.START);
        ModelNode result = executeOperation(client, op, false);
        return SUCCESS.equals(result.get(OUTCOME).asString());
    }

    public static boolean bundleStop(ModelControllerClient client, Object resId) throws MgmtOperationException, IOException {
        ModelNode op = ModelUtil.createOpNode("subsystem=osgi/bundle=" + resId, ModelConstants.STOP);
        ModelNode result = executeOperation(client, op, false);
        return SUCCESS.equals(result.get(OUTCOME).asString());
    }

    public static List<Long> listBundleIDs(ModelControllerClient client) throws MgmtOperationException, IOException {
        ModelNode op = ModelUtil.createOpNode("subsystem=osgi", READ_CHILDREN_NAMES_OPERATION);
        op.get(ModelDescriptionConstants.CHILD_TYPE).set(ModelConstants.BUNDLE);
        ModelNode result = executeOperation(client, op, true);

        List<Long> ids = new ArrayList<Long>();
        for (ModelNode s : result.asList()) {
            ids.add(Long.parseLong(s.asString()));
        }
        return ids;
    }

    public static Long getBundleId(ModelControllerClient client, String symbolicName, Version version) throws MgmtOperationException, IOException {
        Long result = new Long(-1);
        ModelNode op = ModelUtil.createOpNode("subsystem=osgi", READ_RESOURCE_OPERATION);
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

    public static String getBundleState(ModelControllerClient client, Object resId) throws MgmtOperationException, IOException {
        ModelNode op = ModelUtil.createOpNode("subsystem=osgi/bundle=" + resId, READ_RESOURCE_OPERATION);
        op.get(ModelDescriptionConstants.INCLUDE_RUNTIME).set("true");
        op.get(ModelDescriptionConstants.RECURSIVE).set("true");
        ModelNode result = executeOperation(client, op);
        return result.get(ModelConstants.STATE).asString();
    }

    public static ModelNode getBundleInfo(ModelControllerClient client, Object resId) throws MgmtOperationException, IOException {
        ModelNode op = ModelUtil.createOpNode("subsystem=osgi/bundle=" + resId, READ_RESOURCE_OPERATION);
        op.get(ModelDescriptionConstants.INCLUDE_RUNTIME).set("true");
        op.get(ModelDescriptionConstants.RECURSIVE).set("true");
        return executeOperation(client, op);
    }

    public static boolean addCapability(ModelControllerClient client, String name, Integer startLevel) throws MgmtOperationException, IOException {
        ModelNode op = ModelUtil.createOpNode("subsystem=osgi/capability=" + name, ADD);
        op.get(ModelConstants.STARTLEVEL).set(startLevel.toString());
        ModelNode result = executeOperation(client, op, false);
        return SUCCESS.equals(result.get(OUTCOME).asString());
    }

    public static List<String> listCapabilities(ModelControllerClient client) throws MgmtOperationException, IOException {
        return listChildrenNames(client, ModelConstants.CAPABILITY);
    }

    public static boolean removeCapability(ModelControllerClient client, String name) throws MgmtOperationException, IOException {
        return removeResource(client, ModelConstants.CAPABILITY, name);
    }

    public static boolean addProperty(ModelControllerClient client, String name, String value) throws MgmtOperationException, IOException {
        ModelNode op = ModelUtil.createOpNode("subsystem=osgi/property=" + name, ADD);
        op.get(ModelDescriptionConstants.VALUE).set(value);
        ModelNode result = executeOperation(client, op, false);
        return SUCCESS.equals(result.get(OUTCOME).asString());
    }

    public static List<String> listProperties(ModelControllerClient client) throws MgmtOperationException, IOException {
        return listChildrenNames(client, ModelConstants.PROPERTY);
    }

    public static String readProperty(ModelControllerClient client, String name) throws MgmtOperationException, IOException {
        ModelNode op = ModelUtil.createOpNode("subsystem=osgi/property=" + name, READ_RESOURCE_OPERATION);
        ModelNode result = executeOperation(client, op);
        return result.get(ModelConstants.VALUE).asString();
    }

    public static boolean removeProperty(ModelControllerClient client, String name) throws MgmtOperationException, IOException {
        return removeResource(client, ModelConstants.PROPERTY, name);
    }

    private static ModelNode executeOperation(final ModelControllerClient client, ModelNode op) throws IOException, MgmtOperationException {
        return executeOperation(client, op, true);
    }

    private static ModelNode executeOperation(final ModelControllerClient client, ModelNode op, boolean unwrapResult) throws IOException, MgmtOperationException {
        System.out.println(op);
        ModelNode result = unwrapResult ? ManagementOperations.executeOperation(client, op) : ManagementOperations.executeOperationRaw(client, op);
        System.out.println(result);
        return result;
    }

    private static List<String> listChildrenNames(ModelControllerClient client, String type) throws IOException, MgmtOperationException {
        ModelNode op = ModelUtil.createOpNode("subsystem=osgi", READ_CHILDREN_NAMES_OPERATION);
        op.get(ModelDescriptionConstants.CHILD_TYPE).set(type);
        ModelNode result = executeOperation(client, op);

        List<String> names = new ArrayList<String>();
        for (ModelNode n : result.asList()) {
            names.add(n.asString());
        }
        return names;
    }

    private static boolean removeResource(ModelControllerClient client, String type, String name) throws IOException, MgmtOperationException {
        ModelNode op = ModelUtil.createOpNode("subsystem=osgi/" + type + "=" + name, REMOVE);
        ModelNode result = executeOperation(client, op, false);
        return SUCCESS.equals(result.get(OUTCOME).asString());
    }

    private static String readAttribute(ModelControllerClient client, String attributeName) throws IOException, MgmtOperationException {
        ModelNode op = ModelUtil.createOpNode("subsystem=osgi", READ_ATTRIBUTE_OPERATION);
        op.get(ModelDescriptionConstants.NAME).set(attributeName);
        ModelNode result = executeOperation(client, op);
        return result.asString();
    }

    private static boolean writeAttribute(ModelControllerClient client, String attributeName, String value)  throws IOException, MgmtOperationException {
        ModelNode op = ModelUtil.createOpNode("subsystem=osgi", WRITE_ATTRIBUTE_OPERATION);
        op.get(ModelDescriptionConstants.NAME).set(attributeName);
        op.get(ModelDescriptionConstants.VALUE).set(value);
        ModelNode result = executeOperation(client, op, false);
        return SUCCESS.equals(result.get(OUTCOME).asString());
    }
}
