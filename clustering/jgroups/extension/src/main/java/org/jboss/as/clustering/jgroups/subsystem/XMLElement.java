/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups.subsystem;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.wildfly.subsystem.resource.AttributeDefinitionProvider;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public enum XMLElement {
    // must be first
    UNKNOWN(""),

    AUTH_PROTOCOL("auth-protocol"),
    CIPHER_TOKEN("cipher-token"),
    CHANNEL(ChannelResourceDefinitionRegistrar.WILDCARD_PATH),
    CHANNELS("channels"),
    DEFAULT_THREAD_POOL("default-thread-pool"),
    DIGEST_TOKEN("digest-token"),
    ENCRYPT_PROTOCOL("encrypt-protocol"),
    FORK(ForkResourceDefinitionRegistrar.WILDCARD_PATH),
    INTERNAL_THREAD_POOL("internal-thread-pool"),
    JDBC_PROTOCOL("jdbc-protocol"),
    KEY_CREDENTIAL_REFERENCE(EncryptProtocolResourceDefinitionRegistrar.KEY_CREDENTIAL),
    OOB_THREAD_POOL("oob-thread-pool"),
    PLAIN_TOKEN("plain-token"),
    PROPERTY(ModelDescriptionConstants.PROPERTY),
    PROTOCOL(ProtocolResourceDefinitionRegistrar.WILDCARD_PATH),
    RELAY(RelayResourceDefinitionRegistrar.WILDCARD_PATH),
    REMOTE_SITE(RemoteSiteResourceDefinitionRegistrar.WILDCARD_PATH),
    SHARED_SECRET_CREDENTIAL_REFERENCE(AuthTokenResourceDefinitionRegistrar.SHARED_SECRET),
    SOCKET_PROTOCOL("socket-protocol"),
    SOCKET_DISCOVERY_PROTOCOL("socket-discovery-protocol"),
    STACK(StackResourceDefinitionRegistrar.WILDCARD_PATH),
    STACKS("stacks"),
    TIMER_THREAD_POOL("timer-thread-pool"),
    TRANSPORT(TransportResourceDefinitionRegistrar.WILDCARD_PATH),
    ;

    private final String name;

    XMLElement(PathElement path) {
        this.name = path.isWildcard() ? path.getKey() : path.getValue();
    }

    XMLElement(AttributeDefinitionProvider attribute) {
        this(attribute.get());
    }

    XMLElement(AttributeDefinition attribute) {
        this.name = attribute.getXmlName();
    }

    XMLElement(String name) {
        this.name = name;
    }

    /**
     * Get the local name of this element.
     *
     * @return the local name
     */
    public String getLocalName() {
        return this.name;
    }

    @Override
    public String toString() {
        return this.name;
    }

    private enum XMLElementFunction implements Function<ModelNode, XMLElement> {
        PROTOCOL(XMLElement.PROTOCOL),
        SOCKET_PROTOCOL(XMLElement.SOCKET_PROTOCOL),
        JDBC_PROTOCOL(XMLElement.JDBC_PROTOCOL),
        ENCRYPT_PROTOCOL(XMLElement.ENCRYPT_PROTOCOL),
        SOCKET_DISCOVERY_PROTOCOL(XMLElement.SOCKET_DISCOVERY_PROTOCOL),
        AUTH_PROTOCOL(XMLElement.AUTH_PROTOCOL),
        ;
        private final XMLElement element;

        XMLElementFunction(XMLElement element) {
            this.element = element;
        }

        @Override
        public XMLElement apply(ModelNode ignored) {
            return this.element;
        }
    }

    private static final Map<String, XMLElement> elements = new HashMap<>();
    private static final Map<String, Function<ModelNode, XMLElement>> protocols = new HashMap<>();
    private static final Map<String, XMLElement> tokens = new HashMap<>();

    static {
        for (XMLElement element : values()) {
            String name = element.getLocalName();
            if (name != null) {
                elements.put(name, element);
            }
        }

        Function<ModelNode, XMLElement> function = new Function<>() {
            @Override
            public XMLElement apply(ModelNode model) {
                // Use socket-protocol element only if optional socket-binding was defined
                return model.hasDefined(SocketProtocolResourceDefinitionRegistrar.SocketBindingAttribute.SERVER.getName()) ? XMLElement.SOCKET_PROTOCOL : XMLElement.PROTOCOL;
            }
        };
        for (ProtocolResourceDefinitionRegistrar.SocketProtocol protocol : EnumSet.allOf(ProtocolResourceDefinitionRegistrar.SocketProtocol.class)) {
            protocols.put(protocol.name(), function);
        }
        for (ProtocolResourceDefinitionRegistrar.MulticastProtocol protocol : EnumSet.allOf(ProtocolResourceDefinitionRegistrar.MulticastProtocol.class)) {
            protocols.put(protocol.name(), XMLElementFunction.SOCKET_PROTOCOL);
        }
        for (ProtocolResourceDefinitionRegistrar.JdbcProtocol protocol : EnumSet.allOf(ProtocolResourceDefinitionRegistrar.JdbcProtocol.class)) {
            protocols.put(protocol.name(), XMLElementFunction.JDBC_PROTOCOL);
        }
        for (ProtocolResourceDefinitionRegistrar.EncryptProtocol protocol : EnumSet.allOf(ProtocolResourceDefinitionRegistrar.EncryptProtocol.class)) {
            protocols.put(protocol.name(), XMLElementFunction.ENCRYPT_PROTOCOL);
        }
        for (ProtocolResourceDefinitionRegistrar.InitialHostsProtocol protocol : EnumSet.allOf(ProtocolResourceDefinitionRegistrar.InitialHostsProtocol.class)) {
            protocols.put(protocol.name(), XMLElementFunction.SOCKET_DISCOVERY_PROTOCOL);
        }
        for (ProtocolResourceDefinitionRegistrar.AuthProtocol protocol : EnumSet.allOf(ProtocolResourceDefinitionRegistrar.AuthProtocol.class)) {
            protocols.put(protocol.name(), XMLElementFunction.AUTH_PROTOCOL);
        }

        tokens.put(PlainAuthTokenResourceDefinitionRegistrar.PATH.getValue(), XMLElement.PLAIN_TOKEN);
        tokens.put(DigestAuthTokenResourceDefinitionRegistrar.PATH.getValue(), XMLElement.DIGEST_TOKEN);
        tokens.put(CipherAuthTokenResourceDefinitionRegistrar.PATH.getValue(), XMLElement.CIPHER_TOKEN);
    }

    public static XMLElement forName(String localName) {
        return elements.getOrDefault(localName, UNKNOWN);
    }

    public static XMLElement forProtocolName(Property protocol) {
        return protocols.getOrDefault(protocol.getName(), XMLElementFunction.PROTOCOL).apply(protocol.getValue());
    }

    public static XMLElement forAuthTokenName(String token) {
        XMLElement element = tokens.get(token);
        if (element == null) throw new IllegalArgumentException(token);
        return element;
    }
}
