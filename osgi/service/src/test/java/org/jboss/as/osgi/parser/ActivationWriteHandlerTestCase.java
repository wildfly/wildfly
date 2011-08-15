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
package org.jboss.as.osgi.parser;

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationContext.Stage;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.osgi.parser.SubsystemState.Activation;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.osgi.framework.Services;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * @author David Bosschaert
 */
public class ActivationWriteHandlerTestCase {
    @Test
    public void testHandlerLazy() throws Exception {
        ModelNode targetNode = new ModelNode();
        targetNode.get(CommonAttributes.ACTIVATION).set("eager");

        OperationContext context = Mockito.mock(OperationContext.class);
        Resource resource = Mockito.mock(Resource.class);
        Mockito.when(resource.getModel()).thenReturn(targetNode);
        Mockito.when(context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS)).thenReturn(resource);

        ActivationWriteHandler handler = new ActivationWriteHandler();

        ModelNode operation = new ModelNode();
        operation.get(ModelDescriptionConstants.VALUE).set(Activation.LAZY.toString().toLowerCase());
        handler.execute(context, operation);

        Mockito.verify(context).completeStep();

        Assert.assertEquals(Activation.LAZY.toString().toLowerCase(), targetNode.get(CommonAttributes.ACTIVATION).asString());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testHandlerEagerActivate() throws Exception {
        ModelNode targetNode = new ModelNode();
        targetNode.get(CommonAttributes.ACTIVATION).set("lazy");

        OperationContext context = Mockito.mock(OperationContext.class);
        Resource resource = Mockito.mock(Resource.class);
        Mockito.when(resource.getModel()).thenReturn(targetNode);
        Mockito.when(context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS)).thenReturn(resource);

        final List<OperationStepHandler> addedSteps = new ArrayList<OperationStepHandler>();
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                addedSteps.add((OperationStepHandler) invocation.getArguments()[0]);
                return null;
            }
        }).when(context).addStep((OperationStepHandler) Mockito.any(), Mockito.eq(Stage.RUNTIME));

        ActivationWriteHandler handler = new ActivationWriteHandler();

        ModelNode operation = new ModelNode();
        operation.get(ModelDescriptionConstants.VALUE).set(Activation.EAGER.toString().toLowerCase());

        Assert.assertEquals("Precondition", 0, addedSteps.size());
        handler.execute(context, operation);
        Mockito.verify(context).completeStep();
        Assert.assertEquals(Activation.EAGER.toString().toLowerCase(), targetNode.get(CommonAttributes.ACTIVATION).asString());

        // Now test the runtime piece...
        ServiceRegistry registry = Mockito.mock(ServiceRegistry.class);
        ServiceController svcCtrl = Mockito.mock(ServiceController.class);
        Mockito.when(registry.getRequiredService(Services.AUTOINSTALL_PROVIDER)).thenReturn(svcCtrl);

        OperationContext context2 = Mockito.mock(OperationContext.class);
        Mockito.when(context2.getServiceRegistry(true)).thenReturn(registry);

        Assert.assertEquals(1, addedSteps.size());
        addedSteps.get(0).execute(context2, operation);

        Mockito.verify(svcCtrl).setMode(Mode.ACTIVE);
        Mockito.verify(context2).completeStep();
    }
}
