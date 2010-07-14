/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

import org.jboss.as.deployment.module.DeploymentModuleLoader;
import org.jboss.as.deployment.module.DeploymentModuleLoaderImpl;
import org.jboss.as.deployment.test.PassthroughService;
import org.jboss.as.deployment.unit.DeploymentChain;
import org.jboss.as.deployment.unit.DeploymentChainImpl;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * Test to verify the DeploymentManager correctly installs the deployment service.
 *
 * @author John E. Bailey
 */
public class DeploymentManagerTestCase {

    private ServiceContainer serviceContainer;
    private final DeploymentChain deploymentChain =  new DeploymentChainImpl("test.chain");
    private final DeploymentModuleLoader deploymentModuleLoader = new DeploymentModuleLoaderImpl(null);
    private DeploymentManager deploymentManager;

    @Before
    public void setupDeploymentManager() throws Exception {
        serviceContainer = ServiceContainer.Factory.create();
        final ServiceName chainServiceName = ServiceName.JBOSS.append("deployment", "chain");
        final ServiceName moduleLoaderServiceName = ServiceName.JBOSS.append("deployment", "module", "loader");

        deploymentManager = new DeploymentManager(serviceContainer) {
            @Override
            protected ServiceName determineDeploymentChain() {
                return chainServiceName;
            }

            @Override
            protected ServiceName determineDeploymentModuleLoader() {
                return moduleLoaderServiceName;
            }
        };

        final BatchBuilder batchBuilder = serviceContainer.batchBuilder();
        batchBuilder.addService(chainServiceName, new PassthroughService(deploymentChain));
        batchBuilder.addService(moduleLoaderServiceName, new PassthroughService(deploymentModuleLoader));
        batchBuilder.install();
    }

    @After
    public void shutdown() {
        serviceContainer.shutdown();
    }

    @Test
    public void testDeployVirtualFile() throws Exception {
        final VirtualFile virtualFile = VFS.getChild(getResource("/test/deploymentOne"));

        deploymentManager.deploy(virtualFile);

        // Verify the DeploymentService is correctly setup
        final ServiceController<?> serviceController = serviceContainer.getService(DeploymentService.SERVICE_NAME.append(virtualFile.getPathName()));
        assertNotNull(serviceController);
        assertEquals(ServiceController.State.UP, serviceController.getState());
        final DeploymentService deploymentService = (DeploymentService)serviceController.getValue();
        assertNotNull(deploymentService);

        assertEquals(virtualFile.getPathName(), deploymentService.getDeploymentName());
        assertEquals(deploymentChain, getPrivateFieldValue(deploymentService, "deploymentChain", DeploymentChain.class));
        assertEquals(deploymentModuleLoader, getPrivateFieldValue(deploymentService, "deploymentModuleLoader", DeploymentModuleLoader.class));

        // Verify the mount service is setup
        ServiceController<?> mountServiceController = serviceContainer.getService(ServiceName.JBOSS.append("mounts").append(virtualFile.getPathName()));
        assertNotNull(mountServiceController);
        assertEquals(ServiceController.State.UP, mountServiceController.getState());
        assertNull(mountServiceController.getValue());
    }

    @Test
    public void testDeploymentException() throws Exception {
        final VirtualFile virtualFile = VFS.getChild("/test/bogus");

        try {
            deploymentManager.deploy(virtualFile);
            fail("Should have thrown a DeploymentException");
        } catch(DeploymentException expected){
        }
    }

    private URL getResource(final String path) {
        return DeploymentManagerTestCase.class.getResource(path);
    }

    private <T> T getPrivateFieldValue(final Object target, final String fieldName, final Class<T> fieldType) throws Exception {
        final Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return (T)field.get(target);
    }
}