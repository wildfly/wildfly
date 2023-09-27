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
import org.jboss.dmr.ModelType;

/**
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
public class RemoteHttpConnectionDefinition extends SimpleResourceDefinition {

    public static final SimpleAttributeDefinition URI = new SimpleAttributeDefinitionBuilder(
            EJB3SubsystemModel.URI, ModelType.STRING).setRequired(true).setAllowExpression(true)
            .build();

    private static final AttributeDefinition[] ATTRIBUTES = new AttributeDefinition[]{URI};

    RemoteHttpConnectionDefinition() {
        super(PathElement.pathElement(EJB3SubsystemModel.REMOTE_HTTP_CONNECTION), EJB3Extension
                .getResourceDescriptionResolver(EJB3SubsystemModel.REMOTE_HTTP_CONNECTION), new RemotingProfileChildResourceAddHandler(
                ATTRIBUTES), new RemotingProfileChildResourceRemoveHandler());
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (final AttributeDefinition attr : ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attr, null, new RemotingProfileResourceChildWriteAttributeHandler(attr));
        }
    }
}

