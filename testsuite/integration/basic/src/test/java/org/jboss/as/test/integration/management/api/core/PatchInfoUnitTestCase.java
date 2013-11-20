/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.management.api.core;

import java.io.IOException;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE_RUNTIME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.management.base.ContainerResourceMgmtTestBase;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Emanuel Muckenhuber
 */
@RunWith(Arquillian.class)
@RunAsClient
public class PatchInfoUnitTestCase extends ContainerResourceMgmtTestBase {

    private static final PathAddress ROOT_RESOURCE = PathAddress.pathAddress(PathElement.pathElement(CORE_SERVICE, "patching"));
    private static final PathAddress BASE_LAYER = ROOT_RESOURCE.append(PathElement.pathElement("layer", "base"));

    /**
     * Skip this testcase if patching is not enabled.
     */
    @Before
    public void assumePatchingIsEnabled() throws IOException, MgmtOperationException {
        final ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).set(ROOT_RESOURCE.toModelNode());
        operation.get(OP).set(READ_RESOURCE_OPERATION);
        final ModelNode result = executeOperation(operation, false);
        Assume.assumeTrue(result.get(ModelDescriptionConstants.OUTCOME).asString()
                .equals(ModelDescriptionConstants.SUCCESS));
    }

    @Test
    public void testRootInfo() throws Exception {

        final ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).set(ROOT_RESOURCE.toModelNode());
        operation.get(OP).set(READ_RESOURCE_OPERATION);
        operation.get(RECURSIVE).set(false);
        operation.get(INCLUDE_RUNTIME).set(true);

        final ModelNode result = executeOperation(operation, true);
        Assert.assertTrue(result.hasDefined("cumulative-patch-id"));
        Assert.assertTrue(result.hasDefined("patches"));

    }

    @Test
    public void testBaseLayer() throws Exception {

        final ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).set(BASE_LAYER.toModelNode());
        operation.get(OP).set(READ_RESOURCE_OPERATION);
        operation.get(RECURSIVE).set(false);
        operation.get(INCLUDE_RUNTIME).set(true);

        final ModelNode result = executeOperation(operation, true);
        Assert.assertTrue(result.hasDefined("cumulative-patch-id"));
        Assert.assertTrue(result.hasDefined("patches"));

    }


    @Test
    public void testDescription() throws Exception {

        final ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).set(ROOT_RESOURCE.toModelNode());
        operation.get(OP).set(READ_RESOURCE_DESCRIPTION_OPERATION);
        operation.get(RECURSIVE).set(true);
        operation.get(INCLUDE_RUNTIME).set(true);

        executeOperation(operation, true);
    }

}
