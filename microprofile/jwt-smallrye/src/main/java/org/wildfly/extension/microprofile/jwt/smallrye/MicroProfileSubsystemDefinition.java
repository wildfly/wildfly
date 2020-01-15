/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019 Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.microprofile.jwt.smallrye;

import static org.wildfly.extension.microprofile.jwt.smallrye.Capabilities.CONFIG_CAPABILITY_NAME;
import static org.wildfly.extension.microprofile.jwt.smallrye.Capabilities.EE_SECURITY_CAPABILITY_NAME;
import static org.wildfly.extension.microprofile.jwt.smallrye.Capabilities.ELYTRON_CAPABILITY_NAME;
import static org.wildfly.extension.microprofile.jwt.smallrye.Capabilities.JWT_CAPABILITY_NAME;
import static org.wildfly.extension.microprofile.jwt.smallrye.Capabilities.WELD_CAPABILITY_NAME;

import java.util.Collection;
import java.util.Collections;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.ManagementResourceRegistration;


/**
 * Root subsystem definition for the MicroProfile JWT subsystem using SmallRye JWT.
 *
 * <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
class MicroProfileSubsystemDefinition extends PersistentResourceDefinition {

    static final RuntimeCapability<Void> CONFIG_CAPABILITY =
            RuntimeCapability.Builder.of(JWT_CAPABILITY_NAME)
                    .setServiceType(Void.class)
                    .addRequirements(CONFIG_CAPABILITY_NAME, EE_SECURITY_CAPABILITY_NAME,
                            ELYTRON_CAPABILITY_NAME, WELD_CAPABILITY_NAME)
                    .build();

    protected MicroProfileSubsystemDefinition() {
        super(new SimpleResourceDefinition.Parameters(MicroProfileJWTExtension.SUBSYSTEM_PATH, MicroProfileJWTExtension.getResourceDescriptionResolver(MicroProfileJWTExtension.SUBSYSTEM_NAME))
                .setAddHandler(new MicroProfileJWTSubsystemAdd())
                .setRemoveHandler(ReloadRequiredRemoveStepHandler.INSTANCE)
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
