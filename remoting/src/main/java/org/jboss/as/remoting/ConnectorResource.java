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
import static org.jboss.as.remoting.CommonAttributes.FORWARD_SECRECY;
import static org.jboss.as.remoting.CommonAttributes.INCLUDE_MECHANISMS;
import static org.jboss.as.remoting.CommonAttributes.NO_ACTIVE;
import static org.jboss.as.remoting.CommonAttributes.NO_ANONYMOUS;
import static org.jboss.as.remoting.CommonAttributes.NO_DICTIONARY;
import static org.jboss.as.remoting.CommonAttributes.NO_PLAIN_TEXT;
import static org.jboss.as.remoting.CommonAttributes.PASS_CREDENTIALS;
import static org.jboss.as.remoting.CommonAttributes.POLICY;
import static org.jboss.as.remoting.CommonAttributes.PROPERTY;
import static org.jboss.as.remoting.CommonAttributes.QOP;
import static org.jboss.as.remoting.CommonAttributes.REUSE_SESSION;
import static org.jboss.as.remoting.CommonAttributes.SASL;
import static org.jboss.as.remoting.CommonAttributes.SASL_POLICY;
import static org.jboss.as.remoting.CommonAttributes.SECURITY;
import static org.jboss.as.remoting.CommonAttributes.SERVER_AUTH;
import static org.jboss.as.remoting.CommonAttributes.STRENGTH;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
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

    static final ConnectorResource INSTANCE = new ConnectorResource();

    static final SimpleAttributeDefinition AUTHENTICATION_PROVIDER = new NamedValueAttributeDefinition(CommonAttributes.AUTHENTICATION_PROVIDER, Attribute.NAME, null, ModelType.STRING, true);
    static final SimpleAttributeDefinition SOCKET_BINDING = new SimpleAttributeDefinition(CommonAttributes.SOCKET_BINDING, ModelType.STRING, false);

    private ConnectorResource() {
        super(PathElement.pathElement(CommonAttributes.CONNECTOR), RemotingExtension.getResourceDescriptionResolver(CONNECTOR),
                ConnectorAdd.INSTANCE, ConnectorRemove.INSTANCE);
    }

    protected static OptionMap getFullOptions(ModelNode fullModel) {
        OptionMap.Builder builder = OptionMap.builder();
        ModelNode properties = fullModel.get(PROPERTY);
        if (properties.isDefined() && properties.asInt() > 0) {
            addOptions(properties, builder);
        }
        if (fullModel.hasDefined(SECURITY)) {
            ModelNode security = fullModel.require(SECURITY);
            if (security.hasDefined(SASL)) {
                ModelNode sasl = security.require(SASL);
                addSasl(sasl, builder);
            }
        }

        return builder.getMap();
    }

    protected static OptionMap getOptions(ModelNode properties) {
        if (properties.isDefined() && properties.asInt() > 0) {
            OptionMap.Builder builder = OptionMap.builder();
            addOptions(properties, builder);
            return builder.getMap();
        } else {
            return OptionMap.EMPTY;
        }
    }

    private static void addSasl(ModelNode sasl, OptionMap.Builder builder) {
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
            builder.set(Options.SASL_SERVER_AUTH, sasl.get(SERVER_AUTH).asBoolean());
        }
        if (sasl.hasDefined(REUSE_SESSION)) {
            builder.set(Options.SASL_REUSE, sasl.get(REUSE_SESSION).asBoolean());
        }
        ModelNode saslPolicy;
        if (sasl.hasDefined(SASL_POLICY) && (saslPolicy = sasl.get(SASL_POLICY)).hasDefined(POLICY)) {
            ModelNode policy = saslPolicy.get(POLICY);
            if (policy.hasDefined(FORWARD_SECRECY)) {
                builder.set(Options.SASL_POLICY_FORWARD_SECRECY, policy.get(FORWARD_SECRECY).asBoolean());
            }
            if (policy.hasDefined(NO_ACTIVE)) {
                builder.set(Options.SASL_POLICY_NOACTIVE, policy.get(NO_ACTIVE).asBoolean());
            }
            if (policy.hasDefined(NO_ANONYMOUS)) {
                builder.set(Options.SASL_POLICY_NOANONYMOUS, policy.get(NO_ANONYMOUS).asBoolean());
            }
            if (policy.hasDefined(NO_DICTIONARY)) {
                builder.set(Options.SASL_POLICY_NODICTIONARY, policy.get(NO_DICTIONARY).asBoolean());
            }
            if (policy.hasDefined(NO_PLAIN_TEXT)) {
                builder.set(Options.SASL_POLICY_NOPLAINTEXT, policy.get(NO_PLAIN_TEXT).asBoolean());
            }
            if (policy.hasDefined(PASS_CREDENTIALS)) {
                builder.set(Options.SASL_POLICY_PASS_CREDENTIALS, policy.get(PASS_CREDENTIALS).asBoolean());
            }
        }

        if (sasl.hasDefined(PROPERTY)) {
            ModelNode property = sasl.get(PROPERTY);
            List<Property> props = property.asPropertyList();
            List<org.xnio.Property> converted = new ArrayList<org.xnio.Property>(props.size());
            for (Property current : props) {
                converted.add(org.xnio.Property.of(current.getName(), current.getValue().asString()));
            }
            builder.set(Options.SASL_PROPERTIES, Sequence.of(converted));
        }
    }

    private static void addOptions(ModelNode properties, OptionMap.Builder builder) {
        final ClassLoader loader = SecurityActions.getClassLoader(ConnectorResource.class);
        for (Property property : properties.asPropertyList()) {
            String name = property.getName();
            if (!name.contains(".")) {
                name = "org.xnio.Options." + name;
            }
            final Option option = Option.fromString(name, loader);
            builder.set(option, option.parseValue(property.getValue().get(CommonAttributes.VALUE).asString(), loader));
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
                SOCKET_BINDING);
        resourceRegistration.registerReadWriteAttribute(AUTHENTICATION_PROVIDER, null, writeHandler);
        resourceRegistration.registerReadWriteAttribute(SOCKET_BINDING, null, writeHandler);
    }
}
