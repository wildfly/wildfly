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

package org.jboss.as.core.model.test.socketbindinggroups;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CLIENT_MAPPINGS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import java.util.List;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.resource.AbstractSocketBindingResourceDefinition;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.core.model.test.AbstractCoreModelTest;
import org.jboss.as.core.model.test.KernelServices;
import org.jboss.as.core.model.test.KernelServicesBuilder;
import org.jboss.as.core.model.test.LegacyKernelServicesInitializer;
import org.jboss.as.core.model.test.TestModelType;
import org.jboss.as.core.model.test.util.TransformersTestParameters;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Tests of socket-binding transformation.
 *
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 */
@RunWith(Parameterized.class)
public class SocketBindingTransformersTestCase extends AbstractCoreModelTest {

    private static final String CLIENT_MAPPING_SOURCE_NETWORK = AbstractSocketBindingResourceDefinition.CLIENT_MAPPING_SOURCE_NETWORK.getName();
    private static final String CLIENT_MAPPING_DESTINATION_ADDRESS = AbstractSocketBindingResourceDefinition.CLIENT_MAPPING_DESTINATION_ADDRESS.getName();
    private static final String CLIENT_MAPPING_DESTINATION_PORT = AbstractSocketBindingResourceDefinition.CLIENT_MAPPING_DESTINATION_PORT.getName();

    private final ModelVersion modelVersion;
    private final LegacyKernelServicesInitializer.TestControllerVersion testControllerVersion;

    @Parameterized.Parameters
    public static List<Object[]> parameters(){
        return TransformersTestParameters.setupVersions();
    }

    public SocketBindingTransformersTestCase(TransformersTestParameters params) {
        this.modelVersion = params.getModelVersion();
        this.testControllerVersion = params.getTestControllerVersion();
    }

    @Test
    public void testClientMappingTransformer() throws Exception {

        if (modelVersion.getMajor() > 1 || modelVersion.getMinor() >= 4) {
            return;
        }

        KernelServicesBuilder builder = createKernelServicesBuilder(TestModelType.DOMAIN)
                .setXmlResource("domain-transformers-1.3.xml");

        LegacyKernelServicesInitializer legacyInit = builder.createLegacyKernelServicesBuilder(modelVersion, testControllerVersion);
        if (modelVersion.getMajor() == 1 && modelVersion.getMinor() <= 3) {
            //The 7.1.2/3 operation validator does not like expressions very much
            legacyInit.setDontValidateOperations();
        }

        KernelServices mainServices = builder.build();
        Assert.assertTrue(mainServices.isSuccessfulBoot());

        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        checkCoreModelTransformation(mainServices, modelVersion);

        String[] address = { SOCKET_BINDING_GROUP, "standard-sockets", SOCKET_BINDING, "client-mapping" };
        testMappingsWriteAttributeTransformation(mainServices, address, CLIENT_MAPPING_SOURCE_NETWORK, "${test.exp:127.0.0.0/24}");
        testMappingsWriteAttributeTransformation(mainServices, address, CLIENT_MAPPING_DESTINATION_ADDRESS, "${test.exp:localhost}");
        testMappingsWriteAttributeTransformation(mainServices, address, CLIENT_MAPPING_DESTINATION_PORT, "${test.exp:3334}");

        String[] add1Addr = { SOCKET_BINDING_GROUP, "standard-sockets", SOCKET_BINDING, "add-1" };
        testMappingsAddTransformation(mainServices, add1Addr, CLIENT_MAPPING_SOURCE_NETWORK, "${test.exp:127.0.0.0/24}");
        String[] add2Addr = { SOCKET_BINDING_GROUP, "standard-sockets", SOCKET_BINDING, "add-2" };
        testMappingsAddTransformation(mainServices, add2Addr, CLIENT_MAPPING_DESTINATION_ADDRESS, "${test.exp:localhost}");
        String[] add3Addr = { SOCKET_BINDING_GROUP, "standard-sockets", SOCKET_BINDING, "add-3" };
        testMappingsAddTransformation(mainServices, add3Addr, CLIENT_MAPPING_DESTINATION_PORT, "${test.exp:3334}");
    }

