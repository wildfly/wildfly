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

package org.wildfly.extension.undertow;

import io.undertow.UndertowOptions;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.io.OptionAttributeDefinition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
public class HttpListenerResourceDefinition extends ListenerResourceDefinition {
    protected static final HttpListenerResourceDefinition INSTANCE = new HttpListenerResourceDefinition();


    protected static final SimpleAttributeDefinition CERTIFICATE_FORWARDING = new SimpleAttributeDefinitionBuilder(Constants.CERTIFICATE_FORWARDING, ModelType.BOOLEAN)
            .setAllowNull(true)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .setDefaultValue(new ModelNode(false))
            .setAllowExpression(true)
            .build();

    protected static final SimpleAttributeDefinition PROXY_ADDRESS_FORWARDING = new SimpleAttributeDefinitionBuilder("proxy-address-forwarding", ModelType.BOOLEAN)
            .setAllowNull(true)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .setDefaultValue(new ModelNode(false))
            .setAllowExpression(true)
            .build();

    protected static final OptionAttributeDefinition ENABLE_HTTP2 = OptionAttributeDefinition.builder("enable-http2", UndertowOptions.ENABLE_HTTP2)
            .setAllowNull(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(false))
            .build();

    private HttpListenerResourceDefinition() {
        super(UndertowExtension.HTTP_LISTENER_PATH);
    }

    @Override
    protected ListenerAdd getAddHandler() {
        return new HttpListenerAdd(this);
    }

    public Collection<AttributeDefinition> getAttributes() {
        List<AttributeDefinition> attrs = new ArrayList<>(super.getAttributes());
        attrs.add(CERTIFICATE_FORWARDING);
        attrs.add(REDIRECT_SOCKET);
        attrs.add(PROXY_ADDRESS_FORWARDING);
        attrs.add(ENABLE_HTTP2);
        return attrs;
    }
}
