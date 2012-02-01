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
package org.jboss.as.osgi.service;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.jboss.as.osgi.parser.SubsystemState;
import org.jboss.as.osgi.parser.SubsystemState.OSGiCapability;
import org.jboss.logmanager.Level;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.osgi.framework.BundleManagerService;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * @author David Bosschaert
 */
public class AutoInstallIntegrationTestCase {
    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void testUpdateAddModule() throws Exception {
        // This is a fairly involved unit test, but this is unavoidable as the relevant code is quite dependent
        // on other infrastructure which is being mocked up here.

        // What is being tested here is that the AutoInstallIntegration code correctly responds to new modules
        // being added through the management console at runtime.

        // First we create a test version of AutoInstallIntegration that intercepts the installModule and startBundle
        // methods so that it can be checked that they are called.
        final ServiceName dummyService = ServiceName.of("dummy");
        final List<OSGiCapability> installedModules = new ArrayList<SubsystemState.OSGiCapability>();
        final List<OSGiCapability> startedBundles = new ArrayList<SubsystemState.OSGiCapability>();
        AutoInstallIntegration aii = new AutoInstallIntegration() {
            @Override
            ServiceName installModule(BundleManagerService bundleManager, OSGiCapability moduleMetaData) {
                installedModules.add(moduleMetaData);
                return dummyService;
            }

            @Override
            void startBundle(ServiceContainer serviceContainer, ServiceName serviceName, OSGiCapability moduleMetaData) {
                startedBundles.add(moduleMetaData);
            }
        };

        // Now we set up the SubsystemState object.
        List<OSGiCapability> modules = new ArrayList<SubsystemState.OSGiCapability>();
        OSGiCapability module = new OSGiCapability("abc", null);
        modules.add(module);
        SubsystemState state = Mockito.mock(SubsystemState.class);
        Mockito.when(state.getCapabilities()).thenReturn(modules);

        // Provide some mock injected services into AutoInstallIntegration
        aii.injectedSubsystemState.setValue(new ImmediateValue<SubsystemState>(state));
        aii.injectedBundleManager.setValue(Mockito.mock(BundleManagerService.class));

        // Here we create a mock serviceController that allows us to catch the (temporary) service
        // created by the update and start it later from inside this test.
        final List<Service<?>> addedServices = new ArrayList<Service<?>>(); // the caught services
        final ServiceBuilder<Void> builder = Mockito.mock(ServiceBuilder.class);
        ServiceContainer container = Mockito.mock(ServiceContainer.class);
        Mockito.when(container.addService((ServiceName) Mockito.any(), (Service<?>) Mockito.any())).thenAnswer(
            new Answer<ServiceBuilder<Void>>() {
                @Override
                public ServiceBuilder<Void> answer(InvocationOnMock invocation) throws Throwable {
                    addedServices.add((Service<?>) invocation.getArguments()[1]);
                    return builder;
                }
        });
        ServiceController<?> controller = Mockito.mock(ServiceController.class);
        Mockito.when(controller.getServiceContainer()).thenReturn(container);
        aii.serviceController = controller;

        // Do the actual Observer invocation on the AutoInstallIntegration object.
        SubsystemState.ChangeEvent event = new SubsystemState.ChangeEvent(SubsystemState.ChangeType.CAPABILITY, false, "abc");
        aii.update(null, event);

        Assert.assertEquals("The new module should have been installed in the system",
            1, installedModules.size());
        Assert.assertEquals(module, installedModules.get(0));
        Assert.assertEquals("The new bundle is not yet started, this is done in a service instead",
            0, startedBundles.size());
        Mockito.verify(builder).addDependency(dummyService);
        Mockito.verify(builder).install();

        // Now we're going to check that the service that was created inside aii.update() works correctly...
        // First, mock up a separate ServiceController, specific to the service created in aii.update()
        ServiceController innerController = Mockito.mock(ServiceController.class);
        StartContext context = Mockito.mock(StartContext.class);
        Mockito.when(context.getController()).thenReturn(innerController);
        Assert.assertEquals(1, addedServices.size());

        // Call start() on the service, which should do its work...
        addedServices.get(0).start(context);

        Assert.assertEquals("The bundle should have been started", 1, startedBundles.size());
        Assert.assertEquals(module, startedBundles.get(0));
        // The service should have been removed again after doing its work.
        Mockito.verify(innerController).setMode(Mode.REMOVE);
    }

    @Test
    public void testUpdateNull() {
        assertPreconditionForUpdateTest();

        TestHandler testHandler = new TestHandler();
        try {
            AutoInstallIntegration aii = new AutoInstallIntegration();
            aii.update(null, null);
            Assert.assertEquals("There should not be any error logs", 0, testHandler.records.size());
        } finally {
            testHandler.remove();
        }
    }

    @Test
    public void testUpdateOther() {
        assertPreconditionForUpdateTest();

        TestHandler testHandler = new TestHandler();
        try {
            AutoInstallIntegration aii = new AutoInstallIntegration();
            SubsystemState.ChangeEvent event = new SubsystemState.ChangeEvent(SubsystemState.ChangeType.PROPERTY, false, "testing");
            aii.update(null, event);
            Assert.assertEquals("There should not be any error logs", 0, testHandler.records.size());
        } finally {
            testHandler.remove();
        }
    }

    @Test
    public void testUpdateRemoveModule() {
        // Removing a module has no effect at this point in this. This tests verifies that it has no effect
        // but in the future it might be taken into effect at which point this test needs to be altered accordingly.
        assertPreconditionForUpdateTest();

        TestHandler testHandler = new TestHandler();
        try {
            AutoInstallIntegration aii = new AutoInstallIntegration();
            ModuleIdentifier id = ModuleIdentifier.fromString("testing");
            SubsystemState.ChangeEvent event = new SubsystemState.ChangeEvent(SubsystemState.ChangeType.CAPABILITY, true, id.toString());
            aii.update(null, event);
            Assert.assertEquals("There should not be any error logs", 0, testHandler.records.size());
        } finally {
            testHandler.remove();
        }
    }

    /**
     * The tests that call this method rely on intercepting logging messages for their assertions. These tests generally
     * expect no error log messages which means that all went well. To ensure that the actual error log messages do actually
     * exist, this precondition message makes an invalid invocation and checks that the error gets logged.
     */
    public void assertPreconditionForUpdateTest() {
        TestHandler testHandler = new TestHandler();

        try {
            AutoInstallIntegration aii = new AutoInstallIntegration();
            ModuleIdentifier id = ModuleIdentifier.fromString("testing");
            SubsystemState.ChangeEvent event = new SubsystemState.ChangeEvent(SubsystemState.ChangeType.CAPABILITY, false, id.toString());
            Assert.assertEquals("Precondition", 0, testHandler.records.size());

            aii.update(null, event);
            Assert.assertEquals("There should be an error log, because the update was called with insufficient services available",
                1, testHandler.records.size());
            Assert.assertEquals(Level.ERROR, testHandler.records.get(0).getLevel());
        } finally {
            testHandler.remove();
        }
    }

    private static class TestHandler extends ConsoleHandler {
        private final List<LogRecord> records = new ArrayList<LogRecord>();
        private final Logger logger;

        private TestHandler() {
            logger = Logger.getLogger("org.jboss.as.osgi");
            logger.addHandler(this);
        }

        @Override
        public void publish(LogRecord record) {
            records.add(record);
        }

        public void remove() {
            logger.removeHandler(this);
        }
    }
}
