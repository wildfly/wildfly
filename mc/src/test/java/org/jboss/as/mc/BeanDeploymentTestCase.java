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

import javax.naming.Context;
import javax.naming.NamingException;

import org.jboss.as.deployment.attachment.VirtualFileAttachment;
import org.jboss.as.deployment.chain.DeploymentChain;
import org.jboss.as.deployment.chain.DeploymentChainProvider;
import org.jboss.as.deployment.chain.JarDeploymentActivator;
import org.jboss.as.deployment.module.DeploymentModuleLoaderImpl;
import org.jboss.as.deployment.module.DeploymentModuleLoaderService;
import org.jboss.as.deployment.naming.ContextNames;
import org.jboss.as.deployment.unit.DeploymentUnitContext;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;

import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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

        final DeploymentModuleLoaderService deploymentModuleLoaderService = new DeploymentModuleLoaderService(new DeploymentModuleLoaderImpl());
        batchBuilder.addService(DeploymentModuleLoaderService.SERVICE_NAME, deploymentModuleLoaderService);

        new JarDeploymentActivator().activate(new ServiceActivatorContext() {
            public BatchBuilder getBatchBuilder() {
                return batchBuilder;
            }
        });

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

        DeploymentChain deploymentChain = (DeploymentChain) serviceContainer.getService(JarDeploymentActivator.JAR_DEPLOYMENT_CHAIN_SERVICE_NAME).getValue();
        DeploymentChainProvider.INSTANCE.addDeploymentChain(deploymentChain,
            new DeploymentChainProvider.Selector() {
                public boolean supports(DeploymentUnitContext deploymentUnitContext) {
                    VirtualFile virtualFile = VirtualFileAttachment.getVirtualFileAttachment(deploymentUnitContext);
                    return "mcXmlDeployment.jar".equals(virtualFile.getName());
                }
            }
        );
        deploymentChain.addProcessor(new KernelDeploymentParsingProcessor(), KernelDeploymentParsingProcessor.PRIORITY);
        deploymentChain.addProcessor(new ParsedKernelDeploymentProcessor(), ParsedKernelDeploymentProcessor.PRIORITY);

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
