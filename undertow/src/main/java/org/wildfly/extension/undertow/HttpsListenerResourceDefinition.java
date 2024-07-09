/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow;

import static org.wildfly.extension.undertow.Capabilities.REF_SSL_CONTEXT;
import static org.xnio.Options.SSL_CLIENT_AUTH_MODE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import io.undertow.UndertowOptions;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
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
public class HttpsListenerResourceDefinition extends AbstractHttpListenerResourceDefinition {
    static final PathElement PATH_ELEMENT = PathElement.pathElement(Constants.HTTPS_LISTENER);

    protected static final SimpleAttributeDefinition SSL_CONTEXT = new SimpleAttributeDefinitionBuilder(Constants.SSL_CONTEXT, ModelType.STRING, false)
            .setAlternatives(Constants.SECURITY_REALM, Constants.VERIFY_CLIENT, Constants.ENABLED_CIPHER_SUITES, Constants.ENABLED_PROTOCOLS, Constants.SSL_SESSION_CACHE_SIZE, Constants.SSL_SESSION_TIMEOUT)
            .setCapabilityReference(LISTENER_CAPABILITY, REF_SSL_CONTEXT)
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
            .setValidator(EnumValidator.create(SslClientAuthMode.class))
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

    protected static final OptionAttributeDefinition ENABLE_SPDY = OptionAttributeDefinition.builder(Constants.ENABLE_SPDY, UndertowOptions.ENABLE_SPDY)
            .setRequired(false)
            .setDeprecated(ModelVersion.create(3, 2))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setAllowExpression(true)
            .setDefaultValue(ModelNode.FALSE)
            .build();

    public static final OptionAttributeDefinition SSL_SESSION_CACHE_SIZE = OptionAttributeDefinition.builder(Constants.SSL_SESSION_CACHE_SIZE, Options.SSL_SERVER_SESSION_CACHE_SIZE)
            .setDeprecated(ModelVersion.create(4, 0, 0)).setRequired(false).setAllowExpression(true)
            .setAlternatives(Constants.SSL_CONTEXT).build();
    public static final OptionAttributeDefinition SSL_SESSION_TIMEOUT = OptionAttributeDefinition.builder(Constants.SSL_SESSION_TIMEOUT, Options.SSL_SERVER_SESSION_TIMEOUT)
            .setDeprecated(ModelVersion.create(4, 0, 0)).setMeasurementUnit(MeasurementUnit.SECONDS).setRequired(false).setAllowExpression(true).setAlternatives(Constants.SSL_CONTEXT).build();

    static final List<AttributeDefinition> ATTRIBUTES = List.of(SSL_CONTEXT, SECURITY_REALM, VERIFY_CLIENT, ENABLED_CIPHER_SUITES, ENABLED_PROTOCOLS, ENABLE_SPDY, SSL_SESSION_CACHE_SIZE, SSL_SESSION_TIMEOUT);

    HttpsListenerResourceDefinition() {
        super(new SimpleResourceDefinition.Parameters(PATH_ELEMENT, UndertowExtension.getResolver(Constants.LISTENER))
                .setCapabilities(HTTP_UPGRADE_REGISTRY_CAPABILITY), new HttpsListenerAdd());
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        Collection<AttributeDefinition> attributes = new ArrayList<>(ListenerResourceDefinition.ATTRIBUTES.size() + AbstractHttpListenerResourceDefinition.ATTRIBUTES.size() + ATTRIBUTES.size());
        attributes.addAll(ListenerResourceDefinition.ATTRIBUTES);
        attributes.addAll(AbstractHttpListenerResourceDefinition.ATTRIBUTES);
        attributes.addAll(ATTRIBUTES);
        return Collections.unmodifiableCollection(attributes);
    }
}
