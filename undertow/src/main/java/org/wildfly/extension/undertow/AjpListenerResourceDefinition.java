/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import io.undertow.UndertowOptions;
import io.undertow.protocols.ajp.AjpClientRequestClientStreamSinkChannel;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.version.Stability;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.io.OptionAttributeDefinition;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
public class AjpListenerResourceDefinition extends ListenerResourceDefinition {
    static final PathElement PATH_ELEMENT = PathElement.pathElement(Constants.AJP_LISTENER);

    protected static final SimpleAttributeDefinition SCHEME = new SimpleAttributeDefinitionBuilder(Constants.SCHEME, ModelType.STRING)
            .setRequired(false)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setAllowExpression(true)
            .build();
    public static final OptionAttributeDefinition MAX_AJP_PACKET_SIZE = OptionAttributeDefinition
            .builder("max-ajp-packet-size", UndertowOptions.MAX_AJP_PACKET_SIZE)
            .setMeasurementUnit(MeasurementUnit.BYTES)
            .setRequired(false)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(AjpClientRequestClientStreamSinkChannel.DEFAULT_MAX_DATA_SIZE))
            .setValidator(new IntRangeValidator(1))
            .build();

    protected static final OptionAttributeDefinition ALLOWED_REQUEST_ATTRIBUTES_PATTERN = OptionAttributeDefinition
            .builder(Constants.ALLOWED_REQUEST_ATTRIBUTES_PATTERN, UndertowOptions.AJP_ALLOWED_REQUEST_ATTRIBUTES_PATTERN)
            .setRequired(false)
            .setAllowExpression(true)
            .setDefaultValue(null)
            .setStability(Stability.COMMUNITY)
            .build();

    static final List<AttributeDefinition> ATTRIBUTES = List.of(SCHEME, REDIRECT_SOCKET, MAX_AJP_PACKET_SIZE, ALLOWED_REQUEST_ATTRIBUTES_PATTERN);

    AjpListenerResourceDefinition() {
        super(new SimpleResourceDefinition.Parameters(PATH_ELEMENT, UndertowExtension.getResolver(Constants.LISTENER)), new AjpListenerAdd(), Map.of());
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        List<AttributeDefinition> attributes = new ArrayList<>(ListenerResourceDefinition.ATTRIBUTES.size() + ATTRIBUTES.size());
        attributes.addAll(ListenerResourceDefinition.ATTRIBUTES);
        attributes.addAll(ATTRIBUTES);
        return attributes;
    }
}
