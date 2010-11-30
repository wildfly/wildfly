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

package org.jboss.as.service;

import org.jboss.as.deployment.Phase;
import org.jboss.as.deployment.chain.DeploymentChain;
import org.jboss.as.deployment.chain.DeploymentChainImpl;
import org.jboss.as.deployment.chain.DeploymentChainService;
import org.jboss.as.deployment.module.DeploymentModuleLoaderImpl;
import org.jboss.as.deployment.module.DeploymentModuleLoaderProcessor;
import org.jboss.as.deployment.module.ManifestAttachmentProcessor;
import org.jboss.as.deployment.module.ModuleConfigProcessor;
import org.jboss.as.deployment.module.ModuleDependencyProcessor;
import org.jboss.as.deployment.module.ModuleDeploymentProcessor;
import org.jboss.as.deployment.processor.AnnotationIndexProcessor;
import org.jboss.as.jmx.MBeanServerService;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Test case to do some basic Service deployment functionality checking.
 *
 * @author John E. Bailey
 */
public class ServiceDeploymentTestCase extends AbstractSarDeploymentTest {

    private static final ServiceName TEST_SERVICE_NAME = ServiceName.JBOSS.append("mbean", "service", "jboss:name=test,type=service");
    private static final ServiceName TEST_TWO_SERVICE_NAME = ServiceName.JBOSS.append("mbean", "service", "jboss:name=testTwo,type=service");
    private static final ServiceName TEST_THREE_SERVICE_NAME = ServiceName.JBOSS.append("mbean", "service", "jboss:name=testThree,type=service");

    @Override
    protected void setupServices(BatchBuilder batchBuilder) throws Exception {
        super.setupServices(batchBuilder);
        batchBuilder.addService(MBeanServerService.SERVICE_NAME, new MBeanServerService()).install();

        final DeploymentChain deploymentChain = new DeploymentChainImpl();
        batchBuilder.addService(DeploymentChain.SERVICE_NAME, new DeploymentChainService(deploymentChain)).install();
        deploymentChain.addProcessor(new ManifestAttachmentProcessor(), Phase.MANIFEST_ATTACHMENT_PROCESSOR);
        deploymentChain.addProcessor(new AnnotationIndexProcessor(), Phase.ANNOTATION_INDEX_PROCESSOR);
        deploymentChain.addProcessor(new ModuleDependencyProcessor(), Phase.MODULE_DEPENDENCY_PROCESSOR);
        deploymentChain.addProcessor(new ModuleConfigProcessor(), Phase.MODULE_CONFIG_PROCESSOR);
        deploymentChain.addProcessor(new DeploymentModuleLoaderProcessor(new DeploymentModuleLoaderImpl()), Phase.DEPLOYMENT_MODULE_LOADER_PROCESSOR);
        deploymentChain.addProcessor(new ModuleDeploymentProcessor(), Phase.MODULE_DEPLOYMENT_PROCESSOR);
        deploymentChain.addProcessor(new ServiceDeploymentParsingProcessor(), Phase.SERVICE_DEPLOYMENT_PARSING_PROCESSOR);
        deploymentChain.addProcessor(new ParsedServiceDeploymentProcessor(), Phase.PARSED_SERVICE_DEPLOYMENT_PROCESSOR);
    }

    @Test
    public void testDeployment() throws Exception {
        executeDeployment(initializeDeployment("/test/serviceXmlDeployment.jar"));

        final ServiceController<?> testServiceController = serviceContainer.getService(TEST_SERVICE_NAME.append("start"));
        assertNotNull(testServiceController);
        assertEquals(ServiceController.State.UP, testServiceController.getState());
        final LegacyService legacyService = (LegacyService)testServiceController.getValue();
        assertNotNull(legacyService);
        assertEquals("Test Value", legacyService.getSomethingElse());

        final ServiceController<?> testServiceControllerTwo = serviceContainer.getService(TEST_TWO_SERVICE_NAME.append("start"));
        assertNotNull(testServiceControllerTwo);
        assertEquals(ServiceController.State.UP, testServiceControllerTwo.getState());
        final LegacyService legacyServiceTwo = (LegacyService)testServiceControllerTwo.getValue();
        assertNotNull(legacyServiceTwo);
        assertEquals(legacyService, legacyServiceTwo.getOther());
        assertEquals("Test Value - more value", legacyServiceTwo.getSomethingElse());

        final ServiceController<?> testServiceControllerThree = serviceContainer.getService(TEST_THREE_SERVICE_NAME.append("start"));
        assertNotNull(testServiceControllerThree);
        assertEquals(ServiceController.State.UP, testServiceControllerThree.getState());
        final LegacyService legacyServiceThree = (LegacyService)testServiceControllerThree.getValue();
        assertNotNull(legacyServiceThree);
        assertEquals(legacyService, legacyServiceThree.getOther());
        assertEquals("Another test value", legacyServiceThree.getSomethingElse());
    }

    private VirtualFile initializeDeployment(final String path) throws Exception {
        final VirtualFile virtualFile = VFS.getChild(getResource(ServiceDeploymentTestCase.class, path));
        copyResource(ServiceDeploymentTestCase.class, "/org/jboss/as/service/LegacyService.class", path, "org/jboss/as/service");
        return virtualFile;
    }
}
