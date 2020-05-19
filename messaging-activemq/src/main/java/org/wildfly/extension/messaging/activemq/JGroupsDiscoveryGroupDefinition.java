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
import org.wildfly.clustering.spi.ClusteringDefaultRequirement;
import org.wildfly.clustering.spi.ClusteringRequirement;


/**
 * Discovery group resource definition using JGroups.
 * @author Emmanuel Hugonnet (c) 2019 Red Hat, Inc.
 */
public class JGroupsDiscoveryGroupDefinition extends PersistentResourceDefinition {

    public static final PathElement PATH = PathElement.pathElement(CommonAttributes.JGROUPS_DISCOVERY_GROUP);

    public static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of("org.wildfly.messaging.activemq.jgroups-discovery-group", true)
            .setDynamicNameMapper(DynamicNameMappers.PARENT)
            .addRequirements(ClusteringDefaultRequirement.COMMAND_DISPATCHER_FACTORY.getName())
            // WFLY-10518 - only the name of the discovery-group is used for its capability as the resource can be
            // either under server (and it is deprecated) or under the subsystem.
            .build();

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

    @Deprecated public static final SimpleAttributeDefinition JGROUPS_CHANNEL_FACTORY = create(CommonAttributes.JGROUPS_CHANNEL_FACTORY)
            .setCapabilityReference("org.wildfly.clustering.jgroups.channel-factory")
            .build();

    public static final SimpleAttributeDefinition JGROUPS_CLUSTER = create(CommonAttributes.JGROUPS_CLUSTER)
            .setRequired(true)
            .setAlternatives(new String[0])
            .build();

    public static final SimpleAttributeDefinition JGROUPS_CHANNEL = create(CommonAttributes.JGROUPS_CHANNEL)
            .setCapabilityReference(ClusteringRequirement.COMMAND_DISPATCHER_FACTORY.getName())
            .build();

    public static final AttributeDefinition[] ATTRIBUTES = {
        JGROUPS_CHANNEL_FACTORY, JGROUPS_CHANNEL, JGROUPS_CLUSTER, REFRESH_TIMEOUT, INITIAL_WAIT_TIMEOUT
    };

    private final boolean registerRuntimeOnly;

    protected JGroupsDiscoveryGroupDefinition(final boolean registerRuntimeOnly, final boolean subsystemResource) {
        super(new SimpleResourceDefinition.Parameters(PATH, MessagingExtension.getResourceDescriptionResolver(CommonAttributes.DISCOVERY_GROUP))
                .setAddHandler(JGroupsDiscoveryGroupAdd.INSTANCE)
                .setRemoveHandler(JGroupsDiscoveryGroupRemove.INSTANCE)
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
