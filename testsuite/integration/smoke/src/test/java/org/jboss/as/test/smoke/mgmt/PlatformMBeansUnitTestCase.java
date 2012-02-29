/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.smoke.mgmt;

import java.util.HashSet;
import java.util.Set;

import junit.framework.Assert;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PLATFORM_MBEAN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;

/**
 * Test validating that the platform mbean resources exist and are reachable.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class PlatformMBeansUnitTestCase {

    private static final Set<String> ignored = new HashSet<String>();

    static {
        // Only a few subsystems are NOT supposed to work in the domain mode
        ignored.add("deployment-scanner");
    }

    @ContainerResource
    private ManagementClient managementClient;

    @Test
    public void testReadClassLoadingMXBean() throws Exception {
        // Get a list of all registered subsystems
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_ATTRIBUTE_OPERATION);
        ModelNode address = new ModelNode();
        address.add(CORE_SERVICE, PLATFORM_MBEAN);
        address.add(TYPE, "class-loading");
        operation.get(OP_ADDR).set(address);
        operation.get(NAME).set("loaded-class-count");

        final ModelNode result = executeForResult(operation);
        org.junit.Assert.assertEquals(ModelType.INT, result.getType());
    }

    private ModelNode executeForResult(final ModelNode operation) throws Exception {
        final ModelNode result = managementClient.getControllerClient().execute(operation);
        checkSuccessful(result, operation);
        return result.get(RESULT);
    }

    static void checkSuccessful(final ModelNode result, final ModelNode operation) {
        if(! SUCCESS.equals(result.get(OUTCOME).asString())) {
            System.out.println("Failed result:\n" + result + "\n for operation:\n" + operation);
            Assert.fail("operation failed: " + result.get(FAILURE_DESCRIPTION));
        }
    }

}
