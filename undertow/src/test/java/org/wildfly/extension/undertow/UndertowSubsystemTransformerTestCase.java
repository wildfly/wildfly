/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.ModelFixer;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.as.subsystem.test.LegacyKernelServicesInitializer;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.wildfly.extension.undertow.filters.FilterDefinitions;
import org.wildfly.extension.undertow.handlers.HandlerDefinitions;

/**
 * Validates Undertow subsystem transformations.
 * @author Paul Ferraro
 */
@RunWith(value = Parameterized.class)
public class UndertowSubsystemTransformerTestCase extends AbstractSubsystemTest {
    @Parameters
    public static Collection<Object[]> parameters() {
        return List.<Object[]>of(
                new Object[] { ModelTestControllerVersion.EAP_7_4_0, UndertowSubsystemModel.VERSION_11_0_0 }
        );
    }

    private void addDependencies(LegacyKernelServicesInitializer initializer) throws ClassNotFoundException, IOException {
        initializer
                .addMavenResourceURL(this.controllerVersion.createGAV("wildfly-undertow"))
                .addMavenResourceURL(this.controllerVersion.createGAV("wildfly-web-common"))
                .addMavenResourceURL(this.controllerVersion.createGAV("wildfly-clustering-common"))
                .addMavenResourceURL(this.controllerVersion.createGAV("wildfly-clustering-web-container"))
                .addParentFirstClassPattern("org.jboss.msc.service.ServiceName")
                .addParentFirstClassPattern("org.jboss.as.clustering.controller.CapabilityServiceConfigurator")
        ;
        switch (this.controllerVersion) {
            case EAP_7_4_0:
                initializer.addMavenResourceURL("io.undertow:undertow-core:2.2.5.Final");
                initializer.addMavenResourceURL("io.undertow:undertow-servlet:2.2.5.Final");
                initializer.addMavenResourceURL("org.jboss.spec.javax.security.jacc:jboss-jacc-api_1.5_spec:2.0.0.Final");
                break;
            default: {
                throw new IllegalArgumentException();
            }
        }
    }

    private final ModelTestControllerVersion controllerVersion;
    private final ModelVersion modelVersion;

    public UndertowSubsystemTransformerTestCase(ModelTestControllerVersion controllerVersion, UndertowSubsystemModel subsystemModel) {
        super(UndertowExtension.SUBSYSTEM_NAME, new UndertowExtension());
        this.controllerVersion = controllerVersion;
        this.modelVersion = subsystemModel.getVersion();
    }

    private AdditionalInitialization createAdditionalInitialization() {
        return AdditionalInitialization.withCapabilities(
                RuntimeCapability.resolveCapabilityName(SocketBinding.SERVICE_DESCRIPTOR, "ajp"),
                RuntimeCapability.resolveCapabilityName(SocketBinding.SERVICE_DESCRIPTOR, "http"),
                RuntimeCapability.resolveCapabilityName(SocketBinding.SERVICE_DESCRIPTOR, "https"),
                RuntimeCapability.buildDynamicCapabilityName(Capabilities.REF_SSL_CONTEXT, "ssl")
                );
    }

    private KernelServices build(KernelServicesBuilder builder) throws Exception {
        LegacyKernelServicesInitializer initializer = builder.createLegacyKernelServicesBuilder(this.createAdditionalInitialization(), this.controllerVersion, this.modelVersion)
                .skipReverseControllerCheck()
                .dontPersistXml();
        this.addDependencies(initializer);

        KernelServices services = builder.build();
        verifyBoot(ModelTestControllerVersion.MASTER, services);
        verifyBoot(this.controllerVersion, services.getLegacyServices(this.modelVersion));

        return services;
    }

    private static void verifyBoot(ModelTestControllerVersion version, KernelServices services) {
        try {
            Assert.assertTrue(version.getMavenGavVersion() + " boot failed: " + services.getBootErrorDescription() + (services.hasBootErrorCollectorFailures() ? ": " + services.getBootErrorCollectorFailures().toJSONString(false) : ""), services.isSuccessfulBoot());
        } finally {
            Throwable exception = services.getBootError();
            if (exception != null) {
                exception.printStackTrace(System.err);
            }
        }
    }

    @Test
    public void testTransformations() throws Exception {
        KernelServicesBuilder builder = this.createKernelServicesBuilder(this.createAdditionalInitialization()).setSubsystemXml(this.readResource("undertow-transform.xml"));
        KernelServices services = this.build(builder);

        ModelFixer fixer = model -> {
            model.get(HandlerDefinitions.PATH_ELEMENT.getKeyValuePair()).setEmptyObject();
            model.get(FilterDefinitions.PATH_ELEMENT.getKeyValuePair()).setEmptyObject();
            return model;
        };

        this.checkSubsystemModelTransformation(services, this.modelVersion, fixer, false);
    }

    @Test
    public void testRejections() throws Exception {
        KernelServicesBuilder builder = this.createKernelServicesBuilder(this.createAdditionalInitialization());
        KernelServices services = this.build(builder);

        FailedOperationTransformationConfig config = new FailedOperationTransformationConfig();
        PathAddress subsystemAddress = PathAddress.pathAddress(UndertowRootDefinition.PATH_ELEMENT);
        PathAddress servletContainerAddress = subsystemAddress.append(PathElement.pathElement(ServletContainerDefinition.PATH_ELEMENT.getKey(), "rejected-container"));
        PathAddress affinityCookiePath = subsystemAddress.append(PathElement.pathElement(ServletContainerDefinition.PATH_ELEMENT.getKey(), "affinity-cookie-container")).append(AffinityCookieDefinition.PATH_ELEMENT);

        if (UndertowSubsystemModel.VERSION_13_0_0.requiresTransformation(this.modelVersion)) {
            config.addFailedAttribute(servletContainerAddress, new FailedOperationTransformationConfig.NewAttributesConfig(ServletContainerDefinition.ORPHAN_SESSION_ALLOWED));

            config.addFailedAttribute(affinityCookiePath, FailedOperationTransformationConfig.REJECTED_RESOURCE);
        }

        List<ModelNode> operations = builder.parseXmlResource("undertow-transform-reject.xml");
        ModelTestUtils.checkFailedTransformedBootOperations(services, this.modelVersion, operations, config);
    }
}
