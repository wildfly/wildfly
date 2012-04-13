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

package org.jboss.as.modcluster;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.descriptions.DefaultOperationDescriptionProvider;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link ResourceDefinition} implementation for the core mod-cluster configuration resource.
 * <p/>
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
class ModClusterConfigResourceDefinition extends SimpleResourceDefinition {

    static final SimpleAttributeDefinition ADVERTISE_SOCKET = SimpleAttributeDefinitionBuilder.create(CommonAttributes.ADVERTISE_SOCKET, ModelType.STRING, true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition CONNECTOR = SimpleAttributeDefinitionBuilder.create(CommonAttributes.CONNECTOR, ModelType.STRING, false)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    // TODO: Convert into xs:list of outbound socket binding names
    static final SimpleAttributeDefinition PROXY_LIST = SimpleAttributeDefinitionBuilder.create(CommonAttributes.PROXY_LIST, ModelType.STRING, true)
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition PROXY_URL = SimpleAttributeDefinitionBuilder.create(CommonAttributes.PROXY_URL, ModelType.STRING, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode("/"))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition ADVERTISE = SimpleAttributeDefinitionBuilder.create(CommonAttributes.ADVERTISE, ModelType.BOOLEAN, true)
            .setDefaultValue(new ModelNode(true))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition ADVERTISE_SECURITY_KEY = SimpleAttributeDefinitionBuilder.create(CommonAttributes.ADVERTISE_SECURITY_KEY, ModelType.STRING, true)
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    // TODO: Convert into an xs:list of host:context
    static final SimpleAttributeDefinition EXCLUDED_CONTEXTS = SimpleAttributeDefinitionBuilder.create(CommonAttributes.EXCLUDED_CONTEXTS, ModelType.STRING, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode("ROOT,invoker,jbossws,juddi,console"))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition AUTO_ENABLE_CONTEXTS = SimpleAttributeDefinitionBuilder.create(CommonAttributes.AUTO_ENABLE_CONTEXTS, ModelType.BOOLEAN, true)
            .setDefaultValue(new ModelNode(true))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition STOP_CONTEXT_TIMEOUT = SimpleAttributeDefinitionBuilder.create(CommonAttributes.STOP_CONTEXT_TIMEOUT, ModelType.INT, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(10))
            .setMeasurementUnit(MeasurementUnit.SECONDS)
            .setValidator(new IntRangeValidator(1, true, true))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition SOCKET_TIMEOUT = SimpleAttributeDefinitionBuilder.create(CommonAttributes.SOCKET_TIMEOUT, ModelType.INT, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(20))
            .setMeasurementUnit(MeasurementUnit.SECONDS)
            .setValidator(new IntRangeValidator(1, true, true))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition STICKY_SESSION = SimpleAttributeDefinitionBuilder.create(CommonAttributes.STICKY_SESSION, ModelType.BOOLEAN, true)
            .setDefaultValue(new ModelNode(true))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition STICKY_SESSION_REMOVE = SimpleAttributeDefinitionBuilder.create(CommonAttributes.STICKY_SESSION_REMOVE, ModelType.BOOLEAN, true)
            .setDefaultValue(new ModelNode(false))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition STICKY_SESSION_FORCE = SimpleAttributeDefinitionBuilder.create(CommonAttributes.STICKY_SESSION_FORCE, ModelType.BOOLEAN, true)
            .setDefaultValue(new ModelNode(false))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition WORKER_TIMEOUT = SimpleAttributeDefinitionBuilder.create(CommonAttributes.WORKER_TIMEOUT, ModelType.INT, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(-1))
            .setMeasurementUnit(MeasurementUnit.SECONDS)
            .setValidator(new IntRangeValidator(-1, true, true))
            .setCorrector(ZeroToNegativeOneParameterCorrector.INSTANCE)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition MAX_ATTEMPTS = SimpleAttributeDefinitionBuilder.create(CommonAttributes.MAX_ATTEMPTS, ModelType.INT, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(1))
            .setValidator(new IntRangeValidator(-1, true, true))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition FLUSH_PACKETS = SimpleAttributeDefinitionBuilder.create(CommonAttributes.FLUSH_PACKETS, ModelType.BOOLEAN, true)
            .setDefaultValue(new ModelNode(false))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition FLUSH_WAIT = SimpleAttributeDefinitionBuilder.create(CommonAttributes.FLUSH_WAIT, ModelType.INT, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(-1))
            .setMeasurementUnit(MeasurementUnit.SECONDS)
            .setValidator(new IntRangeValidator(-1, true, true))
            .setCorrector(ZeroToNegativeOneParameterCorrector.INSTANCE)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition PING = SimpleAttributeDefinitionBuilder.create(CommonAttributes.PING, ModelType.INT, true)
            .setDefaultValue(new ModelNode(10))
            .setMeasurementUnit(MeasurementUnit.SECONDS)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition SMAX = SimpleAttributeDefinitionBuilder.create(CommonAttributes.SMAX, ModelType.INT, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(-1))
            .setValidator(new IntRangeValidator(-1, true, true))
            .setCorrector(ZeroToNegativeOneParameterCorrector.INSTANCE)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition TTL = SimpleAttributeDefinitionBuilder.create(CommonAttributes.TTL, ModelType.INT, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(-1))
            .setMeasurementUnit(MeasurementUnit.SECONDS)
            .setValidator(new IntRangeValidator(-1, true, true))
            .setCorrector(ZeroToNegativeOneParameterCorrector.INSTANCE)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition NODE_TIMEOUT = SimpleAttributeDefinitionBuilder.create(CommonAttributes.NODE_TIMEOUT, ModelType.INT, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(-1))
            .setMeasurementUnit(MeasurementUnit.SECONDS)
            .setValidator(new IntRangeValidator(-1, true, true))
            .setCorrector(ZeroToNegativeOneParameterCorrector.INSTANCE)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition BALANCER = SimpleAttributeDefinitionBuilder.create(CommonAttributes.BALANCER, ModelType.STRING, true)
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition LOAD_BALANCING_GROUP = SimpleAttributeDefinitionBuilder.create(CommonAttributes.LOAD_BALANCING_GROUP, ModelType.STRING, true)
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .addAlternatives("domain")
            .build();

    static final SimpleAttributeDefinition SIMPLE_LOAD_PROVIDER = SimpleAttributeDefinitionBuilder.create(CommonAttributes.SIMPLE_LOAD_PROVIDER_FACTOR, ModelType.INT, true)
            .addFlag(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setXmlName(CommonAttributes.FACTOR)
                    //.setDefaultValue(new ModelNode(1))
            .setAllowExpression(true)
            .setValidator(new IntRangeValidator(1, true, true))
            .build();

    // order here controls the order of writing into xml, should follow xsd schema
    static final SimpleAttributeDefinition[] ATTRIBUTES = {
            ADVERTISE_SOCKET,
            PROXY_LIST,
            PROXY_URL,
            BALANCER,
            ADVERTISE,
            ADVERTISE_SECURITY_KEY,
            EXCLUDED_CONTEXTS,
            AUTO_ENABLE_CONTEXTS,
            STOP_CONTEXT_TIMEOUT,
            SOCKET_TIMEOUT,
            STICKY_SESSION,
            STICKY_SESSION_REMOVE,
            STICKY_SESSION_FORCE,
            WORKER_TIMEOUT,
            MAX_ATTEMPTS,
            FLUSH_PACKETS,
            FLUSH_WAIT,
            PING,
            SMAX,
            TTL,
            NODE_TIMEOUT,
            LOAD_BALANCING_GROUP, // was called "domain" in the 1.0 xsd
            CONNECTOR, // not in the 1.0 xsd
    };


    public static final Map<String, SimpleAttributeDefinition> ATTRIBUTES_BY_NAME;

    static {
        Map<String, SimpleAttributeDefinition> attrs = new HashMap<String, SimpleAttributeDefinition>();
        for (AttributeDefinition attr : ATTRIBUTES) {
            attrs.put(attr.getName(), (SimpleAttributeDefinition) attr);
        }
        ATTRIBUTES_BY_NAME = Collections.unmodifiableMap(attrs);
    }

    public ModClusterConfigResourceDefinition() {
        super(ModClusterExtension.CONFIGURATION_PATH,
                ModClusterExtension.getResourceDescriptionResolver(CommonAttributes.CONFIGURATION),
                ModClusterConfigAdd.INSTANCE,
                new ReloadRequiredRemoveStepHandler());
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition attr : ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attr, null, new ReloadRequiredWriteAttributeHandler(attr));
        }
        resourceRegistration.registerReadWriteAttribute(SIMPLE_LOAD_PROVIDER, null, new ReloadRequiredWriteAttributeHandler(SIMPLE_LOAD_PROVIDER));
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);

