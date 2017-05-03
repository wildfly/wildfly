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
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * {@link org.jboss.as.controller.ResourceDefinition} for remoting ejb receiver in remoting profile.
 *
 * This is deprecated, but is still required for domain most support for older servers.
 *
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
public class RemotingEjbReceiverDefinition extends SimpleResourceDefinition {

    public static final SimpleAttributeDefinition OUTBOUND_CONNECTION_REF = new SimpleAttributeDefinitionBuilder(
            EJB3SubsystemModel.OUTBOUND_CONNECTION_REF, ModelType.STRING, true).setRequired(true).setAllowExpression(true)
            .build();

    public static final SimpleAttributeDefinition CONNECT_TIMEOUT = new SimpleAttributeDefinitionBuilder(
            EJB3SubsystemModel.CONNECT_TIMEOUT, ModelType.LONG, true).setDefaultValue(new ModelNode(5000L))
            .setAllowExpression(true).build();

    public static final Map<String, AttributeDefinition> ATTRIBUTES;

    static {
        Map<String, AttributeDefinition> map = new LinkedHashMap<String, AttributeDefinition>();
        map.put(OUTBOUND_CONNECTION_REF.getName(), OUTBOUND_CONNECTION_REF);
        map.put(CONNECT_TIMEOUT.getName(), CONNECT_TIMEOUT);

        ATTRIBUTES = Collections.unmodifiableMap(map);
    }

    public static final RemotingEjbReceiverDefinition INSTANCE = new RemotingEjbReceiverDefinition();

    private RemotingEjbReceiverDefinition() {
        super(PathElement.pathElement(EJB3SubsystemModel.REMOTING_EJB_RECEIVER), EJB3Extension
                .getResourceDescriptionResolver(EJB3SubsystemModel.REMOTING_EJB_RECEIVER), new RemotingProfileChildResourceAddHandler(
                ATTRIBUTES.values()), new RemotingProfileChildResourceRemoveHandler());
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (final AttributeDefinition attr : ATTRIBUTES.values()) {
            resourceRegistration.registerReadWriteAttribute(attr, null, new RemotingProfileResourceChildWriteAttributeHandler(attr));
        }
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        super.registerChildren(resourceRegistration);
        resourceRegistration.registerSubModel(new RemotingEjbReceiverChannelCreationOptionResource());
    }
}
