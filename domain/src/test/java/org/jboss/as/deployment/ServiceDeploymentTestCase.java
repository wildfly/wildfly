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
import org.jboss.as.deployment.chain.DeploymentChainProvider;
import org.jboss.as.deployment.chain.DeploymentChainService;
import org.jboss.as.deployment.module.DeploymentModuleLoader;
import org.jboss.as.deployment.module.DeploymentModuleLoaderImpl;
import org.jboss.as.deployment.module.DeploymentModuleLoaderProvider;
import org.jboss.as.deployment.module.DeploymentModuleLoaderSelector;
import org.jboss.as.deployment.processor.ModuleConfgProcessor;
import org.jboss.as.deployment.processor.ModuleDependencyProcessor;
import org.jboss.as.deployment.processor.ModuleDeploymentProcessor;
import org.jboss.as.deployment.processor.ParsedServiceDeploymentProcessor;
import org.jboss.as.deployment.processor.ServiceDeploymentParsingProcessor;
import org.jboss.as.deployment.processor.ServiceDeploymentProcessor;
import org.jboss.as.deployment.test.LegacyService;
import org.jboss.as.deployment.test.PassthroughService;
import org.jboss.as.deployment.test.TestModuleDependencyProcessor;
import org.jboss.as.deployment.test.TestServiceDeployment;
import org.jboss.as.deployment.unit.DeploymentUnitProcessor;
import org.jboss.as.deployment.unit.DeploymentUnitProcessorService;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoaderSelector;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.BatchServiceBuilder;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.TimingServiceListener;
import org.jboss.msc.value.Values;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Test case to do some basic Service deployment functionality checking.
 *
 * @author John E. Bailey
 */
public class ServiceDeploymentTestCase extends AbstractDeploymentTest {
    private static final ServiceName DEPLOYMENT_MANAGER_NAME = ServiceName.JBOSS.append("deployment", "manager");
    private static final ServiceName CHAIN_SERVICE_NAME = ServiceName.JBOSS.append("deployment", "chain", "service");
    private static final ServiceName DEPLOYMENT_MODULE_LOADER_SERVICE_NAME = ServiceName.JBOSS.append("deployment", "module", "loader");

    @Test
    public void testServiceDeployment() throws Exception {
        Module.setModuleLoaderSelector(new DeploymentModuleLoaderSelector());
        final ServiceContainer serviceContainer = ServiceContainer.Factory.create();
        final DeploymentManager deploymentManager = setupDeploymentManger(serviceContainer);
        setupProcessors(serviceContainer);
        final DeploymentResult result = deploymentManager.deploy(initializeDeployment("/test/serviceDeployment.jar")).getDeploymentResult();
        assertDeploymentSuccess(result);

        final ServiceController<?> testServiceController = serviceContainer.getService(TestServiceDeployment.TEST_SERVICE_NAME);
        assertNotNull(testServiceController);
        assertEquals(ServiceController.State.UP, testServiceController.getState());
        serviceContainer.shutdown();
    }

