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
import org.jboss.as.deployment.chain.DeploymentChainService;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Test to verify the DeploymentUnitElement correctly installs the deployment service.
 *
 * @author John E. Bailey
 */
public class DeploymentUnitTestCase extends AbstractDeploymentTest {

    @Override
    protected void setupServices(BatchBuilder batchBuilder) throws Exception {
        super.setupServices(batchBuilder);

        final DeploymentChain deploymentChain = new DeploymentChainImpl();
        batchBuilder.addService(DeploymentChain.SERVICE_NAME, new DeploymentChainService(deploymentChain)).install();
    }

    @Test
    public void testDeployVirtualFile() throws Exception {
        final VirtualFile virtualFile = VFS.getChild(getResource(DeploymentUnitTestCase.class, "/test/testDeployment"));
        final String expectedDeploymentName = getDeploymentName(virtualFile);

        executeDeployment(virtualFile);

        // Verify the DeploymentService is correctly setup
        final ServiceController<?> serviceController = serviceContainer.getService(DeploymentService.SERVICE_NAME.append(expectedDeploymentName));
        assertNotNull(serviceController);

        assertEquals(ServiceController.State.UP, serviceController.getState());
    }
}
