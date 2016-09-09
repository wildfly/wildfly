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

package org.wildfly.extension.messaging.activemq;

import static org.jboss.as.controller.transform.description.RejectAttributeChecker.DEFINED;

import java.util.Collection;
import java.util.Collections;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.transform.description.AttributeConverter.DefaultValueAttributeConverter;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescription;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.wildfly.extension.messaging.activemq.jms.ConnectionFactoryAttributes;

/**
 * {@link org.jboss.as.controller.ResourceDefinition} for the messaging subsystem root resource.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class MessagingSubsystemRootResourceDefinition extends PersistentResourceDefinition {

    public static final MessagingSubsystemRootResourceDefinition INSTANCE = new MessagingSubsystemRootResourceDefinition();

    private MessagingSubsystemRootResourceDefinition() {
        super(MessagingExtension.SUBSYSTEM_PATH,
                MessagingExtension.getResourceDescriptionResolver(MessagingExtension.SUBSYSTEM_NAME),
                MessagingSubsystemAdd.INSTANCE,
                ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Collections.emptyList();
    }

    public static void registerTransformers(SubsystemRegistration subsystemRegistration) {
        registerTransformers_EAP_7_0_0(subsystemRegistration);
    }

    private static void registerTransformers_EAP_7_0_0(SubsystemRegistration subsystemRegistration) {
        final ResourceTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createSubsystemInstance();

        ResourceTransformationDescriptionBuilder server = builder.addChildResource(MessagingExtension.SERVER_PATH);
        // reject journal-datasource, journal-bindings-table introduced in management version 2.0.0 if it is defined and different from the default value.
        rejectDefinedAttributeWithDefaultValue(server, ServerDefinition.JOURNAL_DATASOURCE,
                ServerDefinition.JOURNAL_MESSAGES_TABLE,
                ServerDefinition.JOURNAL_BINDINGS_TABLE,
                ServerDefinition.JOURNAL_LARGE_MESSAGES_TABLE,
                ServerDefinition.JOURNAL_SQL_PROVIDER_FACTORY);
        ResourceTransformationDescriptionBuilder bridge = server.addChildResource(MessagingExtension.BRIDGE_PATH);
        // reject producer-window-size introduced in management version 2.0.0 if it is defined and different from the default value.
        rejectDefinedAttributeWithDefaultValue(bridge, BridgeDefinition.PRODUCER_WINDOW_SIZE);
        ResourceTransformationDescriptionBuilder clusterConnection = server.addChildResource(MessagingExtension.CLUSTER_CONNECTION_PATH);
        // reject producer-window-size introduced in management version 2.0.0 if it is defined and different from the default value.
        rejectDefinedAttributeWithDefaultValue(clusterConnection, ClusterConnectionDefinition.PRODUCER_WINDOW_SIZE);
        ResourceTransformationDescriptionBuilder pooledConnectionFactory = server.addChildResource(MessagingExtension.POOLED_CONNECTION_FACTORY_PATH);
        // reject rebalance-connections introduced in management version 2.0.0 if it is defined and different from the default value.
        rejectDefinedAttributeWithDefaultValue(pooledConnectionFactory, ConnectionFactoryAttributes.Pooled.REBALANCE_CONNECTIONS);
        // reject statistics-enabled introduced in management version 2.0.0 if it is defined and different from the default value.
        rejectDefinedAttributeWithDefaultValue(pooledConnectionFactory, ConnectionFactoryAttributes.Pooled.STATISTICS_ENABLED);
        // reject max-pool-size whose default value has been changed in  management version 2.0.0
        defaultValueAttributeConverter(pooledConnectionFactory, ConnectionFactoryAttributes.Pooled.MAX_POOL_SIZE);
        // reject min-pool-size whose default value has been changed in  management version 2.0.0
        defaultValueAttributeConverter(pooledConnectionFactory, ConnectionFactoryAttributes.Pooled.MIN_POOL_SIZE);

        TransformationDescription.Tools.register(builder.build(), subsystemRegistration, MessagingExtension.VERSION_1_0_0);
    }

    /**
     * Reject the attributes if they are defined or discard them if they are undefined or set to their default value.
     */
    private static void rejectDefinedAttributeWithDefaultValue(ResourceTransformationDescriptionBuilder builder, AttributeDefinition... attrs) {
        for (AttributeDefinition attr : attrs) {
            builder.getAttributeBuilder()
                    .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(attr.getDefaultValue()), attr)
                    .addRejectCheck(DEFINED, attr);
        }
    }

    /**
     * Reject the attributes if they are defined.
     */
    private static void rejectDefinedAttribute(ResourceTransformationDescriptionBuilder builder, AttributeDefinition... attrs) {
        for (AttributeDefinition attr : attrs) {
            builder.getAttributeBuilder()
                    .addRejectCheck(DEFINED, attr);
        }
    }

    private static void defaultValueAttributeConverter(ResourceTransformationDescriptionBuilder builder, AttributeDefinition attr) {
       builder.getAttributeBuilder()
                .setValueConverter(new DefaultValueAttributeConverter(attr), attr);
    }
}
