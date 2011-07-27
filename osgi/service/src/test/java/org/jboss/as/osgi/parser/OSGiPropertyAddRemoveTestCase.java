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
import org.jboss.dmr.ModelNode;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author David Bosschaert
 */
public class OSGiPropertyAddRemoveTestCase extends ResourceAddRemoveTestBase {
    @Test
    public void testOSGiPropertyAddRemove() throws Exception {
        SubsystemState stateService = new SubsystemState();
        List<OperationStepHandler> addedSteps = new ArrayList<OperationStepHandler>();
        OperationContext context = mockOperationContext(stateService, addedSteps, OperationContext.ResultAction.KEEP);

        ModelNode address = new ModelNode();
        address.add(new ModelNode().set(ModelDescriptionConstants.SUBSYSTEM, OSGiExtension.SUBSYSTEM_NAME));
        address.add(new ModelNode().set(CommonAttributes.PROPERTY, "PropertyX"));
        ModelNode data = new ModelNode();
        data.get(CommonAttributes.VALUE).set("hi");
        ModelNode op = OSGiPropertyAdd.getAddOperation(address, data);

        Assert.assertEquals("Precondition", 0, addedSteps.size());
        OSGiPropertyAdd.INSTANCE.execute(context, op);
        Assert.assertEquals(1, addedSteps.size());

        Assert.assertNull("Precondition", stateService.getProperties().get("PropertyX"));
        addedSteps.get(0).execute(context, op);
        Assert.assertEquals("hi", stateService.getProperties().get("PropertyX"));

        OSGiPropertyRemove.INSTANCE.execute(context, op);
        Assert.assertEquals("Actual remove added as async step", 2, addedSteps.size());

        Mockito.when(context.completeStep()).thenReturn(OperationContext.ResultAction.ROLLBACK);
        addedSteps.get(1).execute(context, op);
        Assert.assertEquals("Property should have been kept as the operation was rolled back",
            "hi", stateService.getProperties().get("PropertyX"));

        Mockito.when(context.completeStep()).thenReturn(OperationContext.ResultAction.KEEP);
        addedSteps.get(1).execute(context, op);
        Assert.assertNull("Property should have been removed", stateService.getProperties().get("PropertyX"));
    }

    @Test
    public void testOSGiPropertyAddRollback() throws Exception {
        SubsystemState stateService = new SubsystemState();
        List<OperationStepHandler> addedSteps = new ArrayList<OperationStepHandler>();
        OperationContext context = mockOperationContext(stateService, addedSteps, OperationContext.ResultAction.ROLLBACK);

        ModelNode address = new ModelNode();
        address.add(new ModelNode().set(ModelDescriptionConstants.SUBSYSTEM, OSGiExtension.SUBSYSTEM_NAME));
        address.add(new ModelNode().set(CommonAttributes.PROPERTY, "PropertyX"));
        ModelNode data = new ModelNode();
        data.get(CommonAttributes.VALUE).set("hi");
        ModelNode op = OSGiPropertyAdd.getAddOperation(address, data);

        Assert.assertEquals("Precondition", 0, addedSteps.size());
        OSGiPropertyAdd.INSTANCE.execute(context, op);
        Assert.assertEquals(1, addedSteps.size());

        Assert.assertNull("Precondition", stateService.getProperties().get("PropertyX"));
        addedSteps.get(0).execute(context, op);
        Assert.assertNull("Operation should have been rolled back", stateService.getProperties().get("PropertyX"));
    }
}
