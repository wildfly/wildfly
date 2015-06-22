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

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.DeprecatedResourceDescriptionResolver;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * Abstract acceptor resource definition
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2012 Red Hat Inc.
 */
public abstract class AbstractTransportDefinition extends SimpleResourceDefinition {

    private final boolean registerRuntimeOnly;
    private final AttributeDefinition[] attrs;
    protected final boolean isAcceptor;

    /**
     * If the keys are not known at compile time (e.g. for generic transport), the method
     * must return an empty set.
     *
     * @return the set of keys that are allowed by the transport.
     */
    protected abstract Set<String> getAllowedKeys();

    protected AbstractTransportDefinition(final boolean registerRuntimeOnly, final boolean isAcceptor, final String specificType, AttributeDefinition... attrs) {
        super(PathElement.pathElement(specificType),
                new DeprecatedResourceDescriptionResolver(MessagingExtension.SUBSYSTEM_NAME, (isAcceptor ? CommonAttributes.ACCEPTOR : CommonAttributes.CONNECTOR),
                        MessagingExtension.RESOURCE_NAME, MessagingExtension.class.getClassLoader(), true, false) {
                    @Override
                    public String getResourceDescription(Locale locale, ResourceBundle bundle) {
                        return bundle.getString(specificType);
                    }
                },
                new HornetQReloadRequiredHandlers.AddStepHandler(attrs),
                new HornetQReloadRequiredHandlers.RemoveStepHandler());
        this.registerRuntimeOnly = registerRuntimeOnly;
        this.isAcceptor = isAcceptor;
        this.attrs = attrs;
        setDeprecated(MessagingExtension.DEPRECATED_SINCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registry) {
        super.registerAttributes(registry);

        OperationStepHandler attributeHandler = new ReloadRequiredWriteAttributeHandler(attrs);
        for (AttributeDefinition attr : attrs) {
            if (registerRuntimeOnly || !attr.getFlags().contains(AttributeAccess.Flag.STORAGE_RUNTIME)) {
                registry.registerReadWriteAttribute(attr, null, attributeHandler);
            }
        }

        if (isAcceptor && registerRuntimeOnly) {
            AcceptorControlHandler.INSTANCE.registerAttributes(registry);
        }
    }

    @Override
    public void registerOperations(ManagementResourceRegistration registry) {
        if (isAcceptor && registerRuntimeOnly) {
            AcceptorControlHandler.INSTANCE.registerOperations(registry, getResourceDescriptionResolver());
        }

        super.registerOperations(registry);
    }

    @Override
    public void registerChildren(ManagementResourceRegistration registry) {
        super.registerChildren(registry);

        registry.registerSubModel(new TransportParamDefinition(getAllowedKeys()));
    }
}
