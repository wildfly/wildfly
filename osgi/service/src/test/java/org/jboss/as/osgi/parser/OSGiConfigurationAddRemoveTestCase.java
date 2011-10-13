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
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author David Bosschaert
 * @author Thomas.Diesler@jboss.com
 */
public class OSGiConfigurationAddRemoveTestCase extends ResourceAddRemoveTestBase {

    @Test
    public void testConfigurationAddRemove() throws Exception {
        SubsystemState stateService = new SubsystemState();
        List<OperationStepHandler> addedSteps = new ArrayList<OperationStepHandler>();
        OperationContext context = mockOperationContext(stateService, addedSteps, OperationContext.ResultAction.KEEP);

        String pid = "org.acme.pid1";
        Map<String, String> data = Collections.singletonMap("mykey", "myval");
        ModelNode op = getAddOperation(pid, data);

        Assert.assertEquals("Precondition", 0, addedSteps.size());
        execute(OSGiConfigurationAdd.INSTANCE, context, op);
        Assert.assertEquals(1, addedSteps.size());

        Assert.assertEquals("Precondition", 0, stateService.getConfigurations().size());
        execute(addedSteps.get(0), context, op);
        Assert.assertEquals(1, stateService.getConfigurations().size());
        Dictionary<String, String> config = stateService.getConfiguration(pid);
        Assert.assertEquals(1, config.size());
        Assert.assertEquals("myval", config.get("mykey"));

        execute(OSGiConfigurationRemove.INSTANCE, context, op);
        Assert.assertEquals("Actual remove added as async step", 2, addedSteps.size());

        configureForRollback(context, op);
        execute(addedSteps.get(1), context, op);
        Assert.assertEquals("Configuration rolled back", 1, stateService.getConfiguration(pid).size());
        Assert.assertEquals("Configuration rolled back", "myval", stateService.getConfiguration(pid).get("mykey"));

        configureForSuccess(context);
        execute(addedSteps.get(1), context, op);
        Assert.assertNull("Configuration should have been removed", stateService.getConfiguration(pid));
    }

    @Test
    public void testConfigurationAddRollback() throws Exception {
        SubsystemState stateService = new SubsystemState();
        List<OperationStepHandler> addedSteps = new ArrayList<OperationStepHandler>();
        OperationContext context = mockOperationContext(stateService, addedSteps, OperationContext.ResultAction.ROLLBACK);

        String pid = "org.acme.pid1";
        Map<String, String> data = Collections.singletonMap("mykey", "myval");
        ModelNode op = getAddOperation(pid, data);

        Assert.assertEquals("Precondition", 0, addedSteps.size());
        execute(OSGiConfigurationAdd.INSTANCE, context, op);
        Assert.assertEquals(1, addedSteps.size());

        Assert.assertEquals("Precondition", 0, stateService.getConfigurations().size());
        execute(addedSteps.get(0), context, op);
        Assert.assertEquals("Operation should have been rolled back", 0, stateService.getConfigurations().size());
    }

    private ModelNode getAddOperation(String pid, Map<String, String> data) {
        ModelNode address = new ModelNode();
        address.add(new ModelNode().set(ModelDescriptionConstants.SUBSYSTEM, OSGiExtension.SUBSYSTEM_NAME));
        address.add(new ModelNode().set(ModelConstants.CONFIGURATION, pid));
        ModelNode entries = new ModelNode();
        for (String key : data.keySet()) {
            entries.get(key).set(data.get(key));
        }
        ModelNode op = Util.getEmptyOperation(ModelDescriptionConstants.ADD, address);
        op.get(ModelConstants.ENTRIES).set(entries);
        return op;
    }
}
