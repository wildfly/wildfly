/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups.subsystem;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.UnaryOperator;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SubsystemSchema;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.persistence.xml.NamedResourceRegistrationXMLChoice;
import org.jboss.as.controller.persistence.xml.NamedResourceRegistrationXMLElement;
import org.jboss.as.controller.persistence.xml.ResourceRegistrationXMLElement;
import org.jboss.as.controller.persistence.xml.ResourceXMLChoice;
import org.jboss.as.controller.persistence.xml.ResourceXMLElement;
import org.jboss.as.controller.persistence.xml.ResourceXMLElementLocalName;
import org.jboss.as.controller.persistence.xml.ResourceXMLParticleFactory;
import org.jboss.as.controller.persistence.xml.ResourceXMLSequence;
import org.jboss.as.controller.persistence.xml.SingletonResourceRegistrationXMLElement;
import org.jboss.as.controller.persistence.xml.SubsystemResourceRegistrationXMLElement;
import org.jboss.as.controller.persistence.xml.SubsystemResourceXMLSchema;
import org.jboss.as.controller.xml.VersionedNamespace;
import org.jboss.as.controller.xml.XMLCardinality;
import org.jboss.as.version.Stability;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.staxmapper.IntVersion;

/**
 * Enumeration of the supported subsystem xml schemas.
 * @author Paul Ferraro
 */
public enum JGroupsSubsystemSchema implements SubsystemResourceXMLSchema<JGroupsSubsystemSchema> {

    VERSION_1_0(1, 0), // AS 7.0
    VERSION_1_1(1, 1), // AS 7.1
    VERSION_2_0(2, 0), // WildFly 8
    VERSION_3_0(3, 0), // WildFly 9
    VERSION_4_0(4, 0), // WildFly 10, EAP 7.0
    VERSION_5_0(5, 0), // WildFly 11, EAP 7.1
    VERSION_6_0(6, 0), // WildFly 12-16, EAP 7.2
    VERSION_7_0(7, 0), // WildFly 17-19, EAP 7.3
    VERSION_8_0(8, 0), // WildFly 20-26, EAP 7.4
    VERSION_9_0(9, 0), // WildFly 27-present, EAP 8.0-8.1
    VERSION_9_0_COMMUNITY(9, 0, Stability.COMMUNITY), // WildFly 39-present
    ;
    static final Set<JGroupsSubsystemSchema> CURRENT = Set.of(VERSION_9_0_COMMUNITY, VERSION_9_0);

    private final ResourceXMLParticleFactory factory = ResourceXMLParticleFactory.newInstance(this);
    private final VersionedNamespace<IntVersion, JGroupsSubsystemSchema> namespace;

    JGroupsSubsystemSchema(int major, int minor) {
        this(major, minor, Stability.DEFAULT);
    }

    JGroupsSubsystemSchema(int major, int minor, Stability stability) {
        this.namespace = SubsystemSchema.createLegacySubsystemURN(JGroupsSubsystemResourceDefinitionRegistrar.REGISTRATION.getName(), stability, new IntVersion(major, minor));
    }

    @Override
    public VersionedNamespace<IntVersion, JGroupsSubsystemSchema> getNamespace() {
        return this.namespace;
    }

