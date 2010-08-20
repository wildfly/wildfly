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
import org.jboss.as.deployment.managedbean.ManagedBeanAnnotationProcessor;
import org.jboss.as.deployment.managedbean.ManagedBeanDeploymentProcessor;
import org.jboss.as.deployment.managedbean.ManagedBeanService;
import org.jboss.as.deployment.module.DeploymentModuleLoaderProcessor;
import org.jboss.as.deployment.module.ModuleConfigProcessor;
import org.jboss.as.deployment.module.ModuleDependencyProcessor;
import org.jboss.as.deployment.module.ModuleDeploymentProcessor;
import org.jboss.as.deployment.naming.ContextNames;
import org.jboss.as.deployment.naming.ModuleContextProcessor;
import org.jboss.as.deployment.processor.AnnotationIndexProcessor;
import org.jboss.as.deployment.test.MockContext;
import org.jboss.as.deployment.test.PassthroughService;
import org.jboss.as.deployment.test.TestManagedBean;
import org.jboss.as.deployment.test.TestManagedBeanWithInjection;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.naming.Context;
import javax.naming.LinkRef;

import static org.jboss.as.deployment.TestUtils.copyResource;
import static org.jboss.as.deployment.TestUtils.getResource;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * Test case to do some basic Managed bean deployment functionality checking.
 *
 * @author John E. Bailey
 */
public class ManagedBeanDeploymentTestCase extends AbstractDeploymentTest {
    private static final DeploymentChain deploymentChain = new DeploymentChainImpl("deployment.chain.managedbean");
    private static DeploymentModuleLoaderProcessor deploymentModuleLoaderProcessor = new DeploymentModuleLoaderProcessor();
    
    Context javaContext;

    @BeforeClass
    public static void setupChain() {
        deploymentChain.addProcessor(new AnnotationIndexProcessor(), AnnotationIndexProcessor.PRIORITY);
        deploymentChain.addProcessor(new ManagedBeanAnnotationProcessor(), ManagedBeanAnnotationProcessor.PRIORITY);
        deploymentChain.addProcessor(new ModuleDependencyProcessor(), ModuleDependencyProcessor.PRIORITY);
        deploymentChain.addProcessor(new ModuleConfigProcessor(), ModuleConfigProcessor.PRIORITY);
        deploymentChain.addProcessor(new ModuleDeploymentProcessor(), ModuleDeploymentProcessor.PRIORITY);
        deploymentChain.addProcessor(new ModuleContextProcessor(), ModuleContextProcessor.PRIORITY);
        deploymentChain.addProcessor(new ManagedBeanDeploymentProcessor(), ManagedBeanDeploymentProcessor.PRIORITY);

        DeploymentChainProvider.INSTANCE.addDeploymentChain(deploymentChain,
            new DeploymentChainProvider.Selector() {
                public boolean supports(VirtualFile root) {
                    return "managedBeanDeployment.jar".equals(root.getName());
                }
            }
        );
    }

    @Override
    protected void setupServices(BatchBuilder batchBuilder) throws Exception {
        deploymentChain.removeProcessor(deploymentModuleLoaderProcessor, DeploymentModuleLoaderProcessor.PRIORITY);
        deploymentModuleLoaderProcessor = new DeploymentModuleLoaderProcessor();
        deploymentChain.addProcessor(deploymentModuleLoaderProcessor, DeploymentModuleLoaderProcessor.PRIORITY);

        javaContext = new MockContext();
        batchBuilder.addService(ContextNames.JAVA_CONTEXT_SERVICE_NAME, new PassthroughService<Context>(javaContext));
        final Context globalContext = javaContext.createSubcontext("global");
        batchBuilder.addService(ContextNames.GLOBAL_CONTEXT_SERVICE_NAME, new PassthroughService<Context>(globalContext));
        globalContext.bind("someNumber", Integer.valueOf(99));
    }

    @Test
    public void testDeployment() throws Exception {
        final VirtualFile deploymentRoot = initializeDeployment("/test/managedBeanDeployment.jar");

        final String expectedDeploymentName = getDeploymentName(deploymentRoot);
        final LinkRef moduleLink = new LinkRef("java:global/" + expectedDeploymentName);
        javaContext.rebind("module", moduleLink);

        executeDeployment(deploymentRoot);

        final ServiceController<?> testServiceController = serviceContainer.getService(ManagedBeanService.SERVICE_NAME.append(expectedDeploymentName, "TestBean"));
        assertNotNull(testServiceController);
        final TestManagedBean testManagedBean = (TestManagedBean) testServiceController.getValue();
        assertNotNull(testManagedBean);
        assertFalse(testManagedBean.equals(testServiceController.getValue()));
    }

    @Test
    public void testBasicInjection() throws Exception {
        final VirtualFile deploymentRoot = initializeDeployment("/test/managedBeanDeployment.jar");

        final String expectedDeploymentName = getDeploymentName(deploymentRoot);
        final LinkRef moduleLink = new LinkRef("java:global/" + expectedDeploymentName);
        javaContext.rebind("module", moduleLink);
        
        executeDeployment(deploymentRoot);

        final ServiceController<?> testServiceController = serviceContainer.getService(ManagedBeanService.SERVICE_NAME.append(expectedDeploymentName, "TestBeanWithInjection"));
        assertNotNull(testServiceController);
        final TestManagedBeanWithInjection testManagedBean = (TestManagedBeanWithInjection) testServiceController.getValue();
        assertNotNull(testManagedBean);
        assertNotNull(testManagedBean.getOther());
        assertFalse(testManagedBean.equals(testServiceController.getValue()));
    }

    private VirtualFile initializeDeployment(final String path) throws Exception {
        final VirtualFile virtualFile = VFS.getChild(getResource(ManagedBeanDeploymentTestCase.class, path));
        copyResource(ManagedBeanDeploymentTestCase.class, "/org/jboss/as/deployment/test/TestManagedBean.class", path, "org/jboss/as/deployment/test");
        copyResource(ManagedBeanDeploymentTestCase.class, "/org/jboss/as/deployment/test/TestManagedBeanWithInjection.class", path, "org/jboss/as/deployment/test");
        return virtualFile;
    }
}