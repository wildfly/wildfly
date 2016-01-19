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

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * {@link org.jboss.as.controller.ResourceDefinition} for a {@link org.jboss.as.ejb3.component.messagedriven.MessageDrivenComponent}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class MessageDrivenBeanResourceDefinition extends AbstractEJBComponentResourceDefinition {

    public static final MessageDrivenBeanResourceDefinition INSTANCE = new MessageDrivenBeanResourceDefinition();

    public static final AttributeDefinition DELIVERY_ACTIVE = new SimpleAttributeDefinitionBuilder("delivery-active", ModelType.BOOLEAN)
            .setDefaultValue(new ModelNode(true))
            .setStorageRuntime()
            .build();

    static final String START_DELIVERY = "start-delivery";
    static final String STOP_DELIVERY = "stop-delivery";

    private MessageDrivenBeanResourceDefinition() {
        super(EJBComponentType.MESSAGE_DRIVEN);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registry) {
        super.registerAttributes(registry);

        registry.registerReadOnlyAttribute(DELIVERY_ACTIVE, MessageDrivenBeanRuntimeHandler.INSTANCE);
    }

    @Override
    public void registerOperations(ManagementResourceRegistration registry) {
        super.registerOperations(registry);

        final SimpleOperationDefinition startDelivery = new SimpleOperationDefinitionBuilder(START_DELIVERY, getResourceDescriptionResolver())
                        .setRuntimeOnly()
                        .build();
        registry.registerOperationHandler(startDelivery, MessageDrivenBeanRuntimeHandler.INSTANCE);
        final SimpleOperationDefinition stopDelivery = new SimpleOperationDefinitionBuilder(STOP_DELIVERY, getResourceDescriptionResolver())
                        .setRuntimeOnly()
                        .build();
        registry.registerOperationHandler(stopDelivery, MessageDrivenBeanRuntimeHandler.INSTANCE);
    }
}