    @Test
    public void testParsedDeployment() throws Exception {
        Module.setModuleLoaderSelector(new DeploymentModuleLoaderSelector());
        final ServiceContainer serviceContainer = ServiceContainer.Factory.create();
        final DeploymentManager deploymentManager = setupDeploymentManger(serviceContainer);
        setupProcessors(serviceContainer);
        final DeploymentResult result = deploymentManager.deploy(initializeDeployment("/test/serviceXmlDeployment.jar")).getDeploymentResult();
        assertDeploymentSuccess(result);

        final ServiceController<?> testServiceController = serviceContainer.getService(TestServiceDeployment.TEST_SERVICE_NAME);
        assertNotNull(testServiceController);
        assertEquals(ServiceController.State.UP, testServiceController.getState());
        final LegacyService legacyService = (LegacyService)testServiceController.getValue();
        assertNotNull(legacyService);
        assertEquals("Test Value", legacyService.getSomethingElse());

        final ServiceController<?> testServiceControllerTwo = serviceContainer.getService(TestServiceDeployment.TEST_SERVICE_NAME.append("second"));
        assertNotNull(testServiceControllerTwo);
        assertEquals(ServiceController.State.UP, testServiceControllerTwo.getState());
        final LegacyService legacyServiceTwo = (LegacyService)testServiceControllerTwo.getValue();
        assertNotNull(legacyServiceTwo);
        assertEquals(legacyService, legacyServiceTwo.getOther());
        assertEquals("Test Value - more value", legacyServiceTwo.getSomethingElse());

        final ServiceController<?> testServiceControllerThree = serviceContainer.getService(TestServiceDeployment.TEST_SERVICE_NAME.append("third"));
        assertNotNull(testServiceControllerThree);
        assertEquals(ServiceController.State.UP, testServiceControllerThree.getState());
        final LegacyService legacyServiceThree = (LegacyService)testServiceControllerThree.getValue();
        assertNotNull(legacyServiceThree);
        assertEquals(legacyService, legacyServiceThree.getOther());
        assertEquals("Another test value", legacyServiceThree.getSomethingElse());

        serviceContainer.shutdown();
    }

    private DeploymentManager setupDeploymentManger(final ServiceContainer serviceContainer) throws Exception {
        final DeploymentChain deploymentChain = new DeploymentChainImpl("deployment.chain.service");
        final DeploymentModuleLoader deploymentModuleLoader = new DeploymentModuleLoaderImpl(ModuleLoaderSelector.DEFAULT.getCurrentLoader());
        final DeploymentManagerImpl deploymentManager = new DeploymentManagerImpl(serviceContainer);

        final BatchBuilder builder = serviceContainer.batchBuilder();
        final CountDownLatch latch = new CountDownLatch(1);
        final TimingServiceListener listener = new TimingServiceListener(new Runnable() {
            @Override
            public void run() {
                latch.countDown();
            }
        });
        builder.addListener(listener);

        builder.addService(DeploymentChainProvider.SERVICE_NAME, new DeploymentChainProvider());
        final DeploymentChainService deploymentChainService = new DeploymentChainService(deploymentChain);
        builder.addService(CHAIN_SERVICE_NAME, deploymentChainService)
            .addDependency(DeploymentChainProvider.SERVICE_NAME).toInjector(
                new DeploymentChainProvider.SelectorInjector(deploymentChainService,
                        Values.immediateValue(new DeploymentChainProvider.Selector() {
                            public boolean supports(VirtualFile root) {
                                return true;
                            }
                }), 0));

        builder.addService(DeploymentModuleLoaderProvider.SERVICE_NAME, new DeploymentModuleLoaderProvider());
        final Service<DeploymentModuleLoader> deploymentModuleLoaderService = new PassthroughService(deploymentModuleLoader);
        builder.addService(DEPLOYMENT_MODULE_LOADER_SERVICE_NAME, deploymentModuleLoaderService)
            .addDependency(DeploymentModuleLoaderProvider.SERVICE_NAME).toInjector(
                new DeploymentModuleLoaderProvider.SelectorInjector(deploymentModuleLoaderService,
                        Values.immediateValue(new DeploymentModuleLoaderProvider.Selector() {
                            public boolean supports(VirtualFile root) {
                                return true;
                            }
                }), 0));

        final BatchServiceBuilder deploymentManagerServiceBuilder = builder.addService(DEPLOYMENT_MANAGER_NAME, deploymentManager);
        deploymentManagerServiceBuilder.addDependency(CHAIN_SERVICE_NAME);
        deploymentManagerServiceBuilder.addDependency(DEPLOYMENT_MODULE_LOADER_SERVICE_NAME);
        builder.install();
        listener.finishBatch();
        latch.await(1L, TimeUnit.SECONDS);
        if (!listener.finished())
            fail("Did not install deployment manager within 1 second.");

        if (!ServiceController.State.UP.equals(serviceContainer.getService(DEPLOYMENT_MANAGER_NAME).getState()))
            Thread.sleep(100L);
        assertEquals(ServiceController.State.UP, serviceContainer.getService(DEPLOYMENT_MANAGER_NAME).getState());
        return deploymentManager;
    }

