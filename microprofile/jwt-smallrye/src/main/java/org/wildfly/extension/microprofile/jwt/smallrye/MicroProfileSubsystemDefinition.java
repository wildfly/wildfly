/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
import org.jboss.as.controller.registry.RuntimePackageDependency;


/**
 * Root subsystem definition for the MicroProfile JWT subsystem using SmallRye JWT.
 *
 * <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
class MicroProfileSubsystemDefinition extends PersistentResourceDefinition {

    static final String EE_SECURITY_IMPL = "org.wildfly.security.jakarta.security";

    static final RuntimeCapability<Void> CONFIG_CAPABILITY =
            RuntimeCapability.Builder.of(JWT_CAPABILITY_NAME)
                    .setServiceType(Void.class)
                    .addRequirements(CONFIG_CAPABILITY_NAME, EE_SECURITY_CAPABILITY_NAME,
                            ELYTRON_CAPABILITY_NAME, WELD_CAPABILITY_NAME)
                    .build();

    protected MicroProfileSubsystemDefinition() {
        super(new SimpleResourceDefinition.Parameters(MicroProfileJWTExtension.SUBSYSTEM_PATH, MicroProfileJWTExtension.SUBSYSTEM_RESOLVER)
                .setAddHandler(new MicroProfileJWTSubsystemAdd())
                .setRemoveHandler(ReloadRequiredRemoveStepHandler.INSTANCE)
                .setCapabilities(CONFIG_CAPABILITY)
                .setAdditionalPackages(RuntimePackageDependency.required(EE_SECURITY_IMPL))
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
