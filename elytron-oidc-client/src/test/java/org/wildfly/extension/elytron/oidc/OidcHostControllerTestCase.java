/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.elytron.oidc;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_REQUIRES_RELOAD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESPONSE_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.wildfly.extension.elytron.oidc.ElytronOidcSubsystemDefinition.ELYTRON_CAPABILITY_NAME;
import static org.wildfly.extension.elytron.oidc.SecureDeploymentDefinition.HTTP_AUTHENTICATION_FACTORY_CAPABILITY;

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.capability.registry.CapabilityScope;
import org.jboss.as.controller.capability.registry.RegistrationPoint;
import org.jboss.as.controller.capability.registry.RuntimeCapabilityRegistration;
import org.jboss.as.controller.capability.registry.RuntimeCapabilityRegistry;
import org.jboss.as.controller.descriptions.NonResolvingResourceDescriptionResolver;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.server.mgmt.domain.ExtensibleHttpManagement;
import org.jboss.as.server.security.VirtualDomainUtil;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for securing the management console of a managed domain with OpenID Connect. In a managed domain the
 * management console is served by the host controller rather than by a server, so the subsystem needs to be usable
 * under {@code /host=<host-name>/subsystem=elytron-oidc-client}.
 *
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
public class OidcHostControllerTestCase extends AbstractSubsystemTest {

    private static final String HOST_NAME = "primary";
    private static final String HTTP_MANAGEMENT_EXTENSIBLE_CAPABILITY = "org.wildfly.management.http.extensible";
    private static final String SECURE_DEPLOYMENT_NAME = "wildfly-management";
    private static final String SECURE_SERVER_NAME = "wildfly-console";

    private static final PathAddress HOST_SUBSYSTEM_ADDRESS = PathAddress.pathAddress(
            PathElement.pathElement(HOST, HOST_NAME), PathElement.pathElement(SUBSYSTEM, ElytronOidcExtension.SUBSYSTEM_NAME));
    /** The domain wide, i.e. profile scoped, registration of the subsystem. */
    private static final PathAddress DOMAIN_SUBSYSTEM_ADDRESS = PathAddress.pathAddress(
            PathElement.pathElement(SUBSYSTEM, ElytronOidcExtension.SUBSYSTEM_NAME));

    public OidcHostControllerTestCase() {
        super(ElytronOidcExtension.SUBSYSTEM_NAME, new ElytronOidcExtension());
    }

    @Test
    public void testSecureDeploymentProvidesHttpAuthenticationFactory() throws Exception {
        KernelServices services = boot(hostControllerBootOperations());

        assertNotNull("No http-authentication-factory was installed for the secure deployment",
                services.getContainer().getService(HTTP_AUTHENTICATION_FACTORY_CAPABILITY.getCapabilityServiceName(SECURE_DEPLOYMENT_NAME)));
    }

    /**
     * Virtual security domains are only ever created for deployments, which a host controller does not have.
     */
    @Test
    public void testVirtualSecurityDomainCreationServiceIsNotInstalled() throws Exception {
        KernelServices services = boot(hostControllerBootOperations());

        assertNull(services.getContainer().getService(VirtualDomainUtil.OIDC_VIRTUAL_SECURITY_DOMAIN_CREATION_SERVICE));
    }

    @Test
    public void testSecureDeploymentConfigurationIsAvailable() throws Exception {
        boot(hostControllerBootOperations());

        String expectedJson = "{\"provider-url\" : \"http://localhost:8080/realms/wildfly-infra\", "
                + "\"client-id\" : \"wildfly-management\", \"bearer-only\" : true, \"ssl-required\" : \"EXTERNAL\", "
                + "\"principal-attribute\" : \"preferred_username\"}";
        assertEquals(expectedJson, OidcConfigService.getInstance().getJSON(SECURE_DEPLOYMENT_NAME));
    }

    @Test
    public void testSecureServerPublishesConsoleConfiguration() throws Exception {
        KernelServices services = boot(hostControllerBootOperations());

        // the console configuration is published by the HTTP management interface of the host controller
        ServiceName serviceName = ServiceName.of(ElytronOidcDescriptionConstants.SECURE_SERVER, SECURE_SERVER_NAME)
                .append("http-management-context");
        assertNotNull("No HTTP management context was installed for the secure server",
                services.getContainer().getService(serviceName));
    }