    @Override
    public SubsystemResourceRegistrationXMLElement getSubsystemXMLElement() {
        SubsystemResourceRegistrationXMLElement.Builder builder = this.factory.subsystemElement(JGroupsSubsystemResourceDefinitionRegistrar.REGISTRATION);

        AttributeDefinition defaultStackAttribute = new SimpleAttributeDefinitionBuilder("default-stack", ModelType.STRING)
                .setXmlName(this.since(VERSION_3_0) ? "default" : "default-stack")
                .setRequired(!this.since(VERSION_3_0))
                .build();

        if (this.since(VERSION_3_0)) {
            ResourceXMLElement channels = this.factory.element(this.factory.resolve("channels"))
                    .withCardinality(XMLCardinality.Single.OPTIONAL)
                    .addAttribute(JGroupsSubsystemResourceDefinitionRegistrar.DEFAULT_CHANNEL)
                    .withContent(this.factory.sequence().addElement(this.channelElement()).build())
                    .build();

            ResourceXMLElement.Builder stacksBuilder = this.factory.element(this.factory.resolve("stacks"))
                    .withCardinality(XMLCardinality.Single.OPTIONAL)
                    ;
            if (!this.since(VERSION_9_0)) {
                stacksBuilder.addAttribute(defaultStackAttribute);
            }
            ResourceXMLElement stacks = stacksBuilder.withContent(this.factory.sequence().addElement(this.stackElement()).build()).build();

            builder.withContent(this.factory.all()
                    .addElement(channels)
                    .addElement(stacks)
                    .build());
        } else {
            builder.addAttribute(defaultStackAttribute);
            builder.withContent(this.factory.choice().withCardinality(XMLCardinality.Unbounded.OPTIONAL).addElement(this.stackElement()).build());
        }
        if (!this.since(VERSION_9_0)) {
            builder.withOperationTransformation(new BiConsumer<>() {
                @Override
                public void accept(Map<PathAddress, ModelNode> operations, PathAddress operationKey) {
                    ModelNode operation = operations.get(operationKey);
                    PathAddress address = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR));
                    // Remove default-stack from operation
                    ModelNode defaultStack = operation.remove(defaultStackAttribute.getName());
                    if (defaultStack != null) {
                        if (!JGroupsSubsystemSchema.this.since(VERSION_3_0)) {
                            // Fabricate a default channel resource
                            PathAddress channelAddress = address.append(JGroupsResourceRegistration.CHANNEL.pathElement("ee"));
                            ModelNode channelOperation = Util.createAddOperation(channelAddress);
                            channelOperation.get(ChannelResourceDefinitionRegistrar.STACK.getName()).set(defaultStack);
                            channelOperation.get(ChannelResourceDefinitionRegistrar.CLUSTER.getName()).set("ejb");
                            operations.put(channelAddress, channelOperation);
                            operation.get(JGroupsSubsystemResourceDefinitionRegistrar.DEFAULT_CHANNEL.getName()).set(channelAddress.getLastElement().getValue());
                        } else if (!JGroupsSubsystemSchema.this.since(VERSION_4_0)) {
                            // stack attribute of channel resource was optional
                            // Set stack to default-stack if undefined
                            for (Map.Entry<PathAddress, ModelNode> entry : operations.entrySet()) {
                                PathAddress key = entry.getKey();
                                if (key.getLastElement().getKey().equals(JGroupsResourceRegistration.CHANNEL.getPathElement().getKey())) {
                                    ModelNode channelOperation = entry.getValue();
                                    if (!channelOperation.hasDefined(ChannelResourceDefinitionRegistrar.STACK.getName())) {
                                        channelOperation.get(ChannelResourceDefinitionRegistrar.STACK.getName()).set(defaultStack);
                                    }
                                }
                            }
                        }
                    }
                }
            });
        }
        return builder.build();
    }

    private ResourceRegistrationXMLElement channelElement() {
        NamedResourceRegistrationXMLElement.Builder builder = this.factory.namedElement(JGroupsResourceRegistration.CHANNEL)
                .withCardinality(XMLCardinality.Unbounded.REQUIRED)
                .addAttribute(ChannelResourceDefinitionRegistrar.MODULE)
                ;
        if (this.since(VERSION_4_0)) {
            builder.addAttributes(List.of(ChannelResourceDefinitionRegistrar.STACK, ChannelResourceDefinitionRegistrar.CLUSTER));
            if (this.since(VERSION_5_0)) {
                builder.addAttribute(ChannelResourceDefinitionRegistrar.STATISTICS_ENABLED);
            }
        } else {
            builder.addAttribute(new SimpleAttributeDefinitionBuilder(ChannelResourceDefinitionRegistrar.STACK).setRequired(false).build());
        }

        return builder.withContent(this.factory.sequence().withCardinality(XMLCardinality.Single.OPTIONAL).addElement(this.forkElement()).build()).build();
    }

    private ResourceRegistrationXMLElement forkElement() {
        return this.factory.namedElement(JGroupsResourceRegistration.FORK)
                .withCardinality(XMLCardinality.Unbounded.OPTIONAL)
                .withContent(this.protocolChoice())
                .build();
    }

    private ResourceRegistrationXMLElement stackElement() {
        NamedResourceRegistrationXMLElement.Builder builder = this.factory.namedElement(JGroupsResourceRegistration.STACK)
                .withCardinality(XMLCardinality.Unbounded.REQUIRED)
                ;
        if (this.since(VERSION_5_0)) {
            builder.addAttribute(StackResourceDefinitionRegistrar.STATISTICS_ENABLED);
        }

        ResourceXMLSequence.Builder contentBuilder = this.factory.sequence()
                .addChoice(this.transportChoice())
                .addChoice(this.protocolChoice())
                ;
        if (this.since(VERSION_2_0)) {
            contentBuilder.addElement(this.relayElement());
        }
        return builder.withContent(contentBuilder.build()).build();
    }

    private ResourceXMLChoice transportChoice() {
        NamedResourceRegistrationXMLElement transportElement = this.transportBuilder(StackResourceDefinitionRegistrar.Component.TRANSPORT).build();
        NamedResourceRegistrationXMLChoice.Builder builder = this.factory.namedElementChoice(transportElement);

        if (JGroupsSubsystemSchema.this.since(VERSION_7_0)) {
            for (SocketTransportResourceDefinitionRegistrar.Transport transport : EnumSet.allOf(SocketTransportResourceDefinitionRegistrar.Transport.class)) {
                builder.addElement(this.transportBuilder(transport).withElementLocalName(ResourceXMLElementLocalName.KEY).addAttribute(SocketTransportResourceDefinitionRegistrar.CLIENT_SOCKET_BINDING).build());
            }
        }

        return builder.build();
    }

    private NamedResourceRegistrationXMLElement.Builder transportBuilder(ResourceRegistration registration) {
        NamedResourceRegistrationXMLElement.Builder builder = this.protocolChildElementBuilder(registration)
                .withOperationKey(StackResourceDefinitionRegistrar.Component.TRANSPORT.getPathElement())
                ;
        if (!JGroupsSubsystemSchema.this.since(VERSION_9_0)) {
            builder.ignoreAttributeLocalNames(Set.of("shared", "default-executor", "oob-executor", "timer-executor", "thread-factory"));
        }
        builder.provideAttributes(EnumSet.allOf(AbstractTransportResourceDefinitionRegistrar.SocketBindingAttribute.class));
        if (JGroupsSubsystemSchema.this.since(VERSION_1_1)) {
            builder.provideAttributes(EnumSet.allOf(AbstractTransportResourceDefinitionRegistrar.TopologyAttribute.class));
        }

        ResourceXMLSequence.Builder contentBuilder = this.protocolContentBuilder();

        for (ThreadPoolResourceRegistration pool : EnumSet.allOf(ThreadPoolResourceDefinitionRegistrar.class)) {
            SingletonResourceRegistrationXMLElement.Builder threadPoolBuilder = this.factory.singletonElement(pool)
                    .implyIfAbsent()
                    .withElementLocalName(ResourceXMLElementLocalName.VALUE_KEY)
                    .addAttributes(List.of(pool.getMinThreads(), pool.getMaxThreads(), pool.getKeepAlive()))
                    ;
            if (!this.since(VERSION_6_0)) {
                threadPoolBuilder.ignoreAttributeLocalNames(Set.of("queue-length"));
            }
            contentBuilder.addElement(threadPoolBuilder.build());
        }

        if (!this.since(VERSION_6_0)) {
            for (String localName : List.of("internal-thread-pool", "oob-thread-pool", "timer-thread-pool")) {
                contentBuilder.addElement(ResourceXMLElement.ignore(this.factory.resolve(localName), XMLCardinality.Single.OPTIONAL));
            }
        }
        if (!this.since(JGroupsSubsystemSchema.VERSION_5_0)) {
            // Set default port_range
            String portRangeProperty = "port_range";
            builder.withOperationTransformation(new UnaryOperator<>() {
                @Override
                public ModelNode apply(ModelNode operation) {
                    if (!operation.hasDefined(ProtocolChildResourceDefinitionRegistrar.PROPERTIES.getName(), portRangeProperty)) {
                        operation.get(ProtocolChildResourceDefinitionRegistrar.PROPERTIES.getName()).get(portRangeProperty).set("50");
                    }
                    return operation;
                }
            });
        }

        if (this.since(JGroupsSubsystemSchema.VERSION_9_0_COMMUNITY)) {
            ResourceXMLElement ssl = this.factory.element(this.factory.resolve("ssl"))
                    .withCardinality(XMLCardinality.Single.OPTIONAL)
                    .addAttributes(List.of(SocketTransportResourceDefinitionRegistrar.CLIENT_SSL_CONTEXT, SocketTransportResourceDefinitionRegistrar.SERVER_SSL_CONTEXT))
                    .build();
            contentBuilder.addElement(ssl);
        }

        return builder.withContent(contentBuilder.build());
    }

    private ResourceXMLChoice protocolChoice() {
        NamedResourceRegistrationXMLElement protocolElement = this.protocolElementBuilder(StackResourceDefinitionRegistrar.Component.PROTOCOL)
                .withContent(this.protocolContentBuilder().build())
                .build();
        NamedResourceRegistrationXMLChoice.Builder builder = this.factory.namedElementChoice(protocolElement).withCardinality(XMLCardinality.Unbounded.OPTIONAL);

        // Override registrations
        if (this.since(VERSION_5_0)) {
            for (AuthProtocolResourceDefinitionRegistrar.Protocol protocol : EnumSet.allOf(AuthProtocolResourceDefinitionRegistrar.Protocol.class)) {
                NamedResourceRegistrationXMLElement.Builder protocolBuilder = this.protocolElementBuilder(protocol).withElementLocalName("auth-protocol");
                builder.addElement(protocolBuilder.withContent(this.protocolContentBuilder().addChoice(this.tokenChoice()).build()).build());
            }

            for (EncryptProtocolResourceDefinitionRegistrar.Protocol protocol : EnumSet.allOf(EncryptProtocolResourceDefinitionRegistrar.Protocol.class)) {
                NamedResourceRegistrationXMLElement.Builder protocolBuilder = this.protocolElementBuilder(protocol).withElementLocalName("encrypt-protocol");
                protocolBuilder.addAttributes(List.of(EncryptProtocolResourceDefinitionRegistrar.KEY_ALIAS, EncryptProtocolResourceDefinitionRegistrar.KEY_STORE));
                builder.addElement(protocolBuilder.withContent(this.protocolContentBuilder().addElement(EncryptProtocolResourceDefinitionRegistrar.KEY_CREDENTIAL).build()).build());
            }

            for (JDBCProtocolResourceDefinitionRegistrar.Protocol protocol : EnumSet.allOf(JDBCProtocolResourceDefinitionRegistrar.Protocol.class)) {
                NamedResourceRegistrationXMLElement.Builder protocolBuilder = this.protocolElementBuilder(protocol).withElementLocalName("jdbc-protocol").addAttribute(JDBCProtocolResourceDefinitionRegistrar.DATA_SOURCE);
                builder.addElement(protocolBuilder.withContent(this.protocolContentBuilder().build()).build());
            }

            for (SocketProtocolResourceRegistration protocol : EnumSet.allOf(SocketProtocolResourceRegistration.class)) {
                NamedResourceRegistrationXMLElement.Builder protocolBuilder = this.protocolElementBuilder(protocol).withElementLocalName("socket-protocol");
                protocolBuilder.addAttribute(FailureDetectionProtocolResourceDefinitionRegistrar.SocketBindingAttribute.SERVER.get());
                if (this.since(VERSION_7_0)) {
                    protocolBuilder.addAttribute(FailureDetectionProtocolResourceDefinitionRegistrar.SocketBindingAttribute.CLIENT.get());
                }
                builder.addElement(protocolBuilder.withContent(this.protocolContentBuilder().build()).build());
            }
            for (MulticastProtocolResourceDefinitionRegistrar.Protocol protocol : EnumSet.allOf(MulticastProtocolResourceDefinitionRegistrar.Protocol.class)) {
                NamedResourceRegistrationXMLElement.Builder protocolBuilder = this.protocolElementBuilder(protocol).withElementLocalName("socket-protocol");
                protocolBuilder.addAttribute(MulticastProtocolResourceDefinitionRegistrar.SOCKET_BINDING);
                builder.addElement(protocolBuilder.withContent(this.protocolContentBuilder().build()).build());
            }

            for (SocketDiscoveryProtocolResourceDefinitionRegistrar.Protocol protocol : EnumSet.allOf(SocketDiscoveryProtocolResourceDefinitionRegistrar.Protocol.class)) {
                NamedResourceRegistrationXMLElement.Builder protocolBuilder = this.protocolElementBuilder(protocol).withElementLocalName("socket-discovery-protocol");
                protocolBuilder.addAttribute(SocketDiscoveryProtocolResourceDefinitionRegistrar.OUTBOUND_SOCKET_BINDINGS);
                builder.addElement(protocolBuilder.withContent(this.protocolContentBuilder().build()).build());
            }
        }
        return builder.build();
    }

    private ResourceXMLChoice tokenChoice() {
        return this.factory.choice()
                .addElement(this.tokenElementBuilder(AuthTokenResourceDefinitionRegistrar.Token.PLAIN)
                        .withContent(this.tokenContentBuilder().build())
                        .build())
                .addElement(this.tokenElementBuilder(AuthTokenResourceDefinitionRegistrar.Token.DIGEST)
                        .provideAttributes(EnumSet.allOf(DigestAuthTokenResourceDefinitionRegistrar.Attribute.class))
                        .withContent(this.tokenContentBuilder().build())
                        .build())
                .addElement(this.tokenElementBuilder(AuthTokenResourceDefinitionRegistrar.Token.CIPHER)
                        .provideAttributes(EnumSet.allOf(CipherAuthTokenResourceDefinitionRegistrar.Attribute.class))
                        .addAttribute(CipherAuthTokenResourceDefinitionRegistrar.KEY_STORE)
                        .withContent(this.tokenContentBuilder().addElement(CipherAuthTokenResourceDefinitionRegistrar.KEY_CREDENTIAL).build())
                        .build())
                .build();
    }

    private SingletonResourceRegistrationXMLElement.Builder tokenElementBuilder(ResourceRegistration registration) {
        return this.factory.singletonElement(registration).withElementLocalName(ResourceXMLElementLocalName.VALUE_KEY);
    }

    private ResourceXMLSequence.Builder tokenContentBuilder() {
        return this.factory.sequence().addElement(AuthTokenResourceDefinitionRegistrar.SHARED_SECRET);
    }

    private NamedResourceRegistrationXMLElement.Builder protocolElementBuilder(ResourceRegistration registration) {
        NamedResourceRegistrationXMLElement.Builder builder = this.protocolChildElementBuilder(registration).withCardinality(XMLCardinality.Single.REQUIRED);
        if (!this.since(VERSION_5_0)) {
            builder.addAttributes(List.of(new SimpleAttributeDefinitionBuilder(JDBCProtocolResourceDefinitionRegistrar.DATA_SOURCE).setRequired(false).setXmlName("datasource").build(), FailureDetectionProtocolResourceDefinitionRegistrar.SocketBindingAttribute.SERVER.get()));
        }
        return builder;
    }

    private NamedResourceRegistrationXMLElement.Builder protocolChildElementBuilder(ResourceRegistration registration) {
        NamedResourceRegistrationXMLElement.Builder builder = this.factory.namedElement(registration)
                .withCardinality(XMLCardinality.Single.REQUIRED)
                .addAttribute(ProtocolChildResourceDefinitionRegistrar.PROPERTIES);
        if (this.since(VERSION_3_0)) {
            builder.addAttribute(ProtocolChildResourceDefinitionRegistrar.MODULE);
        }
        if (this.since(VERSION_5_0)) {
            builder.addAttribute(ProtocolChildResourceDefinitionRegistrar.STATISTICS_ENABLED);
        }
        return builder.withResourceAttributeLocalName("type");
    }

    private ResourceXMLSequence.Builder protocolContentBuilder() {
        return this.factory.sequence().addElement(ProtocolChildResourceDefinitionRegistrar.PROPERTIES).withCardinality(XMLCardinality.Single.OPTIONAL);
    }

    private ResourceRegistrationXMLElement relayElement() {
        return this.factory.singletonElement(RelayResourceDefinitionRegistrar.Protocol.RELAY2)
                .withCardinality(XMLCardinality.Single.OPTIONAL)
                .withElementLocalName(ResourceXMLElementLocalName.KEY)
                .provideAttributes(EnumSet.allOf(RelayResourceDefinitionRegistrar.Attribute.class))
                .withContent(this.factory.choice().withCardinality(XMLCardinality.Unbounded.OPTIONAL).addElement(this.remoteSiteElement()).build())
                .build();
    }

    private ResourceRegistrationXMLElement remoteSiteElement() {
        NamedResourceRegistrationXMLElement.Builder builder = this.factory.namedElement(JGroupsResourceRegistration.REMOTE_SITE);
        if (this.since(VERSION_3_0)) {
            builder.addAttribute(RemoteSiteResourceDefinitionRegistrar.CHANNEL_CONFIGURATION);
        } else {
            AttributeDefinition stack = new SimpleAttributeDefinitionBuilder("stack", ModelType.STRING).build();
            AttributeDefinition cluster = new SimpleAttributeDefinitionBuilder("cluster", ModelType.STRING).build();
            builder.addAttributes(List.of(stack, cluster));
            builder.withOperationTransformation(new BiConsumer<>() {
                @Override
                public void accept(Map<PathAddress, ModelNode> operations, PathAddress key) {
                    ModelNode operation = operations.get(key);
                    ModelNode stackName = operation.remove(stack.getName());
                    // Use cluster name for channel name
                    ModelNode channel = operation.remove(cluster.getName());
                    if (channel == null) {
                        // Fallback to site name, if cluster name was undefined
                        PathAddress operationAddress = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR));
                        channel = new ModelNode(operationAddress.getLastElement().getValue());
                    }
                    operation.get(RemoteSiteResourceDefinitionRegistrar.CHANNEL_CONFIGURATION.getName()).set(channel);

                    // Create a corresponding channel add operation
                    PathAddress channelAddress = PathAddress.pathAddress(JGroupsSubsystemResourceDefinitionRegistrar.REGISTRATION.getPathElement()).append(JGroupsResourceRegistration.CHANNEL.pathElement(channel.asString()));
                    ModelNode channelOperation = Util.createAddOperation(channelAddress);
                    channelOperation.get(ChannelResourceDefinitionRegistrar.STACK.getName()).set(stackName);
                    operations.put(channelAddress, channelOperation);
                }
            });
        }
        return builder.build();
    }
}
