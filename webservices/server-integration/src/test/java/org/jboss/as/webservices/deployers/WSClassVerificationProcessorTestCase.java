/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

package org.jboss.as.webservices.deployers;

import org.jboss.as.server.deployment.AttachmentList;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class WSClassVerificationProcessorTestCase {

    private final DeploymentUnit unit = Mockito.mock(DeploymentUnit.class);
    private final DeploymentUnit rootUnit = Mockito.mock(DeploymentUnit.class);
    private final DeploymentUnit siblingUnit = Mockito.mock(DeploymentUnit.class);
    private final ModuleSpecification spec = new ModuleSpecification();
    private final ModuleSpecification rootSpec = new ModuleSpecification();
    private final ModuleSpecification siblingSpec = new ModuleSpecification();

    @Before
    public void setup() {
        Mockito.when(unit.getName()).thenReturn("sample.jar");
        Mockito.when(unit.getParent()).thenReturn(rootUnit);
        Mockito.when(unit.getAttachment(Attachments.MODULE_SPECIFICATION)).thenReturn(spec);

        Mockito.when(siblingUnit.getName()).thenReturn("sample-sibling.jar");
        Mockito.when(siblingUnit.getParent()).thenReturn(rootUnit);
        Mockito.when(siblingUnit.getAttachment(Attachments.MODULE_SPECIFICATION)).thenReturn(siblingSpec);

        AttachmentList<DeploymentUnit> subdeployments = new AttachmentList<>(DeploymentUnit.class);
        subdeployments.add(unit);
        subdeployments.add(siblingUnit);

        Mockito.when(rootUnit.getName()).thenReturn("sample.ear");
        Mockito.when(rootUnit.getParent()).thenReturn(null);
        Mockito.when(rootUnit.getAttachment(Attachments.MODULE_SPECIFICATION)).thenReturn(rootSpec);
        Mockito.when(rootUnit.getAttachment(Attachments.SUB_DEPLOYMENTS)).thenReturn(subdeployments);
    }

    @Test
    public void testCxfModuleDependencyPresent() {
        spec.addUserDependency(new ModuleDependency(null, "org.apache.cxf", false, false, false, false));

        Assert.assertTrue(WSClassVerificationProcessor.hasCxfModuleDependency(unit));
    }

    @Test
    public void testRootExportedCxfModuleDependencyPresent() {
        rootSpec.addUserDependency(new ModuleDependency(null, "org.apache.cxf", false, true, false, false));

        Assert.assertTrue(WSClassVerificationProcessor.hasCxfModuleDependency(unit));
    }

    @Test
    public void testRootNonExportedCxfModuleDependencyPresent() {
        rootSpec.addUserDependency(new ModuleDependency(null, "org.apache.cxf", false, false, false, false));

        Assert.assertFalse(WSClassVerificationProcessor.hasCxfModuleDependency(unit)); // parent dep not exported, should return false
    }

    @Test
    public void testSiblingExportedCxfModuleDependencyPresent() {
        setSubDeploymentsIsolated(false);
        siblingSpec.addUserDependency(new ModuleDependency(null, "org.apache.cxf", false, true, false, false));

        Assert.assertTrue(WSClassVerificationProcessor.hasCxfModuleDependency(unit));
    }

    @Test
    public void testSiblingNonExportedCxfModuleDependencyPresent() {
        setSubDeploymentsIsolated(false);
        siblingSpec.addUserDependency(new ModuleDependency(null, "org.apache.cxf", false, false, false, false));

        Assert.assertFalse(WSClassVerificationProcessor.hasCxfModuleDependency(unit)); // parent dep not exported, should return false
    }

    @Test
    public void testSiblingCxfModuleDependencyPresentIsolatedDeploymentsTrue() {
        setSubDeploymentsIsolated(true);
        siblingSpec.addUserDependency(new ModuleDependency(null, "org.apache.cxf", false, true, false, false));

        Assert.assertFalse(WSClassVerificationProcessor.hasCxfModuleDependency(unit));
    }

    @Test
    public void testCxfModuleDependencyMissing() {
        Assert.assertFalse(WSClassVerificationProcessor.hasCxfModuleDependency(unit));
    }

    private void setSubDeploymentsIsolated(boolean value) {
        rootSpec.setSubDeploymentModulesIsolated(value);
    }
}
