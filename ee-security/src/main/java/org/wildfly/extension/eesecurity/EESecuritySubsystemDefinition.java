/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.eesecurity;

import java.util.Collection;
import java.util.Collections;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.RuntimePackageDependency;

/**
 * @author Stuart Douglas
 */
public class EESecuritySubsystemDefinition extends PersistentResourceDefinition {

    static final String EE_SECURITY_CAPABILITY_NAME = "org.wildfly.ee.security";
    static final String WELD_CAPABILITY_NAME = "org.wildfly.weld";
    static final String ELYTRON_JAKARTA_SECURITY = "org.wildfly.security.jakarta.security";

    static final RuntimeCapability<Void> EE_SECURITY_CAPABILITY =
            RuntimeCapability.Builder.of(EE_SECURITY_CAPABILITY_NAME)
                    .setServiceType(Void.class)
                    .addRequirements(WELD_CAPABILITY_NAME)
                    .build();

    EESecuritySubsystemDefinition() {
        super(new SimpleResourceDefinition.Parameters(EESecurityExtension.SUBSYSTEM_PATH, EESecurityExtension.SUBSYSTEM_RESOLVER)
                .setAddHandler(EESecuritySubsystemAdd.INSTANCE)
                .addCapabilities(EE_SECURITY_CAPABILITY)
                .setRemoveHandler(ReloadRequiredRemoveStepHandler.INSTANCE)
                .setAdditionalPackages(RuntimePackageDependency.required(ELYTRON_JAKARTA_SECURITY))
        );
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Collections.emptyList();
    }
}