    /**
     * A host controller running in admin-only mode does not serve the management console, it serves an error page
     * saying so, see {@code ConsoleMode.ADMIN_ONLY}. Publishing the configuration of the console is therefore
     * pointless, which matches the behaviour on a standalone server. The authentication factory on the other hand
     * guards the whole HTTP management interface and not just the console, so it is still needed.
     */
    @Test
    public void testOnlyTheAuthenticationFactoryIsInstalledInAdminOnlyMode() throws Exception {
        KernelServices services = boot(hostControllerBootOperations(), RunningMode.ADMIN_ONLY);

        assertNotNull("The HTTP management interface needs to be able to authenticate in admin-only mode too",
                services.getContainer().getService(HTTP_AUTHENTICATION_FACTORY_CAPABILITY.getCapabilityServiceName(SECURE_DEPLOYMENT_NAME)));

        ServiceName secureServerName = ServiceName.of(ElytronOidcDescriptionConstants.SECURE_SERVER, SECURE_SERVER_NAME)
                .append("http-management-context");
        assertNull("The console configuration should not be published in admin-only mode",
                services.getContainer().getService(secureServerName));
    }

    /**
     * Removing the secure deployment has to deregister the {@code http-authentication-factory} capability again,
     * otherwise the host controller would keep believing the capability is available after the configuration backing
     * it is gone. The installed service itself is only discarded on reload because the HTTP management interface
     * depends on it.
     */
    @Test
    public void testSecureDeploymentCanBeRemoved() throws Exception {
        KernelServices services = boot(hostControllerBootOperations());
        PathAddress address = HOST_SUBSYSTEM_ADDRESS.append(ElytronOidcDescriptionConstants.SECURE_DEPLOYMENT, SECURE_DEPLOYMENT_NAME);

        ModelNode result = services.executeOperation(Util.createRemoveOperation(address));
        assertEquals(result.toString(), "success", result.get(OUTCOME).asString());
        assertTrue("Removing the secure deployment should require a reload",
                result.get(RESPONSE_HEADERS, OPERATION_REQUIRES_RELOAD).asBoolean(false));

        ModelNode readResource = services.executeOperation(Util.createOperation(READ_RESOURCE_OPERATION, address));
        assertEquals(readResource.toString(), "failed", readResource.get(OUTCOME).asString());
    }

    /**
     * A secure deployment that is part of the domain wide configuration applies to servers rather than to the host
     * controller, so it must neither provide the {@code http-authentication-factory} capability used to secure the
     * HTTP management interface nor conflict with a host controller resource of the same name.
     */
    @Test
    public void testDomainSecureDeploymentDoesNotProvideHttpAuthenticationFactory() throws Exception {
        List<ModelNode> bootOperations = hostControllerBootOperations();
        bootOperations.add(Util.createAddOperation(DOMAIN_SUBSYSTEM_ADDRESS));
        bootOperations.add(providerAddOperation(DOMAIN_SUBSYSTEM_ADDRESS));
        bootOperations.add(secureDeploymentAddOperation(DOMAIN_SUBSYSTEM_ADDRESS));

        // the boot would fail if the same capability was registered by both secure deployments
        KernelServices services = boot(bootOperations);

        assertNotNull(services.getContainer().getService(HTTP_AUTHENTICATION_FACTORY_CAPABILITY.getCapabilityServiceName(SECURE_DEPLOYMENT_NAME)));
    }

    private KernelServices boot(List<ModelNode> bootOperations) throws Exception {
        return boot(bootOperations, RunningMode.NORMAL);
    }

    private KernelServices boot(List<ModelNode> bootOperations, RunningMode runningMode) throws Exception {
        KernelServices services = createKernelServicesBuilder(new HostControllerInitializer(runningMode))
                .setBootOperations(bootOperations).build();
        if (! services.isSuccessfulBoot()) {
            Assert.fail(services.getBootError().toString());
        }
        return services;
    }

    private static List<ModelNode> hostControllerBootOperations() {
        List<ModelNode> bootOperations = new ArrayList<>();
        bootOperations.add(Util.createAddOperation(HOST_SUBSYSTEM_ADDRESS));
        bootOperations.add(providerAddOperation(HOST_SUBSYSTEM_ADDRESS));
        bootOperations.add(secureDeploymentAddOperation(HOST_SUBSYSTEM_ADDRESS));

        ModelNode secureServerAdd = Util.createAddOperation(
                HOST_SUBSYSTEM_ADDRESS.append(ElytronOidcDescriptionConstants.SECURE_SERVER, SECURE_SERVER_NAME));
        secureServerAdd.get(ElytronOidcDescriptionConstants.PROVIDER).set("keycloak");
        secureServerAdd.get(ElytronOidcDescriptionConstants.CLIENT_ID).set(SECURE_SERVER_NAME);
        secureServerAdd.get(ElytronOidcDescriptionConstants.PUBLIC_CLIENT).set(true);
        bootOperations.add(secureServerAdd);
        return bootOperations;
    }

