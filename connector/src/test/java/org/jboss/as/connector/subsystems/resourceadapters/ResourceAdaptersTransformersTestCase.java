/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.subsystems.resourceadapters;

import org.jboss.as.connector.logging.ConnectorLogger;
import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.model.test.SingleClassFilter;
import org.jboss.as.naming.service.NamingService;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.as.subsystem.test.LegacyKernelServicesInitializer;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RESOURCEADAPTER_NAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.WM_SECURITY;
import static org.jboss.as.connector.subsystems.resourceadapters.ResourceAdaptersExtension.SUBSYSTEM_PATH;
import static org.jboss.as.connector.subsystems.resourceadapters.ResourceAdaptersExtension.VERSION_6_0_0;
import static org.jboss.as.connector.subsystems.resourceadapters.ResourceAdaptersExtension.VERSION_6_1_0;

/**
 * Class for testing the {@link ResourceAdaptersTransformers}.
 *
 * @author <a href="pberan@redhat.com">Petr Beran</a>
 */
public class ResourceAdaptersTransformersTestCase extends AbstractSubsystemBaseTest {

    public ResourceAdaptersTransformersTestCase() {
        super(ResourceAdaptersExtension.SUBSYSTEM_NAME, new ResourceAdaptersExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("resource-adapters-transform_7_1.xml");
    }

    @Override
    protected String getSubsystemXsdPath() {
        return "schema/wildfly-resource-adapters_7_1.xsd";
    }

    /**
     * Simple switch for returning the proper resource adapters subsystem version based on the EAP version.
     *
     * @param controllerVersion the EAP version based on which the subsystem version is decided.
     * @return resource adapters subsystem version that specified EAP uses.
     */
    private static ModelVersion getResourceAdapterModel(ModelTestControllerVersion controllerVersion) {
        switch (controllerVersion) {
            case EAP_7_4_0:
                return VERSION_6_0_0; // EAP 7.4.0. uses the 6.0 version of resource adapters subsystem
            default:
                throw new IllegalArgumentException("Unsupported EAP version");
        }
    }

    /**
     * Test method for 7.0.0 to 6.0.0 resource adapters subsystem transformers for valid and rejecting configuration.
     *
     * @throws Exception if an error occurs during the kernel initialization or the subsystem transformation validation.
     */
    @Test
    public void test740transformers() throws Exception {
        testTransformer(ModelTestControllerVersion.EAP_7_4_0);
        testTransformerReject(ModelTestControllerVersion.EAP_7_4_0);
    }

    /**
     * Tests valid transformation of resource adapters subsystem from 7.0.0 to 6.0.0
     *
     * @param controllerVersion version of the EAP running on the older resource adapters subsystem.
     * @throws Exception if an error occurs during the kernel initialization or the subsystem transformation validation.
     */
    private void testTransformer(final ModelTestControllerVersion controllerVersion) throws Exception {
        String subsystemXml = "resource-adapters-transform_7_1.xml";
        ModelVersion modelVersion = getResourceAdapterModel(controllerVersion);
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization()).setSubsystemXmlResource(subsystemXml);
        KernelServices mainServices = initialKernelServices(builder, controllerVersion);

        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        Assert.assertNotNull(legacyServices);
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        checkSubsystemModelTransformation(mainServices, modelVersion);
    }

    /**
     * Tests valid transformation of resource adapters subsystem from 7.0.0 to 6.0.0
     *
     * @param controllerVersion version of the EAP running on the older resource adapters subsystem.
     * @throws Exception if an error occurs during the kernel initialization or the subsystem transformation validation.
     */
    private void testTransformerReject(final ModelTestControllerVersion controllerVersion) throws Exception {
        // subsystemXml contains expression not understand by 6.0.0 version of the subsystem
        String subsystemXml = "resource-adapters-transform-reject_7_0.xml";
        ModelVersion modelVersion = getResourceAdapterModel(controllerVersion);

        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization());
        KernelServices mainServices = initialKernelServices(builder, controllerVersion);

        // the legacyServices are not able to boot successfully due to the rejected attribute in subsystemXml
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        Assert.assertNotNull(legacyServices);

        // Setting the subsystem based on the subsystemXml configuration
        List<ModelNode> subsystem = builder.parseXmlResource(subsystemXml);
        PathAddress subsystemAddress = PathAddress.pathAddress(SUBSYSTEM_PATH);
        FailedOperationTransformationConfig config = new FailedOperationTransformationConfig();

        // In case of resource adapters subsystem 6.0.0 or 6.1.0 the configuration should be rejected because
        // the WM_SECURITY contains unsupported expression.
        if (modelVersion.equals(VERSION_6_0_0) || modelVersion.equals(VERSION_6_1_0)) {
            config.addFailedAttribute(subsystemAddress.append(RESOURCEADAPTER_NAME),
                    new FailedOperationTransformationConfig.NewAttributesConfig(WM_SECURITY));
        }
        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, modelVersion, subsystem, config);
    }

    /**
     * Builds the service container containing necessary capabilities and dependencies needed to run the tests.
     *
     * @param builder builder object for initializing the kernel services.
     * @param controllerVersion version of EAP containing the old version of the subsystem.
     * @return built kernel services based on the EAP version supplied.
     * @throws Exception if it's not possible to add specified Maven dependencies.
     */
    private KernelServices initialKernelServices(KernelServicesBuilder builder, ModelTestControllerVersion controllerVersion) throws Exception {
        String mavenGroupId = controllerVersion.getMavenGroupId();
        ModelVersion modelVersion = getResourceAdapterModel(controllerVersion);

        LegacyKernelServicesInitializer initializer = builder.createLegacyKernelServicesBuilder(createAdditionalInitialization(), controllerVersion, modelVersion);

        initializer.addMavenResourceURL(controllerVersion.createGAV("wildfly-connector")) // Adds the resource adapter subsystem package
                .addMavenResourceURL("org.jboss.spec.javax.resource:jboss-connector-api_1.7_spec:2.0.0.Final-redhat-00001") // The 6.0.0 subsystem on EAP 7.4.0 uses the javax prefix
                .setExtensionClassName("org.jboss.as.connector.subsystems.resourceadapters.ResourceAdaptersExtension") // Adds the transformer registration
                .skipReverseControllerCheck()
                .dontPersistXml()
                .excludeFromParent(SingleClassFilter.createFilter(ConnectorLogger.class));

        KernelServices mainServices = builder.build();
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        return mainServices;
    }

    /**
     * Provides additional capabilities for the model controller.
     *
     * @return additional capabilities for the model controller.
     */
    protected AdditionalInitialization createAdditionalInitialization() {
        // Adds additional capabilities, both TRANSACTION_INTEGRATION_CAPABILITY_NAME and CAPABILITY_NAME
        // are needed for the testTransformerReject method.
        return AdditionalInitialization.withCapabilities
                (ConnectorServices.TRANSACTION_INTEGRATION_CAPABILITY_NAME,
                NamingService.CAPABILITY_NAME);
    }

}
