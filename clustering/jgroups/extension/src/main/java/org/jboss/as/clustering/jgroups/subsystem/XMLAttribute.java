/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups.subsystem;

import java.util.HashMap;
import java.util.Map;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.wildfly.subsystem.resource.AttributeDefinitionProvider;

public enum XMLAttribute {
    // must be first
    UNKNOWN(""),

    ALGORITHM(DigestAuthTokenResourceDefinitionRegistrar.Attribute.ALGORITHM),
    CHANNEL(RemoteSiteResourceDefinitionRegistrar.CHANNEL_CONFIGURATION),
    CLIENT_SOCKET_BINDING(SocketProtocolResourceDefinitionRegistrar.SocketBindingAttribute.CLIENT),
    CLUSTER(ChannelResourceDefinitionRegistrar.CLUSTER),
    DATA_SOURCE(JDBCProtocolResourceDefinitionRegistrar.DATA_SOURCE),
    @Deprecated DEFAULT_EXECUTOR("default-executor"),
    @Deprecated DEFAULT_STACK("default-stack"),
    DEFAULT("default"),
    DIAGNOSTICS_SOCKET_BINDING(AbstractTransportResourceDefinitionRegistrar.SocketBindingAttribute.DIAGNOSTICS),
    KEEPALIVE_TIME(ThreadPoolResourceDefinitionRegistrar.DEFAULT.getKeepAlive()),
    KEY_ALIAS(EncryptProtocolResourceDefinitionRegistrar.KEY_ALIAS),
    KEY_STORE(EncryptProtocolResourceDefinitionRegistrar.KEY_STORE),
    MACHINE(AbstractTransportResourceDefinitionRegistrar.Attribute.MACHINE),
    MAX_THREADS(ThreadPoolResourceDefinitionRegistrar.DEFAULT.getMaxThreads()),
    MIN_THREADS(ThreadPoolResourceDefinitionRegistrar.DEFAULT.getMinThreads()),
    MODULE(ProtocolChildResourceDefinitionRegistrar.MODULE),
    NAME(ModelDescriptionConstants.NAME),
    @Deprecated OOB_EXECUTOR("oob-executor"),
    OUTBOUND_SOCKET_BINDINGS(SocketDiscoveryProtocolResourceDefinitionRegistrar.OUTBOUND_SOCKET_BINDINGS),
    QUEUE_LENGTH("queue-length"),
    RACK(AbstractTransportResourceDefinitionRegistrar.Attribute.RACK),
    @Deprecated SHARED("shared"),
    SITE(AbstractTransportResourceDefinitionRegistrar.Attribute.SITE),
    SOCKET_BINDING(MulticastProtocolResourceDefinitionRegistrar.SOCKET_BINDING),
    STACK(ChannelResourceDefinitionRegistrar.STACK),
    STATISTICS_ENABLED(ChannelResourceDefinitionRegistrar.STATISTICS_ENABLED),
    @Deprecated THREAD_FACTORY("thread-factory"),
    @Deprecated TIMER_EXECUTOR("timer-executor"),
    TYPE(ModelDescriptionConstants.TYPE),
    ;

    private final String name;

    XMLAttribute(AttributeDefinitionProvider attribute) {
        this(attribute.get());
    }

    XMLAttribute(AttributeDefinition attribute) {
        this(attribute.getXmlName());
    }

    XMLAttribute(String name) {
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

    private static final Map<String, XMLAttribute> attributes = new HashMap<>();

    static {
        for (XMLAttribute attribute : values()) {
            String name = attribute.getLocalName();
            if (name != null) {
                attributes.put(name, attribute);
            }
        }
    }

    public static XMLAttribute forName(String localName) {
        XMLAttribute attribute = attributes.get(localName);
        return (attribute != null) ? attribute : UNKNOWN;
    }
}
