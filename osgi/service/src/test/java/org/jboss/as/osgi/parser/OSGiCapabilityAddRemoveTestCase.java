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

import junit.framework.Assert;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.osgi.parser.Namespace11.Constants;
import org.jboss.as.osgi.parser.SubsystemState.OSGiCapability;
import org.jboss.dmr.ModelNode;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author David Bosschaert
 */
public class OSGiCapabilityAddRemoveTestCase extends ResourceAddRemoveTestBase {
    @Test
    public void testOSGiCapabilityAddRemove() throws Exception {
        SubsystemState stateService = new SubsystemState();
        List<OperationStepHandler> addedSteps = new ArrayList<OperationStepHandler>();
        OperationContext context = mockOperationContext(stateService, addedSteps, OperationContext.ResultAction.KEEP);

        ModelNode address = new ModelNode();
        address.add(new ModelNode().set(ModelDescriptionConstants.SUBSYSTEM, OSGiExtension.SUBSYSTEM_NAME));
        address.add(new ModelNode().set(Constants.CAPABILITY, "org.acme.module1"));
        ModelNode data = new ModelNode();
        data.get(Constants.STARTLEVEL).set("4");
        ModelNode op = getAddOperation(address, data);

        Assert.assertEquals("Precondition", 0, addedSteps.size());
        OSGiCapabilityAdd.INSTANCE.execute(context, op);
        Assert.assertEquals(1, addedSteps.size());

        Assert.assertEquals("Precondition", 0, stateService.getCapabilities().size());
        addedSteps.get(0).execute(context, op);
        Assert.assertEquals(1, stateService.getCapabilities().size());
        OSGiCapability module = stateService.getCapabilities().get(0);
        Assert.assertEquals("org.acme.module1:main", module.getIdentifier().toString());
        Assert.assertEquals(new Integer(4), module.getStartLevel());

        OSGiCapabilityRemove.INSTANCE.execute(context, op);
        Assert.assertEquals("Actual remove added as async step", 2, addedSteps.size());

        Mockito.when(context.completeStep()).thenReturn(OperationContext.ResultAction.ROLLBACK);
        addedSteps.get(1).execute(context, op);
        Assert.assertEquals("Module should have been kept as the operation was rolled back", module, stateService.getCapabilities().get(0));

        Mockito.when(context.completeStep()).thenReturn(OperationContext.ResultAction.KEEP);
        addedSteps.get(1).execute(context, op);
        Assert.assertEquals("Module should have been removed", 0, stateService.getCapabilities().size());
    }

    @Test
    public void testOSGiCapabilityAddRollback() throws Exception {
        SubsystemState stateService = new SubsystemState();
        List<OperationStepHandler> addedSteps = new ArrayList<OperationStepHandler>();
        OperationContext context = mockOperationContext(stateService, addedSteps, OperationContext.ResultAction.ROLLBACK);

        ModelNode address = new ModelNode();
        address.add(new ModelNode().set(ModelDescriptionConstants.SUBSYSTEM, OSGiExtension.SUBSYSTEM_NAME));
        address.add(new ModelNode().set(Constants.CAPABILITY, "org.acme.module1"));
        ModelNode data = new ModelNode();
        data.get(Constants.STARTLEVEL).set("4");
        ModelNode op = getAddOperation(address, data);

        Assert.assertEquals("Precondition", 0, addedSteps.size());
        OSGiCapabilityAdd.INSTANCE.execute(context, op);
        Assert.assertEquals(1, addedSteps.size());

        Assert.assertEquals("Precondition", 0, stateService.getCapabilities().size());
        addedSteps.get(0).execute(context, op);
        Assert.assertEquals("Operation should have been rolled back", 0, stateService.getCapabilities().size());
    }

    private ModelNode getAddOperation(ModelNode address, ModelNode existing) {
        ModelNode op = Util.getEmptyOperation(ModelDescriptionConstants.ADD, address);
        if (existing.hasDefined(Constants.STARTLEVEL)) {
            op.get(Constants.STARTLEVEL).set(existing.get(Constants.STARTLEVEL));
        }
        return op;
    }
}
