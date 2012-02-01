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

import junit.framework.Assert;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.osgi.framework.Services;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.service.startlevel.StartLevel;

/**
 * @author David Bosschaert
 */
//@Ignore("[AS7-3556] Replace mocked subsystem model tests with functional tests")
public class StartLevelHandlerTestCase {

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testReadHandler() throws Exception {
        StartLevel sls = Mockito.mock(StartLevel.class);
        Mockito.when(sls.getStartLevel()).thenReturn(999);

        ServiceController sc = Mockito.mock(ServiceController.class);
        Mockito.when(sc.getValue()).thenReturn(sls);
        Mockito.when(sc.getState()).thenReturn(ServiceController.State.UP);

        ServiceRegistry sr = Mockito.mock(ServiceRegistry.class);
        Mockito.when(sr.getRequiredService(Services.START_LEVEL)).thenReturn(sc);

        OperationContext ctx = Mockito.mock(OperationContext.class);
        ModelNode result = new ModelNode();
        Mockito.when(ctx.getServiceRegistry(false)).thenReturn(sr);
        Mockito.when(ctx.getResult()).thenReturn(result);

        StartLevelHandler handler = StartLevelHandler.READ_HANDLER;
        Assert.assertFalse(result.isDefined());
        handler.execute(ctx, new ModelNode());
        Assert.assertEquals(999, result.asInt());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testWriteHandler() throws Exception {
        StartLevel sls = Mockito.mock(StartLevel.class);

        ServiceController sc = Mockito.mock(ServiceController.class);
        Mockito.when(sc.getValue()).thenReturn(sls);
        Mockito.when(sc.getState()).thenReturn(ServiceController.State.UP);

        ServiceRegistry sr = Mockito.mock(ServiceRegistry.class);
        Mockito.when(sr.getRequiredService(Services.START_LEVEL)).thenReturn(sc);

        OperationContext ctx = Mockito.mock(OperationContext.class);
        Mockito.when(ctx.getServiceRegistry(false)).thenReturn(sr);

        ModelNode op = new ModelNode();
        op.get(ModelDescriptionConstants.VALUE).set(42);

        StartLevelHandler handler = StartLevelHandler.WRITE_HANDLER;
        Mockito.verifyZeroInteractions(sls);
        handler.execute(ctx, op);
        Mockito.verify(sls).setStartLevel(42);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testSubsystemDown() throws Exception {
        ServiceController sc = Mockito.mock(ServiceController.class);
        Mockito.when(sc.getState()).thenReturn(ServiceController.State.DOWN);

        ServiceRegistry sr = Mockito.mock(ServiceRegistry.class);
        Mockito.when(sr.getRequiredService(Services.START_LEVEL)).thenReturn(sc);

        ModelNode result = new ModelNode("test");
        OperationContext ctx = Mockito.mock(OperationContext.class);
        Mockito.when(ctx.getServiceRegistry(false)).thenReturn(sr);
        Mockito.when(ctx.getResult()).thenReturn(result);

        StartLevelHandler handler = StartLevelHandler.READ_HANDLER;
        Assert.assertTrue(result.isDefined());
        handler.execute(ctx, new ModelNode());
        Assert.assertFalse(result.isDefined());
    }
}
