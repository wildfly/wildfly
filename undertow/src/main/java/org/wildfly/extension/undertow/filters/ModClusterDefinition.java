/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.undertow.AbstractHandlerDefinition;
import org.wildfly.extension.undertow.Constants;
import org.wildfly.extension.undertow.UndertowService;

import io.undertow.predicate.Predicate;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.proxy.ProxyHandler;

/**
 * mod_cluster front end handler. This acts like a filter, but does not re-use a lot of the filter code as it
 * needs to inject various services.
 *
 * @author Stuart Douglas
 */
public class ModClusterDefinition extends AbstractHandlerDefinition {

    public static final AttributeDefinition MANAGEMENT_SOCKET_BINDING = new SimpleAttributeDefinitionBuilder(Constants.MANAGEMENT_SOCKET_BINDING, ModelType.STRING)
            .setAllowExpression(true)
            .setAllowNull(false)
            .build();

    public static final AttributeDefinition ADVERTISE_SOCKET_BINDING = new SimpleAttributeDefinitionBuilder(Constants.ADVERTISE_SOCKET_BINDING, ModelType.STRING)
            .setAllowExpression(true)
            .setAllowNull(false)
            .build();

    public static final AttributeDefinition SECURITY_KEY = new SimpleAttributeDefinitionBuilder(Constants.SECURITY_KEY, ModelType.STRING)
            .setAllowExpression(true)
            .setAllowNull(true)
            .build();

    public static final AttributeDefinition ADVERTISE_PROTOCOL = new SimpleAttributeDefinitionBuilder(Constants.ADVERTISE_PROTOCOL, ModelType.STRING)
            .setAllowExpression(true)
            .setAllowNull(true)
            .setDefaultValue(new ModelNode("http"))
            .build();

    public static final AttributeDefinition ADVERTISE_PATH = new SimpleAttributeDefinitionBuilder(Constants.ADVERTISE_PATH, ModelType.STRING)
            .setAllowExpression(true)
            .setAllowNull(true)
            .setDefaultValue(new ModelNode("/"))
            .build();

    public static final AttributeDefinition ADVERTISE_FREQUENCY = new SimpleAttributeDefinitionBuilder(Constants.ADVERTISE_FREQUENCY, ModelType.INT)
            .setAllowExpression(true)
            .setMeasurementUnit(MeasurementUnit.MILLISECONDS)
            .setAllowNull(true)
            .setDefaultValue(new ModelNode(10000))
            .build();

    public static final AttributeDefinition HEALTH_CHECK_INTERVAL = new SimpleAttributeDefinitionBuilder(Constants.HEALTH_CHECK_INTERVAL, ModelType.INT)
            .setAllowExpression(true)
            .setMeasurementUnit(MeasurementUnit.MILLISECONDS)
            .setAllowNull(true)
            .setDefaultValue(new ModelNode(10000))
            .build();

    public static final AttributeDefinition BROKEN_NODE_TIMEOUT = new SimpleAttributeDefinitionBuilder(Constants.BROKEN_NODE_TIMEOUT, ModelType.INT)
            .setAllowExpression(true)
            .setMeasurementUnit(MeasurementUnit.MILLISECONDS)
            .setAllowNull(true)
            .setDefaultValue(new ModelNode(60000)) //TODO: what is a good default?
            .build();

    public static final AttributeDefinition WORKER = new SimpleAttributeDefinitionBuilder(Constants.WORKER, ModelType.STRING)
            .setAllowExpression(true)
            .setAllowNull(true)
            .setDefaultValue(new ModelNode("default"))
            .build();


    public static final AttributeDefinition MAX_REQUEST_TIME = new SimpleAttributeDefinitionBuilder(Constants.MAX_REQUEST_TIME, ModelType.INT)
            .setAllowExpression(true)
            .setMeasurementUnit(MeasurementUnit.MILLISECONDS)
            .setAllowNull(true)
            .setDefaultValue(new ModelNode(-1))
            .build();


    public static final AttributeDefinition MANAGEMENT_ACCESS_PREDICATE = new SimpleAttributeDefinitionBuilder(Constants.MANAGEMENT_ACCESS_PREDICATE, ModelType.STRING)
            .setAllowExpression(true)
            .setAllowNull(true)
            .build();

    public static final AttributeDefinition CONNECTIONS_PER_THREAD = new SimpleAttributeDefinitionBuilder(Constants.CONNECTIONS_PER_THREAD, ModelType.INT)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(10))
            .build();


    public static final AttributeDefinition CACHED_CONNECTIONS_PER_THREAD = new SimpleAttributeDefinitionBuilder(Constants.CACHED_CONNECTIONS_PER_THREAD, ModelType.INT)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(5))
            .build();

    public static final AttributeDefinition CONNECTION_IDLE_TIMEOUT = new SimpleAttributeDefinitionBuilder(Constants.CONNECTION_IDLE_TIMEOUT, ModelType.INT)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setMeasurementUnit(MeasurementUnit.SECONDS)
            .setDefaultValue(new ModelNode(60))
            .build();

    public static final AttributeDefinition REQUEST_QUEUE_SIZE = new SimpleAttributeDefinitionBuilder(Constants.REQUEST_QUEUE_SIZE, ModelType.INT)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(10))
            .build();

    public static final Collection<AttributeDefinition> ATTRIBUTES = Collections.unmodifiableCollection(Arrays.asList(MANAGEMENT_SOCKET_BINDING, ADVERTISE_SOCKET_BINDING, SECURITY_KEY, ADVERTISE_PROTOCOL,
                ADVERTISE_PATH, ADVERTISE_FREQUENCY, HEALTH_CHECK_INTERVAL, BROKEN_NODE_TIMEOUT, WORKER, MAX_REQUEST_TIME, MANAGEMENT_ACCESS_PREDICATE,
            CONNECTIONS_PER_THREAD, CACHED_CONNECTIONS_PER_THREAD, CONNECTION_IDLE_TIMEOUT, REQUEST_QUEUE_SIZE));
    public static final ModClusterDefinition INSTANCE = new ModClusterDefinition();

    private ModClusterDefinition() {
        super(Constants.MOD_CLUSTER, new ModClusterAdd(), new ServiceRemoveStepHandler(UndertowService.FILTER, new ModClusterAdd()));
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return ATTRIBUTES;
    }

    @Override
    public Class<? extends HttpHandler> getHandlerClass() {
        return ProxyHandler.class;
    }

    @Override
    public HttpHandler createHttpHandler(Predicate predicate, ModelNode model, HttpHandler next) {
        throw new IllegalStateException(); //this is not used for mod_cluster, as it required injection and socket binding

    }

    static class ModClusterAdd extends AbstractAddStepHandler {

        @Override
        protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
            for (AttributeDefinition def : ATTRIBUTES) {
                def.validateAndSet(operation, model);
            }
        }

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
            final String name = address.getLastElement().getValue();
            ModClusterService.install(name, context.getServiceTarget(), model, context);
        }
    }
}
