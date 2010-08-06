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
import org.jboss.as.model.DeploymentUnitElement;
import org.jboss.as.model.Domain;
import org.jboss.as.model.DomainDeploymentUnitUpdate;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.ServiceActivatorContextImpl;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.TimingServiceListener;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

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

        new DeploymentActivator().activate(new ServiceActivatorContextImpl(batchBuilder));

        DeploymentChainProvider.INSTANCE.addDeploymentChain(deploymentChain,
            new DeploymentChainProvider.Selector() {
                public boolean supports(VirtualFile root) {
                    return true;
                }
            }, 0);

        batchBuilder.install();
    }

    @After
    public void shutdown() {
        serviceContainer.shutdown();
    }

    @Test
    public void testDeployVirtualFile() throws Exception {
        final VirtualFile virtualFile = VFS.getChild(getResource("/test/serviceXmlDeployment.jar"));
        final String expectedDeploymentName = virtualFile.getPathName() + ":";

        final BatchBuilder batchBuilder = serviceContainer.batchBuilder();
        final CountDownLatch latch = new CountDownLatch(1);
        final TimingServiceListener listener = new TimingServiceListener(new Runnable() {
            @Override
            public void run() {
                latch.countDown();
            }
        });
        batchBuilder.addListener(listener);

        final DomainDeploymentUnitUpdate update = new DomainDeploymentUnitUpdate(virtualFile.getPathName(), new byte[0]);
        executeUpdate(update);

        new DeploymentUnitElement(null, virtualFile.getPathName(), new byte[0], true, true).activate(new ServiceActivatorContextImpl(batchBuilder));

        batchBuilder.install();
        listener.finishBatch();
        latch.await(1L, TimeUnit.SECONDS);
        if (!listener.finished())
            fail("Did not install deployment within 1 second.");

        // Verify the DeploymentService is correctly setup
        final ServiceController<?> serviceController = serviceContainer.getService(DeploymentService.SERVICE_NAME.append(expectedDeploymentName));
        assertNotNull(serviceController);

        assertEquals(ServiceController.State.UP, serviceController.getState());
    }

    private void executeUpdate(DomainDeploymentUnitUpdate update) throws Exception {
        final Method method = DomainDeploymentUnitUpdate.class.getDeclaredMethod("applyUpdate", Domain.class);
        method.setAccessible(true);
        method.invoke(update, (Domain)null);
    }
}