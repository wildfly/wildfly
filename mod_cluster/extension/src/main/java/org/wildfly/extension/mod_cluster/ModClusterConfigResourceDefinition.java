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

package org.wildfly.extension.mod_cluster;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeMarshaller;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.description.AttributeConverter;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.modcluster.config.impl.SessionDrainingStrategyEnum;

/**
 * {@link org.jboss.as.controller.ResourceDefinition} implementation for the core mod-cluster configuration resource.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 * @author Radoslav Husar
 */
class ModClusterConfigResourceDefinition extends SimpleResourceDefinition {

    private static final String MOD_CLUSTER_SSL_CONTEXT_CAPABILITY_NAME = "org.wildfly.mod_cluster.ssl-context";
    private static final RuntimeCapability<Void> MOD_CLUSTER_CAPABILITY = RuntimeCapability.Builder.of(MOD_CLUSTER_SSL_CONTEXT_CAPABILITY_NAME, false).build();

    static final String SSL_CONTEXT_CAPABILITY_NAME = "org.wildfly.security.ssl-context";

    static final PathElement PATH = PathElement.pathElement(CommonAttributes.MOD_CLUSTER_CONFIG, CommonAttributes.CONFIGURATION);

    static final SimpleAttributeDefinition ADVERTISE_SOCKET = SimpleAttributeDefinitionBuilder.create(CommonAttributes.ADVERTISE_SOCKET, ModelType.STRING, true)
            .setRestartAllServices()
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.SOCKET_BINDING_REF)
            .build();

    static final SimpleAttributeDefinition CONNECTOR = SimpleAttributeDefinitionBuilder.create(CommonAttributes.CONNECTOR, ModelType.STRING, false)
            .setRestartAllServices()
            .build();

    static final SimpleAttributeDefinition SESSION_DRAINING_STRATEGY = SimpleAttributeDefinitionBuilder.create(CommonAttributes.SESSION_DRAINING_STRATEGY, ModelType.STRING, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(SessionDrainingStrategyEnum.DEFAULT.name()))
            .setValidator(new EnumValidator<>(SessionDrainingStrategyEnum.class, true, true,
                    SessionDrainingStrategyEnum.ALWAYS,
                    SessionDrainingStrategyEnum.DEFAULT,
                    SessionDrainingStrategyEnum.NEVER))
            .setRestartAllServices()
            .build();

    static final StringListAttributeDefinition PROXIES = new StringListAttributeDefinition.Builder(CommonAttributes.PROXIES)
            // We don't allow expressions for model references!
            .setAllowExpression(false)
            .setRequired(false)
            .setAttributeMarshaller(AttributeMarshaller.STRING_LIST)
            .addAccessConstraint(ModClusterExtension.MOD_CLUSTER_PROXIES_DEF)
            .setRestartAllServices()
            .build();

    static final SimpleAttributeDefinition PROXY_LIST = SimpleAttributeDefinitionBuilder.create(CommonAttributes.PROXY_LIST, ModelType.STRING, true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .setValidator(new ProxyListValidator())
            .addAccessConstraint(ModClusterExtension.MOD_CLUSTER_PROXIES_DEF)
            .setDeprecated(ModClusterModel.VERSION_2_0_0.getVersion())
            .addAlternatives(PROXIES.getName())
            .build();

    static final SimpleAttributeDefinition PROXY_URL = SimpleAttributeDefinitionBuilder.create(CommonAttributes.PROXY_URL, ModelType.STRING, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode("/"))
            .setRestartAllServices()
            .addAccessConstraint(ModClusterExtension.MOD_CLUSTER_PROXIES_DEF)
            .build();

    static final SimpleAttributeDefinition ADVERTISE = SimpleAttributeDefinitionBuilder.create(CommonAttributes.ADVERTISE, ModelType.BOOLEAN, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(true))
            .setRestartAllServices()
            .build();

    static final SimpleAttributeDefinition ADVERTISE_SECURITY_KEY = SimpleAttributeDefinitionBuilder.create(CommonAttributes.ADVERTISE_SECURITY_KEY, ModelType.STRING, true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.CREDENTIAL)
            .addAccessConstraint(ModClusterExtension.MOD_CLUSTER_SECURITY_DEF)
            .build();

    static final SimpleAttributeDefinition STATUS_INTERVAL = SimpleAttributeDefinitionBuilder.create(CommonAttributes.STATUS_INTERVAL, ModelType.INT, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(10)) // This default value is used in the transformer definition, change with caution.
            .setMeasurementUnit(MeasurementUnit.SECONDS)
            .setValidator(new IntRangeValidator(1, true, true))
            .setRestartAllServices()
            .build();

    // TODO: WFLY-3583 Convert into an xs:list of host:context
    static final SimpleAttributeDefinition EXCLUDED_CONTEXTS = SimpleAttributeDefinitionBuilder.create(CommonAttributes.EXCLUDED_CONTEXTS, ModelType.STRING, true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    static final SimpleAttributeDefinition AUTO_ENABLE_CONTEXTS = SimpleAttributeDefinitionBuilder.create(CommonAttributes.AUTO_ENABLE_CONTEXTS, ModelType.BOOLEAN, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(true))
            .setRestartAllServices()
            .build();

    static final SimpleAttributeDefinition STOP_CONTEXT_TIMEOUT = SimpleAttributeDefinitionBuilder.create(CommonAttributes.STOP_CONTEXT_TIMEOUT, ModelType.INT, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(10))
            .setMeasurementUnit(MeasurementUnit.SECONDS)
            .setValidator(new IntRangeValidator(1, true, true))
            .setRestartAllServices()
            .build();

    static final SimpleAttributeDefinition SOCKET_TIMEOUT = SimpleAttributeDefinitionBuilder.create(CommonAttributes.SOCKET_TIMEOUT, ModelType.INT, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(20))
            .setMeasurementUnit(MeasurementUnit.SECONDS)
            .setValidator(new IntRangeValidator(1, true, true))
            .setRestartAllServices()
            .build();

    static final SimpleAttributeDefinition SSL_CONTEXT = new SimpleAttributeDefinitionBuilder(CommonAttributes.SSL_CONTEXT, ModelType.STRING, true)
            .setCapabilityReference(SSL_CONTEXT_CAPABILITY_NAME, MOD_CLUSTER_SSL_CONTEXT_CAPABILITY_NAME, false)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setValidator(new StringLengthValidator(1))
            .build();

    static final SimpleAttributeDefinition STICKY_SESSION = SimpleAttributeDefinitionBuilder.create(CommonAttributes.STICKY_SESSION, ModelType.BOOLEAN, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(true))
            .setRestartAllServices()
            .build();

    static final SimpleAttributeDefinition STICKY_SESSION_REMOVE = SimpleAttributeDefinitionBuilder.create(CommonAttributes.STICKY_SESSION_REMOVE, ModelType.BOOLEAN, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(false))
            .setRestartAllServices()
            .build();

    static final SimpleAttributeDefinition STICKY_SESSION_FORCE = SimpleAttributeDefinitionBuilder.create(CommonAttributes.STICKY_SESSION_FORCE, ModelType.BOOLEAN, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(false))
            .setRestartAllServices()
            .build();

    static final SimpleAttributeDefinition WORKER_TIMEOUT = SimpleAttributeDefinitionBuilder.create(CommonAttributes.WORKER_TIMEOUT, ModelType.INT, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(-1))
            .setMeasurementUnit(MeasurementUnit.SECONDS)
            .setValidator(new IntRangeValidator(-1, true, true))
            .setCorrector(ZeroToNegativeOneParameterCorrector.INSTANCE)
            .setRestartAllServices()
            .build();

    static final SimpleAttributeDefinition MAX_ATTEMPTS = SimpleAttributeDefinitionBuilder.create(CommonAttributes.MAX_ATTEMPTS, ModelType.INT, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(1))
            .setValidator(new IntRangeValidator(-1, true, true))
            .setRestartAllServices()
            .build();

    static final SimpleAttributeDefinition FLUSH_PACKETS = SimpleAttributeDefinitionBuilder.create(CommonAttributes.FLUSH_PACKETS, ModelType.BOOLEAN, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(false))
            .setRestartAllServices()
            .build();

    static final SimpleAttributeDefinition FLUSH_WAIT = SimpleAttributeDefinitionBuilder.create(CommonAttributes.FLUSH_WAIT, ModelType.INT, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(-1))
            .setMeasurementUnit(MeasurementUnit.SECONDS)
            .setValidator(new IntRangeValidator(-1, true, true))
            .setCorrector(ZeroToNegativeOneParameterCorrector.INSTANCE)
            .setRestartAllServices()
            .build();

    static final SimpleAttributeDefinition PING = SimpleAttributeDefinitionBuilder.create(CommonAttributes.PING, ModelType.INT, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(10))
            .setMeasurementUnit(MeasurementUnit.SECONDS)
            .setRestartAllServices()
            .build();

    static final SimpleAttributeDefinition SMAX = SimpleAttributeDefinitionBuilder.create(CommonAttributes.SMAX, ModelType.INT, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(-1))
            .setValidator(new IntRangeValidator(-1, true, true))
            .setCorrector(ZeroToNegativeOneParameterCorrector.INSTANCE)
            .setRestartAllServices()
            .build();

    static final SimpleAttributeDefinition TTL = SimpleAttributeDefinitionBuilder.create(CommonAttributes.TTL, ModelType.INT, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(-1))
            .setMeasurementUnit(MeasurementUnit.SECONDS)
            .setValidator(new IntRangeValidator(-1, true, true))
            .setCorrector(ZeroToNegativeOneParameterCorrector.INSTANCE)
            .setRestartAllServices()
            .build();

    static final SimpleAttributeDefinition NODE_TIMEOUT = SimpleAttributeDefinitionBuilder.create(CommonAttributes.NODE_TIMEOUT, ModelType.INT, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(-1))
            .setMeasurementUnit(MeasurementUnit.SECONDS)
            .setValidator(new IntRangeValidator(-1, true, true))
            .setCorrector(ZeroToNegativeOneParameterCorrector.INSTANCE)
            .setRestartAllServices()
            .build();

    static final SimpleAttributeDefinition BALANCER = SimpleAttributeDefinitionBuilder.create(CommonAttributes.BALANCER, ModelType.STRING, true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    static final SimpleAttributeDefinition LOAD_BALANCING_GROUP = SimpleAttributeDefinitionBuilder.create(CommonAttributes.LOAD_BALANCING_GROUP, ModelType.STRING, true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .addAlternatives(CommonAttributes.DOMAIN)
            .build();

    static final SimpleAttributeDefinition SIMPLE_LOAD_PROVIDER = SimpleAttributeDefinitionBuilder.create(CommonAttributes.SIMPLE_LOAD_PROVIDER_FACTOR, ModelType.INT, true)
            .setRestartAllServices()
            .setXmlName(CommonAttributes.FACTOR)
            .setAllowExpression(true)
            .setValidator(new IntRangeValidator(0, true, true))
            .build();

    // Order here controls the order of writing into XML, should follow XSD schema and default configuration order
    static final AttributeDefinition[] ATTRIBUTES = {
            ADVERTISE_SOCKET,
            PROXY_LIST,
            PROXIES,
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
            SESSION_DRAINING_STRATEGY, // not in the 1.1 xsd
            STATUS_INTERVAL, // since 2.0 xsd
            SSL_CONTEXT, // since 3.0 xsd
    };


    public static final Map<String, AttributeDefinition> ATTRIBUTES_BY_NAME;

    static {
        Map<String, AttributeDefinition> attributes = new HashMap<>();
        for (AttributeDefinition attr : ATTRIBUTES) {
            attributes.put(attr.getName(), attr);
        }
        ATTRIBUTES_BY_NAME = Collections.unmodifiableMap(attributes);
    }

    public static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder parent) {
        ResourceTransformationDescriptionBuilder builder = parent.addChildResource(PATH);

        if (ModClusterModel.VERSION_4_1_0.requiresTransformation(version)) {
            builder.getAttributeBuilder()
                    .setDiscard(DiscardAttributeChecker.UNDEFINED, SSL_CONTEXT)
                    .addRejectCheck(RejectAttributeChecker.DEFINED, SSL_CONTEXT)
                    .end();
        }

        if (ModClusterModel.VERSION_4_0_0.requiresTransformation(version)) {
            builder.getAttributeBuilder()
                    .setValueConverter(new AttributeConverter.DefaultAttributeConverter() {
                        @Override
                        protected void convertAttribute(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context) {
                            if (!attributeValue.isDefined()) {
                                // Workaround legacy slaves not accepting null/empty values
                                // JBAS014704: '' is an invalid value for parameter excluded-contexts. Values must have a minimum length of 1 characters
                                attributeValue.set(" ");
                            }
                        }
                    }, EXCLUDED_CONTEXTS)
                    .end();
        }

        if (ModClusterModel.VERSION_3_0_0.requiresTransformation(version)) {
            builder.getAttributeBuilder()
                    // Discard if using default value, reject if set to other than previously hard-coded default of 10 seconds
                    .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(STATUS_INTERVAL.getDefaultValue()), STATUS_INTERVAL)
                    .addRejectCheck(new RejectAttributeChecker.SimpleAcceptAttributeChecker(STATUS_INTERVAL.getDefaultValue()), STATUS_INTERVAL)
                    // Reject if using proxies, discard if undefined
                    .setDiscard(DiscardAttributeChecker.UNDEFINED, PROXIES)
                    .addRejectCheck(RejectAttributeChecker.DEFINED, PROXIES)
                    .end();
        }

        if (ModClusterModel.VERSION_1_5_0.requiresTransformation(version)) {
            builder.getAttributeBuilder()
                    .addRejectCheck(SessionDrainingStrategyChecker.INSTANCE, SESSION_DRAINING_STRATEGY)
                    .setDiscard(SessionDrainingStrategyChecker.INSTANCE, SESSION_DRAINING_STRATEGY)
                    .end();
        }

        DynamicLoadProviderDefinition.buildTransformation(version, builder);
        ModClusterSSLResourceDefinition.buildTransformation(version, builder);
    }

    public ModClusterConfigResourceDefinition() {
        super(new Parameters(PATH, ModClusterExtension.getResourceDescriptionResolver(CommonAttributes.CONFIGURATION))
                .setAddHandler(ModClusterConfigAdd.INSTANCE)
                .setRemoveHandler(new ReloadRequiredRemoveStepHandler())
                .setCapabilities(MOD_CLUSTER_CAPABILITY)
        );
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition attr : ATTRIBUTES) {
            if (attr.equals(PROXY_LIST) || attr.equals(PROXIES)) {
                resourceRegistration.registerReadWriteAttribute(attr, null, new ProxyConfigurationWriteAttributeHandler(attr));
            } else {
                resourceRegistration.registerReadWriteAttribute(attr, null, new ReloadRequiredWriteAttributeHandler(attr));
            }
        }
        resourceRegistration.registerReadWriteAttribute(SIMPLE_LOAD_PROVIDER, null, new ReloadRequiredWriteAttributeHandler(SIMPLE_LOAD_PROVIDER));
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);

        final ResourceDescriptionResolver rootResolver = getResourceDescriptionResolver();

        final OperationDefinition addMetricDef = new SimpleOperationDefinitionBuilder(CommonAttributes.ADD_METRIC, rootResolver)
                .setParameters(LoadMetricDefinition.ATTRIBUTES)
                .setRuntimeOnly()
                .build();
        final OperationDefinition addCustomDef = new SimpleOperationDefinitionBuilder(CommonAttributes.ADD_CUSTOM_METRIC, rootResolver)
                .setParameters(CustomLoadMetricDefinition.ATTRIBUTES)
                .setRuntimeOnly()
                .build();
        final OperationDefinition removeMetricDef = new SimpleOperationDefinitionBuilder(CommonAttributes.REMOVE_METRIC, rootResolver)
                .setParameters(LoadMetricDefinition.TYPE)
                .setRuntimeOnly()
                .build();
        final OperationDefinition removeCustomDef = new SimpleOperationDefinitionBuilder(CommonAttributes.REMOVE_CUSTOM_METRIC, rootResolver)
                .setParameters(CustomLoadMetricDefinition.CLASS)
                .setRuntimeOnly()
                .build();

        resourceRegistration.registerOperationHandler(addMetricDef, ModClusterAddMetric.INSTANCE);
        resourceRegistration.registerOperationHandler(addCustomDef, ModClusterAddCustomMetric.INSTANCE);
        resourceRegistration.registerOperationHandler(removeMetricDef, ModClusterRemoveMetric.INSTANCE);
        resourceRegistration.registerOperationHandler(removeCustomDef, ModClusterRemoveCustomMetric.INSTANCE);
    }
}
