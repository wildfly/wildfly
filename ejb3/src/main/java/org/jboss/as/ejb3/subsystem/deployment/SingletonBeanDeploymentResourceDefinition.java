/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.subsystem.deployment;


import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;

/**
 * {@link org.jboss.as.controller.ResourceDefinition} for a {@link org.jboss.as.ejb3.component.singleton.SingletonComponent}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class SingletonBeanDeploymentResourceDefinition extends AbstractEJBComponentResourceDefinition {

    static final SimpleAttributeDefinition INIT_ON_STARTUP = new SimpleAttributeDefinitionBuilder("init-on-startup", ModelType.BOOLEAN)
            .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME)
            .build();

    static final StringListAttributeDefinition DEPENDS_ON = StringListAttributeDefinition.Builder.of("depends-on")
            .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME)
            .build();

    static final SimpleAttributeDefinition CONCURRENCY_MANAGEMENT_TYPE = new SimpleAttributeDefinitionBuilder("concurrency-management-type", ModelType.STRING)
            .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME)
            .build();

    public SingletonBeanDeploymentResourceDefinition() {
        super(EJBComponentType.SINGLETON);
    }

    @Override
    public void registerAttributes(final ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
        final AbstractEJBComponentRuntimeHandler<?> handler = componentType.getRuntimeHandler();
        resourceRegistration.registerReadOnlyAttribute(CONCURRENCY_MANAGEMENT_TYPE, handler);
        resourceRegistration.registerReadOnlyAttribute(INIT_ON_STARTUP, handler);
        resourceRegistration.registerReadOnlyAttribute(DEPENDS_ON, handler);
    }
}
