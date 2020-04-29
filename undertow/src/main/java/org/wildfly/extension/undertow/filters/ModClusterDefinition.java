/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow.filters;

import static org.jboss.as.controller.PathElement.pathElement;
import static org.wildfly.extension.undertow.Capabilities.CAPABILITY_MOD_CLUSTER_FILTER;
import static org.wildfly.extension.undertow.Capabilities.REF_SSL_CONTEXT;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import io.undertow.UndertowOptions;
import io.undertow.predicate.Predicate;
import io.undertow.protocols.ajp.AjpClientRequestClientStreamSinkChannel;
import io.undertow.protocols.http2.Http2Channel;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.proxy.ProxyHandler;
import org.jboss.as.clustering.controller.AddStepHandler;
import org.jboss.as.clustering.controller.AddStepHandlerDescriptor;
import org.jboss.as.clustering.controller.RemoveStepHandler;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceRegistration;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.UnaryCapabilityNameResolver;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.io.OptionAttributeDefinition;
import org.wildfly.extension.undertow.AbstractHandlerDefinition;
import org.wildfly.extension.undertow.Capabilities;
import org.wildfly.extension.undertow.Constants;
import org.wildfly.extension.undertow.PredicateValidator;
import org.wildfly.extension.undertow.UndertowExtension;
import org.wildfly.extension.undertow.UndertowService;

/**
 * mod_cluster front-end handler. This acts like a filter, but does not re-use a lot of the filter code as it
 * needs to inject various services.
 *
 * @author Stuart Douglas
 * @author Radoslav Husar
 */
public class ModClusterDefinition extends AbstractHandlerDefinition {

    public static final PathElement PATH = pathElement(Constants.MOD_CLUSTER);
    public static final ModClusterDefinition INSTANCE = new ModClusterDefinition();

    enum Capability implements org.jboss.as.clustering.controller.Capability {
        MOD_CLUSTER_FILTER_CAPABILITY(CAPABILITY_MOD_CLUSTER_FILTER, FilterService.class),
        ;
        private final RuntimeCapability<Void> definition;

        Capability(String name, Class<?> serviceValueType) {
            this.definition = RuntimeCapability.Builder.of(name, true, serviceValueType).setDynamicNameMapper(UnaryCapabilityNameResolver.DEFAULT).build();
        }

        @Override
        public RuntimeCapability<Void> getDefinition() {
            return this.definition;
        }
    }

