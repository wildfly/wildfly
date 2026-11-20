/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.elytron.oidc;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.junit.Assert.assertNull;
import static org.wildfly.extension.elytron.oidc.ElytronOidcSubsystemDefinition.ELYTRON_CAPABILITY_NAME;

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.capability.registry.RuntimeCapabilityRegistry;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests that a {@code secure-server} resource does not install the HTTP management context service on a server that
 * is part of a managed domain. Such a server has no HTTP management interface, so the
 * {@code org.wildfly.management.http.extensible} capability that service depends on is never present there.
 */
public class OidcDomainServerTestCase extends AbstractSubsystemTest {

    private static final String SECURE_SERVER_NAME = "wildfly-console";

    private static final PathAddress SUBSYSTEM_ADDRESS = PathAddress.pathAddress(
            PathElement.pathElement(SUBSYSTEM, ElytronOidcExtension.SUBSYSTEM_NAME));

    public OidcDomainServerTestCase() {
        super(ElytronOidcExtension.SUBSYSTEM_NAME, new ElytronOidcExtension());
    }

    @Test
    public void testSecureServerDoesNotInstallHttpManagementContext() throws Exception {
        KernelServices services = boot();

        ServiceName serviceName = ServiceName.of(ElytronOidcDescriptionConstants.SECURE_SERVER, SECURE_SERVER_NAME)
                .append("http-management-context");
        assertNull("A secure server must not install the HTTP management context service on a server in a managed domain",
                services.getContainer().getService(serviceName));
    }

    private KernelServices boot() throws Exception {
        List<ModelNode> bootOperations = new ArrayList<>();
        bootOperations.add(Util.createAddOperation(SUBSYSTEM_ADDRESS));

        ModelNode providerAdd = Util.createAddOperation(
                SUBSYSTEM_ADDRESS.append(ElytronOidcDescriptionConstants.PROVIDER, "keycloak"));
        providerAdd.get(ElytronOidcDescriptionConstants.PROVIDER_URL).set("http://localhost:8080/realms/wildfly-infra");
        bootOperations.add(providerAdd);

        ModelNode secureServerAdd = Util.createAddOperation(
                SUBSYSTEM_ADDRESS.append(ElytronOidcDescriptionConstants.SECURE_SERVER, SECURE_SERVER_NAME));
        secureServerAdd.get(ElytronOidcDescriptionConstants.PROVIDER).set("keycloak");
        secureServerAdd.get(ElytronOidcDescriptionConstants.CLIENT_ID).set(SECURE_SERVER_NAME);
        secureServerAdd.get(ElytronOidcDescriptionConstants.PUBLIC_CLIENT).set(true);
        bootOperations.add(secureServerAdd);

        KernelServices services = createKernelServicesBuilder(new DomainServerInitializer())
                .setBootOperations(bootOperations).build();
        if (! services.isSuccessfulBoot()) {
            Assert.fail(services.getBootError().toString());
        }
        return services;
    }

    private static class DomainServerInitializer extends AdditionalInitialization {

        @Override
        protected ProcessType getProcessType() {
            return ProcessType.DOMAIN_SERVER;
        }

        @Override
        protected RunningMode getRunningMode() {
            return RunningMode.NORMAL;
        }

        @Override
        protected void initializeExtraSubystemsAndModel(ExtensionRegistry extensionRegistry, Resource rootResource,
                                                        ManagementResourceRegistration rootRegistration, RuntimeCapabilityRegistry capabilityRegistry) {
            super.initializeExtraSubystemsAndModel(extensionRegistry, rootResource, rootRegistration, capabilityRegistry);
            registerCapabilities(capabilityRegistry, ELYTRON_CAPABILITY_NAME);
        }
    }
}
