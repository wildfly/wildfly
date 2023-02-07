/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import io.undertow.UndertowOptions;
import io.undertow.protocols.http2.Http2Channel;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.io.OptionAttributeDefinition;

/**
 * Common base class for HTTP and HTTPS resource definitions.
 * @author Paul Ferraro
 */
abstract class AbstractHttpListenerResourceDefinition extends ListenerResourceDefinition {

    static final OptionAttributeDefinition HTTP2_ENABLE_PUSH = OptionAttributeDefinition.builder("http2-enable-push", UndertowOptions.HTTP2_SETTINGS_ENABLE_PUSH)
            .setRequired(false)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setAllowExpression(true)
            .setDefaultValue(ModelNode.TRUE)
            .build();

    static final OptionAttributeDefinition HTTP2_HEADER_TABLE_SIZE = OptionAttributeDefinition.builder("http2-header-table-size", UndertowOptions.HTTP2_SETTINGS_HEADER_TABLE_SIZE)
            .setRequired(false)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setAllowExpression(true)
            .setMeasurementUnit(MeasurementUnit.BYTES)
            .setDefaultValue(new ModelNode(UndertowOptions.HTTP2_SETTINGS_HEADER_TABLE_SIZE_DEFAULT))
            .setValidator(new IntRangeValidator(1))
            .build();

    static final OptionAttributeDefinition HTTP2_INITIAL_WINDOW_SIZE = OptionAttributeDefinition.builder("http2-initial-window-size", UndertowOptions.HTTP2_SETTINGS_INITIAL_WINDOW_SIZE)
            .setRequired(false)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setAllowExpression(true)
            .setMeasurementUnit(MeasurementUnit.BYTES)
            .setDefaultValue(new ModelNode(Http2Channel.DEFAULT_INITIAL_WINDOW_SIZE))
            .setValidator(new IntRangeValidator(1))
            .build();

    static final OptionAttributeDefinition HTTP2_MAX_CONCURRENT_STREAMS = OptionAttributeDefinition.builder("http2-max-concurrent-streams", UndertowOptions.HTTP2_SETTINGS_MAX_CONCURRENT_STREAMS)
            .setRequired(false)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setAllowExpression(true)
            .setValidator(new IntRangeValidator(1))
            .build();

    static final OptionAttributeDefinition HTTP2_MAX_HEADER_LIST_SIZE = OptionAttributeDefinition.builder("http2-max-header-list-size", UndertowOptions.HTTP2_SETTINGS_MAX_HEADER_LIST_SIZE)
            .setRequired(false)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setAllowExpression(true)
            .setMeasurementUnit(MeasurementUnit.BYTES)
            .setValidator(new IntRangeValidator(1))
            .build();

    static final OptionAttributeDefinition HTTP2_MAX_FRAME_SIZE = OptionAttributeDefinition.builder("http2-max-frame-size", UndertowOptions.HTTP2_SETTINGS_MAX_FRAME_SIZE)
            .setRequired(false)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setAllowExpression(true)
            .setMeasurementUnit(MeasurementUnit.BYTES)
            .setDefaultValue(new ModelNode(Http2Channel.DEFAULT_MAX_FRAME_SIZE))
            .setValidator(new IntRangeValidator(1))
            .build();

    static final SimpleAttributeDefinition CERTIFICATE_FORWARDING = new SimpleAttributeDefinitionBuilder(Constants.CERTIFICATE_FORWARDING, ModelType.BOOLEAN)
            .setRequired(false)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .setDefaultValue(ModelNode.FALSE)
            .setAllowExpression(true)
            .build();

    static final SimpleAttributeDefinition PROXY_ADDRESS_FORWARDING = new SimpleAttributeDefinitionBuilder("proxy-address-forwarding", ModelType.BOOLEAN)
            .setRequired(false)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .setDefaultValue(ModelNode.FALSE)
            .setAllowExpression(true)
            .build();

    static final OptionAttributeDefinition REQUIRE_HOST_HTTP11 = OptionAttributeDefinition.builder("require-host-http11", UndertowOptions.REQUIRE_HOST_HTTP11)
            .setRequired(false)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setAllowExpression(true)
            .setDefaultValue(ModelNode.FALSE)
            .build();

    static final SimpleAttributeDefinition PROXY_PROTOCOL = new SimpleAttributeDefinitionBuilder(Constants.PROXY_PROTOCOL, ModelType.BOOLEAN)
            .setDefaultValue(ModelNode.FALSE)
            .setRequired(false)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .setAllowExpression(true)
            .build();

    static final OptionAttributeDefinition ENABLE_HTTP2 = OptionAttributeDefinition.builder(Constants.ENABLE_HTTP2, UndertowOptions.ENABLE_HTTP2)
            .setRequired(false)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setAllowExpression(true)
            .setDefaultValue(ModelNode.FALSE)
            .build();

    static final Collection<AttributeDefinition> ATTRIBUTES = List.of(
            ENABLE_HTTP2,
            HTTP2_ENABLE_PUSH,
            HTTP2_HEADER_TABLE_SIZE,
            HTTP2_INITIAL_WINDOW_SIZE,
            HTTP2_MAX_CONCURRENT_STREAMS,
            HTTP2_MAX_HEADER_LIST_SIZE,
            HTTP2_MAX_FRAME_SIZE,
            CERTIFICATE_FORWARDING,
            PROXY_ADDRESS_FORWARDING,
            REQUIRE_HOST_HTTP11,
            PROXY_PROTOCOL);

    AbstractHttpListenerResourceDefinition(SimpleResourceDefinition.Parameters parameters, Function<Collection<AttributeDefinition>, AbstractAddStepHandler> addHandlerFactory) {
        super(parameters, addHandlerFactory, Map.of(WORKER, new HttpListenerWorkerAttributeWriteHandler(WORKER)));
    }
}
