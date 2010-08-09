/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.deployment;

import org.jboss.as.deployment.chain.DeploymentChain;
import org.jboss.as.deployment.chain.DeploymentChainImpl;
import org.jboss.as.deployment.chain.DeploymentChainProcessorInjector;
import org.jboss.as.deployment.item.DeploymentItemRegistry;
import org.jboss.as.deployment.module.DeploymentModuleLoaderSelector;
import org.jboss.as.deployment.service.ServiceDeploymentActivator;
import org.jboss.as.deployment.test.LegacyService;
import org.jboss.as.deployment.test.TestModuleDependencyProcessor;
import org.jboss.as.deployment.unit.DeploymentUnitProcessorService;
import org.jboss.as.model.DeploymentUnitKey;
import org.jboss.as.model.ServerGroupAddDeploymentUpdate;
import org.jboss.as.model.ServerGroupDeploymentElement;
import org.jboss.as.model.ServerGroupElement;
import org.jboss.modules.Module;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceActivatorContextImpl;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.TimingServiceListener;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Test case to do some basic Service deployment functionality checking.
 *
 * @author John E. Bailey
 */
public class ServiceDeploymentTestCase extends AbstractDeploymentTest {

    private static final ServiceName TEST_SERVICE_NAME = ServiceName.JBOSS.append("test", "service");
    private static final byte[] BLANK_SHA1 = new byte[20];

    private ServiceContainer serviceContainer;

    @Test
    public void testParsedDeployment() throws Exception {
        final BatchBuilder batchBuilder = serviceContainer.batchBuilder();
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicBoolean completed = new AtomicBoolean(false);
        final DeploymentServiceListener listener = new DeploymentServiceListener(new DeploymentServiceListener.Callback() {
            public void run(Map<ServiceName, StartException> serviceFailures, long elapsedTime, int numberServices) {
                completed.set(true);
                if(serviceFailures.size() > 0)
                    fail("Service failures: " + serviceFailures);
                // Ensure we count down the latch when all other tasks are done JBAS-8321
                latch.countDown();
            }
        });
        batchBuilder.addListener(listener);

        final VirtualFile deploymentFile = initializeDeployment("/test/serviceXmlDeployment.jar");
        final ServerGroupAddDeploymentUpdate update = new ServerGroupAddDeploymentUpdate(deploymentFile.getPathName(), BLANK_SHA1);
        executeUpdate(update);

        listener.startBatch();

        new ServerGroupDeploymentElement(null, deploymentFile.getPathName(), BLANK_SHA1, true, DeploymentItemRegistry.getDeploymentItems(new DeploymentUnitKey(deploymentFile.getPathName(), BLANK_SHA1))).activate(new ServiceActivatorContextImpl(batchBuilder));

        batchBuilder.install();
        listener.finishBatch();
        listener.finishDeployment();
        latch.await(10L, TimeUnit.SECONDS);
        if(!completed.get())
            fail("Services were not installed within a second");

        final ServiceController<?> testServiceController = serviceContainer.getService(TEST_SERVICE_NAME);
        assertNotNull(testServiceController);
        assertEquals(ServiceController.State.UP, testServiceController.getState());
        final LegacyService legacyService = (LegacyService)testServiceController.getValue();
        assertNotNull(legacyService);
        assertEquals("Test Value", legacyService.getSomethingElse());

        final ServiceController<?> testServiceControllerTwo = serviceContainer.getService(TEST_SERVICE_NAME.append("second"));
        assertNotNull(testServiceControllerTwo);
        assertEquals(ServiceController.State.UP, testServiceControllerTwo.getState());
        final LegacyService legacyServiceTwo = (LegacyService)testServiceControllerTwo.getValue();
        assertNotNull(legacyServiceTwo);
        assertEquals(legacyService, legacyServiceTwo.getOther());
        assertEquals("Test Value - more value", legacyServiceTwo.getSomethingElse());

        final ServiceController<?> testServiceControllerThree = serviceContainer.getService(TEST_SERVICE_NAME.append("third"));
        assertNotNull(testServiceControllerThree);
        assertEquals(ServiceController.State.UP, testServiceControllerThree.getState());
        final LegacyService legacyServiceThree = (LegacyService)testServiceControllerThree.getValue();
        assertNotNull(legacyServiceThree);
        assertEquals(legacyService, legacyServiceThree.getOther());
        assertEquals("Another test value", legacyServiceThree.getSomethingElse());

        serviceContainer.shutdown();
    }

    @Before
    public void setup() throws Exception {
        Module.setModuleLoaderSelector(new DeploymentModuleLoaderSelector());
        serviceContainer = ServiceContainer.Factory.create();
        final DeploymentChain deploymentChain = new DeploymentChainImpl("deployment.chain.service");

        final BatchBuilder builder = serviceContainer.batchBuilder();
        final CountDownLatch latch = new CountDownLatch(1);
        final TimingServiceListener listener = new TimingServiceListener(new Runnable() {
            @Override
            public void run() {
                latch.countDown();
            }
        });
        builder.addListener(listener);
        final ServiceActivatorContext serviceActivatorContext = new ServiceActivatorContextImpl(builder);
        new DeploymentActivator().activate(serviceActivatorContext);
        new ServiceDeploymentActivator().activate(serviceActivatorContext);

        final DeploymentUnitProcessorService<TestModuleDependencyProcessor> deploymentUnitProcessorService = new DeploymentUnitProcessorService<TestModuleDependencyProcessor>(new TestModuleDependencyProcessor());
        builder.addService(ServiceName.JBOSS.append("deployment", "processor", "module", "dependency", "test"), deploymentUnitProcessorService)
            .addDependency(ServiceDeploymentActivator.SERVICE_DEPLOYMENT_CHAIN_NAME, DeploymentChain.class, new DeploymentChainProcessorInjector<TestModuleDependencyProcessor>(deploymentUnitProcessorService, TestModuleDependencyProcessor.PRIORITY));

        builder.install();
        listener.finishBatch();
        latch.await(1L, TimeUnit.SECONDS);
        if (!listener.finished())
            fail("Did not install service deployment components within 1 second.");
    }

    private VirtualFile initializeDeployment(final String path) throws Exception {
        final VirtualFile virtualFile = VFS.getChild(getResource(path));
        copyResource("/org/jboss/as/deployment/test/LegacyService.class", path, "org/jboss/as/deployment/test");
        return virtualFile;
    }

    private void executeUpdate(ServerGroupAddDeploymentUpdate update) throws Exception {
        final Method method = ServerGroupAddDeploymentUpdate.class.getDeclaredMethod("applyUpdate", ServerGroupElement.class);
        method.setAccessible(true);
        method.invoke(update, (ServerGroupElement)null);
    }
}
