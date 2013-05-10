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

import org.apache.catalina.core.StandardContext;
import org.jboss.as.ee.structure.Attachments;
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.web.common.WarMetaData;
import org.jboss.modules.Module;
import org.jboss.vfs.VirtualFile;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * Unit tests for replacing {@link StandardContext} in web deployment processors
 *
 * @author Siamak Sadeghianfar
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ResourceRoot.class, Module.class, VirtualFile.class})
@SuppressStaticInitializationFor("org.jboss.modules.Module")
@Ignore("WFLY-1331 - The order in which this test is run seems to matter")
public class JBossWebParsingDeploymentProcessorTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  private DeploymentPhaseContext phaseContext;

  @Mock
  private DeploymentUnit deploymentUnit;

  private JBossWebParsingDeploymentProcessor processor = new JBossWebParsingDeploymentProcessor();

  @Before
  public void init() {
    when(phaseContext.getDeploymentUnit()).thenReturn(deploymentUnit);
    when(deploymentUnit.getAttachment(Attachments.DEPLOYMENT_TYPE)).thenReturn(DeploymentType.WAR);
    WarMetaData metadata = mock(WarMetaData.class);
    when(deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY)).thenReturn(metadata);
  }

  @Test
  public void testLoadCorrectJbossWeb() throws Exception {
    final VirtualFile jbossWebxml = mock(VirtualFile.class);
    when(jbossWebxml.exists()).thenReturn(Boolean.TRUE);
    when(jbossWebxml.openStream()).thenReturn(JBossWebParsingDeploymentProcessorTest.class
        .getResourceAsStream("jboss-web.xml"));

    final VirtualFile deploymentRoot = mock(VirtualFile.class);
    when(deploymentRoot.getChild("WEB-INF/jboss-web.xml")).thenReturn(jbossWebxml);

    final ResourceRoot resourceRoot = mock(ResourceRoot.class);
    when(resourceRoot.getRoot()).thenReturn(deploymentRoot);
    when(deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.DEPLOYMENT_ROOT))
        .thenReturn(resourceRoot);
    processor.deploy(phaseContext);
  }

  @Test
  public void testLoadIncorrectJbossWeb() throws Exception {
    expectedException.expect(DeploymentUnitProcessingException.class);
    expectedException.expectMessage("JBAS018014: Failed to parse XML descriptor \"/content/basic.war/WEB-INF/jboss-web.xml\" at [4,5]");

    final VirtualFile jbossWebxml = mock(VirtualFile.class);
    when(jbossWebxml.exists()).thenReturn(Boolean.TRUE);
    when(jbossWebxml.openStream()).thenReturn(JBossWebParsingDeploymentProcessorTest.class
        .getResourceAsStream("jboss-error-web.xml"));
    when(jbossWebxml.toString()).thenReturn("\"/content/basic.war/WEB-INF/jboss-web.xml\"");

    final VirtualFile deploymentRoot = mock(VirtualFile.class);
    when(deploymentRoot.getChild("WEB-INF/jboss-web.xml")).thenReturn(jbossWebxml);

    final ResourceRoot resourceRoot = mock(ResourceRoot.class);
    when(resourceRoot.getRoot()).thenReturn(deploymentRoot);
    when(deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.DEPLOYMENT_ROOT))
        .thenReturn(resourceRoot);
    processor.deploy(phaseContext);
  }

}
