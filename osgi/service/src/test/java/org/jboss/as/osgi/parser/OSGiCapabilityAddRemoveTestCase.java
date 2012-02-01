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
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.Assert;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.osgi.parser.SubsystemState.OSGiCapability;
import org.jboss.dmr.ModelNode;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author David Bosschaert
 * @author Thomas.Diesler@jboss.com
 */
@Ignore("[AS7-3556] Replace mocked subsystem model tests with functional tests")
public class OSGiCapabilityAddRemoveTestCase extends ResourceAddRemoveTestBase {
    @Test
    public void testOSGiCapabilityAddRemove() throws Exception {
        SubsystemState stateService = new SubsystemState();
        List<OperationStepHandler> addedSteps = new ArrayList<OperationStepHandler>();
        OperationContext context = mockOperationContext(stateService, addedSteps, OperationContext.ResultAction.KEEP);

        ModelNode op = getAddOperation("org.acme.module1", 4);

        Assert.assertEquals("Precondition", 0, addedSteps.size());
        execute(OSGiCapabilityAdd.INSTANCE, context, op);
        Assert.assertEquals(1, addedSteps.size());

        Assert.assertEquals("Precondition", 0, stateService.getCapabilities().size());
        execute(addedSteps.get(0), context, op);
        Assert.assertEquals(1, stateService.getCapabilities().size());
        OSGiCapability module = stateService.getCapabilities().get(0);
        Assert.assertEquals("org.acme.module1", module.getIdentifier());
        Assert.assertEquals(new Integer(4), module.getStartLevel());

        execute(OSGiCapabilityRemove.INSTANCE, context, op);
        Assert.assertEquals("Actual remove added as async step", 2, addedSteps.size());

        configureForRollback(context, op);
        execute(addedSteps.get(1), context, op);
        Assert.assertEquals("Module should have been kept as the operation was rolled back", module, stateService.getCapabilities().get(0));

        configureForSuccess(context);
        execute(addedSteps.get(1), context, op);
        Assert.assertEquals("Module should have been removed", 0, stateService.getCapabilities().size());
    }

    @Test
    public void testOSGiCapabilityAddRollback() throws Exception {
        SubsystemState stateService = new SubsystemState();
        List<OperationStepHandler> addedSteps = new ArrayList<OperationStepHandler>();
        OperationContext context = mockOperationContext(stateService, addedSteps, OperationContext.ResultAction.ROLLBACK);

        ModelNode op = getAddOperation("org.acme.module1", 4);

        Assert.assertEquals("Precondition", 0, addedSteps.size());
        execute(OSGiCapabilityAdd.INSTANCE, context, op);
        Assert.assertEquals(1, addedSteps.size());

        Assert.assertEquals("Precondition", 0, stateService.getCapabilities().size());
        execute(addedSteps.get(0), context, op);
        Assert.assertEquals("Operation should have been rolled back", 0, stateService.getCapabilities().size());
    }

    private ModelNode getAddOperation(String name, Integer startlevel) {
        ModelNode address = new ModelNode();
        address.add(new ModelNode().set(ModelDescriptionConstants.SUBSYSTEM, OSGiExtension.SUBSYSTEM_NAME));
        address.add(new ModelNode().set(ModelConstants.CAPABILITY, name));
        ModelNode op = Util.getEmptyOperation(ModelDescriptionConstants.ADD, address);
        if (startlevel != null) {
            op.get(ModelConstants.STARTLEVEL).set(startlevel);
        }
        return op;
    }
}
