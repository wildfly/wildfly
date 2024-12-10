/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups.subsystem;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.jboss.as.clustering.controller.ResourceDescription;
import org.jboss.as.clustering.controller.persistence.xml.ResourceXMLChoice;
import org.jboss.as.clustering.controller.persistence.xml.ResourceXMLElement;
import org.jboss.as.clustering.controller.persistence.xml.ResourceXMLElementLocalName;
import org.jboss.as.clustering.controller.persistence.xml.SubsystemResourceXMLSchema;
import org.jboss.as.clustering.controller.xml.XMLCardinality;
import org.jboss.as.clustering.controller.xml.XMLChoice;
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
import org.wildfly.subsystem.resource.AttributeDefinitionProvider;

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

    private final ResourceXMLElement.Builder.Factory factory = ResourceXMLElement.Builder.Factory.newInstance(this);
    private final VersionedNamespace<IntVersion, JGroupsSubsystemSchema> namespace;

    JGroupsSubsystemSchema(int major, int minor) {
        this.namespace = SubsystemSchema.createLegacySubsystemURN(JGroupsSubsystemResourceDescription.INSTANCE.getName(), new IntVersion(major, minor));
    }

    @Override
    public VersionedNamespace<IntVersion, JGroupsSubsystemSchema> getNamespace() {
        return this.namespace;
    }

    @Override
    public ResourceXMLElement getSubsystemResourceXMLElement() {
        ResourceXMLElement.Builder builder = this.factory.createBuilder(JGroupsSubsystemResourceDescription.INSTANCE);
        if (this.since(VERSION_3_0)) {
            builder.appendChild(JGroupsSubsystemResourceDescription.DEFAULT_CHANNEL, this.createChannelElement());
        }
        if (this.since(VERSION_9_0)) {
            builder.appendChild(XMLElement.wrap(this.factory.resolveQName("stacks"), this.createStackElement()));
        } else {
            AttributeDefinition defaultStackAttribute = new SimpleAttributeDefinitionBuilder("default-stack", ModelType.STRING)
                    .setAttributeGroup(this.since(VERSION_3_0) ? "stacks" : null)
                    .setXmlName(this.since(VERSION_3_0) ? "default" : "default-stack")
                    .build();
            builder.includeAttribute(defaultStackAttribute);
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
                            PathAddress channelAddress = address.append(ChannelResourceDescription.pathElement("ee"));
                            ModelNode channelOperation = Util.createAddOperation(channelAddress);
                            channelOperation.get(ChannelResourceDescription.STACK.getName()).set(defaultStack);
                            channelOperation.get(ChannelResourceDescription.CLUSTER.getName()).set("ejb");
                            operations.put(channelAddress, channelOperation);
                            operation.get(JGroupsSubsystemResourceDescription.DEFAULT_CHANNEL.getName()).set(channelAddress.getLastElement().getValue());
                        } else if (!JGroupsSubsystemSchema.this.since(VERSION_4_0)) {
                            // stack attribute of channel resource was optional
                            // Set stack to default-stack if undefined
                            for (Map.Entry<PathAddress, ModelNode> entry : operations.entrySet()) {
                                PathAddress key = entry.getKey();
                                if (key.getLastElement().getKey().equals(ChannelResourceDescription.INSTANCE.getPathElement().getKey())) {
                                    ModelNode channelOperation = entry.getValue();
                                    if (!channelOperation.hasDefined(ChannelResourceDescription.STACK.getName())) {
                                        channelOperation.get(ChannelResourceDescription.STACK.getName()).set(defaultStack);
                                    }
                                }
                            }
                        }
                    }
                }
            });
            if (this.since(VERSION_3_0)) {
                builder.appendChild(defaultStackAttribute, this.createStackElement());
            } else {
                builder.appendChild(this.createStackElement());
            }
        }
        return builder.build();
    }

    private ResourceXMLElement createChannelElement() {
        ResourceXMLElement.Builder builder = this.factory.createBuilder(ChannelResourceDescription.INSTANCE);
        if (!this.since(VERSION_5_0)) {
            builder.excludeAttribute(ChannelResourceDescription.STATISTICS_ENABLED);
        }
        if (!this.since(VERSION_4_0)) {
            builder.excludeAttribute(ChannelResourceDescription.CLUSTER);
        }
        builder.appendChild(this.createForkElement());
        return builder.build();
    }

    private ResourceXMLElement createForkElement() {
        return this.factory.createBuilder(ForkResourceDescription.INSTANCE)
                .appendChild(this.createProtocolChoice())
                .build();
    }

    private ResourceXMLElement createStackElement() {
        ResourceXMLElement.Builder builder = this.factory.createBuilder(StackResourceDescription.INSTANCE)
                .appendChild(this.createTransportElement())
                .appendChild(this.createProtocolChoice())
                ;
        if (!this.since(VERSION_5_0)) {
            builder.excludeAttribute(StackResourceDescription.STATISTICS_ENABLED);
        }
        if (this.since(VERSION_2_0)) {
            builder.appendChild(this.factory.createBuilder(RelayResourceDescription.INSTANCE).withElementLocalName(ResourceXMLElementLocalName.KEY)
                    .appendChild(this.createRemoteSiteElement())
                    .build());
        }
        return builder.build();
    }

    private ResourceXMLElement createTransportElement() {
        ResourceXMLElement.Builder builder = this.createProtocolElementBuilder(TransportResourceDescription.INSTANCE);
        if (!JGroupsSubsystemSchema.this.since(VERSION_9_0)) {
            builder.ignoreAttributes(Set.of("shared", "default-executor", "oob-executor", "timer-executor", "thread-factory"));
        }
        if (JGroupsSubsystemSchema.this.since(VERSION_7_0)) {
            builder.includeAttribute(TransportResourceDescription.SocketBindingAttribute.CLIENT.get());
        }
        if (!JGroupsSubsystemSchema.this.since(VERSION_1_1)) {
            builder.excludeAttributes(EnumSet.allOf(TransportResourceDescription.TopologyAttribute.class).stream().map(AttributeDefinitionProvider::get).collect(Collectors.toSet()));
        }
        if (!this.since(VERSION_6_0)) {
            for (String ignoredLocalName : Set.of("internal-thread-pool", "oob-thread-pool", "timer-thread-pool")) {
                builder.appendChild(XMLElement.ignore(this.factory.resolveQName(ignoredLocalName)));
            }
        }

        ResourceXMLElement.Builder threadPoolBuilder = this.factory.createBuilder(ThreadPoolResourceDefinitionRegistrar.DEFAULT).withElementLocalName(ResourceXMLElementLocalName.VALUE_KEY).omitIfEmpty();
        if (!this.since(VERSION_6_0)) {
            threadPoolBuilder.ignoreAttribute("queue-length");
        }
        builder.appendChild(threadPoolBuilder.build());

        if (!this.since(JGroupsSubsystemSchema.VERSION_5_0)) {
            // Set default port_range
            String portRangeProperty = "port_range";
            builder.withOperationTransformation(new UnaryOperator<>() {
                @Override
                public ModelNode apply(ModelNode operation) {
                    if (!operation.hasDefined(ProtocolChildResourceDescription.PROPERTIES.getName(), portRangeProperty)) {
                        operation.get(ProtocolChildResourceDescription.PROPERTIES.getName()).get(portRangeProperty).set("50");
                    }
                    return operation;
                }
            });
        }
        return builder.build();
    }

    private ResourceXMLChoice createProtocolChoice() {
        ResourceXMLElement.Builder builder = this.createProtocolElementBuilder(ProtocolResourceDescription.INSTANCE);
        List<ResourceXMLElement> overrides = new LinkedList<>();
        if (this.since(JGroupsSubsystemSchema.VERSION_5_0)) {
            List<ResourceXMLElement> tokens = new ArrayList<>(3);
            tokens.add(this.factory.createBuilder(CipherAuthTokenResourceDescription.INSTANCE).withElementLocalName(ResourceXMLElementLocalName.VALUE_KEY).build());
            tokens.add(this.factory.createBuilder(DigestAuthTokenResourceDescription.INSTANCE).withElementLocalName(ResourceXMLElementLocalName.VALUE_KEY).build());
            tokens.add(this.factory.createBuilder(PlainAuthTokenResourceDescription.INSTANCE).withElementLocalName(ResourceXMLElementLocalName.VALUE_KEY).build());
            for (AuthProtocolResourceDescription protocol : EnumSet.allOf(AuthProtocolResourceDescription.class)) {
                overrides.add(this.createProtocolElementBuilder(protocol).withElementLocalName("auth-protocol")
                        .insertChild(XMLChoice.of(tokens, XMLCardinality.Single.REQUIRED))
                        .build());
            }
            for (EncryptProtocolResourceDescription protocol : EnumSet.allOf(EncryptProtocolResourceDescription.class)) {
                overrides.add(this.createProtocolElementBuilder(protocol).withElementLocalName("encrypt-protocol").build());
            }
            for (JDBCProtocolResourceDescription protocol : EnumSet.allOf(JDBCProtocolResourceDescription.class)) {
                overrides.add(this.createProtocolElementBuilder(protocol).withElementLocalName("jdbc-protocol").build());
            }
            for (MulticastProtocolResourceDescription protocol : EnumSet.allOf(MulticastProtocolResourceDescription.class)) {
                overrides.add(this.createProtocolElementBuilder(protocol).withElementLocalName("socket-protocol").build());
            }
            for (SocketDiscoveryProtocolResourceDescription protocol : EnumSet.allOf(SocketDiscoveryProtocolResourceDescription.class)) {
                overrides.add(this.createProtocolElementBuilder(protocol).withElementLocalName("socket-discovery-protocol").build());
            }
            for (SocketProtocolResourceDescription protocol : EnumSet.allOf(SocketProtocolResourceDescription.class)) {
                ResourceXMLElement.Builder overrideBuilder = this.createProtocolElementBuilder(protocol).withElementLocalName("socket-protocol");
                if (!this.since(VERSION_7_0)) {
                    overrideBuilder.excludeAttribute(SocketProtocolResourceDescription.SocketBindingAttribute.CLIENT.get());
                }
                overrides.add(overrideBuilder.build());
            }
        } else {
            builder.excludeAttribute(ProtocolChildResourceDescription.STATISTICS_ENABLED);
            builder.includeAttributes(Set.of(JDBCProtocolResourceDescription.DATA_SOURCE, SocketProtocolResourceDescription.SocketBindingAttribute.SERVER.get()));
            if (!this.since(VERSION_3_0)) {
                builder.excludeAttribute(ProtocolChildResourceDescription.MODULE);
            }
        }
        return builder.build(overrides);
    }

    private ResourceXMLElement.Builder createProtocolElementBuilder(ResourceDescription description) {
        return this.factory.createBuilder(description).withPathValueAttributeLocalName("type");
    }

    private ResourceXMLElement createRemoteSiteElement() {
        ResourceXMLElement.Builder builder = this.factory.createBuilder(RemoteSiteResourceDescription.INSTANCE);
        if (!this.since(VERSION_3_0)) {
            AttributeDefinition stack = new SimpleAttributeDefinitionBuilder("stack", ModelType.STRING).build();
            AttributeDefinition cluster = new SimpleAttributeDefinitionBuilder("cluster", ModelType.STRING).build();
            builder.includeAttributes(Set.of(stack, cluster)).excludeAttribute(RemoteSiteResourceDescription.CHANNEL_CONFIGURATION);
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
                    operation.get(RemoteSiteResourceDescription.CHANNEL_CONFIGURATION.getName()).set(channel);

                    // Create a corresponding channel add operation
                    PathAddress channelAddress = PathAddress.pathAddress(JGroupsSubsystemResourceDescription.INSTANCE.getPathElement()).append(ChannelResourceDescription.pathElement(channel.asString()));
                    ModelNode channelOperation = Util.createAddOperation(channelAddress);
                    channelOperation.get(ChannelResourceDescription.STACK.getName()).set(stackName);
                    operations.put(channelAddress, channelOperation);
                }
            });
        }
        return builder.build();
    }
}
