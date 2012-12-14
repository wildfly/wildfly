/*
* JBoss, Home of Professional Open Source.
* Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.remoting;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.IGNORED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.dmr.ModelNode;
import org.junit.Test;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2012 Red Hat, inc
 */
public class RemotingSubsystemTransformersTestCase extends AbstractSubsystemBaseTest {

    public RemotingSubsystemTransformersTestCase() {
        super(RemotingExtension.SUBSYSTEM_NAME, new RemotingExtension());
    }

    @Test
    public void testExpressionsAreRejectedByVersion_1_1() throws Exception {
        String subsystemXml = readResource("remoting-with-expressions.xml");
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization())
                .setSubsystemXml(subsystemXml);

        // Add legacy subsystems
        ModelVersion version_1_1 = ModelVersion.create(1, 1);
        builder.createLegacyKernelServicesBuilder(createAdditionalInitialization(), version_1_1)
                .addMavenResourceURL("org.jboss.as:jboss-as-remoting:7.1.2.Final");

        KernelServices mainServices = builder.build();
        assertTrue(mainServices.isSuccessfulBoot());
        KernelServices legacyServices = mainServices.getLegacyServices(version_1_1);
        assertNotNull(legacyServices);
        assertFalse(legacyServices.isSuccessfulBoot());
    }

    @Test
    public void testTransformers() throws Exception {
        String subsystemXml = readResource("remoting-without-expressions.xml");
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization())
                .setSubsystemXml(subsystemXml);

        // Add legacy subsystems
        ModelVersion version_1_1 = ModelVersion.create(1, 1);
        builder.createLegacyKernelServicesBuilder(createAdditionalInitialization(), version_1_1)
                .addMavenResourceURL("org.jboss.as:jboss-as-remoting:7.1.2.Final");

        KernelServices mainServices = builder.build();
        assertTrue(mainServices.isSuccessfulBoot());
        KernelServices legacyServices = mainServices.getLegacyServices(version_1_1);
        assertNotNull(legacyServices);
        assertTrue(legacyServices.isSuccessfulBoot());

        checkSubsystemModelTransformation(mainServices, version_1_1);

        ModelNode operation = new ModelNode();
        operation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, RemotingExtension.SUBSYSTEM_NAME);
        operation.get(OP_ADDR).set(address);
        operation.get(NAME).set("worker-read-threads");
        operation.get(VALUE).set("${worker.read.threads:5}");

        ModelNode mainResult = mainServices.executeOperation(operation);
        assertEquals(mainResult.toJSONString(true), SUCCESS, mainResult.get(OUTCOME).asString());

        ModelNode successResult = new ModelNode();
        successResult.get(OUTCOME).set(SUCCESS);
        successResult.protect();
        ModelNode failedResult = new ModelNode();
        failedResult.get(OUTCOME).set(FAILED);
        failedResult.protect();
        ModelNode ignoreResult = new ModelNode();
        ignoreResult.get(OUTCOME).set(IGNORED);
        ignoreResult.protect();

        final OperationTransformer.TransformedOperation op = mainServices.transformOperation(version_1_1, operation);
        final ModelNode result = mainServices.executeOperation(version_1_1, op);
        assertEquals("should reject the expression", FAILED, result.get(OUTCOME).asString());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("remoting-with-expressions.xml");
    }

    @Override
    protected String getSubsystemXml(String resource) throws IOException {
        return readResource(resource);
    }
}
