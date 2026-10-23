/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.elytron.oidc;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.junit.Assert.assertNull;
import static org.wildfly.extension.elytron.oidc.ElytronOidcSubsystemDefinition.ELYTRON_CAPABILITY_NAME;
import static org.wildfly.extension.elytron.oidc.SecureDeploymentDefinition.HTTP_AUTHENTICATION_FACTORY_CAPABILITY;

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
 * Tests for the subsystem configuration of a server that is part of a managed domain. Such a server does not host the
 * management console, so the resources that are only meaningful for the HTTP management interface must not install
 * any services there.
 *
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
public class OidcDomainServerTestCase extends AbstractSubsystemTest {

    private static final String SECURE_DEPLOYMENT_NAME = "wildfly-management";
    private static final String SECURE_SERVER_NAME = "wildfly-console";

    private static final PathAddress SUBSYSTEM_ADDRESS = PathAddress.pathAddress(
            PathElement.pathElement(SUBSYSTEM, ElytronOidcExtension.SUBSYSTEM_NAME));

    public OidcDomainServerTestCase() {
        super(ElytronOidcExtension.SUBSYSTEM_NAME, new ElytronOidcExtension());
    }

    /**
     * A secure server in a profile used to leave the server with a service depending on the HTTP management interface
     * capability, which a server in a managed domain does not have.
     */
    @Test
    public void testSecureServerIsIgnored() throws Exception {
        KernelServices services = boot();

        ServiceName serviceName = ServiceName.of(ElytronOidcDescriptionConstants.SECURE_SERVER, SECURE_SERVER_NAME)
                .append("http-management-context");
        assertNull("The secure server should be ignored on a server in a managed domain",
                services.getContainer().getService(serviceName));
    }

    @Test
    public void testSecureDeploymentDoesNotProvideHttpAuthenticationFactory() throws Exception {
        KernelServices services = boot();

        assertNull("Only a host controller provides an http-authentication-factory for the management interface",
                services.getContainer().getService(HTTP_AUTHENTICATION_FACTORY_CAPABILITY.getCapabilityServiceName(SECURE_DEPLOYMENT_NAME)));
    }

    private KernelServices boot() throws Exception {
        List<ModelNode> bootOperations = new ArrayList<>();
        bootOperations.add(Util.createAddOperation(SUBSYSTEM_ADDRESS));

        ModelNode providerAdd = Util.createAddOperation(SUBSYSTEM_ADDRESS.append(ElytronOidcDescriptionConstants.PROVIDER, "keycloak"));
        providerAdd.get(ElytronOidcDescriptionConstants.PROVIDER_URL).set("http://localhost:8080/realms/wildfly-infra");
        bootOperations.add(providerAdd);

        ModelNode secureDeploymentAdd = Util.createAddOperation(
                SUBSYSTEM_ADDRESS.append(ElytronOidcDescriptionConstants.SECURE_DEPLOYMENT, SECURE_DEPLOYMENT_NAME));
        secureDeploymentAdd.get(ElytronOidcDescriptionConstants.PROVIDER).set("keycloak");
        secureDeploymentAdd.get(ElytronOidcDescriptionConstants.CLIENT_ID).set(SECURE_DEPLOYMENT_NAME);
        secureDeploymentAdd.get(ElytronOidcDescriptionConstants.BEARER_ONLY).set(true);
        bootOperations.add(secureDeploymentAdd);

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