    private static ModelNode providerAddOperation(PathAddress subsystemAddress) {
        ModelNode providerAdd = Util.createAddOperation(subsystemAddress.append(ElytronOidcDescriptionConstants.PROVIDER, "keycloak"));
        providerAdd.get(ElytronOidcDescriptionConstants.PROVIDER_URL).set("http://localhost:8080/realms/wildfly-infra");
        return providerAdd;
    }

    private static ModelNode secureDeploymentAddOperation(PathAddress subsystemAddress) {
        ModelNode secureDeploymentAdd = Util.createAddOperation(
                subsystemAddress.append(ElytronOidcDescriptionConstants.SECURE_DEPLOYMENT, SECURE_DEPLOYMENT_NAME));
        secureDeploymentAdd.get(ElytronOidcDescriptionConstants.PROVIDER).set("keycloak");
        secureDeploymentAdd.get(ElytronOidcDescriptionConstants.CLIENT_ID).set(SECURE_DEPLOYMENT_NAME);
        secureDeploymentAdd.get(ElytronOidcDescriptionConstants.PRINCIPAL_ATTRIBUTE).set("preferred_username");
        secureDeploymentAdd.get(ElytronOidcDescriptionConstants.BEARER_ONLY).set(true);
        secureDeploymentAdd.get(ElytronOidcDescriptionConstants.SSL_REQUIRED).set("EXTERNAL");
        return secureDeploymentAdd;
    }

    /**
     * Registers the subsystem under {@code /host=primary} in addition to the domain wide registration created by the
     * test framework, the same way a host controller registers a
     * {@link org.jboss.as.controller.SubsystemRegistration#setHostCapable() host capable} subsystem.
     */
    private static class HostControllerInitializer extends AdditionalInitialization {

        private final RunningMode runningMode;

        HostControllerInitializer(RunningMode runningMode) {
            this.runningMode = runningMode;
        }

        @Override
        protected ProcessType getProcessType() {
            return ProcessType.HOST_CONTROLLER;
        }

        @Override
        protected RunningMode getRunningMode() {
            return runningMode;
        }

        @Override
        protected void initializeExtraSubystemsAndModel(ExtensionRegistry extensionRegistry, Resource rootResource,
                                                        ManagementResourceRegistration rootRegistration, RuntimeCapabilityRegistry capabilityRegistry) {
            super.initializeExtraSubystemsAndModel(extensionRegistry, rootResource, rootRegistration, capabilityRegistry);
            registerCapabilities(capabilityRegistry, ELYTRON_CAPABILITY_NAME);

            // the capabilities the subsystem depends on are provided by the host controller itself, so they also have
            // to be registered in the host scope rather than only in the global scope used by AdditionalInitialization
            registerHostCapability(capabilityRegistry, RuntimeCapability.Builder.of(ELYTRON_CAPABILITY_NAME).build());
            registerHostCapability(capabilityRegistry,
                    RuntimeCapability.Builder.of(HTTP_MANAGEMENT_EXTENSIBLE_CAPABILITY, ExtensibleHttpManagement.class).build());

            ManagementResourceRegistration hostRegistration = rootRegistration.registerSubModel(
                    new SimpleResourceDefinition(new SimpleResourceDefinition.Parameters(PathElement.pathElement(HOST),
                            NonResolvingResourceDescriptionResolver.INSTANCE)));
            hostRegistration.registerSubModel(new ElytronOidcSubsystemDefinition());
            rootResource.registerChild(PathElement.pathElement(HOST, HOST_NAME), Resource.Factory.create());
        }

        private static void registerHostCapability(RuntimeCapabilityRegistry capabilityRegistry, RuntimeCapability<?> capability) {
            CapabilityScope hostScope = CapabilityScope.Factory.create(ProcessType.HOST_CONTROLLER, HOST_SUBSYSTEM_ADDRESS);
            capabilityRegistry.registerCapability(new RuntimeCapabilityRegistration(capability, hostScope,
                    new RegistrationPoint(PathAddress.EMPTY_ADDRESS, null)));
        }
    }
}
