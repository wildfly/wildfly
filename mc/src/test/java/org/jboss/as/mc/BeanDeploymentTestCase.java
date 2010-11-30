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

package org.jboss.as.mc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.naming.Context;
import javax.naming.NamingException;

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
import org.jboss.as.deployment.naming.ContextNames;
import org.jboss.as.deployment.processor.AnnotationIndexProcessor;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Simple MC beans test.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class BeanDeploymentTestCase extends AbstractMcDeploymentTest {
    private static final ServiceName TEST_SERVICE_NAME = ServiceName.of("some-old-mc-bean");

    @BeforeClass
    public static void setupChain() {

    }

    @Override
    protected void setupServices(final BatchBuilder batchBuilder) throws Exception {
        super.setupServices(batchBuilder);

        final DeploymentChain deploymentChain = new DeploymentChainImpl();
        deploymentChain.addProcessor(new ManifestAttachmentProcessor(), Phase.MANIFEST_ATTACHMENT_PROCESSOR);
        deploymentChain.addProcessor(new AnnotationIndexProcessor(), Phase.ANNOTATION_INDEX_PROCESSOR);
        deploymentChain.addProcessor(new ModuleDependencyProcessor(), Phase.MODULE_DEPENDENCY_PROCESSOR);
        deploymentChain.addProcessor(new ModuleConfigProcessor(), Phase.MODULE_CONFIG_PROCESSOR);
        deploymentChain.addProcessor(new DeploymentModuleLoaderProcessor(new DeploymentModuleLoaderImpl()), Phase.DEPLOYMENT_MODULE_LOADER_PROCESSOR);
        deploymentChain.addProcessor(new ModuleDeploymentProcessor(), Phase.MODULE_DEPLOYMENT_PROCESSOR);

        deploymentChain.addProcessor(new KernelDeploymentParsingProcessor(), Phase.MC_BEAN_DEPLOYMENT_PARSING_PROCESSOR);
        deploymentChain.addProcessor(new ParsedKernelDeploymentProcessor(), Phase.PARSED_MC_BEAN_DEPLOYMENT_PROCESSOR);

        batchBuilder.addService(DeploymentChain.SERVICE_NAME, new DeploymentChainService(deploymentChain));

        Service<Context> ns = new AbstractService<Context>() {
            @Override
            public Context getValue() throws IllegalStateException {
                try {
                    return new MockContext();
                } catch (NamingException e) {
                    throw new IllegalStateException(e);
                }
            }
        };
        batchBuilder.addService(ContextNames.GLOBAL_CONTEXT_SERVICE_NAME, ns);
    }

    @Test
    public void testDeployment() throws Exception {

        executeDeployment(initializeDeployment("/test/mcXmlDeployment.jar"));

        final ServiceController<?> testBeanController = serviceContainer.getService(TEST_SERVICE_NAME);
        assertNotNull(testBeanController);
        Object bean = testBeanController.getValue();
        assertNotNull(bean);
        LegacyBean lb = (LegacyBean) bean;
        assertEquals("somestring", lb.getString());
    }

    private VirtualFile initializeDeployment(final String path) throws Exception {
        final VirtualFile virtualFile = VFS.getChild(getResource(BeanDeploymentTestCase.class, path));
        copyResource(BeanDeploymentTestCase.class, "/org/jboss/as/mc/LegacyBean.class", path, "org/jboss/as/mc");
        return virtualFile;
    }

}