        final ResourceDescriptionResolver rootResolver = getResourceDescriptionResolver();

        // Metric for the  dynamic-load-provider
        EnumSet<OperationEntry.Flag> runtimeOnlyFlags = EnumSet.of(OperationEntry.Flag.RUNTIME_ONLY);

        final DescriptionProvider addMetric = new DefaultOperationDescriptionProvider(CommonAttributes.ADD_METRIC, rootResolver, LoadMetricDefinition.ATTRIBUTES);
        resourceRegistration.registerOperationHandler(CommonAttributes.ADD_METRIC, ModClusterAddMetric.INSTANCE, addMetric, false, runtimeOnlyFlags);

        final DescriptionProvider addCustomMetric = new DefaultOperationDescriptionProvider(CommonAttributes.ADD_CUSTOM_METRIC, rootResolver, CustomLoadMetricDefinition.ATTRIBUTES);
        resourceRegistration.registerOperationHandler(CommonAttributes.ADD_CUSTOM_METRIC, ModClusterAddCustomMetric.INSTANCE, addCustomMetric, false, runtimeOnlyFlags);

        final DescriptionProvider removeMetric = new DefaultOperationDescriptionProvider(CommonAttributes.REMOVE_METRIC, rootResolver, LoadMetricDefinition.TYPE);
        resourceRegistration.registerOperationHandler(CommonAttributes.REMOVE_METRIC, ModClusterRemoveMetric.INSTANCE, removeMetric, false, runtimeOnlyFlags);

        final DescriptionProvider removeCustomMetric = new DefaultOperationDescriptionProvider(CommonAttributes.REMOVE_CUSTOM_METRIC, rootResolver, CustomLoadMetricDefinition.CLASS);
        resourceRegistration.registerOperationHandler(CommonAttributes.REMOVE_CUSTOM_METRIC, ModClusterRemoveCustomMetric.INSTANCE, removeCustomMetric, false, runtimeOnlyFlags);
    }
}
