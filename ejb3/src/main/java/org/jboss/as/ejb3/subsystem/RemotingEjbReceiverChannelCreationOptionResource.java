/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.subsystem;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;


/**
 * {@link SimpleResourceDefinition} for the channel creation option(s) that are configured for the remoting Jakarta Enterprise Beans receivers
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
            EJB3SubsystemModel.TYPE, ModelType.STRING).setRequired(true)
            .setValidator(AllowedChannelOptionTypesValidator.INSTANCE).build();

    private static final AttributeDefinition[] ATTRIBUTES = new AttributeDefinition[] { CHANNEL_CREATION_OPTION_VALUE, CHANNEL_CREATION_OPTION_TYPE };

    static final RemotingEjbReceiverChannelCreationOptionResource INSTANCE = new RemotingEjbReceiverChannelCreationOptionResource();

    RemotingEjbReceiverChannelCreationOptionResource() {
        super(PathElement.pathElement(EJB3SubsystemModel.CHANNEL_CREATION_OPTIONS),
                EJB3Extension.getResourceDescriptionResolver(EJB3SubsystemModel.CHANNEL_CREATION_OPTIONS),
                new RemotingProfileChildResourceAddHandler(ATTRIBUTES),
                new RemotingProfileChildResourceRemoveHandler());
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadWriteAttribute(CHANNEL_CREATION_OPTION_VALUE, null, new RemotingProfileResourceChildWriteAttributeHandler(CHANNEL_CREATION_OPTION_VALUE));
        resourceRegistration.registerReadWriteAttribute(CHANNEL_CREATION_OPTION_TYPE, null, new RemotingProfileResourceChildWriteAttributeHandler(CHANNEL_CREATION_OPTION_TYPE));
    }
}
