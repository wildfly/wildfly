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
