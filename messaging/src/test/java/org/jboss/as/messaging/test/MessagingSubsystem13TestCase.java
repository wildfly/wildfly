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

package org.jboss.as.messaging.test;

import junit.framework.Assert;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
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
import org.jboss.as.controller.transform.OperationTransformer;
import static org.jboss.as.messaging.CommonAttributes.HORNETQ_SERVER;
import static org.jboss.as.messaging.CommonAttributes.PARAM;
import static org.jboss.as.messaging.CommonAttributes.POOLED_CONNECTION_FACTORY;
import static org.jboss.as.messaging.CommonAttributes.REMOTE_CONNECTOR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.transform.OperationTransformer.TransformedOperation;
import org.jboss.as.messaging.MessagingExtension;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.dmr.ModelNode;
import org.junit.Test;

/**
 *  * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2012 Red Hat inc

 */
public class MessagingSubsystem13TestCase extends AbstractSubsystemBaseTest {

    public MessagingSubsystem13TestCase() {
        super(MessagingExtension.SUBSYSTEM_NAME, new MessagingExtension());
    }

    /*
     * test 1.3-only features. Compatible features are tested in #testTransformers()
     */
    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("subsystem_incompatible_1_3.xml");
    }

    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return AdditionalInitialization.MANAGEMENT;
    }

    @Test
    public void testTransformers() throws Exception {
        String subsystemXml = readResource("subsystem_1_3.xml");
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization())
                .setSubsystemXml(subsystemXml);

        // Add legacy subsystems
        ModelVersion version_1_1_0 = ModelVersion.create(1, 1, 0);
        builder.createLegacyKernelServicesBuilder(createAdditionalInitialization(), version_1_1_0)
            .addMavenResourceURL("org.jboss.as:jboss-as-messaging:7.1.2.Final")
            .addMavenResourceURL("org.hornetq:hornetq-core:2.2.16.Final")
            .addMavenResourceURL("org.hornetq:hornetq-jms:2.2.16.Final")
            .addMavenResourceURL("org.hornetq:hornetq-ra:2.2.16.Final");

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(version_1_1_0);
        assertNotNull(legacyServices);

        checkSubsystemModelTransformation(mainServices, version_1_1_0);

        ModelNode operation = new ModelNode();
        operation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, MessagingExtension.SUBSYSTEM_NAME);
        address.add("hornetq-server", "default");
        address.add("path", "journal-directory");
        operation.get(OP_ADDR).set(address);
        operation.get(NAME).set("path");
        operation.get(VALUE).set("${my.journal.dir:journal}");

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

        final OperationTransformer.TransformedOperation op = mainServices.transformOperation(version_1_1_0, operation);
        final ModelNode result = mainServices.executeOperation(version_1_1_0, op);
        Assert.assertEquals("should reject the expression", FAILED, result.get(OUTCOME).asString());

        operation = new ModelNode();
        operation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        address = new ModelNode();
        address.add(SUBSYSTEM, MessagingExtension.SUBSYSTEM_NAME);
        address.add(HORNETQ_SERVER, "default");
        address.add(POOLED_CONNECTION_FACTORY, "hornetq-ra-local");
        operation.get(OP_ADDR).set(address);
        operation.get(NAME).set("use-auto-recovery");
        operation.get(VALUE).set("false");

        mainResult = mainServices.executeOperation(operation);
        assertEquals(mainResult.toJSONString(true), SUCCESS, mainResult.get(OUTCOME).asString());

        TransformedOperation transformedOperation = mainServices.transformOperation(version_1_1_0, operation);
        ModelNode transformedResult = transformedOperation.getResultTransformer().transformResult(successResult);
        assertEquals("success transformed to failed", FAILED, transformedResult.get(OUTCOME).asString());
        transformedResult = transformedOperation.getResultTransformer().transformResult(successResult);
        assertEquals("failed transformed to failed", FAILED, transformedResult.get(OUTCOME).asString());
        assertTrue("failed transformed with failure description", transformedResult.hasDefined(FAILURE_DESCRIPTION));
        transformedResult = transformedOperation.getResultTransformer().transformResult(ignoreResult);
        assertEquals("ignored result untransformed", IGNORED, transformedResult.get(OUTCOME).asString());
    }

    @Test
    public void testRejectExpressionsForTransportParams() throws Exception {
        String subsystemXml = readResource("subsystem_1_3.xml");
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization())
                .setSubsystemXml(subsystemXml);

        // Add legacy subsystems
        ModelVersion version_1_1_0 = ModelVersion.create(1, 1, 0);
        builder.createLegacyKernelServicesBuilder(createAdditionalInitialization(), version_1_1_0)
            .addMavenResourceURL("org.jboss.as:jboss-as-messaging:7.1.2.Final")
            .addMavenResourceURL("org.hornetq:hornetq-core:2.2.16.Final")
            .addMavenResourceURL("org.hornetq:hornetq-jms:2.2.16.Final")
            .addMavenResourceURL("org.hornetq:hornetq-ra:2.2.16.Final");

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(version_1_1_0);
        assertNotNull(legacyServices);

        checkSubsystemModelTransformation(mainServices, version_1_1_0);

        ModelNode operation = new ModelNode();
        operation.get(OP).set(ADD);
        ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, MessagingExtension.SUBSYSTEM_NAME);
        address.add(HORNETQ_SERVER, "default");
        address.add(REMOTE_CONNECTOR, "netty");
        address.add(PARAM, "password");
        operation.get(OP_ADDR).set(address);
        operation.get(VALUE).set("${mypassword:default}");

        ModelNode mainResult = mainServices.executeOperation(operation);
        assertEquals(mainResult.toJSONString(true), SUCCESS, mainResult.get(OUTCOME).asString());

        TransformedOperation transformedOperation = mainServices.transformOperation(version_1_1_0, operation);
        final ModelNode result = mainServices.executeOperation(version_1_1_0, transformedOperation);
        Assert.assertEquals("should reject the expression", FAILED, result.get(OUTCOME).asString());
    }
}
