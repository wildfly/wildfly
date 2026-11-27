/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.connector.subsystems.resourceadapters;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.naming.service.NamingService;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ResourceAdaptersSubsystemTestCase extends AbstractSubsystemBaseTest {

    public ResourceAdaptersSubsystemTestCase() {
        // FIXME ResourceAdaptersSubsystemTestCase constructor
        super(ResourceAdaptersExtension.SUBSYSTEM_NAME, new ResourceAdaptersExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("resource-adapters-full.xml");
    }

    @Override
    protected String getSubsystemXsdPath() throws Exception {
        return "schema/wildfly-resource-adapters_7_2.xsd";
    }

    @Override
    protected Properties getResolvedProperties() {
        Properties properties = new Properties();
        properties.put("genericjms.cf.jndi-name", "genericjms");
        properties.put("genericjms.cf.pool-name", "mypool");
        properties.put("genericjms.cf.jndi.contextfactory", "foo");
        properties.put("genericjms.cf.jndi.url", "bar");
        properties.put("genericjms.cf.jndi.lookup", "baz");
        return properties;
    }

    @Test
    public void testFullConfig() throws Exception {
        standardSubsystemTest("resource-adapters-pool.xml", null, true);
    }

    @Test
    public void testFullConfigXa() throws Exception {
        standardSubsystemTest("resource-adapters-xapool.xml", null, true);
    }

    @Test
    public void testExpressionConfig() throws Exception {
        standardSubsystemTest("resource-adapters-pool-expression.xml", "resource-adapters-pool.xml", true);
    }

    @Test
    public void testExpressionConfigXa() throws Exception {
        standardSubsystemTest("resource-adapters-xapool-expression.xml", "resource-adapters-xapool.xml", true);
    }

    @Test
    public void testExpressionConfigElytron() throws Exception {
        standardSubsystemTest("resource-adapters-pool-elytron-enabled.xml", "resource-adapters-pool-elytron-enabled-expression.xml", true);
    }

        @Test
        public void testLegacyRecovery() throws Exception {
            standardSubsystemTest("resource-adapters-legacy-recovery-parsing-7_0.xml", null, false);
        }


    protected AdditionalInitialization createAdditionalInitialization() {
        return AdditionalInitialization.MANAGEMENT
                .withCapabilities(ConnectorServices.TRANSACTION_INTEGRATION_CAPABILITY_NAME,
                        NamingService.CAPABILITY_NAME);
    }

    //TODO: remove this special method as soon as RA will have a unique name in DMR marshalled in xml
    protected KernelServices standardSubsystemTest(final String configId, final String configIdResolvedModel, boolean compareXml) throws Exception {
        final AdditionalInitialization additionalInit = createAdditionalInitialization();


        // Parse the subsystem xml and install into the first controller
        final String subsystemXml = configId == null ? getSubsystemXml() : getSubsystemXml(configId);
        final KernelServices servicesA = super.createKernelServicesBuilder(additionalInit).setSubsystemXml(subsystemXml).build();
        Assert.assertTrue("Subsystem boot failed!", servicesA.isSuccessfulBoot());
        //Get the model and the persisted xml from the first controller
        final ModelNode modelA = servicesA.readWholeModel();
        validateModel(modelA);

        // Test marshaling
        final String marshalled = servicesA.getPersistedSubsystemXml();
        servicesA.shutdown();


        // validate the the normalized xmls
        String normalizedSubsystem = normalizeXML(subsystemXml);

        if (compareXml) {
            compareXml(configId, normalizedSubsystem, normalizeXML(marshalled));
        }

        //Install the persisted xml from the first controller into a second controller
        final KernelServices servicesB = super.createKernelServicesBuilder(additionalInit).setSubsystemXml(marshalled).build();
        final ModelNode modelB = servicesB.readWholeModel();

        //WE CANT DO THAT FOR RA BECAUSE THEIR NAME IS GENERATED ON THE FLY W/ archive/module concatenated to a counter.
        //IT'S AN ISSUE WM_SECURITY_MAPPING_TO BE FIXED IN SUBSYSTEM
        //Make sure the models from the two controllers are identical
        //compare(modelA, modelB);

        // Test the describe operation
        final ModelNode operation = createDescribeOperation();
        final ModelNode result = servicesB.executeOperation(operation);
        Assert.assertTrue("the subsystem describe operation has to generate a list of operations to recreate the subsystem",
                !result.hasDefined(ModelDescriptionConstants.FAILURE_DESCRIPTION));
        final List<ModelNode> operations = result.get(ModelDescriptionConstants.RESULT).asList();
        servicesB.shutdown();

        final KernelServices servicesC = super.createKernelServicesBuilder(additionalInit).setBootOperations(operations).build();
        final ModelNode modelC = servicesC.readWholeModel();

        compare(modelB, modelC);

        assertRemoveSubsystemResources(servicesC, getIgnoredChildResourcesForRemovalTest());

        if (configIdResolvedModel != null) {
            final String subsystemResolvedXml = getSubsystemXml(configIdResolvedModel);
            final KernelServices servicesD = super.createKernelServicesBuilder(additionalInit).setSubsystemXml(subsystemResolvedXml).build();
            Assert.assertTrue("Subsystem w/ reolved xml boot failed!", servicesD.isSuccessfulBoot());
            final ModelNode modelD = servicesD.readWholeModel();
            validateModel(modelD);
            resolveandCompareModel(modelA, modelD);
        }

        return servicesA;
    }


}
