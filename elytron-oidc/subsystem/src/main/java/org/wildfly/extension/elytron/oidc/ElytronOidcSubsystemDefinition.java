/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.extension.elytron.oidc;

import java.util.Collection;
import java.util.Collections;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * Root subsystem definition for the Elytron OpenID Connect subsystem.
 *
 * <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
class ElytronOidcSubsystemDefinition extends PersistentResourceDefinition {
    static final String CONFIG_CAPABILITY_NAME = "org.wildlfly.elytron.oidc";

    static final RuntimeCapability<Void> CONFIG_CAPABILITY =
            RuntimeCapability.Builder.of(CONFIG_CAPABILITY_NAME)
                    .setServiceType(Void.class)
                    .build();
    // TODO - Identify the required capabilities.

    protected ElytronOidcSubsystemDefinition() {
        super(new SimpleResourceDefinition.Parameters(ElytronOidcExtension.SUBSYSTEM_PATH,
                ElytronOidcExtension.getResourceDescriptionResolver(ElytronOidcExtension.SUBSYSTEM_NAME))
                .setAddHandler(new ElytronOidcSubsystemAdd())
                .setRemoveHandler(new ElytronOidcSubsystemRemove())
                .setCapabilities(CONFIG_CAPABILITY)
        );
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Collections.emptySet();
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
    }
}
