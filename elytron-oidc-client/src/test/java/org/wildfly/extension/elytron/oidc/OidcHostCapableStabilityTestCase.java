/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.elytron.oidc;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.function.Supplier;

import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.descriptions.NonResolvingResourceDescriptionResolver;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.extension.ExtensionRegistryType;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.controller.persistence.SubsystemXmlWriterRegistry;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.version.Stability;
import org.jboss.staxmapper.XMLElementWriter;
import org.junit.Test;

/**
 * Securing the management console of a managed domain requires the subsystem to be registrable under
 * {@code /host=<host-name>/subsystem=elytron-oidc-client}, which only happens when the subsystem declares itself
 * {@link org.jboss.as.controller.SubsystemRegistration#setHostCapable() host capable}. That is a
 * {@link ElytronOidcExtension#HOST_CONTROLLER_STABILITY preview} stability feature, so below that level the
 * registration has to be rejected, while the domain wide, i.e. profile scoped, registration a host controller also
 * maintains must keep working at every stability level.
 */
public class OidcHostCapableStabilityTestCase {

    private static final String EXTENSION_MODULE_NAME = "org.wildfly.extension.elytron-oidc-client";

    private static final PathAddress SUBSYSTEM_ADDRESS = PathAddress.pathAddress(
            PathElement.pathElement(SUBSYSTEM, ElytronOidcExtension.SUBSYSTEM_NAME));

    @Test
    public void testHostRegistrationIsRejectedAtDefaultStability() {
        ManagementResourceRegistration rootRegistration = rootRegistration(Stability.DEFAULT);
        try {
            initialize(Stability.DEFAULT, rootRegistration, ExtensionRegistryType.HOST);
            fail("The subsystem must not be registrable in the host model below the "
                    + ElytronOidcExtension.HOST_CONTROLLER_STABILITY + " stability level");
        } catch (IllegalStateException expected) {
            // thrown as ControllerLogger.nonHostCapableSubsystemInHostModel because setHostCapable() was not called
            assertTrue("Unexpected failure: " + expected.getMessage(),
                    expected.getMessage().contains(EXTENSION_MODULE_NAME));
        }
        assertNull("The subsystem should not have been registered in the host model",
                rootRegistration.getSubModel(SUBSYSTEM_ADDRESS));
    }

    @Test
    public void testHostRegistrationIsAllowedAtPreviewStability() {
        ManagementResourceRegistration rootRegistration = rootRegistration(Stability.PREVIEW);

        initialize(Stability.PREVIEW, rootRegistration, ExtensionRegistryType.HOST);

        assertNotNull("The subsystem should be registrable in the host model at the "
                        + ElytronOidcExtension.HOST_CONTROLLER_STABILITY + " stability level",
                rootRegistration.getSubModel(SUBSYSTEM_ADDRESS));
    }

    /**
     * A host controller also maintains the domain wide configuration it applies to the servers it manages. That part
     * of its management tree does not depend on the host capability, so it stays available at default stability.
     */
    @Test
    public void testDomainRegistrationIsAllowedAtDefaultStability() {
        ManagementResourceRegistration rootRegistration = rootRegistration(Stability.DEFAULT);

        initialize(Stability.DEFAULT, rootRegistration, ExtensionRegistryType.MASTER);

        assertNotNull("The domain wide registration of the subsystem must not depend on the host capability",
                rootRegistration.getSubModel(SUBSYSTEM_ADDRESS));
    }

    private static void initialize(Stability stability, ManagementResourceRegistration rootRegistration,
                                   ExtensionRegistryType extensionRegistryType) {
        ExtensionRegistry extensionRegistry = ExtensionRegistry.builder(ProcessType.HOST_CONTROLLER)
                .withRunningMode(RunningMode.NORMAL)
                .withStability(stability)
                .build();
        extensionRegistry.setWriterRegistry(new NoOpWriterRegistry());
        ExtensionContext context = extensionRegistry.getExtensionContext(EXTENSION_MODULE_NAME, rootRegistration,
                extensionRegistryType);
        new ElytronOidcExtension().initialize(context);
    }

    private static ManagementResourceRegistration rootRegistration(Stability stability) {
        return ManagementResourceRegistration.Factory.forProcessType(ProcessType.HOST_CONTROLLER, stability)
                .createRegistration(ResourceDefinition.builder(ResourceRegistration.root(),
                        NonResolvingResourceDescriptionResolver.INSTANCE).build());
    }

    private static class NoOpWriterRegistry implements SubsystemXmlWriterRegistry {

        @Override
        public void registerSubsystemWriter(String name, Supplier<XMLElementWriter<SubsystemMarshallingContext>> writer) {
        }

        @Override
        public void unregisterSubsystemWriter(String name) {
        }
    }
}
