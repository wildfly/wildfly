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

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.osgi.framework.Services;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author David Bosschaert
 */
//@Ignore("[AS7-3556] Replace mocked subsystem model tests with functional tests")
public class ActivateOperationTestCase {

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testActivateOperation() throws Exception {
        ModelNode activateOp = new ModelNode();
        activateOp.get(ModelDescriptionConstants.OP_ADDR).add(ModelDescriptionConstants.SUBSYSTEM, "osgi");
        activateOp.get(ModelDescriptionConstants.OP).set(ModelConstants.ACTIVATE);

        ServiceController sc = Mockito.mock(ServiceController.class);

        ServiceRegistry sr = Mockito.mock(ServiceRegistry.class);
        Mockito.when(sr.getRequiredService(Services.FRAMEWORK_ACTIVE)).thenReturn(sc);

        OperationContext context = Mockito.mock(OperationContext.class);
        Mockito.when(context.getServiceRegistry(false)).thenReturn(sr);

        ActivateOperationHandler.INSTANCE.executeRuntimeStep(context, activateOp);

        Mockito.verify(sc).setMode(Mode.ACTIVE);
        Mockito.verify(context).completeStep();
    }
}
