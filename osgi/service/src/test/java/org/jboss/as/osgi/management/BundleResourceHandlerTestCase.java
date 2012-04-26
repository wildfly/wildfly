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
package org.jboss.as.osgi.management;

import java.util.Dictionary;
import java.util.Hashtable;

import junit.framework.Assert;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.osgi.management.BundleResourceHandler;
import org.jboss.as.osgi.parser.ModelConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.osgi.framework.Services;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.service.startlevel.StartLevel;

/**
 * @author David Bosschaert
 * @author Thomas.Diesler@jboss.com
 */
public class BundleResourceHandlerTestCase {

    private BundleContext bundleContext;
    private ModelNode contextResult;
    private OperationContext operationContext;
    private StartLevel startLevelService;

    @Test
    public void testRegister() throws Exception {
        ManagementResourceRegistration mrr = Mockito.mock(ManagementResourceRegistration.class);

        BundleResourceHandler handler = BundleResourceHandler.INSTANCE;
        handler.register(mrr);

        Mockito.verify(mrr).registerReadOnlyAttribute(ModelConstants.ID, handler, AttributeAccess.Storage.RUNTIME);
        Mockito.verify(mrr).registerReadOnlyAttribute(ModelConstants.STARTLEVEL, handler, AttributeAccess.Storage.RUNTIME);
        Mockito.verify(mrr).registerReadOnlyAttribute(ModelConstants.SYMBOLIC_NAME, handler, AttributeAccess.Storage.RUNTIME);
        Mockito.verify(mrr).registerReadOnlyAttribute(ModelConstants.VERSION, handler, AttributeAccess.Storage.RUNTIME);
    }

    @Test
    public void testExecuteReadIDAttribute() throws Exception {
        mockEnvironment();
        ModelNode readOp = getReadOperation("17", ModelConstants.ID);

        Bundle testBundle = Mockito.mock(Bundle.class);
        Mockito.when(bundleContext.getBundle(17)).thenReturn(testBundle);
        Mockito.when(testBundle.getBundleId()).thenReturn(new Long(17));

        BundleResourceHandler.INSTANCE.executeRuntimeStep(operationContext, readOp);
        Assert.assertEquals(17L, contextResult.asLong());
    }

    @Test
    public void testExecuteReadSymbolicNameAttribute() throws Exception {
        mockEnvironment();
        ModelNode readOp = getReadOperation("1", ModelConstants.SYMBOLIC_NAME);

        Bundle testBundle = Mockito.mock(Bundle.class);
        Mockito.when(bundleContext.getBundle(1)).thenReturn(testBundle);
        Mockito.when(testBundle.getSymbolicName()).thenReturn("myTestBundle");

        BundleResourceHandler.INSTANCE.executeRuntimeStep(operationContext, readOp);
        Assert.assertEquals("myTestBundle", contextResult.asString());
    }

    @Test
    public void testExecuteReadStartLevelAttibute() throws Exception {
        mockEnvironment();
        ModelNode readOp = getReadOperation("1", ModelConstants.STARTLEVEL);

        Bundle testBundle = Mockito.mock(Bundle.class);
        Mockito.when(bundleContext.getBundle(1)).thenReturn(testBundle);
        Mockito.when(startLevelService.getBundleStartLevel(testBundle)).thenReturn(7);

        BundleResourceHandler.INSTANCE.executeRuntimeStep(operationContext, readOp);
        Assert.assertEquals(7, contextResult.asInt());
    }

    @Test
    public void testExecuteReadStateOperation() throws Exception {
        mockEnvironment();
        ModelNode readOp = getReadOperation("1", ModelConstants.STATE);

        Bundle testBundle = Mockito.mock(Bundle.class);
        Mockito.when(bundleContext.getBundle(1)).thenReturn(testBundle);
        Mockito.when(testBundle.getState()).thenReturn(Bundle.ACTIVE);

        BundleResourceHandler.INSTANCE.executeRuntimeStep(operationContext, readOp);
        Assert.assertEquals("ACTIVE", contextResult.asString());
    }

    @Test
    public void testTypeAttributeFragmen() throws Exception {
        mockEnvironment();
        ModelNode readOp = getReadOperation("1", ModelConstants.TYPE);

        Dictionary<String, String> headers = new Hashtable<String, String>();
        headers.put(Constants.FRAGMENT_HOST, "somebundle");
        Bundle testBundle = Mockito.mock(Bundle.class);
        Mockito.when(bundleContext.getBundle(1)).thenReturn(testBundle);
        Mockito.when(testBundle.getHeaders()).thenReturn(headers);

        BundleResourceHandler.INSTANCE.executeRuntimeStep(operationContext, readOp);
        Assert.assertEquals(ModelConstants.FRAGMENT, contextResult.asString());
    }

    @Test
    public void testTypeAttributeBundle() throws Exception {
        mockEnvironment();
        ModelNode readOp = getReadOperation("1", ModelConstants.TYPE);

        Dictionary<String, String> headers = new Hashtable<String, String>();

        Bundle testBundle = Mockito.mock(Bundle.class);
        Mockito.when(bundleContext.getBundle(1)).thenReturn(testBundle);
        Mockito.when(testBundle.getHeaders()).thenReturn(headers);

        BundleResourceHandler.INSTANCE.executeRuntimeStep(operationContext, readOp);
        Assert.assertEquals(ModelConstants.BUNDLE, contextResult.asString());
    }

