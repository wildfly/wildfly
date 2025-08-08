/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.security;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.capability.registry.RuntimeCapabilityRegistry;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.AdditionalInitialization.ManagementAdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author Tomaz Cerar (c) 2017 Red Hat Inc.
 */
public class SecurityTransformersTestCase extends AbstractSubsystemBaseTest {

    public SecurityTransformersTestCase() {
        super(SecurityExtension.SUBSYSTEM_NAME, new SecurityExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("securitysubsystemv20.xml");
    }

    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        String[] capabilities = new String[] {"org.wildfly.clustering.infinispan.cache-container.security",
                "org.wildfly.clustering.infinispan.default-cache-configuration.security"};
        return new ManagementAdditionalInitialization() {

            @Override
            protected ProcessType getProcessType() {
                return ProcessType.HOST_CONTROLLER;
            }

            @Override
            protected void initializeExtraSubystemsAndModel(ExtensionRegistry extensionRegistry, Resource rootResource, ManagementResourceRegistration rootRegistration, RuntimeCapabilityRegistry capabilityRegistry) {
                super.initializeExtraSubystemsAndModel(extensionRegistry, rootResource, rootRegistration, capabilityRegistry);
                registerCapabilities(capabilityRegistry, capabilities);
            }

        };
    }

    @Ignore("Figure out the set of deps needed by the legacy subsystem and add them")
    @Test
    public void testTransformersEAP74() throws Exception {
        testTransformers(ModelTestControllerVersion.EAP_7_4_0);
    }

    private void testTransformers(ModelTestControllerVersion controllerVersion) throws Exception {
        ModelVersion version = ModelVersion.create(2, 0, 0);
        String wildflySecurityGAV = controllerVersion.createGAV("wildfly-security");

        testTransformers(controllerVersion, version, wildflySecurityGAV);
        testReject(controllerVersion, version, wildflySecurityGAV);
    }

    private void testReject(ModelTestControllerVersion controllerVersion, ModelVersion targetVersion, String mavenGAV) throws Exception {
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization());
        builder.createLegacyKernelServicesBuilder(null, controllerVersion, targetVersion)
                .configureReverseControllerCheck(createAdditionalInitialization(), null)
                //.skipReverseControllerCheck()
                .addMavenResourceURL(mavenGAV)
                .dontPersistXml();

        KernelServices mainServices = builder.build();
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        KernelServices legacyServices = mainServices.getLegacyServices(targetVersion);
        Assert.assertTrue(legacyServices.isSuccessfulBoot());
        Assert.assertNotNull(legacyServices);

        // any elytron-related resources in the model should get rejected as those are not supported in model version 1.3.0.
        PathAddress subsystemAddress = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, getMainSubsystemName()));
        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, targetVersion,
                builder.parseXmlResource("security-transformers-reject_2.0.xml"),
                new FailedOperationTransformationConfig()
                        .addFailedAttribute(PathAddress.pathAddress(subsystemAddress, PathElement.pathElement(Constants.ELYTRON_REALM)),
                                FailedOperationTransformationConfig.REJECTED_RESOURCE)
                        .addFailedAttribute(PathAddress.pathAddress(subsystemAddress, PathElement.pathElement(Constants.ELYTRON_KEY_STORE)),
                                FailedOperationTransformationConfig.REJECTED_RESOURCE)
                        .addFailedAttribute(PathAddress.pathAddress(subsystemAddress, PathElement.pathElement(Constants.ELYTRON_TRUST_STORE)),
                                FailedOperationTransformationConfig.REJECTED_RESOURCE)
                        .addFailedAttribute(PathAddress.pathAddress(subsystemAddress, PathElement.pathElement(Constants.ELYTRON_KEY_MANAGER)),
                                FailedOperationTransformationConfig.REJECTED_RESOURCE)
                        .addFailedAttribute(PathAddress.pathAddress(subsystemAddress, PathElement.pathElement(Constants.ELYTRON_TRUST_MANAGER)),
                                FailedOperationTransformationConfig.REJECTED_RESOURCE)
                        .addFailedAttribute(
                                PathAddress.pathAddress(subsystemAddress,
                                        PathElement.pathElement(Constants.SECURITY_DOMAIN, "domain-with-custom-audit-provider"),
                                        SecurityExtension.PATH_AUDIT_CLASSIC,
                                        PathElement.pathElement(Constants.PROVIDER_MODULE,
                                                "org.myorg.security.MyCustomLogAuditProvider")),
                                new FailedOperationTransformationConfig.NewAttributesConfig(Constants.MODULE))
                        .addFailedAttribute(PathAddress.pathAddress(subsystemAddress),
                                new FailedOperationTransformationConfig.NewAttributesConfig(Constants.INITIALIZE_JACC)));
        legacyServices.shutdown();
        mainServices.shutdown();
    }


    private void testTransformers(ModelTestControllerVersion controllerVersion, ModelVersion targetVersion, String mavenGAV) throws Exception {
        //Boot up empty controllers with the resources needed for the ops coming from the xml to work
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization())
                .setSubsystemXmlResource("security-transformers_2.0.xml");
        builder.createLegacyKernelServicesBuilder(null, controllerVersion, targetVersion)
                .addMavenResourceURL(mavenGAV)
                .configureReverseControllerCheck(createAdditionalInitialization(), null)
                .dontPersistXml();

        KernelServices mainServices = builder.build();
        assertTrue(mainServices.isSuccessfulBoot());
        assertTrue(mainServices.getLegacyServices(targetVersion).isSuccessfulBoot());

        checkSubsystemModelTransformation(mainServices, targetVersion, null);
        mainServices.shutdown();
    }


    @Override
    public void testSchema() throws Exception {
    }


}
