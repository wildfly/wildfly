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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
        return readResource("subsystem_1_3.xml");
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
            .addMavenResourceURL("org.jboss.as:jboss-as-messaging:7.1.2.Final");

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

        try {
            TransformedOperation transformedOperation = mainServices.transformOperation(version_1_1_0, operation);
            // legacyServices.executeOperation(operation); would actually work - however it does not understand the expr
            // so we need to reject the expression on the DC already
            fail("should reject the expression,instead got " + transformedOperation.getTransformedOperation().toJSONString(true));
        } catch (OperationFailedException e) {
            // OK
        }

        operation = new ModelNode();
        operation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        address = new ModelNode();
        address.add(SUBSYSTEM, MessagingExtension.SUBSYSTEM_NAME);
        address.add("hornetq-server", "default");
        address.add("pooled-connection-factory", "hornetq-ra-local");
        operation.get(OP_ADDR).set(address);
        operation.get(NAME).set("use-auto-recovery");
        operation.get(VALUE).set("false");

        mainResult = mainServices.executeOperation(operation);
        assertEquals(mainResult.toJSONString(true), SUCCESS, mainResult.get(OUTCOME).asString());

        try {
            TransformedOperation transformedOperation = mainServices.transformOperation(version_1_1_0, operation);
            // use-auto-recovery has been added in version 1.2, it must fail for previous versions
            fail("should reject the write-attribute operation,instead got " + transformedOperation.getTransformedOperation().toJSONString(true));
        } catch (OperationFailedException e) {
            // OK
        }
    }
}
