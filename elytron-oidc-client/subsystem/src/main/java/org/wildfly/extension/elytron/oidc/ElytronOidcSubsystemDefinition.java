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
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * Root subsystem definition for the Elytron OpenID Connect subsystem.
 *
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
class ElytronOidcSubsystemDefinition extends PersistentResourceDefinition {
    static final String CONFIG_CAPABILITY_NAME = "org.wildlfly.elytron.oidc";
    static final String ELYTRON_CAPABILITY_NAME = "org.wildfly.security.elytron";

    static final RuntimeCapability<Void> CONFIG_CAPABILITY =
            RuntimeCapability.Builder.of(CONFIG_CAPABILITY_NAME)
                    .setServiceType(Void.class)
                    .addRequirements(ELYTRON_CAPABILITY_NAME)
                    .build();

    protected ElytronOidcSubsystemDefinition() {
        super(new SimpleResourceDefinition.Parameters(ElytronOidcExtension.SUBSYSTEM_PATH,
                ElytronOidcExtension.getResourceDescriptionResolver())
                .setAddHandler(new ElytronOidcSubsystemAdd())
                .setRemoveHandler(ReloadRequiredRemoveStepHandler.INSTANCE)
                .setCapabilities(CONFIG_CAPABILITY)
        );
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Collections.emptySet();
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerSubModel(new RealmDefinition());
        resourceRegistration.registerSubModel(new ProviderDefinition());
        resourceRegistration.registerSubModel(new SecureDeploymentDefinition());
    }
}
