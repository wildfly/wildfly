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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.messaging.MessagingExtension.VERSION_1_2_0;
import static org.jboss.as.messaging.test.MessagingDependencies.getHornetQDependencies;
import static org.jboss.as.messaging.test.MessagingDependencies.getMessagingGAV;
import static org.jboss.as.messaging.test.ModelFixers.PATH_FIXER;
import static org.jboss.as.model.test.ModelTestControllerVersion.V7_2_0_FINAL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.messaging.CommonAttributes;
import org.jboss.as.messaging.HornetQServerResourceDefinition;
import org.jboss.as.messaging.MessagingExtension;
import org.jboss.as.model.test.ModelFixer;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.dmr.ModelNode;
import org.junit.Test;

/**
 *  * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2012 Red Hat inc
 */
public class MessagingSubsystem20TestCase extends AbstractSubsystemBaseTest {

    public MessagingSubsystem20TestCase() {
        super(MessagingExtension.SUBSYSTEM_NAME, new MessagingExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        // subsystem_2_0_expressions.xml contains new http-connector resource
        // introduces in 2.0 management version
        return readResource("subsystem_2_0_expressions.xml");
    }

    @Test
    public void testClusteredTo120() throws Exception {
        // this hornetq-server has 1 cluster-connection resource defined and so is clustered.
        KernelServicesBuilder builder = createKernelServicesBuilder(V7_2_0_FINAL, VERSION_1_2_0, PATH_FIXER, "subsystem_with_cluster_connection_2_0.xml");

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(VERSION_1_2_0);
        assertNotNull(legacyServices);
        assertTrue("main services did not boot", mainServices.isSuccessfulBoot());
        assertTrue(legacyServices.isSuccessfulBoot());

        clusteredTo120Test(VERSION_1_2_0, mainServices, true);
        clusteredTo120Test(VERSION_1_2_0, mainServices, false);
    }

    @Test
    public void testMessageCounterEnabled() throws Exception {
        standardSubsystemTest("subsystem_2_0_message_counter.xml", false);
    }

    protected void compareXml(String configId, final String original, final String marshalled) throws Exception {
        // XML from messaging 2.0 does not have the same output than 3.0
        return;
    }

    private void clusteredTo120Test(ModelVersion version120, KernelServices mainServices, boolean clustered) throws OperationFailedException {

        PathAddress pa = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, MessagingExtension.SUBSYSTEM_NAME),
                // stuff if the empty hornetq-server defined in empty_subsystem_2_0
                PathElement.pathElement(HornetQServerResourceDefinition.HORNETQ_SERVER_PATH.getKey(), "stuff"));

        ModelNode addOp = Util.createAddOperation(pa);
        addOp.get(CommonAttributes.CLUSTERED.getName()).set(clustered);

        OperationTransformer.TransformedOperation transformedOperation = mainServices.transformOperation(version120, addOp);
        assertFalse(transformedOperation.getTransformedOperation().has(CommonAttributes.CLUSTERED.getName()));

        ModelNode result = new ModelNode();
        result.get(OUTCOME).set(SUCCESS);
        result.get(RESULT);
        assertFalse(transformedOperation.rejectOperation(result));
        assertEquals(result, transformedOperation.transformResult(result));

        ModelNode writeOp = Util.createEmptyOperation(WRITE_ATTRIBUTE_OPERATION, pa);
        writeOp.get(NAME).set(CommonAttributes.CLUSTERED.getName());
        writeOp.get(VALUE).set(clustered);

        transformedOperation = mainServices.transformOperation(version120, writeOp);
        assertNull(transformedOperation.getTransformedOperation());
    }

    private KernelServicesBuilder createKernelServicesBuilder(ModelTestControllerVersion controllerVersion, ModelVersion messagingVersion, ModelFixer fixer, String xmlFileName) throws IOException, XMLStreamException, ClassNotFoundException {
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXmlResource(xmlFileName);
        // create builder for legacy subsystem version
        builder.createLegacyKernelServicesBuilder(createAdditionalInitialization(), controllerVersion, messagingVersion)
                .addMavenResourceURL(getMessagingGAV(controllerVersion))
                .configureReverseControllerCheck(null, fixer)
                .addMavenResourceURL(getHornetQDependencies(controllerVersion));
        return builder;
    }
}