    @Test
    public void testExecuteReadVersionAttribute() throws Exception {
        mockEnvironment();
        ModelNode readOp = getReadOperation("1", ModelConstants.VERSION);

        Bundle testBundle = Mockito.mock(Bundle.class);
        Mockito.when(bundleContext.getBundle(1)).thenReturn(testBundle);
        Mockito.when(testBundle.getVersion()).thenReturn(new Version(1, 2, 3, "qual"));

        BundleResourceHandler.INSTANCE.executeRuntimeStep(operationContext, readOp);
        Assert.assertEquals("1.2.3.qual", contextResult.asString());
    }

    @Test
    public void testBundleStateString() {
        Assert.assertEquals("UNINSTALLED", BundleResourceHandler.getBundleState(mockBundle(Bundle.UNINSTALLED)));
        Assert.assertEquals("INSTALLED", BundleResourceHandler.getBundleState(mockBundle(Bundle.INSTALLED)));
        Assert.assertEquals("RESOLVED", BundleResourceHandler.getBundleState(mockBundle(Bundle.RESOLVED)));
        Assert.assertEquals("STARTING", BundleResourceHandler.getBundleState(mockBundle(Bundle.STARTING)));
        Assert.assertEquals("STOPPING", BundleResourceHandler.getBundleState(mockBundle(Bundle.STOPPING)));
        Assert.assertEquals("ACTIVE", BundleResourceHandler.getBundleState(mockBundle(Bundle.ACTIVE)));
    }

    @Test
    public void testExecuteStart() throws Exception {
        mockEnvironment();
        ModelNode startOp = new ModelNode();
        startOp.get(ModelDescriptionConstants.OP_ADDR).add("subsystem", "osgi");
        startOp.get(ModelDescriptionConstants.OP_ADDR).add("bundle", "" + Long.MAX_VALUE);
        startOp.get(ModelDescriptionConstants.OP).set(ModelConstants.START);

        Bundle testBundle = Mockito.mock(Bundle.class);
        Mockito.when(bundleContext.getBundle(Long.MAX_VALUE)).thenReturn(testBundle);

        Mockito.verifyZeroInteractions(testBundle);
        BundleResourceHandler.INSTANCE.executeRuntimeStep(operationContext, startOp);
        Mockito.verify(testBundle).start();
    }

    @Test
    public void testExecuteStop() throws Exception {
        mockEnvironment();
        ModelNode startOp = new ModelNode();
        startOp.get(ModelDescriptionConstants.OP_ADDR).add("subsystem", "osgi");
        startOp.get(ModelDescriptionConstants.OP_ADDR).add("bundle", "" + Long.MAX_VALUE);
        startOp.get(ModelDescriptionConstants.OP).set(ModelConstants.STOP);

        Bundle testBundle = Mockito.mock(Bundle.class);
        Mockito.when(bundleContext.getBundle(Long.MAX_VALUE)).thenReturn(testBundle);

        Mockito.verifyZeroInteractions(testBundle);
        BundleResourceHandler.INSTANCE.executeRuntimeStep(operationContext, startOp);
        Mockito.verify(testBundle).stop();
    }

    private Bundle mockBundle(int state) {
        Bundle bundle = Mockito.mock(Bundle.class);
        Mockito.when(bundle.getState()).thenReturn(state);
        return bundle;
    }

    private ModelNode getReadOperation(String bundleId, String attribute) {
        ModelNode readOp = new ModelNode();
        readOp.get(ModelDescriptionConstants.OP_ADDR).add("subsystem", "osgi");
        readOp.get(ModelDescriptionConstants.OP_ADDR).add("bundle", bundleId);
        readOp.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION);
        readOp.get(ModelDescriptionConstants.NAME).set(attribute);
        return readOp;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void mockEnvironment() {
        startLevelService = Mockito.mock(StartLevel.class);
        bundleContext = Mockito.mock(BundleContext.class);

        Bundle systemBundle = Mockito.mock(Bundle.class);
        Mockito.when(systemBundle.getBundleContext()).thenReturn(bundleContext);

        ServiceController sbsc = Mockito.mock(ServiceController.class);
        Mockito.when(sbsc.getValue()).thenReturn(systemBundle);

        ServiceController scsc = Mockito.mock(ServiceController.class);
        Mockito.when(scsc.getValue()).thenReturn(bundleContext);

        ServiceController slsc = Mockito.mock(ServiceController.class);
        Mockito.when(slsc.getValue()).thenReturn(startLevelService);

        ServiceRegistry sr = Mockito.mock(ServiceRegistry.class);
        Mockito.when(sr.getService(Services.SYSTEM_CONTEXT)).thenReturn(scsc);
        Mockito.when(sr.getService(Services.SYSTEM_BUNDLE)).thenReturn(sbsc);
        Mockito.when(sr.getService(Services.START_LEVEL)).thenReturn(slsc);

        contextResult = new ModelNode();
        operationContext = Mockito.mock(OperationContext.class);
        Mockito.when(operationContext.getServiceRegistry(false)).thenReturn(sr);
        Mockito.when(operationContext.getResult()).thenReturn(contextResult);
    }
}
