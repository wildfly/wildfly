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
import org.jboss.as.osgi.parser.SubsystemState.OSGiModule;
import org.jboss.dmr.ModelNode;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author David Bosschaert
 */
public class OSGiModuleAddRemoveTestCase extends ResourceAddRemoveTestBase {
    @Test
    public void testOSGiModuleAddRemove() throws Exception {
        SubsystemState stateService = new SubsystemState();
        List<OperationStepHandler> addedSteps = new ArrayList<OperationStepHandler>();
        OperationContext context = mockOperationContext(stateService, addedSteps, OperationContext.ResultAction.KEEP);

        ModelNode address = new ModelNode();
        address.add(new ModelNode().set(ModelDescriptionConstants.SUBSYSTEM, OSGiExtension.SUBSYSTEM_NAME));
        address.add(new ModelNode().set(CommonAttributes.MODULE, "org.acme.module1"));
        ModelNode data = new ModelNode();
        data.get(CommonAttributes.STARTLEVEL).set("4");
        ModelNode op = OSGiModuleAdd.getAddOperation(address, data);

        Assert.assertEquals("Precondition", 0, addedSteps.size());
        OSGiModuleAdd.INSTANCE.execute(context, op);
        Assert.assertEquals(1, addedSteps.size());

        Assert.assertEquals("Precondition", 0, stateService.getModules().size());
        addedSteps.get(0).execute(context, op);
        Assert.assertEquals(1, stateService.getModules().size());
        OSGiModule module = stateService.getModules().get(0);
        Assert.assertEquals("org.acme.module1:main", module.getIdentifier().toString());
        Assert.assertEquals(new Integer(4), module.getStartLevel());

        OSGiModuleRemove.INSTANCE.execute(context, op);
        Assert.assertEquals("Actual remove added as async step", 2, addedSteps.size());

        Mockito.when(context.completeStep()).thenReturn(OperationContext.ResultAction.ROLLBACK);
        addedSteps.get(1).execute(context, op);
        Assert.assertEquals("Module should have been kept as the operation was rolled back",
            module, stateService.getModules().get(0));

        Mockito.when(context.completeStep()).thenReturn(OperationContext.ResultAction.KEEP);
        addedSteps.get(1).execute(context, op);
        Assert.assertEquals("Module should have been removed", 0, stateService.getModules().size());
    }

    @Test
    public void testOSGiModuleAddRollback() throws Exception {
        SubsystemState stateService = new SubsystemState();
        List<OperationStepHandler> addedSteps = new ArrayList<OperationStepHandler>();
        OperationContext context = mockOperationContext(stateService, addedSteps, OperationContext.ResultAction.ROLLBACK);

        ModelNode address = new ModelNode();
        address.add(new ModelNode().set(ModelDescriptionConstants.SUBSYSTEM, OSGiExtension.SUBSYSTEM_NAME));
        address.add(new ModelNode().set(CommonAttributes.MODULE, "org.acme.module1"));
        ModelNode data = new ModelNode();
        data.get(CommonAttributes.STARTLEVEL).set("4");
        ModelNode op = OSGiModuleAdd.getAddOperation(address, data);

        Assert.assertEquals("Precondition", 0, addedSteps.size());
        OSGiModuleAdd.INSTANCE.execute(context, op);
        Assert.assertEquals(1, addedSteps.size());

        Assert.assertEquals("Precondition", 0, stateService.getModules().size());
        addedSteps.get(0).execute(context, op);
        Assert.assertEquals("Operation should have been rolled back", 0, stateService.getModules().size());
    }
}
