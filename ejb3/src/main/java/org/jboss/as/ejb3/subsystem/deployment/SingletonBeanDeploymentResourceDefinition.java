/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

    public static final SingletonBeanDeploymentResourceDefinition INSTANCE = new SingletonBeanDeploymentResourceDefinition();

    static final SimpleAttributeDefinition INIT_ON_STARTUP = new SimpleAttributeDefinitionBuilder("init-on-startup", ModelType.BOOLEAN)
            .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME)
            .build();

    static final StringListAttributeDefinition DEPENDS_ON = StringListAttributeDefinition.Builder.of("depends-on")
            .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME)
            .build();

    static final SimpleAttributeDefinition CONCURRENCY_MANAGEMENT_TYPE = new SimpleAttributeDefinitionBuilder("concurrency-management-type", ModelType.STRING)
            .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME)
            .build();

    private SingletonBeanDeploymentResourceDefinition() {
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
