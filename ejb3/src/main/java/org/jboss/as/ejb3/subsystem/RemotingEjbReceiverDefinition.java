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
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * {@link org.jboss.as.controller.ResourceDefinition} for remoting Jakarta Enterprise Beans receiver in remoting profile.
 *
 * This is deprecated, but is still required for domain most support for older servers.
 *
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
public class RemotingEjbReceiverDefinition extends SimpleResourceDefinition {

    public static final SimpleAttributeDefinition OUTBOUND_CONNECTION_REF = new SimpleAttributeDefinitionBuilder(
            EJB3SubsystemModel.OUTBOUND_CONNECTION_REF, ModelType.STRING).setRequired(true).setAllowExpression(true)
            .build();

    public static final SimpleAttributeDefinition CONNECT_TIMEOUT = new SimpleAttributeDefinitionBuilder(
            EJB3SubsystemModel.CONNECT_TIMEOUT, ModelType.LONG, true).setDefaultValue(new ModelNode(5000L))
            .setAllowExpression(true).build();

    private static final AttributeDefinition[] ATTRIBUTES = new AttributeDefinition[] { OUTBOUND_CONNECTION_REF, CONNECT_TIMEOUT };

    RemotingEjbReceiverDefinition() {
        super(PathElement.pathElement(EJB3SubsystemModel.REMOTING_EJB_RECEIVER), EJB3Extension
                .getResourceDescriptionResolver(EJB3SubsystemModel.REMOTING_EJB_RECEIVER), new RemotingProfileChildResourceAddHandler(
                ATTRIBUTES), new RemotingProfileChildResourceRemoveHandler());
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (final AttributeDefinition attr : ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attr, null, new RemotingProfileResourceChildWriteAttributeHandler(attr));
        }
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        super.registerChildren(resourceRegistration);
        resourceRegistration.registerSubModel(new RemotingEjbReceiverChannelCreationOptionResource());
    }
}