    public static final AttributeDefinition MANAGEMENT_SOCKET_BINDING = new SimpleAttributeDefinitionBuilder(Constants.MANAGEMENT_SOCKET_BINDING, ModelType.STRING)
            .setAllowExpression(true)
            .setRequired(true)
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.SOCKET_BINDING_REF)
            .setCapabilityReference(Capabilities.REF_SOCKET_BINDING)
            .setRestartAllServices()
            .build();

    public static final AttributeDefinition ADVERTISE_SOCKET_BINDING = new SimpleAttributeDefinitionBuilder(Constants.ADVERTISE_SOCKET_BINDING, ModelType.STRING)
            .setAllowExpression(true)
            .setRequired(false)
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.SOCKET_BINDING_REF)
            .setCapabilityReference(Capabilities.REF_SOCKET_BINDING)
            .setRestartAllServices()
            .build();

    public static final AttributeDefinition SECURITY_KEY = new SimpleAttributeDefinitionBuilder(Constants.SECURITY_KEY, ModelType.STRING)
            .setAllowExpression(true)
            .setRequired(false)
            .setRestartAllServices()
            .build();

    public static final AttributeDefinition ADVERTISE_PROTOCOL = new SimpleAttributeDefinitionBuilder(Constants.ADVERTISE_PROTOCOL, ModelType.STRING)
            .setAllowExpression(true)
            .setRequired(false)
            .setDefaultValue(new ModelNode("http"))
            .setRestartAllServices()
            .build();

    public static final AttributeDefinition ADVERTISE_PATH = new SimpleAttributeDefinitionBuilder(Constants.ADVERTISE_PATH, ModelType.STRING)
            .setAllowExpression(true)
            .setRequired(false)
            .setDefaultValue(new ModelNode("/"))
            .setRestartAllServices()
            .build();

    public static final AttributeDefinition ADVERTISE_FREQUENCY = new SimpleAttributeDefinitionBuilder(Constants.ADVERTISE_FREQUENCY, ModelType.INT)
            .setAllowExpression(true)
            .setMeasurementUnit(MeasurementUnit.MILLISECONDS)
            .setRequired(false)
            .setDefaultValue(new ModelNode(10000))
            .setRestartAllServices()
            .build();

    public static final AttributeDefinition FAILOVER_STRATEGY = new SimpleAttributeDefinitionBuilder(Constants.FAILOVER_STRATEGY, ModelType.STRING)
            .setRequired(false)
            .setValidator(new EnumValidator<>(FailoverStrategy.class, true, true))
            .setRestartAllServices()
            .setDefaultValue(new ModelNode(FailoverStrategy.LOAD_BALANCED.name()))
            .build();

    public static final AttributeDefinition HEALTH_CHECK_INTERVAL = new SimpleAttributeDefinitionBuilder(Constants.HEALTH_CHECK_INTERVAL, ModelType.INT)
            .setAllowExpression(true)
            .setMeasurementUnit(MeasurementUnit.MILLISECONDS)
            .setRequired(false)
            .setDefaultValue(new ModelNode(10000))
            .setRestartAllServices()
            .build();

    public static final AttributeDefinition BROKEN_NODE_TIMEOUT = new SimpleAttributeDefinitionBuilder(Constants.BROKEN_NODE_TIMEOUT, ModelType.INT)
            .setAllowExpression(true)
            .setMeasurementUnit(MeasurementUnit.MILLISECONDS)
            .setRequired(false)
            .setDefaultValue(new ModelNode(60000)) //TODO: what is a good default?
            .setRestartAllServices()
            .build();

    public static final AttributeDefinition WORKER = new SimpleAttributeDefinitionBuilder(Constants.WORKER, ModelType.STRING)
            .setAllowExpression(true)
            .setRequired(false)
            .setDefaultValue(new ModelNode("default"))
            .setCapabilityReference(Capabilities.REF_IO_WORKER)
            .setRestartAllServices()
            .build();

    public static final AttributeDefinition MAX_REQUEST_TIME = new SimpleAttributeDefinitionBuilder(Constants.MAX_REQUEST_TIME, ModelType.INT)
            .setAllowExpression(true)
            .setMeasurementUnit(MeasurementUnit.MILLISECONDS)
            .setRequired(false)
            .setDefaultValue(new ModelNode(-1))
            .setRestartAllServices()
            .build();


    public static final AttributeDefinition MANAGEMENT_ACCESS_PREDICATE = new SimpleAttributeDefinitionBuilder(Constants.MANAGEMENT_ACCESS_PREDICATE, ModelType.STRING)
            .setAllowExpression(true)
            .setRequired(false)
            .setRestartAllServices()
            .setValidator(PredicateValidator.INSTANCE)
            .build();

    public static final AttributeDefinition CONNECTIONS_PER_THREAD = new SimpleAttributeDefinitionBuilder(Constants.CONNECTIONS_PER_THREAD, ModelType.INT)
            .setRequired(false)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(40))
            .setRestartAllServices()
            .build();


    public static final AttributeDefinition CACHED_CONNECTIONS_PER_THREAD = new SimpleAttributeDefinitionBuilder(Constants.CACHED_CONNECTIONS_PER_THREAD, ModelType.INT)
            .setRequired(false)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(40))
            .setRestartAllServices()
            .build();

    public static final AttributeDefinition CONNECTION_IDLE_TIMEOUT = new SimpleAttributeDefinitionBuilder(Constants.CONNECTION_IDLE_TIMEOUT, ModelType.INT)
            .setRequired(false)
            .setAllowExpression(true)
            .setMeasurementUnit(MeasurementUnit.SECONDS)
            .setDefaultValue(new ModelNode(60))
            .setRestartAllServices()
            .build();

    public static final AttributeDefinition REQUEST_QUEUE_SIZE = new SimpleAttributeDefinitionBuilder(Constants.REQUEST_QUEUE_SIZE, ModelType.INT)
            .setRequired(false)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(1000))
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition SSL_CONTEXT = new SimpleAttributeDefinitionBuilder(Constants.SSL_CONTEXT, ModelType.STRING, true)
            .setAlternatives(Constants.SECURITY_REALM)
            .setCapabilityReference(REF_SSL_CONTEXT)
            .setRestartAllServices()
            .setValidator(new StringLengthValidator(1))
            .setAccessConstraints(SensitiveTargetAccessConstraintDefinition.SSL_REF)
            .build();

    public static final SimpleAttributeDefinition SECURITY_REALM = new SimpleAttributeDefinitionBuilder(Constants.SECURITY_REALM, ModelType.STRING)
            .setAlternatives(Constants.SSL_CONTEXT)
            .setRequired(false)
            .setRestartAllServices()
            .setValidator(new StringLengthValidator(1))
            .setAccessConstraints(SensitiveTargetAccessConstraintDefinition.SECURITY_REALM_REF)
            .setDeprecated(ModelVersion.create(4, 0, 0))
            .build();

    public static final SimpleAttributeDefinition USE_ALIAS = new SimpleAttributeDefinitionBuilder(Constants.USE_ALIAS, ModelType.BOOLEAN)
            .setRequired(false)
            .setDefaultValue(ModelNode.FALSE)
            .setRestartAllServices()
            .build();


    public static final SimpleAttributeDefinition ENABLE_HTTP2 = new SimpleAttributeDefinitionBuilder(Constants.ENABLE_HTTP2, ModelType.BOOLEAN)
            .setRequired(false)
            .setDefaultValue(ModelNode.FALSE)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition MAX_AJP_PACKET_SIZE = new SimpleAttributeDefinitionBuilder(Constants.MAX_AJP_PACKET_SIZE, ModelType.INT)
            .setRequired(false)
            .setRestartAllServices()
            .setMeasurementUnit(MeasurementUnit.BYTES)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(AjpClientRequestClientStreamSinkChannel.DEFAULT_MAX_DATA_SIZE))
            .setValidator(new IntRangeValidator(1))
            .build();


    public static final OptionAttributeDefinition HTTP2_ENABLE_PUSH = OptionAttributeDefinition.builder("http2-enable-push", UndertowOptions.HTTP2_SETTINGS_ENABLE_PUSH)
            .setRequired(false)
            .setRestartAllServices()
            .setAllowExpression(true)
            .setDefaultValue(ModelNode.TRUE)
            .build();

    public static final OptionAttributeDefinition HTTP2_HEADER_TABLE_SIZE = OptionAttributeDefinition.builder("http2-header-table-size", UndertowOptions.HTTP2_SETTINGS_HEADER_TABLE_SIZE)
            .setRequired(false)
            .setRestartAllServices()
            .setAllowExpression(true)
            .setMeasurementUnit(MeasurementUnit.BYTES)
            .setDefaultValue(new ModelNode(UndertowOptions.HTTP2_SETTINGS_HEADER_TABLE_SIZE_DEFAULT))
            .setValidator(new IntRangeValidator(1))
            .build();

    public static final OptionAttributeDefinition HTTP2_INITIAL_WINDOW_SIZE = OptionAttributeDefinition.builder("http2-initial-window-size", UndertowOptions.HTTP2_SETTINGS_INITIAL_WINDOW_SIZE)
            .setRequired(false)
            .setRestartAllServices()
            .setAllowExpression(true)
            .setMeasurementUnit(MeasurementUnit.BYTES)
            .setDefaultValue(new ModelNode(Http2Channel.DEFAULT_INITIAL_WINDOW_SIZE))
            .setValidator(new IntRangeValidator(1))
            .build();

    public static final OptionAttributeDefinition HTTP2_MAX_CONCURRENT_STREAMS = OptionAttributeDefinition.builder("http2-max-concurrent-streams", UndertowOptions.HTTP2_SETTINGS_MAX_CONCURRENT_STREAMS)
            .setRequired(false)
            .setRestartAllServices()
            .setAllowExpression(true)
            .setValidator(new IntRangeValidator(1))
            .build();

    public static final OptionAttributeDefinition HTTP2_MAX_FRAME_SIZE = OptionAttributeDefinition.builder("http2-max-frame-size", UndertowOptions.HTTP2_SETTINGS_MAX_FRAME_SIZE)
            .setRequired(false)
            .setRestartAllServices()
            .setAllowExpression(true)
            .setMeasurementUnit(MeasurementUnit.BYTES)
            .setDefaultValue(new ModelNode(Http2Channel.DEFAULT_MAX_FRAME_SIZE))
            .setValidator(new IntRangeValidator(1))
            .build();

    public static final OptionAttributeDefinition HTTP2_MAX_HEADER_LIST_SIZE = OptionAttributeDefinition.builder("http2-max-header-list-size", UndertowOptions.HTTP2_SETTINGS_MAX_HEADER_LIST_SIZE)
            .setRequired(false)
            .setRestartAllServices()
            .setAllowExpression(true)
            .setMeasurementUnit(MeasurementUnit.BYTES)
            .setValidator(new IntRangeValidator(1))
            .build();

    public static final AttributeDefinition MAX_RETRIES = new SimpleAttributeDefinitionBuilder(Constants.MAX_RETRIES, ModelType.INT)
            .setRequired(false)
            .setRestartAllServices()
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(1L))
            .build();

    public static final Collection<AttributeDefinition> ATTRIBUTES = Collections.unmodifiableCollection(Arrays.asList(MANAGEMENT_SOCKET_BINDING, ADVERTISE_SOCKET_BINDING, SECURITY_KEY, ADVERTISE_PROTOCOL,
            ADVERTISE_PATH, ADVERTISE_FREQUENCY, FAILOVER_STRATEGY, HEALTH_CHECK_INTERVAL, BROKEN_NODE_TIMEOUT, WORKER, MAX_REQUEST_TIME, MANAGEMENT_ACCESS_PREDICATE,
            CONNECTIONS_PER_THREAD, CACHED_CONNECTIONS_PER_THREAD, CONNECTION_IDLE_TIMEOUT, REQUEST_QUEUE_SIZE, SECURITY_REALM, SSL_CONTEXT, USE_ALIAS, ENABLE_HTTP2, MAX_AJP_PACKET_SIZE,
            HTTP2_MAX_HEADER_LIST_SIZE, HTTP2_MAX_FRAME_SIZE, HTTP2_MAX_CONCURRENT_STREAMS, HTTP2_INITIAL_WINDOW_SIZE, HTTP2_HEADER_TABLE_SIZE, HTTP2_ENABLE_PUSH, MAX_RETRIES));

    public ModClusterDefinition() {
        super(new SimpleResourceDefinition.Parameters(PATH, UndertowExtension.getResolver(Constants.HANDLER, Constants.MOD_CLUSTER)));
    }

    @Override
    public Class<? extends HttpHandler> getHandlerClass() {
        return ProxyHandler.class;
    }

    @Override
    public HttpHandler createHttpHandler(Predicate predicate, ModelNode model, HttpHandler next) {
        throw new IllegalStateException(); //this is not used for mod_cluster, as it required injection and socket binding
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver())
                .addAttributes(ATTRIBUTES)
                .addCapabilities(Capability.class)
                .addRequiredSingletonChildren(SingleAffinityResourceDefinition.PATH)
                ;
        ModClusterResourceServiceHandler handler = new ModClusterResourceServiceHandler();
        new ModClusterResourceRegistration(descriptor, handler).register(resourceRegistration);
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        new NoAffinityResourceDefinition().register(resourceRegistration);
        new SingleAffinityResourceDefinition().register(resourceRegistration);
        new RankedAffinityResourceDefinition().register(resourceRegistration);

        resourceRegistration.registerSubModel(ModClusterBalancerDefinition.INSTANCE);
    }

    static class ModClusterResourceRegistration extends ResourceRegistration {

        ModClusterResourceRegistration(AddStepHandlerDescriptor descriptor, ResourceServiceHandler handler) {
            super(descriptor, handler, new ModClusterAddStepHandler(descriptor, handler), new RemoveStepHandler(descriptor, handler));
        }
    }

    static class ModClusterResourceServiceHandler implements ResourceServiceHandler {

        @Override
        public void installServices(OperationContext context, ModelNode model) throws OperationFailedException {
            String name = context.getCurrentAddressValue();
            ModelNode recursiveModel = Resource.Tools.readModel(context.readResourceFromRoot(context.getCurrentAddress(), true));
            ModClusterService.install(name, context.getCapabilityServiceTarget(), recursiveModel, context);
        }

        @Override
        public void removeServices(OperationContext context, ModelNode model) {
            context.removeService(UndertowService.FILTER.append(Constants.MOD_CLUSTER));
        }
    }

    static class ModClusterAddStepHandler extends AddStepHandler {

        ModClusterAddStepHandler(AddStepHandlerDescriptor descriptor, ResourceServiceHandler handler) {
            super(descriptor, handler);
        }

        /**
         * Wraps a standard {@link Resource} implementation which can dynamically register runtime resources at
         * {@code ../mod-cluster=X/balancer=Y} address.
         */
        @Override
        protected Resource createResource(OperationContext context, ModelNode operation) {
            if (context.isDefaultRequiresRuntime()) {
                Resource delegate = Resource.Factory.create();
                Resource result = new ModClusterResource(delegate, context.getCurrentAddressValue());
                context.addResource(PathAddress.EMPTY_ADDRESS, result);
                return result;
            } else {
                return super.createResource(context, operation);
            }
        }

    }
}
