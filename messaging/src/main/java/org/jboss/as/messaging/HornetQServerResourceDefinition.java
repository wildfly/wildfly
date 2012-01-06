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

package org.jboss.as.messaging;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX_OCCURS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_OCCURS;

import java.util.Locale;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.DefaultResourceDescriptionProvider;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.messaging.jms.JMSServerControlHandler;
import org.jboss.dmr.ModelNode;

/**
 * {@link ResourceDefinition} for the messaging subsystem HornetQServer resource.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class HornetQServerResourceDefinition extends SimpleResourceDefinition {

    private static final PathElement HORNETQ_SERVER_PATH = PathElement.pathElement(CommonAttributes.HORNETQ_SERVER);

    private final boolean registerRuntimeOnly;

    HornetQServerResourceDefinition(boolean registerRuntimeOnly) {
        super(HORNETQ_SERVER_PATH, MessagingExtension.getResourceDescriptionResolver(CommonAttributes.HORNETQ_SERVER),
                HornetQServerAdd.INSTANCE, HornetQServerRemove.INSTANCE);
        this.registerRuntimeOnly = registerRuntimeOnly;
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        if (registerRuntimeOnly) {
            HornetQServerControlHandler.INSTANCE.registerOperations(resourceRegistration);
            JMSServerControlHandler.INSTANCE.registerOperations(resourceRegistration);
        }

        // unsupported runtime operations exposed by HornetQServerControl
        // enableMessageCounters, disableMessageCounters
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        HornetQServerControlWriteHandler.INSTANCE.registerAttributes(resourceRegistration, registerRuntimeOnly);
        if (registerRuntimeOnly) {
            HornetQServerControlHandler.INSTANCE.registerAttributes(resourceRegistration);
        }
        // unsupported READ-ATTRIBUTES
        // getConnectors, getAddressNames, getQueueNames, getDivertNames, getBridgeNames,
        // unsupported JMSServerControlHandler READ-ATTRIBUTES
        // getTopicNames, getQueueNames, getConnectionFactoryNames,
    }

    /**
     * {@inheritDoc}
     *
     * The resource description has a small tweak from the standard
     */
    @Override
    public DescriptionProvider getDescriptionProvider(ImmutableManagementResourceRegistration resourceRegistration) {
        return new DefaultResourceDescriptionProvider(resourceRegistration, getResourceDescriptionResolver()) {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                ModelNode result = super.getModelDescription(locale);
                String path = CommonAttributes.PATH.getName();
                result.get(CHILDREN, path, MIN_OCCURS).set(4);
                result.get(CHILDREN, path, MAX_OCCURS).set(4);
                return result;
            }
        };
    }
}