    private void testMappingsWriteAttributeTransformation(KernelServices mainServices, String[] address,
                                                          String field, String expression) throws OperationFailedException {
        ModelNode write = createOperation(WRITE_ATTRIBUTE_OPERATION, address);
        write.get(NAME).set(CLIENT_MAPPINGS);
        ModelNode mappings = getClientMappings();
        mappings.get(1).get(field).set(expression);
        write.get(VALUE).set(mappings);
        ModelNode expected = getClientMappings();
        expected.get(1).get(field).setExpression(expression);

        checkOutcome(mainServices.executeOperation(write.clone()));

        ModelNode model = mainServices.readWholeModel(false).get(address);
        Assert.assertEquals(model + " includes expected mappings", expected, model.get(CLIENT_MAPPINGS));

        OperationTransformer.TransformedOperation transOp = mainServices.transformOperation(modelVersion, write);
        ModelNode translatedWrite = transOp.getTransformedOperation();
        junit.framework.Assert.assertTrue(translatedWrite.hasDefined(VALUE));
        junit.framework.Assert.assertEquals(mappings, translatedWrite.get(VALUE));
        ModelNode result = mainServices.executeOperation(modelVersion, transOp);
        Assert.assertEquals("expected result: " + result, FAILED, result.get(OUTCOME).asString());
    }
    private void testMappingsAddTransformation(KernelServices mainServices, String[] address,
                                               String field, String expression) throws OperationFailedException {
        ModelNode add = createOperation(ADD, address);
        add.get(PORT).set(4444);
        ModelNode mappings = getClientMappings();
        mappings.get(1).get(field).set(expression);
        add.get(CLIENT_MAPPINGS).set(mappings);
        ModelNode expected = getClientMappings();
        expected.get(1).get(field).setExpression(expression);

        checkOutcome(mainServices.executeOperation(add.clone()));
        ModelNode model = mainServices.readWholeModel(false).get(address);
        Assert.assertEquals(model + " includes expected mappings", expected, model.get(CLIENT_MAPPINGS));

        OperationTransformer.TransformedOperation transOp = mainServices.transformOperation(modelVersion, add);
        ModelNode translatedAdd = transOp.getTransformedOperation();
        junit.framework.Assert.assertTrue(translatedAdd.hasDefined(CLIENT_MAPPINGS));
        junit.framework.Assert.assertEquals(mappings, translatedAdd.get(CLIENT_MAPPINGS));
        ModelNode result = mainServices.executeOperation(modelVersion, transOp);
        Assert.assertEquals("expected result: " + result, FAILED, result.get(OUTCOME).asString());

    }

    private ModelNode createOperation(String operationName, String...address) {
        ModelNode operation = new ModelNode();
        operation.get(OP).set(operationName);
        if (address.length > 0) {
            if (address.length % 2 != 0) {
                throw new IllegalArgumentException("Address must be in pairs");
            }
            for (int i = 0 ; i < address.length ; i+=2) {
                operation.get(OP_ADDR).add(address[i], address[i + 1]);
            }
        } else {
            operation.get(OP_ADDR).setEmptyList();
        }

        return operation;
    }

    private static ModelNode getClientMappings() {
        ModelNode mappings = new ModelNode();
        ModelNode one = new ModelNode();
        one.get(CLIENT_MAPPING_DESTINATION_ADDRESS).set("localhost");
        mappings.add(one);
        ModelNode two = new ModelNode();
        two.get(CLIENT_MAPPING_SOURCE_NETWORK).set("127.0.0.0/24");
        two.get(CLIENT_MAPPING_DESTINATION_ADDRESS).set("localhost");
        two.get(CLIENT_MAPPING_DESTINATION_PORT).set(3334);
        mappings.add(two);
        return mappings;
    }
}
