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
import org.jboss.as.deployment.module.DeploymentModuleLoaderProcessor;
import org.jboss.as.deployment.module.ModuleConfigProcessor;
import org.jboss.as.deployment.module.ModuleDependencyProcessor;
import org.jboss.as.deployment.module.ModuleDeploymentProcessor;
import org.jboss.as.deployment.processor.AnnotationIndexProcessor;
import org.jboss.as.deployment.service.ParsedServiceDeploymentProcessor;
import org.jboss.as.deployment.service.ServiceDeploymentParsingProcessor;
import org.jboss.as.deployment.test.LegacyService;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.jboss.as.deployment.TestUtils.copyResource;
import static org.jboss.as.deployment.TestUtils.getResource;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Test case to do some basic Service deployment functionality checking.
 *
 * @author John E. Bailey
 */
public class ServiceDeploymentTestCase extends AbstractDeploymentTest {

    private static final ServiceName TEST_SERVICE_NAME = ServiceName.JBOSS.append("test", "service");

    @BeforeClass
    public static void setupChain() {
        final DeploymentChain deploymentChain = new DeploymentChainImpl("deployment.chain.service");
        deploymentChain.addProcessor(new AnnotationIndexProcessor(), AnnotationIndexProcessor.PRIORITY);
        deploymentChain.addProcessor(new ModuleDependencyProcessor(), ModuleDependencyProcessor.PRIORITY);
        deploymentChain.addProcessor(new ModuleConfigProcessor(), ModuleConfigProcessor.PRIORITY);
        deploymentChain.addProcessor(new DeploymentModuleLoaderProcessor(), DeploymentModuleLoaderProcessor.PRIORITY);
        deploymentChain.addProcessor(new ModuleDeploymentProcessor(), ModuleDeploymentProcessor.PRIORITY);
        deploymentChain.addProcessor(new ServiceDeploymentParsingProcessor(), ServiceDeploymentParsingProcessor.PRIORITY);
        deploymentChain.addProcessor(new ParsedServiceDeploymentProcessor(), ParsedServiceDeploymentProcessor.PRIORITY);
        DeploymentChainProvider.INSTANCE.addDeploymentChain(deploymentChain,
            new DeploymentChainProvider.Selector() {
                public boolean supports(VirtualFile root) {
                    return "serviceXmlDeployment.jar".equals(root.getName());
                }
            }
        );
    }

    @Test
    public void testDeployment() throws Exception {
        executeDeployment(initializeDeployment("/test/serviceXmlDeployment.jar"));

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
    }

    private VirtualFile initializeDeployment(final String path) throws Exception {
        final VirtualFile virtualFile = VFS.getChild(getResource(ServiceDeploymentTestCase.class, path));
        copyResource(ServiceDeploymentTestCase.class, "/org/jboss/as/deployment/test/LegacyService.class", path, "org/jboss/as/deployment/test");
        return virtualFile;
    }
}
