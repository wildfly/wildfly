/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.vertx;

import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ParentResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.SubsystemResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.SubsystemResourceDefinitionRegistrar;

import static org.wildfly.extension.vertx.VertxSubsystemExtension.SUBSYSTEM_NAME;

/**
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
public class VertxSubsystemRegistrar implements SubsystemResourceDefinitionRegistrar {
    static final ParentResourceDescriptionResolver RESOLVER = new SubsystemResourceDescriptionResolver(SUBSYSTEM_NAME,
            VertxSubsystemRegistrar.class);

    @Override
    public ManagementResourceRegistration register(SubsystemRegistration parent,
                                                   ManagementResourceRegistrationContext managementResourceRegistrationContext) {
        return parent.registerSubsystemModel(VertxSubsystemDefinition.INSTANCE);
    }
}
