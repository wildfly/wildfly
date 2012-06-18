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
package org.jboss.as.configadmin.parser;

import static org.junit.Assert.assertEquals;

import java.util.Hashtable;
import java.util.Map;

import org.jboss.as.configadmin.service.ConfigAdminService;
import org.jboss.as.configadmin.service.ConfigAdminServiceImpl;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceRegistry;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;

/**
 * Process a Configuration Update.
 *
 * @author David Bosschaert
 */
public class ConfigurationUpdateTestCase {
    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void testConfigurationUpdate() throws Exception {
        // Set up some mock objects
        ModelNode targetModel = new ModelNode();
        Resource mockResource = Mockito.mock(Resource.class);
        Mockito.when(mockResource.getModel()).thenReturn(targetModel);

        OperationContext mockOperationContext = Mockito.mock(OperationContext.class);
        Mockito.when(mockOperationContext.createResource(PathAddress.EMPTY_ADDRESS)).thenReturn(mockResource);

        Hashtable<String, String> dict = new Hashtable<String, String>();
        dict.put("foo", "bar");
        ModelNode operation = getOperationModelNode("mypid", dict);

        // Call the update operation
        ConfigurationUpdate.INSTANCE.execute(mockOperationContext, operation);

        // Verify the interactions made during the update operation
        InOrder inOrder = Mockito.inOrder(mockOperationContext);
        inOrder.verify(mockOperationContext).removeResource(PathAddress.EMPTY_ADDRESS);
        inOrder.verify(mockOperationContext).createResource(PathAddress.EMPTY_ADDRESS);
        ArgumentCaptor<OperationStepHandler> addedStep = ArgumentCaptor.forClass(OperationStepHandler.class);
        inOrder.verify(mockOperationContext).addStep(
                addedStep.capture(), Mockito.eq(OperationContext.Stage.RUNTIME));
        inOrder.verify(mockOperationContext).completeStep();

        ModelNode expectedModel = new ModelNode();
        ModelNode subModel = new ModelNode();
        subModel.get("foo").set("bar");
        expectedModel.get("entries").set(subModel);
        assertEquals(expectedModel, targetModel);

        // now check the runtime operation, as added by the update operation
        OperationStepHandler step = addedStep.getValue();

        // Set up some more mock objects for the runtime operation
        ConfigAdminServiceImpl mockCAS = Mockito.mock(ConfigAdminServiceImpl.class);

        ServiceController mockCASServiceController = Mockito.mock(ServiceController.class);
        Mockito.when(mockCASServiceController.getValue()).thenReturn(mockCAS);

        ServiceRegistry mockServiceRegistry = Mockito.mock(ServiceRegistry.class);
        Mockito.when(mockServiceRegistry.getService(ConfigAdminService.SERVICE_NAME)).thenReturn(mockCASServiceController);

        OperationContext mockContext2 = Mockito.mock(OperationContext.class);
        Mockito.when(mockContext2.getServiceRegistry(true)).thenReturn(mockServiceRegistry);
        step.execute(mockContext2, operation);

        Mockito.verify(mockCAS).putConfigurationFromDMR("mypid", dict);
        Mockito.verify(mockContext2).completeStep();
    }

    private ModelNode getOperationModelNode(String pid, Map<String, String> props) {
        ModelNode addr = new ModelNode();
        addr.add(new ModelNode().set(ModelDescriptionConstants.SUBSYSTEM, ConfigAdminExtension.SUBSYSTEM_NAME));
        addr.add(new ModelNode().set(ModelConstants.CONFIGURATION, pid));
        ModelNode operation = Util.getEmptyOperation(ModelConstants.UPDATE, addr);
        ModelNode entries = new ModelNode();

        for (Map.Entry<String, String> entry : props.entrySet()) {
            entries.get(entry.getKey()).set(entry.getValue());
        }
        operation.get(ModelConstants.ENTRIES).set(entries);
        return operation;
    }
}
