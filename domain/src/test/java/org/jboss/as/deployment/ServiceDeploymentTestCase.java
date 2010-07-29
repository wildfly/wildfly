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
import org.jboss.as.deployment.module.DeploymentModuleLoaderSelector;
import org.jboss.as.deployment.service.ServiceDeploymentActivator;
import org.jboss.as.deployment.test.LegacyService;
import org.jboss.as.deployment.test.TestModuleDependencyProcessor;
import org.jboss.as.deployment.test.TestServiceDeployment;
import org.jboss.as.deployment.unit.DeploymentUnitProcessorService;
import org.jboss.as.model.DeploymentUnitElement;
import org.jboss.modules.Module;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.TimingServiceListener;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;
import org.junit.Before;
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

    private ServiceContainer serviceContainer;

    @Test
    public void testServiceDeployment() throws Exception {
        final VirtualFile deploymentFile = initializeDeployment("/test/serviceDeployment.jar");

        final DeploymentResult result = new DeploymentUnitElement(null, deploymentFile.getPathName(), new byte[0], true, true).activate(serviceContainer).getDeploymentResult();
        assertDeploymentSuccess(result);

        final ServiceController<?> testServiceController = serviceContainer.getService(TestServiceDeployment.TEST_SERVICE_NAME);
        assertNotNull(testServiceController);
        assertEquals(ServiceController.State.UP, testServiceController.getState());
        serviceContainer.shutdown();
    }

    @Test
    public void testParsedDeployment() throws Exception {

        final VirtualFile deploymentFile = initializeDeployment("/test/serviceXmlDeployment.jar");
        final DeploymentResult result = new DeploymentUnitElement(null, deploymentFile.getPathName(), new byte[0], true, true).activate(serviceContainer).getDeploymentResult();
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

        new DeploymentActivator().activate(serviceContainer, builder);
        new ServiceDeploymentActivator().activate(serviceContainer, builder);

        final DeploymentUnitProcessorService<TestModuleDependencyProcessor> deploymentUnitProcessorService = new DeploymentUnitProcessorService<TestModuleDependencyProcessor>(new TestModuleDependencyProcessor());
        builder.addService(ServiceName.JBOSS.append("deployment", "processor", "module", "dependency", "test"), deploymentUnitProcessorService)
            .addDependency(ServiceDeploymentActivator.SERVICE_DEPLOYMENT_CHAIN_NAME)
                .toInjector(new DeploymentChainProcessorInjector<TestModuleDependencyProcessor>(deploymentUnitProcessorService, TestModuleDependencyProcessor.PRIORITY));

        builder.install();
        listener.finishBatch();
        latch.await(1L, TimeUnit.SECONDS);
        if (!listener.finished())
            fail("Did not install service deployment components within 1 second.");
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
