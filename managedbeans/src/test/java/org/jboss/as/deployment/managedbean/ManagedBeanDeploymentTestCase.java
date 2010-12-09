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

package org.jboss.as.deployment.managedbean;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.naming.Context;
import javax.naming.LinkRef;

import org.jboss.as.deployment.Phase;
import org.jboss.as.deployment.managedbean.container.ManagedBeanContainer;
import org.jboss.as.deployment.managedbean.container.ManagedBeanService;
import org.jboss.as.deployment.managedbean.processors.ManagedBeanAnnotationProcessor;
import org.jboss.as.deployment.managedbean.processors.ManagedBeanDeploymentProcessor;
import org.jboss.as.server.deployment.module.DeploymentModuleLoaderImpl;
import org.jboss.as.server.deployment.module.ManifestAttachmentProcessor;
import org.jboss.as.server.deployment.module.ModuleDependencyProcessor;
import org.jboss.as.server.deployment.module.ModuleDeploymentProcessor;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.deployment.ModuleContextProcessor;
import org.jboss.as.deployment.processor.AnnotationIndexProcessor;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test case to do some basic Managed bean deployment functionality checking.
 *
 * @author John E. Bailey
 */
public class ManagedBeanDeploymentTestCase extends AbstractManagedBeanTest {
    private static final DeploymentChain deploymentChain = new DeploymentChainImpl();
    private static DeploymentModuleLoaderProcessor deploymentModuleLoaderProcessor = new DeploymentModuleLoaderProcessor(new DeploymentModuleLoaderImpl());

    Context javaContext;

    @BeforeClass
    public static void setupChain() {
        deploymentChain.addProcessor(new ManifestAttachmentProcessor(), Phase.PARSE_MANIFEST);
        deploymentChain.addProcessor(new AnnotationIndexProcessor(), Phase.PARSE_ANNOTATION_INDEX);
        deploymentChain.addProcessor(new ManagedBeanAnnotationProcessor(), Phase.POST_MODULE_ANNOTATION_MANAGED_BEAN);
        deploymentChain.addProcessor(new ModuleDependencyProcessor(), Phase.DEPENDENCIES_MODULE);
        deploymentChain.addProcessor(new ModuleConfigProcessor(), Phase.MODULARIZE_CONFIG);
        deploymentChain.addProcessor(new ModuleDeploymentProcessor(), Phase.MODULARIZE_DEPLOYMENT);
        deploymentChain.addProcessor(new ModuleContextProcessor(), Phase.INSTALL_MODULE_CONTEXT);
        deploymentChain.addProcessor(new ManagedBeanDeploymentProcessor(), Phase.INSTALL_MANAGED_BEAN_DEPLOYMENT);
    }

    @Override
    protected void setupServices(final ServiceTarget target) throws Exception {
        super.setupServices(target);
        deploymentChain.removeProcessor(deploymentModuleLoaderProcessor, Phase.MODULARIZE_DEPLOYMENT_MODULE_LOADER);
        deploymentModuleLoaderProcessor = new DeploymentModuleLoaderProcessor(new DeploymentModuleLoaderImpl());
        deploymentChain.addProcessor(deploymentModuleLoaderProcessor, Phase.MODULARIZE_DEPLOYMENT_MODULE_LOADER);

        javaContext = new MockContext();
        target.addService(ContextNames.JAVA_CONTEXT_SERVICE_NAME, new PassthroughService<Context>(javaContext))
            .install();
        final Context globalContext = javaContext.createSubcontext("global");
        target.addService(ContextNames.GLOBAL_CONTEXT_SERVICE_NAME, new PassthroughService<Context>(globalContext))
            .install();
        globalContext.bind("someNumber", Integer.valueOf(99));

        target.addService(DeploymentChain.SERVICE_NAME, new DeploymentChainService(deploymentChain))
            .install();
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

        final ManagedBeanContainer<TestManagedBean> managedBeanContainer = (ManagedBeanContainer<TestManagedBean>)testServiceController.getValue();

        final TestManagedBean testManagedBean = managedBeanContainer.createInstance();
        assertNotNull(testManagedBean);
        assertFalse(testManagedBean.equals(managedBeanContainer.createInstance()));


        System.out.println(javaContext.lookup("java:global/" + expectedDeploymentName + "/TestBean"));
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
        final ManagedBeanContainer<TestManagedBeanWithInjection> managedBeanContainer = (ManagedBeanContainer<TestManagedBeanWithInjection>) testServiceController.getValue();
        final TestManagedBeanWithInjection testManagedBean = managedBeanContainer.createInstance();
        assertNotNull(testManagedBean);
        assertNotNull(testManagedBean.getOther());
        assertFalse(testManagedBean.equals(managedBeanContainer.createInstance()));
    }

    @Test
    public void testInterceptors() throws Exception {
        final VirtualFile deploymentRoot = initializeDeployment("/test/managedBeanDeployment.jar");

        final String expectedDeploymentName = getDeploymentName(deploymentRoot);
        final LinkRef moduleLink = new LinkRef("java:global/" + expectedDeploymentName);
        javaContext.rebind("module", moduleLink);

        executeDeployment(deploymentRoot);

        final ServiceController<?> testServiceController = serviceContainer.getService(ManagedBeanService.SERVICE_NAME.append(expectedDeploymentName, "TestBeanWithInjection"));
        assertNotNull(testServiceController);
        final ManagedBeanContainer<TestManagedBeanWithInjection> managedBeanContainer = (ManagedBeanContainer<TestManagedBeanWithInjection>) testServiceController.getValue();
        final TestManagedBeanWithInjection testManagedBean = managedBeanContainer.createInstance();
        assertNotNull(testManagedBean);

        TestInterceptor.invoked = false;
        TestManagedBeanWithInjection.invoked = false;
        assertNotNull(testManagedBean.getOther());
        assertTrue(TestInterceptor.invoked);
        assertTrue(TestManagedBeanWithInjection.invoked);
    }


    private VirtualFile initializeDeployment(final String path) throws Exception {
        final VirtualFile virtualFile = VFS.getChild(getResource(ManagedBeanDeploymentTestCase.class, path));
        copyResource(ManagedBeanDeploymentTestCase.class, "/org/jboss/as/deployment/managedbean/TestManagedBean.class", path, "org/jboss/as/deployment/managedbean");
        copyResource(ManagedBeanDeploymentTestCase.class, "/org/jboss/as/deployment/managedbean/TestManagedBeanWithInjection.class", path, "org/jboss/as/deployment/managedbean");
        copyResource(ManagedBeanDeploymentTestCase.class, "/org/jboss/as/deployment/managedbean/TestInterceptor.class", path, "org/jboss/as/deployment/managedbean");
        return virtualFile;
    }
}
