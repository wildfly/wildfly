/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.web.deployment;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.io.File;

import org.apache.catalina.core.StandardContext;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.Services;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.web.ext.WebContextFactory;
import org.jboss.dmr.ModelNode;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.vfs.VirtualFile;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Unit tests for replacing {@link StandardContext} in web deployment processors
 *
 * @author Siamak Sadeghianfar
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ ResourceRoot.class, Module.class, VirtualFile.class })
@SuppressStaticInitializationFor("org.jboss.modules.Module")
public class WarDeploymentProcessorTest {
    private static final String HOST = "default-host";

    private WarDeploymentProcessor processor = new WarDeploymentProcessor(HOST);

    @Mock
    private DeploymentPhaseContext phaseContext;

    @Mock
    private DeploymentUnit deploymentUnit;

    @Mock
    private WebContextFactory contextFactory;

    @Captor
    private ArgumentCaptor<StandardContext> contextCaptor;

    @Before
    public void init() throws Exception {
        final WarMetaData warMetaData = new WarMetaData();
        warMetaData.setMergedJBossWebMetaData(new JBossWebMetaData());

        when(phaseContext.getDeploymentUnit()).thenReturn(deploymentUnit);

        final ServiceTarget serviceTarget = mock(ServiceTarget.class, RETURNS_DEEP_STUBS);
        when(phaseContext.getServiceTarget()).thenReturn(serviceTarget);
        when(deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY)).thenReturn(warMetaData);

        final ResourceRoot resourceRoot = mock(ResourceRoot.class);
        final VirtualFile deploymentRoot = mock(VirtualFile.class);
        when(deploymentRoot.getPhysicalFile()).thenReturn(new File("tmp"));
        when(resourceRoot.getRoot()).thenReturn(deploymentRoot);
        when(deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT)).thenReturn(resourceRoot);

        when(deploymentUnit.getAttachment(Attachments.MODULE)).thenReturn(mock(Module.class));
        when(deploymentUnit.getName()).thenReturn("webapp.war");
        when(deploymentUnit.getServiceName()).thenReturn(Services.deploymentUnitName("webapp.war"));
        when(deploymentUnit.getDeploymentSubsystemModel("web")).thenReturn(mock(ModelNode.class, RETURNS_DEEP_STUBS));
    }

    @Test
    public void testContextFactoryCreatesCustomContext() throws Exception {
        final CustomContext customContext = mock(CustomContext.class);

        when(deploymentUnit.getAttachment(WebContextFactory.ATTACHMENT)).thenReturn(contextFactory);
        when(contextFactory.createContext(deploymentUnit)).thenReturn(customContext);

        processor.deploy(phaseContext);

        // verify invocation order
        InOrder inOrder = Mockito.inOrder(customContext, contextFactory);
        inOrder.verify(contextFactory).createContext(deploymentUnit);
        inOrder.verify(customContext).addLifecycleListener(any(JBossContextConfig.class));
        inOrder.verify(contextFactory).postProcessContext(eq(deploymentUnit), contextCaptor.capture());

        // verify if context has been replaced
        assertEquals(contextCaptor.getValue(), customContext);
        assertTrue(contextCaptor.getValue() instanceof CustomContext);
    }

    class CustomContext extends StandardContext {
    }
}
