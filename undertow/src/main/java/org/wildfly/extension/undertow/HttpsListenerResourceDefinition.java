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

import static org.xnio.Options.SSL_CLIENT_AUTH_MODE;

import java.util.Collection;
import java.util.LinkedList;

import io.undertow.UndertowOptions;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.io.OptionAttributeDefinition;
import org.xnio.Options;
import org.xnio.SslClientAuthMode;

/**
 * An extension to the {@see HttpListenerResourceDefinition} to allow a security-realm to be associated to obtain a pre-defined
 * SSLContext.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class HttpsListenerResourceDefinition extends ListenerResourceDefinition {
    protected static final HttpsListenerResourceDefinition INSTANCE = new HttpsListenerResourceDefinition();

    protected static final SimpleAttributeDefinition SECURITY_REALM = new SimpleAttributeDefinitionBuilder(Constants.SECURITY_REALM, ModelType.STRING)
            .setAllowNull(false)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setValidator(new StringLengthValidator(1))
            .build();
    protected static final OptionAttributeDefinition VERIFY_CLIENT = OptionAttributeDefinition.builder("verify-client", SSL_CLIENT_AUTH_MODE)
            .setAllowNull(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setAllowExpression(true)
            .setValidator(new EnumValidator<SslClientAuthMode>(SslClientAuthMode.class, true, true))
            .setDefaultValue(new ModelNode(SslClientAuthMode.NOT_REQUESTED.name()))
            .build();

    protected static final OptionAttributeDefinition ENABLED_CIPHER_SUITES = OptionAttributeDefinition.builder("enabled-cipher-suites", Options.SSL_ENABLED_CIPHER_SUITES)
            .setAllowNull(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setAllowExpression(true)
            .build();

    protected static final OptionAttributeDefinition ENABLED_PROTOCOLS = OptionAttributeDefinition.builder("enabled-protocols", Options.SSL_ENABLED_PROTOCOLS)
            .setAllowNull(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setAllowExpression(true)
            .build();

    protected static final OptionAttributeDefinition ENABLE_HTTP2 = OptionAttributeDefinition.builder("enable-http2", UndertowOptions.ENABLE_HTTP2)
            .setAllowNull(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(false))
            .build();

    protected static final OptionAttributeDefinition ENABLE_SPDY = OptionAttributeDefinition.builder("enable-spdy", UndertowOptions.ENABLE_SPDY)
            .setAllowNull(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(false))
            .build();

    private HttpsListenerResourceDefinition() {
        super(UndertowExtension.HTTPS_LISTENER_PATH);
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        Collection<AttributeDefinition> res = new LinkedList<>(super.getAttributes());
        res.add(SECURITY_REALM);
        res.add(VERIFY_CLIENT);
        res.add(ENABLED_CIPHER_SUITES);
        res.add(ENABLED_PROTOCOLS);
        res.add(ENABLE_HTTP2);
        res.add(ENABLE_SPDY);
        return res;
    }

    @Override
    protected ListenerAdd getAddHandler() {
        return new HttpsListenerAdd(this);
    }
}
