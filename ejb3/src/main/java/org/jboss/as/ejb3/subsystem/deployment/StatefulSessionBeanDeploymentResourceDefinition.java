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

import org.jboss.as.controller.ObjectListAttributeDefinition;
import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;

/**
 * {@link org.jboss.as.controller.ResourceDefinition} for a {@link org.jboss.as.ejb3.component.stateful.StatefulSessionComponent}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class StatefulSessionBeanDeploymentResourceDefinition extends AbstractEJBComponentResourceDefinition {

    public static final StatefulSessionBeanDeploymentResourceDefinition INSTANCE = new StatefulSessionBeanDeploymentResourceDefinition();

    static final SimpleAttributeDefinition STATEFUL_TIMEOUT = new SimpleAttributeDefinitionBuilder("stateful-timeout", ModelType.STRING)
            .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME)
            .build();
    static final SimpleAttributeDefinition AFTER_BEGIN_METHOD = new SimpleAttributeDefinitionBuilder("after-begin-method", ModelType.STRING)
            .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME)
            .build();
    static final SimpleAttributeDefinition BEFORE_COMPLETION_METHOD = new SimpleAttributeDefinitionBuilder("before-completion-method", ModelType.STRING)
            .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME)
            .build();
    static final SimpleAttributeDefinition AFTER_COMPLETION_METHOD = new SimpleAttributeDefinitionBuilder("after-completion-method", ModelType.STRING)
            .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME)
            .build();
    static final SimpleAttributeDefinition PASSIVATION_CAPABLE = new SimpleAttributeDefinitionBuilder("passivation-capable", ModelType.BOOLEAN)
            .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME)
            .build();

    static final SimpleAttributeDefinition BEAN_METHOD = new SimpleAttributeDefinitionBuilder("bean-method", ModelType.STRING)
            .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME)
            .build();
    static final SimpleAttributeDefinition RETAIN_IF_EXCEPTION = new SimpleAttributeDefinitionBuilder("retain-if-exception", ModelType.BOOLEAN)
            .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME)
            .build();
    static final ObjectTypeAttributeDefinition REMOVE_METHOD = new ObjectTypeAttributeDefinition.Builder("remove-method", BEAN_METHOD, RETAIN_IF_EXCEPTION)
            .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME)
            .build();
    static final ObjectListAttributeDefinition REMOVE_METHODS = new ObjectListAttributeDefinition.Builder("remove-methods", REMOVE_METHOD)
            .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME)
            .build();

    private StatefulSessionBeanDeploymentResourceDefinition() {
        super(EJBComponentType.STATEFUL);
    }

    @Override
    public void registerAttributes(final ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
        final AbstractEJBComponentRuntimeHandler<?> handler = componentType.getRuntimeHandler();

        resourceRegistration.registerReadOnlyAttribute(STATEFUL_TIMEOUT, handler);
        resourceRegistration.registerReadOnlyAttribute(AFTER_BEGIN_METHOD, handler);
        resourceRegistration.registerReadOnlyAttribute(BEFORE_COMPLETION_METHOD, handler);
        resourceRegistration.registerReadOnlyAttribute(AFTER_COMPLETION_METHOD, handler);
        resourceRegistration.registerReadOnlyAttribute(PASSIVATION_CAPABLE, handler);
        resourceRegistration.registerReadOnlyAttribute(REMOVE_METHODS, handler);
    }
}
