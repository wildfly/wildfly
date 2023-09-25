/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.keycloak.subsystem.adapter.extension;

import static org.keycloak.subsystem.adapter.extension.KeycloakExtension.REALM_DEFINITION;
import static org.keycloak.subsystem.adapter.extension.KeycloakExtension.SECURE_DEPLOYMENT_DEFINITION;
import static org.keycloak.subsystem.adapter.extension.KeycloakExtension.SECURE_SERVER_DEFINITION;

import org.jboss.as.controller.ModelOnlyAddStepHandler;
import org.jboss.as.controller.ModelOnlyResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * Definition of subsystem=keycloak.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2013 Red Hat Inc.
 */
public class KeycloakSubsystemDefinition extends ModelOnlyResourceDefinition {

    protected KeycloakSubsystemDefinition() {
        super(KeycloakExtension.SUBSYSTEM_PATH,
                KeycloakExtension.getResourceDescriptionResolver("subsystem"),
                new ModelOnlyAddStepHandler()
        );
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerSubModel(REALM_DEFINITION);
        resourceRegistration.registerSubModel(SECURE_DEPLOYMENT_DEFINITION);
        resourceRegistration.registerSubModel(SECURE_SERVER_DEFINITION);
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        MigrateOperation.registerOperations(resourceRegistration, KeycloakExtension.getResourceDescriptionResolver());
    }
}
