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

package org.jboss.as.messaging;

import static org.jboss.as.controller.PathElement.pathElement;
import static org.jboss.as.controller.transform.description.RejectAttributeChecker.DEFINED;
import static org.jboss.as.messaging.AddressSettingDefinition.MAX_REDELIVERY_DELAY;
import static org.jboss.as.messaging.AddressSettingDefinition.REDELIVERY_MULTIPLIER;
import static org.jboss.as.messaging.AddressSettingDefinition.SLOW_CONSUMER_CHECK_PERIOD;
import static org.jboss.as.messaging.AddressSettingDefinition.SLOW_CONSUMER_POLICY;
import static org.jboss.as.messaging.AddressSettingDefinition.SLOW_CONSUMER_THRESHOLD;
import static org.jboss.as.messaging.CommonAttributes.HORNETQ_SERVER;
import static org.jboss.as.messaging.CommonAttributes.OVERRIDE_IN_VM_SECURITY;
import static org.jboss.as.messaging.CommonAttributes.RETRY_INTERVAL_MULTIPLIER;
import static org.jboss.as.messaging.MessagingExtension.VERSION_1_3_0;
import static org.jboss.as.messaging.MessagingExtension.VERSION_1_4_0;
import static org.jboss.as.messaging.MessagingExtension.VERSION_2_0_0;
import static org.jboss.as.messaging.MessagingExtension.VERSION_2_1_0;

import java.math.BigDecimal;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.description.AttributeConverter;
import org.jboss.as.controller.transform.description.ChainedTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker.DiscardAttributeValueChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.jboss.as.messaging.jms.ConnectionFactoryDefinition;
import org.jboss.as.messaging.jms.PooledConnectionFactoryDefinition;
import org.jboss.dmr.ModelNode;

/**
 * Resource transformations for the messaging subsystem.
 * <p>
 * <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2012 Red Hat, inc.
 */

public class MessagingTransformers {

    static void registerTransformers(final SubsystemRegistration subsystem) {
        ChainedTransformationDescriptionBuilder chainedBuilder = TransformationDescriptionBuilder.Factory.createChainedSubystemInstance(subsystem.getSubsystemVersion());

        // Current
        // 3.0.0 -> 2.1.0 (WildFly 8.1.0.Final)
        buildTransformers2_1_0(chainedBuilder.createBuilder(subsystem.getSubsystemVersion(), VERSION_2_1_0));
        // 2.1.0 -> 2.0.0 (WildFly 8.0.0.Final)
        buildTransformers2_0_0(chainedBuilder.createBuilder(VERSION_2_1_0, VERSION_2_0_0));
        // 2.0.0 -> 1.4.0 (AS7 7.5.0) EAP 6.4
        buildTransformers1_4_0(chainedBuilder.createBuilder(VERSION_2_0_0, VERSION_1_4_0));
        // 1.4.0 -> 1.3.0 (AS7 7.3.0) EAP 6.2 & EAP 6.3
        buildTransformers1_3_0(chainedBuilder.createBuilder(VERSION_1_4_0, VERSION_1_3_0));

        chainedBuilder.buildAndRegister(subsystem, new ModelVersion[]{
                VERSION_1_3_0,
                VERSION_1_4_0,
                VERSION_2_0_0,
                VERSION_2_1_0
        });
    }

    /**
     * Transformation for WildFly 8.1.0.Final
     */
    private static void buildTransformers2_1_0(ResourceTransformationDescriptionBuilder builder) {
        ResourceTransformationDescriptionBuilder hornetqServer = builder.addChildResource(pathElement(HORNETQ_SERVER));
        ResourceTransformationDescriptionBuilder addressSetting = hornetqServer.addChildResource(AddressSettingDefinition.PATH);
        rejectDefinedAttributeWithDefaultValue(addressSetting, MAX_REDELIVERY_DELAY, REDELIVERY_MULTIPLIER);

        ResourceTransformationDescriptionBuilder bridge = hornetqServer.addChildResource(BridgeDefinition.PATH);
        bridge.getAttributeBuilder().setValueConverter(new DoubleToBigDecimalConverter(), RETRY_INTERVAL_MULTIPLIER);


        ResourceTransformationDescriptionBuilder clusterConnection = hornetqServer.addChildResource(ClusterConnectionDefinition.PATH);
        clusterConnection.getAttributeBuilder().setValueConverter(new DoubleToBigDecimalConverter(), RETRY_INTERVAL_MULTIPLIER);

        ResourceTransformationDescriptionBuilder connectionFactory = hornetqServer.addChildResource(ConnectionFactoryDefinition.PATH);
        connectionFactory.getAttributeBuilder().setValueConverter(new DoubleToBigDecimalConverter(), RETRY_INTERVAL_MULTIPLIER);

        ResourceTransformationDescriptionBuilder pooledConnectionFactory = hornetqServer.addChildResource(PooledConnectionFactoryDefinition.PATH);
        pooledConnectionFactory.getAttributeBuilder().setValueConverter(new DoubleToBigDecimalConverter(), RETRY_INTERVAL_MULTIPLIER);
    }

