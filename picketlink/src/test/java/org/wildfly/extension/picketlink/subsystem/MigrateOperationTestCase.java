/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.picketlink.subsystem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.wildfly.extension.picketlink.federation.FederationExtension.SUBSYSTEM_NAME;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.registry.RuntimeCapabilityRegistry;
import org.jboss.as.controller.descriptions.common.ControllerResolver;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.extension.ExtensionRegistryType;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.junit.Test;
import org.keycloak.subsystem.adapter.saml.extension.KeycloakSamlExtension;
import org.wildfly.extension.picketlink.federation.FederationExtension;

/**
 * Test case for the picketlink-federation migrate op.
 *
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
public class MigrateOperationTestCase extends AbstractSubsystemTest {

    public static final String KEYCLOAK_SAML_SUBSYSTEM_NAME = "keycloak-saml";

    public MigrateOperationTestCase() {
        super(SUBSYSTEM_NAME, new FederationExtension());
    }

    @Test
    public void testMigrateDefaultPicketLinkFederationConfig() throws Exception {
        String subsystemXml = readResource("federation-subsystem-migration-default-config.xml");
        NewSubsystemAdditionalInitialization additionalInitialization = new NewSubsystemAdditionalInitialization();
        KernelServices services = createKernelServicesBuilder(additionalInitialization).setSubsystemXml(subsystemXml).build();

        ModelNode model = services.readWholeModel();
        assertFalse(additionalInitialization.extensionAdded);
        assertTrue(model.get(SUBSYSTEM, SUBSYSTEM_NAME).isDefined());
        assertFalse(model.get(SUBSYSTEM, KEYCLOAK_SAML_SUBSYSTEM_NAME).isDefined());

        ModelNode migrateOp = new ModelNode();
        migrateOp.get(OP).set("migrate");
        migrateOp.get(OP_ADDR).add(SUBSYSTEM, SUBSYSTEM_NAME);

        ModelNode response = services.executeOperation(migrateOp);

        checkOutcome(response);

        ModelNode warnings = response.get(RESULT, "migration-warnings");
        assertEquals(warnings.toString(), 0, warnings.asList().size());

        model = services.readWholeModel();

        assertTrue(additionalInitialization.extensionAdded);
        assertFalse(model.get(SUBSYSTEM, SUBSYSTEM_NAME).isDefined());
        assertTrue(model.get(SUBSYSTEM, KEYCLOAK_SAML_SUBSYSTEM_NAME).isDefined());

        ModelNode newSubsystem = model.get(SUBSYSTEM, KEYCLOAK_SAML_SUBSYSTEM_NAME);
        ModelNode secureDeployment = newSubsystem.get("secure-deployment");
        assertFalse(secureDeployment.isDefined());
    }

    @Test
    public void testMigrateNonEmptyPicketLinkFederationConfig() throws Exception {
        String subsystemXml = readResource("federation-subsystem-migration-non-empty-config.xml");
        NewSubsystemAdditionalInitialization additionalInitialization = new NewSubsystemAdditionalInitialization();
        KernelServices services = createKernelServicesBuilder(additionalInitialization).setSubsystemXml(subsystemXml).build();

        ModelNode model = services.readWholeModel();
        assertFalse(additionalInitialization.extensionAdded);
        assertTrue(model.get(SUBSYSTEM, SUBSYSTEM_NAME).isDefined());
        assertFalse(model.get(SUBSYSTEM, KEYCLOAK_SAML_SUBSYSTEM_NAME).isDefined());

        ModelNode migrateOp = new ModelNode();
        migrateOp.get(OP).set("migrate");
        migrateOp.get(OP_ADDR).add(SUBSYSTEM, SUBSYSTEM_NAME);

        ModelNode response = services.executeOperation(migrateOp);

        ModelTestUtils.checkFailed(response);

        model = services.readWholeModel();
        assertTrue(additionalInitialization.extensionAdded);

        // op failed so legacy subsystem config still present
        assertTrue(model.get(SUBSYSTEM, SUBSYSTEM_NAME).isDefined());
        assertFalse(model.get(SUBSYSTEM, KEYCLOAK_SAML_SUBSYSTEM_NAME).isDefined());
    }

    private static class NewSubsystemAdditionalInitialization extends AdditionalInitialization {
        KeycloakSamlExtension newSubsystem = new KeycloakSamlExtension();
        boolean extensionAdded = false;

        @Override
        protected void initializeExtraSubystemsAndModel(ExtensionRegistry extensionRegistry, Resource rootResource, ManagementResourceRegistration rootRegistration, RuntimeCapabilityRegistry capabilityRegistry) {
            rootRegistration.registerSubModel(new SimpleResourceDefinition(PathElement.pathElement(EXTENSION),
                    ControllerResolver.getResolver(EXTENSION), new OperationStepHandler() {
                @Override
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    extensionAdded = true;
                    newSubsystem.initialize(extensionRegistry.getExtensionContext("org.keycloak.keycloak-saml-adapter-subsystem",
                            rootRegistration, ExtensionRegistryType.SERVER));
                }
            }, null));
        }

        @Override
        protected ProcessType getProcessType() {
            return ProcessType.HOST_CONTROLLER;
        }

        @Override
        protected RunningMode getRunningMode() {
            return RunningMode.ADMIN_ONLY;
        }
    }
}
