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
import org.jboss.as.deployment.chain.DeploymentChainProvider;
import org.jboss.as.deployment.chain.DeploymentChainService;
import org.jboss.as.model.DeploymentUnitElement;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.value.Values;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Test to verify the DeploymentUnitElement correctly installs the deployment service.
 *
 * @author John E. Bailey
 */
public class DeploymentUnitTestCase extends AbstractDeploymentTest {

    private ServiceContainer serviceContainer;
    private final DeploymentChain deploymentChain = new DeploymentChainImpl("test.chain");

    @Before
    public void setup() throws Exception {
        serviceContainer = ServiceContainer.Factory.create();
        final BatchBuilder batchBuilder = serviceContainer.batchBuilder();

        new DeploymentActivator().activate(serviceContainer, batchBuilder);

        final ServiceName chainServiceName = ServiceName.JBOSS.append("deployment", "chain");
        final DeploymentChainService deploymentChainService = new DeploymentChainService(deploymentChain);
        batchBuilder.addService(chainServiceName, deploymentChainService)
            .addDependency(DeploymentChainProvider.SERVICE_NAME).toInjector(
                new DeploymentChainProvider.SelectorInjector(deploymentChainService,
                        Values.immediateValue(new DeploymentChainProvider.Selector() {
                            public boolean supports(VirtualFile root) {
                                return true;
                            }
                }), 0));

        batchBuilder.install();
    }

    @After
    public void shutdown() {
        serviceContainer.shutdown();
    }

    @Test
    public void testDeployVirtualFile() throws Exception {
        final VirtualFile virtualFile = VFS.getChild(getResource("/test/serviceDeployment.jar"));
        final String expectedDeploymentName = virtualFile.getPathName() + ":";
        final DeploymentResult.Future resultFuture = new DeploymentUnitElement(null, virtualFile.getPathName(), new byte[0], true, true).activate(serviceContainer);
        final DeploymentResult deploymentResult = resultFuture.getDeploymentResult();
        assertNotNull(deploymentResult);
        assertEquals(DeploymentResult.Result.SUCCESS, deploymentResult.getResult());

        // Verify the DeploymentService is correctly setup
        final ServiceController<?> serviceController = serviceContainer.getService(DeploymentService.SERVICE_NAME.append(expectedDeploymentName));
        assertNotNull(serviceController);

        assertEquals(ServiceController.State.UP, serviceController.getState());

        // Verify the mount service is setup
        ServiceController<?> mountServiceController = serviceContainer.getService(ServiceName.JBOSS.append("mounts").append(expectedDeploymentName));
        assertNotNull(mountServiceController);
        assertEquals(ServiceController.State.UP, mountServiceController.getState());
        assertNull(mountServiceController.getValue());
    }

    @Test
    public void testDeploymentFailure() throws Exception {
        final VirtualFile virtualFile = VFS.getChild("/test/bogus");

        final DeploymentResult result = new DeploymentUnitElement(null, virtualFile.getPathName(), new byte[0], true, true).activate(serviceContainer).getDeploymentResult();
        assertNotNull(result);
        assertEquals(DeploymentResult.Result.FAILURE, result.getResult());
        assertNotNull(result.getDeploymentException());
    }
}