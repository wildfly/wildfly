/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.extension.remove;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODEL_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.io.IOException;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.integration.management.extension.ExtensionUtils;
import org.jboss.dmr.ModelNode;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ExtensionRemoveTestCase {

    private static final String MODULE_NAME = "extensionremovemodule";

    @ContainerResource
    private ManagementClient managementClient;

    @Before
    public void installExtensionModule() throws IOException {
        ExtensionUtils.createExtensionModule(MODULE_NAME, TestExtension.class);
    }

    @After
    public void  removeExtensionModule() {
        ExtensionUtils.deleteExtensionModule(MODULE_NAME);
    }

    @Test
    public void testAddAndRemoveExtension() throws Exception {
        ModelControllerClient client = managementClient.getControllerClient();

            //Check extension and subsystem is not there
            Assert.assertFalse(readResource(client).get(EXTENSION, MODULE_NAME).isDefined());
            Assert.assertFalse(readResourceDescription(client).get(SUBSYSTEM, TestExtension.SUBSYSTEM_NAME).isDefined());
            Assert.assertFalse(readResource(client).get(SUBSYSTEM, TestExtension.SUBSYSTEM_NAME).isDefined());

            //Add extension, no subsystem yet
            executeOperation(client, ADD, PathAddress.pathAddress(PathElement.pathElement(EXTENSION, MODULE_NAME)), false);
            Assert.assertTrue(readResource(client).get(EXTENSION, MODULE_NAME).isDefined());
            Assert.assertTrue(readResourceDescription(client).get(CHILDREN, SUBSYSTEM, MODEL_DESCRIPTION, TestExtension.SUBSYSTEM_NAME).isDefined());
            Assert.assertFalse(readResource(client).get(SUBSYSTEM, TestExtension.SUBSYSTEM_NAME).isDefined());

            //Add subsystem
            executeOperation(client, ADD, PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, TestExtension.SUBSYSTEM_NAME)), false);
            executeOperation(client, ADD, PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, TestExtension.SUBSYSTEM_NAME), PathElement.pathElement("child", "one")), false);
            Assert.assertTrue(readResource(client).get(EXTENSION, MODULE_NAME).isDefined());
            Assert.assertTrue(readResourceDescription(client).get(CHILDREN, SUBSYSTEM, MODEL_DESCRIPTION, TestExtension.SUBSYSTEM_NAME).isDefined());
            Assert.assertTrue(readResource(client).get(SUBSYSTEM, TestExtension.SUBSYSTEM_NAME).isDefined());

            //Should not be possible to remove extension before subsystem is removed
            executeOperation(client, REMOVE, PathAddress.pathAddress(PathElement.pathElement(EXTENSION, MODULE_NAME)), true);
            Assert.assertTrue(readResource(client).get(EXTENSION, MODULE_NAME).isDefined());
            Assert.assertTrue(readResourceDescription(client).get(CHILDREN, SUBSYSTEM, MODEL_DESCRIPTION, TestExtension.SUBSYSTEM_NAME).isDefined());
            Assert.assertTrue(readResource(client).get(SUBSYSTEM, TestExtension.SUBSYSTEM_NAME).isDefined());

            //Remove subsystem
            executeOperation(client, REMOVE, PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, TestExtension.SUBSYSTEM_NAME), PathElement.pathElement("child", "one")), false);
            executeOperation(client, REMOVE, PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, TestExtension.SUBSYSTEM_NAME)), false);
            Assert.assertTrue(readResource(client).get(EXTENSION, MODULE_NAME).isDefined());
            Assert.assertTrue(readResourceDescription(client).get(CHILDREN, SUBSYSTEM, MODEL_DESCRIPTION, TestExtension.SUBSYSTEM_NAME).isDefined());
            Assert.assertFalse(readResource(client).get(SUBSYSTEM, TestExtension.SUBSYSTEM_NAME).isDefined());

            //Remove extension
            executeOperation(client, REMOVE, PathAddress.pathAddress(PathElement.pathElement(EXTENSION, MODULE_NAME)), false);
            Assert.assertFalse(readResource(client).get(EXTENSION, MODULE_NAME).isDefined());
            Assert.assertFalse(readResourceDescription(client).get(CHILDREN, SUBSYSTEM, MODEL_DESCRIPTION, TestExtension.SUBSYSTEM_NAME).isDefined());
            Assert.assertFalse(readResource(client).get(SUBSYSTEM, TestExtension.SUBSYSTEM_NAME).isDefined());

    }

    private ModelNode executeOperation(ModelControllerClient client, String name, PathAddress address, boolean fail) throws IOException {
        ModelNode op = new ModelNode();
        op.get(OP).set(name);
        op.get(OP_ADDR).set(address.toModelNode());

        ModelNode result = client.execute(op);
        if (!fail) {
            Assert.assertFalse(result.get(FAILURE_DESCRIPTION).toString(), result.get(FAILURE_DESCRIPTION).isDefined());
        } else {
            Assert.assertTrue(result.get(FAILURE_DESCRIPTION).isDefined());
        }

        return result.get(RESULT);
    }

    private ModelNode readResourceDescription(ModelControllerClient client) throws IOException {
        ModelNode op = new ModelNode();
        op.get(OP).set(READ_RESOURCE_DESCRIPTION_OPERATION);
        op.get(RECURSIVE).set(true);

        ModelNode result = client.execute(op);
        Assert.assertFalse(result.hasDefined(FAILURE_DESCRIPTION));
        return result.get(RESULT);
    }

    private ModelNode readResource(ModelControllerClient client) throws IOException {
        ModelNode op = new ModelNode();
        op.get(OP).set(READ_RESOURCE_OPERATION);
        op.get(RECURSIVE).set(true);

        ModelNode result = client.execute(op);
        Assert.assertFalse(result.hasDefined(FAILURE_DESCRIPTION));
        return result.get(RESULT);
    }
}

