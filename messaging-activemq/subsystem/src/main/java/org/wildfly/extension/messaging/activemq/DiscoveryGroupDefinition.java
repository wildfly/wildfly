/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.messaging.activemq;

import static org.jboss.as.controller.SimpleAttributeDefinitionBuilder.create;
import static org.jboss.as.controller.client.helpers.MeasurementUnit.MILLISECONDS;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.apache.activemq.artemis.api.config.ActiveMQDefaultConfiguration;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.DeprecationData;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.logging.ControllerLogger;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.messaging.activemq.shallow.ShallowResourceDefinition;

/**
 * Discovery group resource definition
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2012 Red Hat Inc.
 * @deprecated Use JGroupsDiscoveryGroupDefinition or SocketDiscoveryGroupDefinition.
 */
public class DiscoveryGroupDefinition extends ShallowResourceDefinition {

    public static final PathElement PATH = PathElement.pathElement(CommonAttributes.DISCOVERY_GROUP);
    /**
     * @see ActiveMQDefaultConfiguration#getDefaultBroadcastRefreshTimeout
     */
    public static final SimpleAttributeDefinition REFRESH_TIMEOUT = create("refresh-timeout", ModelType.LONG)
            .setDefaultValue(new ModelNode(10000))
            .setMeasurementUnit(MILLISECONDS)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    /**
     * @see ActiveMQClient.DEFAULT_DISCOVERY_INITIAL_WAIT_TIMEOUT
     */
    public static final SimpleAttributeDefinition INITIAL_WAIT_TIMEOUT = create("initial-wait-timeout", ModelType.LONG)
            .setDefaultValue(new ModelNode(10000L))
            .setMeasurementUnit(MILLISECONDS)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    @Deprecated
    public static final SimpleAttributeDefinition JGROUPS_CHANNEL_FACTORY = create(CommonAttributes.JGROUPS_CHANNEL_FACTORY)
            .build();

    public static final SimpleAttributeDefinition JGROUPS_CHANNEL = create(CommonAttributes.JGROUPS_CHANNEL)
            .build();
    public static final SimpleAttributeDefinition JGROUPS_CLUSTER = create(CommonAttributes.JGROUPS_CLUSTER)
            .build();
    public static final SimpleAttributeDefinition SOCKET_BINDING = create(CommonAttributes.SOCKET_BINDING)
            .build();

    public static final AttributeDefinition[] ATTRIBUTES = {JGROUPS_CHANNEL_FACTORY, JGROUPS_CHANNEL, JGROUPS_CLUSTER, SOCKET_BINDING,
        REFRESH_TIMEOUT, INITIAL_WAIT_TIMEOUT
    };

    protected DiscoveryGroupDefinition(final boolean registerRuntimeOnly, final boolean subsystemResource) {
        super(new SimpleResourceDefinition.Parameters(PATH, MessagingExtension.getResourceDescriptionResolver(CommonAttributes.DISCOVERY_GROUP))
                .setAddHandler(DiscoveryGroupAdd.INSTANCE)
                .setRemoveHandler(DiscoveryGroupRemove.INSTANCE)
                .setDeprecationData(new DeprecationData(MessagingExtension.VERSION_9_0_0, true))
                .setFeature(false),
                registerRuntimeOnly);
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(ATTRIBUTES);
    }

    @Override
    public PathAddress convert(OperationContext context, ModelNode operation) {
        PathAddress parent = context.getCurrentAddress().getParent();
        PathAddress targetAddress = parent.append(CommonAttributes.JGROUPS_DISCOVERY_GROUP, context.getCurrentAddressValue());
        try {
            context.readResourceFromRoot(targetAddress, false);
            return targetAddress;
        } catch (Resource.NoSuchResourceException ex) {
            return parent.append(CommonAttributes.SOCKET_DISCOVERY_GROUP, context.getCurrentAddressValue());
        }
    }

    @Override
    public Set<String> getIgnoredAttributes(OperationContext context, ModelNode operation) {
        PathAddress targetAddress = context.getCurrentAddress().getParent().append(CommonAttributes.JGROUPS_DISCOVERY_GROUP, context.getCurrentAddressValue());
        Set<String> ignoredAttributes = new HashSet<>();
        try {
            context.readResourceFromRoot(targetAddress, false);
            ignoredAttributes.add(SOCKET_BINDING.getName());
        } catch (Resource.NoSuchResourceException ex) {
            ignoredAttributes.add(JGROUPS_CHANNEL_FACTORY.getName());
            ignoredAttributes.add(JGROUPS_CHANNEL.getName());
            ignoredAttributes.add(JGROUPS_CLUSTER.getName());
        }
        return ignoredAttributes;
    }

    /**
     * This provides more informative message when a user tries to undefine attribute that is required by the
     * jgroups-discovery-group or socket-discovery-group resources (while it hasn't been required on the original
     * discovery-group resource).
     */
    @Override
    public void validateOperation(OperationContext context, PathAddress targetAddress, ModelNode translatedOperation) throws OperationFailedException {
        String attributeName = translatedOperation.get(ModelDescriptionConstants.NAME).asString();
        String resourceName = targetAddress.getLastElement().getKey();
        String operationName = translatedOperation.get(ModelDescriptionConstants.OP).asString();
        ModelNode value = translatedOperation.get(ModelDescriptionConstants.VALUE);
        boolean isSocketDiscoveryGroup = CommonAttributes.SOCKET_DISCOVERY_GROUP.equals(resourceName);

        // if undefining an attribute
        if ((ModelDescriptionConstants.UNDEFINE_ATTRIBUTE_OPERATION.equals(operationName)
                || ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION.equals(operationName))
                && !value.isDefined()) {
            // and the attribute is socket-binding on a socket-discovery-group,
            // or jgroups-cluster on a jgroups-discovery-group, throw an error
            if ((isSocketDiscoveryGroup && attributeName.equals(CommonAttributes.SOCKET_BINDING.getName()))
                    || (!isSocketDiscoveryGroup && attributeName.equals(CommonAttributes.JGROUPS_CLUSTER.getName()))) {
                throw ControllerLogger.ROOT_LOGGER.validationFailedRequiredParameterNotPresent(attributeName, translatedOperation.toString());
            }
        }
    }
}