    /**
     * Transformation for WildFly 8.0.0.Final
     */
    private static void buildTransformers2_0_0(ResourceTransformationDescriptionBuilder builder) {
        // nothing has changed from 8.1.0.Final
    }

    /**
     * Transformers for EAP 6.4 / AS7 7.5.0
     */
    private static void buildTransformers1_4_0(ResourceTransformationDescriptionBuilder builder) {
        ResourceTransformationDescriptionBuilder hornetqServer = builder.addChildResource(pathElement(HORNETQ_SERVER));
        renameAttribute(hornetqServer, CommonAttributes.STATISTICS_ENABLED, CommonAttributes.MESSAGE_COUNTER_ENABLED);

        ResourceTransformationDescriptionBuilder bridge = hornetqServer.addChildResource(BridgeDefinition.PATH);
        rejectDefinedAttributeWithDefaultValue(bridge, BridgeDefinition.RECONNECT_ATTEMPTS_ON_SAME_NODE, BridgeDefinition.INITIAL_CONNECT_ATTEMPTS);

        ResourceTransformationDescriptionBuilder clusterConnection = hornetqServer.addChildResource(ClusterConnectionDefinition.PATH);
        rejectDefinedAttributeWithDefaultValue(clusterConnection, ClusterConnectionDefinition.INITIAL_CONNECT_ATTEMPTS);


        ResourceTransformationDescriptionBuilder addressSetting = hornetqServer.addChildResource(AddressSettingDefinition.PATH);
        rejectDefinedAttributeWithDefaultValue(addressSetting, AddressSettingDefinition.EXPIRY_DELAY);


    }

    /**
     * Transformation for EAP 6.2.0 / AS7 7.3.0
     */
    private static void buildTransformers1_3_0(ResourceTransformationDescriptionBuilder builder) {
        ResourceTransformationDescriptionBuilder hornetqServer = builder.addChildResource(pathElement(HORNETQ_SERVER));
        rejectDefinedAttributeWithDefaultValue(hornetqServer, OVERRIDE_IN_VM_SECURITY);

        hornetqServer.rejectChildResource(HTTPAcceptorDefinition.PATH);
        hornetqServer.rejectChildResource(pathElement(CommonAttributes.HTTP_CONNECTOR));

        ResourceTransformationDescriptionBuilder addressSetting = hornetqServer.addChildResource(AddressSettingDefinition.PATH);
        rejectDefinedAttributeWithDefaultValue(addressSetting, SLOW_CONSUMER_CHECK_PERIOD, SLOW_CONSUMER_POLICY, SLOW_CONSUMER_THRESHOLD);

    }

    /**
     * Reject the attributes if they are defined or discard them if they are undefined or set to their default value.
     */
    private static void rejectDefinedAttributeWithDefaultValue(ResourceTransformationDescriptionBuilder builder, AttributeDefinition... attrs) {
        for (AttributeDefinition attr : attrs) {
            builder.getAttributeBuilder()
                    .setDiscard(new DiscardAttributeValueChecker(attr.getDefaultValue()), attr)
                    .addRejectCheck(DEFINED, attr);
        }
    }

    /**
     * Rename an attribute
     */
    private static void renameAttribute(ResourceTransformationDescriptionBuilder builder,
                                        AttributeDefinition attribute, AttributeDefinition alias) {
        builder.getAttributeBuilder().addRename(attribute, alias.getName());
    }

    private static class DoubleToBigDecimalConverter extends AttributeConverter.DefaultAttributeConverter {
        @Override
        protected void convertAttribute(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context) {
            double value = attributeValue.asDouble(RETRY_INTERVAL_MULTIPLIER.getDefaultValue().asDouble());
            attributeValue.set(new BigDecimal(value));
        }
    }

}
