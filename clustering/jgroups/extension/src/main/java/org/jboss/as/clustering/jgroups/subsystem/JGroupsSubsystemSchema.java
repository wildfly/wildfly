/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups.subsystem;

import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.jboss.as.clustering.controller.persistence.xml.NamedResourceRegistrationXMLElement;
import org.jboss.as.clustering.controller.persistence.xml.ResourceRegistrationXMLChoice;
import org.jboss.as.clustering.controller.persistence.xml.ResourceXMLChoice;
import org.jboss.as.clustering.controller.persistence.xml.ResourceXMLElement;
import org.jboss.as.clustering.controller.persistence.xml.ResourceXMLElementLocalName;
import org.jboss.as.clustering.controller.persistence.xml.ResourceXMLParticleFactory;
import org.jboss.as.clustering.controller.persistence.xml.ResourceXMLSequence;
import org.jboss.as.clustering.controller.persistence.xml.SingletonResourceRegistrationXMLElement;
import org.jboss.as.clustering.controller.persistence.xml.SubsystemResourceRegistrationXMLElement;
import org.jboss.as.clustering.controller.persistence.xml.SubsystemResourceXMLSchema;
import org.jboss.as.clustering.controller.xml.XMLCardinality;
import org.jboss.as.clustering.controller.xml.XMLElement;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SubsystemSchema;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.xml.VersionedNamespace;
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
    VERSION_9_0(9, 0), // WildFly 27-present
    ;
    static final JGroupsSubsystemSchema CURRENT = VERSION_9_0;

    private final ResourceXMLParticleFactory factory = ResourceXMLParticleFactory.newInstance(this);
    private final VersionedNamespace<IntVersion, JGroupsSubsystemSchema> namespace;

    JGroupsSubsystemSchema(int major, int minor) {
        this.namespace = SubsystemSchema.createLegacySubsystemURN(JGroupsSubsystemResourceDefinitionRegistrar.INSTANCE.getName(), new IntVersion(major, minor));
    }

    @Override
    public VersionedNamespace<IntVersion, JGroupsSubsystemSchema> getNamespace() {
        return this.namespace;
    }

    @Override
    public SubsystemResourceRegistrationXMLElement getSubsystemXMLElement() {
        SubsystemResourceRegistrationXMLElement.Builder builder = this.factory.subsystemElement(JGroupsSubsystemResourceDefinitionRegistrar.INSTANCE);

        AttributeDefinition defaultStackAttribute = new SimpleAttributeDefinitionBuilder("default-stack", ModelType.STRING)
                .setXmlName(this.since(VERSION_3_0) ? "default" : "default-stack")
                .build();

        if (this.since(VERSION_3_0)) {
            ResourceXMLElement channels = this.factory.element(this.factory.resolve("channels"))
                    .addAttribute(JGroupsSubsystemResourceDefinitionRegistrar.DEFAULT_CHANNEL)
                    .withContent(this.factory.sequence().addElement(this.channel()).build())
                    .build();

            ResourceXMLElement.Builder stacksBuilder = this.factory.element(this.factory.resolve("stacks"));
            if (!this.since(VERSION_9_0)) {
                stacksBuilder.addAttribute(defaultStackAttribute);
            }
            ResourceXMLElement stacks = stacksBuilder.withContent(this.factory.sequence().addElement(this.stack()).build()).build();

            builder.withContent(this.factory.all().addElement(channels).addElement(stacks).build());
        } else {
            builder.addAttribute(defaultStackAttribute);
            builder.withContent(this.factory.sequence().addElement(this.stack()).build());
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

    private ResourceXMLElement channel() {
        NamedResourceRegistrationXMLElement.Builder builder = this.factory.namedElement(JGroupsResourceRegistration.CHANNEL).addAttributes(List.of(ChannelResourceDefinitionRegistrar.MODULE, ChannelResourceDefinitionRegistrar.STACK));
        if (this.since(VERSION_4_0)) {
            builder.addAttribute(ChannelResourceDefinitionRegistrar.CLUSTER);
            if (this.since(VERSION_5_0)) {
                builder.addAttribute(ChannelResourceDefinitionRegistrar.STATISTICS_ENABLED);
            }
        }

        NamedResourceRegistrationXMLElement fork = this.factory.namedElement(JGroupsResourceRegistration.FORK)
                .withCardinality(XMLCardinality.Unbounded.OPTIONAL)
                .withContent(this.protocol())
                .build();

        return builder.withContent(this.factory.sequence().withCardinality(XMLCardinality.Single.OPTIONAL).addElement(fork).build()).build();
    }

    private ResourceXMLElement stack() {
        NamedResourceRegistrationXMLElement.Builder builder = this.factory.namedElement(JGroupsResourceRegistration.STACK);
        if (this.since(VERSION_5_0)) {
            builder.addAttribute(StackResourceDefinitionRegistrar.STATISTICS_ENABLED);
        }

        ResourceXMLSequence.Builder contentBuilder = this.factory.sequence()
                .addChoice(this.transport())
                .addChoice(this.protocol())
                ;
        if (this.since(VERSION_2_0)) {
            contentBuilder.addElement(this.relay()).withCardinality(XMLCardinality.Single.OPTIONAL);
        }
        return builder.withContent(contentBuilder.build()).build();
    }

    private ResourceXMLChoice transport() {
        ResourceRegistrationXMLChoice.Builder builder = this.factory.choice(this.configureTransport(this.factory.namedElement(StackResourceDefinitionRegistrar.Component.TRANSPORT)).build());

        if (JGroupsSubsystemSchema.this.since(VERSION_7_0)) {
            for (SocketTransportResourceDefinitionRegistrar.Transport transport : EnumSet.allOf(SocketTransportResourceDefinitionRegistrar.Transport.class)) {
                builder.addElement(this.configureTransport(this.factory.namedElement(transport).withElementLocalName(ResourceXMLElementLocalName.KEY).addAttribute(SocketTransportResourceDefinitionRegistrar.CLIENT_SOCKET_BINDING)).build());
            }
        }

        return builder.build();
    }

    private NamedResourceRegistrationXMLElement.Builder configureTransport(NamedResourceRegistrationXMLElement.Builder builder) {
        if (!JGroupsSubsystemSchema.this.since(VERSION_9_0)) {
            for (String ignored : Set.of("shared", "default-executor", "oob-executor", "timer-executor", "thread-factory")) {
                builder.ignoreAttribute(ignored);
            }
        }
        builder.addAttributes(EnumSet.allOf(AbstractTransportResourceDefinitionRegistrar.SocketBindingAttribute.class).stream().map(Supplier::get).toList());
        if (JGroupsSubsystemSchema.this.since(VERSION_1_1)) {
            builder.addAttributes(EnumSet.allOf(AbstractTransportResourceDefinitionRegistrar.TopologyAttribute.class).stream().map(Supplier::get).toList());
        }

        ResourceXMLSequence.Builder contentBuilder = this.configureProtocolContent(this.factory.sequence());
        SingletonResourceRegistrationXMLElement.Builder threadPoolBuilder = this.factory.singletonElement(ThreadPoolResourceDefinitionRegistrar.DEFAULT)
                .withElementLocalName(ResourceXMLElementLocalName.VALUE_KEY)
                .withCardinality(XMLCardinality.Single.OPTIONAL)
                .addAttributes(List.of(ThreadPoolResourceDefinitionRegistrar.DEFAULT.getMinThreads(), ThreadPoolResourceDefinitionRegistrar.DEFAULT.getMaxThreads(), ThreadPoolResourceDefinitionRegistrar.DEFAULT.getKeepAlive()))
                .omitIfEmpty();
        if (!this.since(VERSION_6_0)) {
            threadPoolBuilder.ignoreAttribute("queue-length");
        }
        contentBuilder.addElement(threadPoolBuilder.build());
        if (!this.since(VERSION_6_0)) {
            for (String ignoredLocalName : List.of("internal-thread-pool", "oob-thread-pool", "timer-thread-pool")) {
                contentBuilder.addElement(XMLElement.ignore(this.factory.resolve(ignoredLocalName), XMLCardinality.Single.OPTIONAL));
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
        return this.configureProtocol(builder).withContent(contentBuilder.build());
    }

    @SuppressWarnings("incomplete-switch")
    private ResourceXMLChoice protocol() {
        ResourceRegistrationXMLChoice.Builder builder = this.factory.choice(this.configureProtocol(this.factory.namedElement(StackResourceDefinitionRegistrar.Component.PROTOCOL))
                .withContent(this.configureProtocolContent(this.factory.sequence()).build())
                .build()).withCardinality(XMLCardinality.Unbounded.OPTIONAL);

        if (this.since(VERSION_5_0)) {
            for (AuthProtocolResourceDefinitionRegistrar.Protocol protocol : EnumSet.allOf(AuthProtocolResourceDefinitionRegistrar.Protocol.class)) {
                NamedResourceRegistrationXMLElement.Builder protocolBuilder = this.configureProtocol(this.factory.namedElement(protocol).withElementLocalName("auth-protocol"));
                ResourceXMLChoice.Builder tokens = this.factory.choice();
                for (AuthTokenResourceDefinitionRegistrar.Token token : EnumSet.allOf(AuthTokenResourceDefinitionRegistrar.Token.class)) {
                    SingletonResourceRegistrationXMLElement.Builder tokenBuilder = this.factory.singletonElement(token).withElementLocalName(ResourceXMLElementLocalName.VALUE_KEY).withCardinality(XMLCardinality.Single.REQUIRED);
                    ResourceXMLSequence.Builder tokenContentBuilder = this.factory.sequence().addElement(AuthTokenResourceDefinitionRegistrar.SHARED_SECRET);
                    switch (token) {
                        case DIGEST:
                            tokenBuilder.addAttribute(DigestAuthTokenResourceDefinitionRegistrar.Attribute.ALGORITHM.get());
                            break;
                        case CIPHER:
                            tokenBuilder.addAttributes(EnumSet.allOf(CipherAuthTokenResourceDefinitionRegistrar.Attribute.class).stream().map(Supplier::get).collect(Collectors.toList()));
                            tokenBuilder.addAttribute(CipherAuthTokenResourceDefinitionRegistrar.KEY_STORE);
                            tokenContentBuilder.addElement(CipherAuthTokenResourceDefinitionRegistrar.KEY_CREDENTIAL);
                            break;
                    }
                    tokens.addElement(tokenBuilder.withContent(tokenContentBuilder.build()).build());
                }
                ResourceXMLSequence.Builder protocolContentBuilder = this.configureProtocolContent(this.factory.sequence().addChoice(tokens.build()));
                builder.addElement(protocolBuilder.withContent(protocolContentBuilder.build()).build());
            }

            for (EncryptProtocolResourceDefinitionRegistrar.Protocol protocol : EnumSet.allOf(EncryptProtocolResourceDefinitionRegistrar.Protocol.class)) {
                NamedResourceRegistrationXMLElement.Builder protocolBuilder = this.configureProtocol(this.factory.namedElement(protocol).withElementLocalName("encrypt-protocol"));
                protocolBuilder.addAttributes(List.of(EncryptProtocolResourceDefinitionRegistrar.KEY_ALIAS, EncryptProtocolResourceDefinitionRegistrar.KEY_STORE));
                ResourceXMLSequence.Builder protocolContentBuilder = this.configureProtocolContent(this.factory.sequence().addElement(EncryptProtocolResourceDefinitionRegistrar.KEY_CREDENTIAL));
                builder.addElement(protocolBuilder.withContent(protocolContentBuilder.build()).build());
            }

            for (JDBCProtocolResourceDefinitionRegistrar.Protocol protocol : EnumSet.allOf(JDBCProtocolResourceDefinitionRegistrar.Protocol.class)) {
                NamedResourceRegistrationXMLElement.Builder protocolBuilder = this.configureProtocol(this.factory.namedElement(protocol).withElementLocalName("jdbc-protocol")).addAttribute(JDBCProtocolResourceDefinitionRegistrar.DATA_SOURCE);
                ResourceXMLSequence.Builder protocolContentBuilder = this.configureProtocolContent(this.factory.sequence());
                builder.addElement(protocolBuilder.withContent(protocolContentBuilder.build()).build());
            }

            for (SocketProtocolResourceRegistration protocol : EnumSet.allOf(SocketProtocolResourceRegistration.class)) {
                NamedResourceRegistrationXMLElement.Builder protocolBuilder = this.configureProtocol(this.factory.namedElement(protocol).withElementLocalName("socket-protocol"));
                protocolBuilder.addAttribute(FailureDetectionProtocolResourceDefinitionRegistrar.SocketBindingAttribute.SERVER.get());
                if (this.since(VERSION_7_0)) {
                    protocolBuilder.addAttribute(FailureDetectionProtocolResourceDefinitionRegistrar.SocketBindingAttribute.CLIENT.get());
                }
                ResourceXMLSequence.Builder protocolContentBuilder = this.configureProtocolContent(this.factory.sequence());
                builder.addElement(protocolBuilder.withContent(protocolContentBuilder.build()).build());
            }
            for (MulticastProtocolResourceDefinitionRegistrar.Protocol protocol : EnumSet.allOf(MulticastProtocolResourceDefinitionRegistrar.Protocol.class)) {
                NamedResourceRegistrationXMLElement.Builder protocolBuilder = this.configureProtocol(this.factory.namedElement(protocol).withElementLocalName("socket-protocol"));
                protocolBuilder.addAttribute(MulticastProtocolResourceDefinitionRegistrar.SOCKET_BINDING);
                ResourceXMLSequence.Builder protocolContentBuilder = this.configureProtocolContent(this.factory.sequence());
                builder.addElement(protocolBuilder.withContent(protocolContentBuilder.build()).build());
            }

            for (SocketDiscoveryProtocolResourceDefinitionRegistrar.Protocol protocol : EnumSet.allOf(SocketDiscoveryProtocolResourceDefinitionRegistrar.Protocol.class)) {
                NamedResourceRegistrationXMLElement.Builder protocolBuilder = this.configureProtocol(this.factory.namedElement(protocol).withElementLocalName("socket-discovery-protocol"));
                protocolBuilder.addAttribute(SocketDiscoveryProtocolResourceDefinitionRegistrar.OUTBOUND_SOCKET_BINDINGS);
                ResourceXMLSequence.Builder protocolContentBuilder = this.configureProtocolContent(this.factory.sequence());
                builder.addElement(protocolBuilder.withContent(protocolContentBuilder.build()).build());
            }
        }
        return builder.build();
    }

    private NamedResourceRegistrationXMLElement.Builder configureProtocol(NamedResourceRegistrationXMLElement.Builder builder) {
        List<AttributeDefinition> attributes = new LinkedList<>();
        attributes.add(ProtocolChildResourceDefinitionRegistrar.PROPERTIES);
        if (this.since(VERSION_3_0)) {
            attributes.add(ProtocolChildResourceDefinitionRegistrar.MODULE);
        }
        if (this.since(VERSION_5_0)) {
            attributes.add(ProtocolChildResourceDefinitionRegistrar.STATISTICS_ENABLED);
        } else {
            attributes.add(JDBCProtocolResourceDefinitionRegistrar.DATA_SOURCE);
            attributes.add(FailureDetectionProtocolResourceDefinitionRegistrar.SocketBindingAttribute.SERVER.get());
        }
        return builder.addAttributes(attributes).withResourceAttributeLocalName("type");
    }

    private ResourceXMLSequence.Builder configureProtocolContent(ResourceXMLSequence.Builder builder) {
        return builder.addElement(ProtocolChildResourceDefinitionRegistrar.PROPERTIES).withCardinality(XMLCardinality.Single.OPTIONAL);
    }

    private ResourceXMLElement relay() {
        SingletonResourceRegistrationXMLElement.Builder builder = this.factory.singletonElement(RelayResourceDefinitionRegistrar.Protocol.RELAY2).withElementLocalName(ResourceXMLElementLocalName.KEY);
        for (RelayResourceDefinitionRegistrar.Attribute attribute : EnumSet.allOf(RelayResourceDefinitionRegistrar.Attribute.class)) {
            builder.addAttribute(attribute.get());
        }
        return builder.withContent(this.factory.sequence().addElement(this.remoteSite()).build()).build();
    }

    private ResourceXMLElement remoteSite() {
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
                    PathAddress channelAddress = PathAddress.pathAddress(JGroupsSubsystemResourceDefinitionRegistrar.INSTANCE.getPathElement()).append(JGroupsResourceRegistration.CHANNEL.pathElement(channel.asString()));
                    ModelNode channelOperation = Util.createAddOperation(channelAddress);
                    channelOperation.get(ChannelResourceDefinitionRegistrar.STACK.getName()).set(stackName);
                    operations.put(channelAddress, channelOperation);
                }
            });
        }
        return builder.build();
    }
}
