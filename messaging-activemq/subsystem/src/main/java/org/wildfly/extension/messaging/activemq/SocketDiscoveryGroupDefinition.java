/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq;

import static org.jboss.as.controller.SimpleAttributeDefinitionBuilder.create;
import static org.jboss.as.controller.client.helpers.MeasurementUnit.MILLISECONDS;

import java.util.Arrays;
import java.util.Collection;
import org.apache.activemq.artemis.api.config.ActiveMQDefaultConfiguration;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.DynamicNameMappers;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;


/**
 *  Discovery group resource definition using socket bindings.
 * @author Emmanuel Hugonnet (c) 2019 Red Hat, Inc.
 */
public class SocketDiscoveryGroupDefinition extends PersistentResourceDefinition {

    public static final PathElement PATH = PathElement.pathElement(CommonAttributes.SOCKET_DISCOVERY_GROUP);

    public static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of("org.wildfly.messaging.activemq.socket-discovery-group", true)
            .setDynamicNameMapper(DynamicNameMappers.PARENT)
            // WFLY-10518 - only the name of the discovery-group is used for its capability as the resource can be
            // either under server (and it is deprecated) or under the subsystem.
            .build();

    /**
     * @see ActiveMQDefaultConfiguration#getDefaultBroadcastRefreshTimeout
     */
    public static final SimpleAttributeDefinition REFRESH_TIMEOUT = create("refresh-timeout", ModelType.LONG)
            .setDefaultValue(new ModelNode(10000L))
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

    public static final SimpleAttributeDefinition SOCKET_BINDING = create(CommonAttributes.SOCKET_BINDING)
            .setRequired(true)
            .setAlternatives(new String[0])
            .build();

    public static final AttributeDefinition[] ATTRIBUTES = {SOCKET_BINDING, REFRESH_TIMEOUT, INITIAL_WAIT_TIMEOUT};

    private final boolean registerRuntimeOnly;

    protected SocketDiscoveryGroupDefinition(final boolean registerRuntimeOnly, final boolean subsystemResource) {
        super(new SimpleResourceDefinition.Parameters(PATH, MessagingExtension.getResourceDescriptionResolver(CommonAttributes.DISCOVERY_GROUP))
                .setAddHandler(SocketDiscoveryGroupAdd.INSTANCE)
                .setRemoveHandler(SocketDiscoveryGroupRemove.INSTANCE)
                .addCapabilities(CAPABILITY));
        this.registerRuntimeOnly = registerRuntimeOnly;
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(ATTRIBUTES);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registry) {
        ReloadRequiredWriteAttributeHandler reloadRequiredWriteAttributeHandler = new ReloadRequiredWriteAttributeHandler(ATTRIBUTES);
        for (AttributeDefinition attr : ATTRIBUTES) {
            if (registerRuntimeOnly || !attr.getFlags().contains(AttributeAccess.Flag.STORAGE_RUNTIME)) {
                registry.registerReadWriteAttribute(attr, null, reloadRequiredWriteAttributeHandler);
            }
        }
    }
}
