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

import static org.wildfly.extension.undertow.Capabilities.REF_SSL_CONTEXT;
import static org.xnio.Options.SSL_CLIENT_AUTH_MODE;

import java.util.Collection;
import java.util.LinkedList;

import io.undertow.UndertowOptions;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
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

    protected static final SimpleAttributeDefinition SSL_CONTEXT = new SimpleAttributeDefinitionBuilder(Constants.SSL_CONTEXT, ModelType.STRING, false)
            .setAlternatives(Constants.SECURITY_REALM, Constants.VERIFY_CLIENT, Constants.ENABLED_CIPHER_SUITES, Constants.ENABLED_PROTOCOLS, Constants.SSL_SESSION_CACHE_SIZE, Constants.SSL_SESSION_TIMEOUT)
            .setCapabilityReference(REF_SSL_CONTEXT)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setValidator(new StringLengthValidator(1))
            .setAccessConstraints(SensitiveTargetAccessConstraintDefinition.SSL_REF)
            .build();

    protected static final SimpleAttributeDefinition SECURITY_REALM = new SimpleAttributeDefinitionBuilder(Constants.SECURITY_REALM, ModelType.STRING, false)
            .setAlternatives(Constants.SSL_CONTEXT)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setValidator(new StringLengthValidator(1))
            .setDeprecated(ModelVersion.create(4, 0, 0))
            .setAccessConstraints(SensitiveTargetAccessConstraintDefinition.SECURITY_REALM_REF)
            .build();

    protected static final OptionAttributeDefinition VERIFY_CLIENT = OptionAttributeDefinition.builder(Constants.VERIFY_CLIENT, SSL_CLIENT_AUTH_MODE)
            .setRequired(false)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setAllowExpression(true)
            .setValidator(new EnumValidator<>(SslClientAuthMode.class, true, true))
            .setDefaultValue(new ModelNode(SslClientAuthMode.NOT_REQUESTED.name()))
            .setDeprecated(ModelVersion.create(4, 0, 0))
            .setAlternatives(Constants.SSL_CONTEXT)
            .build();

    protected static final OptionAttributeDefinition ENABLED_CIPHER_SUITES = OptionAttributeDefinition.builder(Constants.ENABLED_CIPHER_SUITES, Options.SSL_ENABLED_CIPHER_SUITES)
            .setRequired(false)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setAllowExpression(true)
            .setDeprecated(ModelVersion.create(4, 0, 0))
            .setAlternatives(Constants.SSL_CONTEXT)
            .build();

    protected static final OptionAttributeDefinition ENABLED_PROTOCOLS = OptionAttributeDefinition.builder(Constants.ENABLED_PROTOCOLS, Options.SSL_ENABLED_PROTOCOLS)
            .setRequired(false)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setAllowExpression(true)
            .setDeprecated(ModelVersion.create(4, 0, 0))
            .setAlternatives(Constants.SSL_CONTEXT)
            .build();

    protected static final OptionAttributeDefinition ENABLE_HTTP2 = OptionAttributeDefinition.builder(Constants.ENABLE_HTTP2, UndertowOptions.ENABLE_HTTP2)
            .setRequired(false)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(false))
            .build();

    protected static final OptionAttributeDefinition ENABLE_SPDY = OptionAttributeDefinition.builder(Constants.ENABLE_SPDY, UndertowOptions.ENABLE_SPDY)
            .setRequired(false)
            .setDeprecated(ModelVersion.create(3, 2))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(false))
            .build();

    public static final OptionAttributeDefinition SSL_SESSION_CACHE_SIZE = OptionAttributeDefinition.builder(Constants.SSL_SESSION_CACHE_SIZE, Options.SSL_SERVER_SESSION_CACHE_SIZE)
            .setDeprecated(ModelVersion.create(4, 0, 0)).setRequired(false).setAllowExpression(true)
            .setAlternatives(Constants.SSL_CONTEXT).build();
    public static final OptionAttributeDefinition SSL_SESSION_TIMEOUT = OptionAttributeDefinition.builder(Constants.SSL_SESSION_TIMEOUT, Options.SSL_SERVER_SESSION_TIMEOUT)
            .setDeprecated(ModelVersion.create(4, 0, 0)).setMeasurementUnit(MeasurementUnit.SECONDS).setRequired(false).setAllowExpression(true).setAlternatives(Constants.SSL_CONTEXT).build();

    private HttpsListenerResourceDefinition() {
        super(UndertowExtension.HTTPS_LISTENER_PATH);
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        Collection<AttributeDefinition> res = new LinkedList<>(super.getAttributes());
        res.add(SSL_CONTEXT);
        res.add(SECURITY_REALM);
        res.add(VERIFY_CLIENT);
        res.add(ENABLED_CIPHER_SUITES);
        res.add(ENABLED_PROTOCOLS);
        res.add(ENABLE_HTTP2);
        res.add(ENABLE_SPDY);
        res.add(SSL_SESSION_CACHE_SIZE);
        res.add(SSL_SESSION_TIMEOUT);
        res.add(HttpListenerResourceDefinition.HTTP2_ENABLE_PUSH);
        res.add(HttpListenerResourceDefinition.HTTP2_HEADER_TABLE_SIZE);
        res.add(HttpListenerResourceDefinition.HTTP2_INITIAL_WINDOW_SIZE);
        res.add(HttpListenerResourceDefinition.HTTP2_MAX_CONCURRENT_STREAMS);
        res.add(HttpListenerResourceDefinition.HTTP2_MAX_HEADER_LIST_SIZE);
        res.add(HttpListenerResourceDefinition.HTTP2_MAX_FRAME_SIZE);
        res.add(HttpListenerResourceDefinition.CERTIFICATE_FORWARDING);
        res.add(HttpListenerResourceDefinition.PROXY_ADDRESS_FORWARDING);
        res.add(HttpListenerResourceDefinition.REQUIRE_HOST_HTTP11);
        res.add(HttpListenerResourceDefinition.PROXY_PROTOCOL);
        return res;
    }

    @Override
    protected ListenerAdd getAddHandler() {
        return new HttpsListenerAdd(this);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        //register as normal
        super.registerAttributes(resourceRegistration);
        //override
        resourceRegistration.unregisterAttribute(WORKER.getName());
        resourceRegistration.registerReadWriteAttribute(WORKER, null, new HttpListenerWorkerAttributeWriteHandler(WORKER));
    }
}
