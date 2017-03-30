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

package org.wildfly.extension.undertow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import io.undertow.UndertowOptions;
import io.undertow.protocols.ajp.AjpClientRequestClientStreamSinkChannel;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.io.OptionAttributeDefinition;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
public class AjpListenerResourceDefinition extends ListenerResourceDefinition {
    protected static final AjpListenerResourceDefinition INSTANCE = new AjpListenerResourceDefinition();

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

    private AjpListenerResourceDefinition() {
        super(UndertowExtension.AJP_LISTENER_PATH);
    }

    @Override
    protected ListenerAdd getAddHandler() {
        return new AjpListenerAdd(this);
    }


    public Collection<AttributeDefinition> getAttributes() {
        List<AttributeDefinition> attrs = new ArrayList<>(super.getAttributes());
        attrs.add(SCHEME);
        attrs.add(REDIRECT_SOCKET);
        attrs.add(MAX_AJP_PACKET_SIZE);
        return attrs;
    }


}
