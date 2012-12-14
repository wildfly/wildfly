/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.remoting;

import static org.jboss.as.remoting.CommonAttributes.CONNECTOR;
import static org.jboss.as.remoting.CommonAttributes.INCLUDE_MECHANISMS;
import static org.jboss.as.remoting.CommonAttributes.POLICY;
import static org.jboss.as.remoting.CommonAttributes.PROPERTY;
import static org.jboss.as.remoting.CommonAttributes.QOP;
import static org.jboss.as.remoting.CommonAttributes.REUSE_SESSION;
import static org.jboss.as.remoting.CommonAttributes.SASL;
import static org.jboss.as.remoting.CommonAttributes.SASL_POLICY;
import static org.jboss.as.remoting.CommonAttributes.SECURITY;
import static org.jboss.as.remoting.CommonAttributes.SERVER_AUTH;
import static org.jboss.as.remoting.CommonAttributes.STRENGTH;
import static org.jboss.as.remoting.SaslPolicyResource.FORWARD_SECRECY;
import static org.jboss.as.remoting.SaslPolicyResource.NO_ACTIVE;
import static org.jboss.as.remoting.SaslPolicyResource.NO_ANONYMOUS;
import static org.jboss.as.remoting.SaslPolicyResource.NO_DICTIONARY;
import static org.jboss.as.remoting.SaslPolicyResource.NO_PLAIN_TEXT;
import static org.jboss.as.remoting.SaslPolicyResource.PASS_CREDENTIALS;
import static org.jboss.as.remoting.SaslResource.REUSE_SESSION_ATTRIBUTE;
import static org.jboss.as.remoting.SaslResource.SERVER_AUTH_ATTRIBUTE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.xnio.Option;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Sequence;
import org.xnio.sasl.SaslQop;
import org.xnio.sasl.SaslStrength;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class ConnectorResource extends SimpleResourceDefinition {

    static final PathElement PATH = PathElement.pathElement(CommonAttributes.CONNECTOR);

    static final ConnectorResource INSTANCE = new ConnectorResource();

    //FIXME is this attribute still used?
    static final SimpleAttributeDefinition AUTHENTICATION_PROVIDER = new SimpleAttributeDefinitionBuilder(CommonAttributes.AUTHENTICATION_PROVIDER, ModelType.STRING)
            .setDefaultValue(null)
            .setAllowNull(true)
            .setAttributeMarshaller(new WrappedAttributeMarshaller(Attribute.NAME))
            .build();

    static final SimpleAttributeDefinition SOCKET_BINDING = new SimpleAttributeDefinition(CommonAttributes.SOCKET_BINDING, ModelType.STRING, false);
    static final SimpleAttributeDefinition SECURITY_REALM = new SimpleAttributeDefinitionBuilder(
            CommonAttributes.SECURITY_REALM, ModelType.STRING, true).setValidator(
            new StringLengthValidator(1, Integer.MAX_VALUE, true, false)).build();

    private ConnectorResource() {
        super(PATH, RemotingExtension.getResourceDescriptionResolver(CONNECTOR),
                ConnectorAdd.INSTANCE, ConnectorRemove.INSTANCE);
    }

    protected static OptionMap getFullOptions(OperationContext context, ModelNode fullModel) throws OperationFailedException {
        OptionMap.Builder builder = OptionMap.builder();
        ModelNode properties = fullModel.get(PROPERTY);
        if (properties.isDefined() && properties.asInt() > 0) {
            addOptions(context, properties, builder);
        }
        if (fullModel.hasDefined(SECURITY)) {
            ModelNode security = fullModel.require(SECURITY);
            if (security.hasDefined(SASL)) {
                ModelNode sasl = security.require(SASL);
                addSasl(context, sasl, builder);
            }
        }
        return builder.getMap();
    }

    protected static OptionMap getOptions(OperationContext context, ModelNode properties) throws OperationFailedException {
        if (properties.isDefined() && properties.asInt() > 0) {
            OptionMap.Builder builder = OptionMap.builder();
            addOptions(context, properties, builder);
            return builder.getMap();
        } else {
            return OptionMap.EMPTY;
        }
    }

    private static void addSasl(OperationContext context, ModelNode sasl, OptionMap.Builder builder) throws OperationFailedException {
        if (sasl.hasDefined(INCLUDE_MECHANISMS)) {
            builder.set(Options.SASL_MECHANISMS, Sequence.of(asStringSet(sasl.get(INCLUDE_MECHANISMS))));
        }
        if (sasl.hasDefined(QOP)) {
            builder.set(Options.SASL_QOP, Sequence.of(asQopSet(sasl.get(QOP))));
        }
        if (sasl.hasDefined(STRENGTH)) {
            ModelNode strength = sasl.get(STRENGTH);
            for (ModelNode current : strength.asList()) {
                builder.set(Options.SASL_STRENGTH, strengthFromString(current.asString()));
            }
        }
        if (sasl.hasDefined(SERVER_AUTH)) {
            builder.set(Options.SASL_SERVER_AUTH, SERVER_AUTH_ATTRIBUTE.resolveModelAttribute(context, sasl).asBoolean());
        }
        if (sasl.hasDefined(REUSE_SESSION)) {
            builder.set(Options.SASL_REUSE, REUSE_SESSION_ATTRIBUTE.resolveModelAttribute(context, sasl).asBoolean());
        }
        ModelNode saslPolicy;
        if (sasl.hasDefined(SASL_POLICY) && (saslPolicy = sasl.get(SASL_POLICY)).hasDefined(POLICY)) {
            ModelNode policy = saslPolicy.get(POLICY);
            if (policy.hasDefined(FORWARD_SECRECY.getName())) {
                builder.set(Options.SASL_POLICY_FORWARD_SECRECY, FORWARD_SECRECY.resolveModelAttribute(context, policy).asBoolean());
            }
            if (policy.hasDefined(NO_ACTIVE.getName())) {
                builder.set(Options.SASL_POLICY_NOACTIVE, NO_ACTIVE.resolveModelAttribute(context, policy).asBoolean());
            }
            if (policy.hasDefined(NO_ANONYMOUS.getName())) {
                builder.set(Options.SASL_POLICY_NOANONYMOUS, NO_ANONYMOUS.resolveModelAttribute(context, policy).asBoolean());
            }
            if (policy.hasDefined(NO_DICTIONARY.getName())) {
                builder.set(Options.SASL_POLICY_NODICTIONARY, NO_DICTIONARY.resolveModelAttribute(context, policy).asBoolean());
            }
            if (policy.hasDefined(NO_PLAIN_TEXT.getName())) {
                builder.set(Options.SASL_POLICY_NOPLAINTEXT, NO_PLAIN_TEXT.resolveModelAttribute(context, policy).asBoolean());
            }
            if (policy.hasDefined(PASS_CREDENTIALS.getName())) {
                builder.set(Options.SASL_POLICY_PASS_CREDENTIALS, PASS_CREDENTIALS.resolveModelAttribute(context, policy).asBoolean());
            }
        }

        if (sasl.hasDefined(PROPERTY)) {
            ModelNode property = sasl.get(PROPERTY);
            List<Property> props = property.asPropertyList();
            List<org.xnio.Property> converted = new ArrayList<org.xnio.Property>(props.size());
            for (Property current : props) {
                converted.add(org.xnio.Property.of(current.getName(), PropertyResource.VALUE.resolveModelAttribute(context, current.getValue()).asString()));
            }
            builder.set(Options.SASL_PROPERTIES, Sequence.of(converted));
        }
    }

    private static void addOptions(OperationContext context, ModelNode properties, OptionMap.Builder builder) throws OperationFailedException {
        final ClassLoader loader = SecurityActions.getClassLoader(ConnectorResource.class);
        for (Property property : properties.asPropertyList()) {
            String name = property.getName();
            if (!name.contains(".")) {
                name = "org.xnio.Options." + name;
            }
            final Option option = Option.fromString(name, loader);
            String value = PropertyResource.VALUE.resolveModelAttribute(context, property.getValue()).asString();
            builder.set(option, option.parseValue(value, loader));
        }
    }

    private static Collection<String> asStringSet(final ModelNode node) {
        final Set<String> set = new HashSet<String>();
        for (final ModelNode element : node.asList()) {
            set.add(element.asString());
        }
        return set;
    }

    private static Collection<SaslQop> asQopSet(final ModelNode node) {
        final Set<SaslQop> set = new HashSet<SaslQop>();
        for (final ModelNode element : node.asList()) {
            set.add(SaslQop.fromString(element.asString()));
        }
        return set;
    }

    private static SaslStrength strengthFromString(String name) {
        if ("low".equals(name)) {
            return SaslStrength.LOW;
        } else if ("medium".equals(name)) {
            return SaslStrength.MEDIUM;
        } else if ("high".equals(name)) {
            return SaslStrength.HIGH;
        } else {
            throw RemotingMessages.MESSAGES.illegalStrength(name);
        }
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        final OperationStepHandler writeHandler = new ReloadRequiredWriteAttributeHandler(AUTHENTICATION_PROVIDER,
                SOCKET_BINDING, SECURITY_REALM);
        resourceRegistration.registerReadWriteAttribute(AUTHENTICATION_PROVIDER, null, writeHandler);
        resourceRegistration.registerReadWriteAttribute(SOCKET_BINDING, null, writeHandler);
        resourceRegistration.registerReadWriteAttribute(SECURITY_REALM, null, writeHandler);
    }
}
