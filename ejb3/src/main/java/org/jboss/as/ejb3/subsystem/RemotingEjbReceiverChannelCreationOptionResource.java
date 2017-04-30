/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ejb3.subsystem;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;


/**
 * {@link SimpleResourceDefinition} for the channel creation option(s) that are configured for the remoting ejb receivers
 *
 * @author Jaikiran Pai
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
class RemotingEjbReceiverChannelCreationOptionResource extends SimpleResourceDefinition {

    /**
     * Attribute definition of the channel creation option "value"
     */
    static final SimpleAttributeDefinition CHANNEL_CREATION_OPTION_VALUE = new SimpleAttributeDefinitionBuilder(
            EJB3SubsystemModel.VALUE, ModelType.STRING, true).setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES).build();

    /**
     * Attribute definition of the channel creation option "type"
     */
    static final SimpleAttributeDefinition CHANNEL_CREATION_OPTION_TYPE = new SimpleAttributeDefinitionBuilder(
            EJB3SubsystemModel.TYPE, ModelType.STRING, true).setRequired(true)
            .setValidator(AllowedChannelOptionTypesValidator.INSTANCE).build();

    public static final Map<String, AttributeDefinition> ATTRIBUTES;

    static {
        Map<String, AttributeDefinition> map = new LinkedHashMap<String, AttributeDefinition>();
        map.put(CHANNEL_CREATION_OPTION_VALUE.getName(), CHANNEL_CREATION_OPTION_VALUE);
        map.put(CHANNEL_CREATION_OPTION_TYPE.getName(), CHANNEL_CREATION_OPTION_TYPE);

        ATTRIBUTES = Collections.unmodifiableMap(map);
    }

    static final RemotingEjbReceiverChannelCreationOptionResource INSTANCE = new RemotingEjbReceiverChannelCreationOptionResource();

    RemotingEjbReceiverChannelCreationOptionResource() {
        super(PathElement.pathElement(EJB3SubsystemModel.CHANNEL_CREATION_OPTIONS),
                EJB3Extension.getResourceDescriptionResolver(EJB3SubsystemModel.CHANNEL_CREATION_OPTIONS),
                new RemotingProfileChildResourceAddHandler(ATTRIBUTES.values()),
                new RemotingProfileChildResourceRemoveHandler());
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadWriteAttribute(CHANNEL_CREATION_OPTION_VALUE, null, new RemotingProfileResourceChildWriteAttributeHandler(CHANNEL_CREATION_OPTION_VALUE));
        resourceRegistration.registerReadWriteAttribute(CHANNEL_CREATION_OPTION_TYPE, null, new RemotingProfileResourceChildWriteAttributeHandler(CHANNEL_CREATION_OPTION_TYPE));
    }
}
