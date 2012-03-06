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

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * {@link ResourceDefinition} implementation for the core mod-cluster configuration resource.
 *
 * TODO this is a minimal implementation for AS7-3933; finish it off with AS7-4050
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
class ModClusterConfigResourceDefinition extends SimpleResourceDefinition {

    static final SimpleAttributeDefinition ADVERTISE_SOCKET = SimpleAttributeDefinitionBuilder.create(CommonAttributes.ADVERTISE_SOCKET, ModelType.STRING, true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

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
            .setDefaultValue(new ModelNode(10))
            .setMeasurementUnit(MeasurementUnit.SECONDS)
            .setValidator(new IntRangeValidator(1, true, true))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition SOCKET_TIMEOUT = SimpleAttributeDefinitionBuilder.create(CommonAttributes.SOCKET_TIMEOUT, ModelType.INT, true)
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
            .setDefaultValue(new ModelNode(-1))
            .setMeasurementUnit(MeasurementUnit.SECONDS)
            .setValidator(new IntRangeValidator(-1, true, true))
            .setCorrector(ZeroToNegativeOneParameterCorrector.INSTANCE)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition MAX_ATTEMPTS = SimpleAttributeDefinitionBuilder.create(CommonAttributes.MAX_ATTEMPTS, ModelType.INT, true)
            .setDefaultValue(new ModelNode(1))
            .setValidator(new IntRangeValidator(-1, true, true))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition FLUSH_PACKETS = SimpleAttributeDefinitionBuilder.create(CommonAttributes.FLUSH_PACKETS, ModelType.BOOLEAN, true)
            .setDefaultValue(new ModelNode(false))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition FLUSH_WAIT = SimpleAttributeDefinitionBuilder.create(CommonAttributes.FLUSH_WAIT, ModelType.INT, true)
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
            .setDefaultValue(new ModelNode(-1))
            .setValidator(new IntRangeValidator(-1, true, true))
            .setCorrector(ZeroToNegativeOneParameterCorrector.INSTANCE)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition TTL = SimpleAttributeDefinitionBuilder.create(CommonAttributes.TTL, ModelType.INT, true)
            .setDefaultValue(new ModelNode(-1))
            .setMeasurementUnit(MeasurementUnit.SECONDS)
            .setValidator(new IntRangeValidator(-1, true, true))
            .setCorrector(ZeroToNegativeOneParameterCorrector.INSTANCE)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition NODE_TIMEOUT = SimpleAttributeDefinitionBuilder.create(CommonAttributes.NODE_TIMEOUT, ModelType.INT, true)
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

    // This is the loadBalancingGroup. TODO rename
    static final SimpleAttributeDefinition DOMAIN = SimpleAttributeDefinitionBuilder.create(CommonAttributes.DOMAIN, ModelType.STRING, true)
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final AttributeDefinition[] ATTRIBUTES = {
        ADVERTISE_SOCKET, PROXY_LIST, PROXY_URL, ADVERTISE, ADVERTISE_SECURITY_KEY, EXCLUDED_CONTEXTS, AUTO_ENABLE_CONTEXTS,
            STOP_CONTEXT_TIMEOUT, SOCKET_TIMEOUT, STICKY_SESSION, STICKY_SESSION_REMOVE, STICKY_SESSION_FORCE, WORKER_TIMEOUT,
            MAX_ATTEMPTS, FLUSH_PACKETS, FLUSH_WAIT, PING, SMAX, TTL, NODE_TIMEOUT, BALANCER, DOMAIN
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
        // TODO AS7-4050 Use a correct path and use a ResourceDescriptionResolver; register handlers
        // When doing AS7-4050 note the currently unused ModClusterConfigurationAdd class that should be used or removed
        super(ModClusterExtension.configurationPath, ModClusterSubsystemDescriptionProviders.CONFIGURATION);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition attr : ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attr, null, new ReloadRequiredWriteAttributeHandler(attr));
        }

        // Special Attributes.  TODO AS7-4050 properly handle these
        resourceRegistration.registerReadWriteAttribute(CommonAttributes.DYNAMIC_LOAD_PROVIDER, null, new WriteDynamicLoadProviderOperationHandler(), AttributeAccess.Storage.CONFIGURATION);
        resourceRegistration.registerReadWriteAttribute(CommonAttributes.SIMPLE_LOAD_PROVIDER, null, new WriteSimpleLoadProviderOperationHandler(), AttributeAccess.Storage.CONFIGURATION);
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);

        // Metric for the  dynamic-load-provider
        EnumSet<OperationEntry.Flag> runtimeOnlyFlags = EnumSet.of(OperationEntry.Flag.RUNTIME_ONLY);
        resourceRegistration.registerOperationHandler("add-metric", ModClusterAddMetric.INSTANCE, ModClusterAddMetric.INSTANCE, false, runtimeOnlyFlags);
        resourceRegistration.registerOperationHandler("add-custom-metric", ModClusterAddCustomMetric.INSTANCE, ModClusterAddCustomMetric.INSTANCE, false, runtimeOnlyFlags);
        resourceRegistration.registerOperationHandler("remove-metric", ModClusterRemoveMetric.INSTANCE, ModClusterRemoveMetric.INSTANCE, false, runtimeOnlyFlags);
        resourceRegistration.registerOperationHandler("remove-custom-metric", ModClusterRemoveCustomMetric.INSTANCE, ModClusterRemoveCustomMetric.INSTANCE, false, runtimeOnlyFlags);
    }


    public static class WriteDynamicLoadProviderOperationHandler implements OperationStepHandler {

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            ModelNode history = operation.get(CommonAttributes.HISTORY);
            ModelNode decay = operation.get(CommonAttributes.DECAY);

            // final ModelNode submodel = context.readModelForUpdate(PathAddress.EMPTY_ADDRESS);
            final ModelNode submodel = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS).getModel();
            final ModelNode currentValue = submodel.get(CommonAttributes.DYNAMIC_LOAD_PROVIDER).clone();
            if (!history.isDefined())
                history = currentValue.get(CommonAttributes.HISTORY);
            submodel.get(CommonAttributes.HISTORY).set(history);
            if (!decay.isDefined())
                decay = currentValue.get(CommonAttributes.DECAY);
            submodel.get(CommonAttributes.DECAY).set(decay);

            submodel.get(CommonAttributes.DYNAMIC_LOAD_PROVIDER).get(CommonAttributes.HISTORY).set(history);
            submodel.get(CommonAttributes.DYNAMIC_LOAD_PROVIDER).get(CommonAttributes.DECAY).set(decay);

            context.completeStep();
        }

    }

    public static class WriteSimpleLoadProviderOperationHandler implements OperationStepHandler {

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            ModelNode factor = operation.get(CommonAttributes.FACTOR);

            final ModelNode submodel = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS).getModel();
            final ModelNode currentValue = submodel.get(CommonAttributes.SIMPLE_LOAD_PROVIDER).clone();
            if (!factor.isDefined())
                factor = currentValue.get(CommonAttributes.HISTORY);
            submodel.get(CommonAttributes.FACTOR).set(factor);

            submodel.get(CommonAttributes.SIMPLE_LOAD_PROVIDER).get(CommonAttributes.FACTOR).set(factor);

            context.completeStep();
        }

    }
}
