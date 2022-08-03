/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