    private void setupProcessors(final ServiceContainer serviceContainer) throws Exception {
        final BatchBuilder builder = serviceContainer.batchBuilder();
        final CountDownLatch latch = new CountDownLatch(1);
        final TimingServiceListener listener = new TimingServiceListener(new Runnable() {
            public void run() {
                latch.countDown();
            }
        });
        builder.addListener(listener);

        addProcessor(builder, ServiceName.JBOSS.append("deployment", "processor", "module", "dependency"), new ModuleDependencyProcessor(), ModuleDependencyProcessor.PRIORITY);
        addProcessor(builder, ServiceName.JBOSS.append("deployment", "processor", "module", "dependency", "test"), new TestModuleDependencyProcessor(), TestModuleDependencyProcessor.PRIORITY);
        addProcessor(builder, ServiceName.JBOSS.append("deployment", "processor", "module", "config"), new ModuleConfgProcessor(), ModuleConfgProcessor.PRIORITY);
        addProcessor(builder, ServiceName.JBOSS.append("deployment", "processor", "module", "deployment"), new ModuleDeploymentProcessor(), ModuleDeploymentProcessor.PRIORITY);
        addProcessor(builder, ServiceName.JBOSS.append("deployment", "processor", "service", "parser"), new ServiceDeploymentParsingProcessor(), ServiceDeploymentParsingProcessor.PRIORITY);
        addProcessor(builder, ServiceName.JBOSS.append("deployment", "processor", "service", "deployment"), new ServiceDeploymentProcessor(), ServiceDeploymentProcessor.PRIORITY);
        addProcessor(builder, ServiceName.JBOSS.append("deployment", "processor", "service", "parsed", "deployment"), new ParsedServiceDeploymentProcessor(), ParsedServiceDeploymentProcessor.PRIORITY);

        builder.install();
        listener.finishBatch();
        latch.await(1L, TimeUnit.SECONDS);
        if (!listener.finished())
            fail("Did not install processors within 1 seconds");
    }

    private <T extends DeploymentUnitProcessor> DeploymentUnitProcessorService<T> addProcessor(final BatchBuilder builder, final ServiceName serviceName, final T deploymentUnitProcessor, final long priority) {
        final DeploymentUnitProcessorService<T> deploymentUnitProcessorService = new DeploymentUnitProcessorService<T>(deploymentUnitProcessor);
        builder.addService(serviceName, deploymentUnitProcessorService)
                .addDependency(CHAIN_SERVICE_NAME).toInjector(new DeploymentChainProcessorInjector<T>(deploymentUnitProcessorService, priority));
        return deploymentUnitProcessorService;
    }

    private VirtualFile initializeDeployment(final String path) throws Exception {
        final VirtualFile virtualFile = VFS.getChild(getResource(path));
        copyResource("/org/jboss/as/deployment/test/TestServiceDeployment.class", path, "org/jboss/as/deployment/test");
        copyResource("/org/jboss/as/deployment/test/LegacyService.class", path, "org/jboss/as/deployment/test");
        return virtualFile;
    }

    private void assertDeploymentSuccess(DeploymentResult result) {
        assertNotNull(result);
        if(result.getDeploymentException() != null) {
            result.getDeploymentException().printStackTrace();
            for(Map.Entry<ServiceName, StartException> entry : result.getServiceFailures().entrySet()) {
                System.out.println("Service [" + entry.getKey() +"] failed to start.");
                entry.getValue().printStackTrace();
            }
            fail("Deployment failed");
        }
        assertEquals(DeploymentResult.Result.SUCCESS, result.getResult());
    }


}
