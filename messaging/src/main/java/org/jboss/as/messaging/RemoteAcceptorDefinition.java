/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.messaging;

import static org.jboss.as.controller.SimpleAttributeDefinitionBuilder.create;
import static org.jboss.dmr.ModelType.STRING;

import java.util.EnumSet;
import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.DefaultOperationDescriptionProvider;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;

/**
 * in-vm acceptor resource definition
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2012 Red Hat Inc.
 */
public class RemoteAcceptorDefinition extends SimpleResourceDefinition {

    public static final AttributeDefinition SOCKET_BINDING = create("socket-binding", STRING)
            .setRestartAllServices()
            .build();

    static AttributeDefinition[] ATTRIBUTES = new AttributeDefinition[] { SOCKET_BINDING };

    static final OperationStepHandler REMOTE_ADD = new TransportConfigOperationHandlers.BasicTransportConfigAdd(ATTRIBUTES);

    static final OperationStepHandler REMOTE_ATTRIBUTE_HANDLER = new TransportConfigOperationHandlers.AttributeWriteHandler(ATTRIBUTES);

    private final boolean registerRuntimeOnly;


    public RemoteAcceptorDefinition(final boolean registerRuntimeOnly) {
        super(PathElement.pathElement(CommonAttributes.REMOTE_ACCEPTOR),
            new StandardResourceDescriptionResolver(CommonAttributes.ACCEPTOR, MessagingExtension.RESOURCE_NAME, MessagingExtension.class.getClassLoader(), true, false) {
                @Override
                public String getResourceDescription(Locale locale, ResourceBundle bundle) {
                    return bundle.getString(CommonAttributes.REMOTE_ACCEPTOR);
                }
            },
            REMOTE_ADD,
            TransportConfigOperationHandlers.REMOVE);
        this.registerRuntimeOnly = registerRuntimeOnly;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registry) {
        super.registerAttributes(registry);

        for (AttributeDefinition attr : ATTRIBUTES) {
            if (registerRuntimeOnly || !attr.getFlags().contains(AttributeAccess.Flag.STORAGE_RUNTIME)) {
                registry.registerReadWriteAttribute(attr, null, REMOTE_ATTRIBUTE_HANDLER);
            }
        }

        if (registerRuntimeOnly) {
            registry.registerReadOnlyAttribute(AcceptorControlHandler.STARTED, AcceptorControlHandler.INSTANCE);
        }
    }

    @Override
    public void registerOperations(ManagementResourceRegistration registry) {
        if (registerRuntimeOnly) {
            for (String operation : AcceptorControlHandler.OPERATIONS) {
                final DescriptionProvider desc = new DefaultOperationDescriptionProvider(operation, getResourceDescriptionResolver());
                registry.registerOperationHandler(operation,
                        AcceptorControlHandler.INSTANCE,
                        desc,
                        EnumSet.of(OperationEntry.Flag.READ_ONLY, OperationEntry.Flag.RUNTIME_ONLY));
            }
        }

        super.registerOperations(registry);
    }

    @Override
    public void registerChildren(ManagementResourceRegistration registry) {
        super.registerChildren(registry);

        registry.registerSubModel(new TransportParamDefinition());
    }
}
