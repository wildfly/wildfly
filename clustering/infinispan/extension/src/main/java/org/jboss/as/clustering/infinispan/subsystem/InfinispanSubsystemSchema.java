/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.clustering.controller.ResourceDescription;
import org.jboss.as.clustering.controller.persistence.xml.ResourceXMLElement;
import org.jboss.as.clustering.controller.persistence.xml.ResourceXMLElementLocalName;
import org.jboss.as.clustering.controller.persistence.xml.SubsystemResourceXMLSchema;
import org.jboss.as.clustering.controller.xml.XMLCardinality;
import org.jboss.as.clustering.controller.xml.XMLChoice;
import org.jboss.as.clustering.controller.xml.XMLElement;
import org.jboss.as.clustering.infinispan.subsystem.remote.ClientThreadPool;
import org.jboss.as.clustering.infinispan.subsystem.remote.ConnectionPoolResourceDescription;
import org.jboss.as.clustering.infinispan.subsystem.remote.RemoteCacheContainerResourceDescription;
import org.jboss.as.clustering.infinispan.subsystem.remote.RemoteClusterResourceDescription;
import org.jboss.as.clustering.infinispan.subsystem.remote.SecurityResourceDescription;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeParsers.AttributeElementParser;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SubsystemSchema;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.xml.VersionedNamespace;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.staxmapper.IntVersion;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * Enumeration of the supported subsystem xml schemas.
 * @author Paul Ferraro
 */
public enum InfinispanSubsystemSchema implements SubsystemResourceXMLSchema<InfinispanSubsystemSchema> {
/*  Unsupported, for documentation purposes only.
    VERSION_1_0(1, 0), // AS 7.0
    VERSION_1_1(1, 1), // AS 7.1.0
    VERSION_1_2(1, 2), // AS 7.1.1
    VERSION_1_3(1, 3), // AS 7.1.2
    VERSION_1_4(1, 4), // AS 7.2.0
*/
    VERSION_1_5(1, 5), // EAP 6.3
    VERSION_2_0(2, 0), // WildFly 8
    VERSION_3_0(3, 0), // WildFly 9
    VERSION_4_0(4, 0), // WildFly 10/11
    VERSION_5_0(5, 0), // WildFly 12
    VERSION_6_0(6, 0), // WildFly 13
    VERSION_7_0(7, 0), // WildFly 14-15
    VERSION_8_0(8, 0), // WildFly 16
    VERSION_9_0(9, 0), // WildFly 17-19
    VERSION_9_1(9, 1), // EAP 7.3.4
    VERSION_10_0(10, 0), // WildFly 20
    VERSION_11_0(11, 0), // WildFly 21
    VERSION_12_0(12, 0), // WildFly 23, EAP 7.4
    VERSION_13_0(13, 0), // WildFly 24-26
    VERSION_14_0(14, 0), // WildFly 27-present
    ;
    static final InfinispanSubsystemSchema CURRENT = VERSION_14_0;

    private final ResourceXMLElement.Builder.Factory factory = ResourceXMLElement.Builder.Factory.newInstance(this);
    private final VersionedNamespace<IntVersion, InfinispanSubsystemSchema> namespace;

    InfinispanSubsystemSchema(int major, int minor) {
        this.namespace = SubsystemSchema.createLegacySubsystemURN(InfinispanSubsystemResourceDescription.INSTANCE.getName(), new IntVersion(major, minor));
    }

    @Override
    public VersionedNamespace<IntVersion, InfinispanSubsystemSchema> getNamespace() {
        return this.namespace;
    }

    @Override
    public ResourceXMLElement getSubsystemResourceXMLElement() {
        return this.factory.createBuilder(InfinispanSubsystemResourceDescription.INSTANCE)
                .appendChild(this.createCacheContainerElement())
                .appendChild(this.createRemoteCacheContainerElement())
                .build();
    }

    ResourceXMLElement createCacheContainerElement() {
        ResourceXMLElement.Builder builder = this.factory.createBuilder(CacheContainerResourceDescription.INSTANCE);
        if (!this.since(VERSION_13_0)) {
            builder.excludeAttribute(CacheContainerResourceDescription.MARSHALLER);
            if (!this.since(VERSION_12_0)) {
                builder.withLocalNames(Map.of(CacheContainerResourceDescription.MODULES, ModelDescriptionConstants.MODULE));
                if (!this.since(VERSION_5_0)) {
                    builder.ignoreAttribute("jndi-name");
                    if (!this.since(VERSION_4_0)) {
                        builder.ignoreAttributes(Set.of("listener-executor", "eviction-executor", "replication-queue-executor"));
                        if (!this.since(VERSION_3_0)) {
                            builder.ignoreAttribute("start");
                        }
                    }
                }
            }
        }

        ResourceXMLElement.Builder transportBuilder = this.factory.createBuilder(JGroupsTransportResourceDescription.INSTANCE).withElementLocalName(ResourceXMLElementLocalName.KEY);
        if (!this.since(VERSION_4_0)) {
            transportBuilder.ignoreAttribute("executor");
            if (!this.since(VERSION_3_0)) {
                AttributeDefinition stack = new SimpleAttributeDefinitionBuilder("stack", ModelType.STRING, true).build();
                AttributeDefinition cluster = new SimpleAttributeDefinitionBuilder("cluster", ModelType.STRING, true).build();
                transportBuilder.includeAttributes(Set.of(stack, cluster));
                transportBuilder.excludeAttribute(JGroupsTransportResourceDescription.CHANNEL_FACTORY);
                transportBuilder.withOperationTransformation(new BiConsumer<>() {
                    @Override
                    public void accept(Map<PathAddress, ModelNode> operations, PathAddress operationKey) {
                        ModelNode operation = operations.get(operationKey);
                        String stackName = Optional.ofNullable(operation.remove(stack.getName())).map(ModelNode::asString).orElse(null);
                        String clusterName = Optional.ofNullable(operation.remove(cluster.getName())).map(ModelNode::asString).orElse(null);
                        // We need to create a corresponding channel add operation
                        String channel = "ee-" + operationKey.getParent().getLastElement().getValue();
                        operation.get(JGroupsTransportResourceDescription.CHANNEL_FACTORY.getName()).set(channel);
                        PathAddress subsystemAddress = PathAddress.pathAddress(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, "jgroups"));
                        PathAddress channelAddress = subsystemAddress.append(PathElement.pathElement("channel", channel));
                        ModelNode channelOperation = Util.createAddOperation(channelAddress);
                        if (stackName != null) {
                            channelOperation.get("stack").set(stackName);
                        }
                        if (clusterName != null) {
                            channelOperation.get("cluster").set(clusterName);
                        }
                        operations.put(channelAddress, channelOperation);
                    }
                });
            }
        }
        builder.appendChild(transportBuilder.build());

        if (this.since(VERSION_11_0)) {
            builder.appendChild(this.factory.createBuilder(ThreadPool.BLOCKING).withElementLocalName(ResourceXMLElementLocalName.VALUE_KEY).omitIfEmpty().build());
        }
        if (!this.since(VERSION_14_0)) {
            builder.withOperationTransformation(new AttributeOverrideOperationTransformer(Set.of(CacheContainerResourceDescription.MARSHALLER)));
            builder.appendChild(XMLElement.ignore(this.factory.resolveQName("async-operations-thread-pool")));
        }
        builder.appendChild(this.factory.createBuilder(ThreadPool.LISTENER).withElementLocalName(ResourceXMLElementLocalName.VALUE_KEY).omitIfEmpty().build());
        if (this.since(VERSION_11_0)) {
            builder.appendChild(this.factory.createBuilder(ThreadPool.NON_BLOCKING).withElementLocalName(ResourceXMLElementLocalName.VALUE_KEY).omitIfEmpty().build());
        }
        if (!this.since(VERSION_14_0)) {
            if (this.since(VERSION_10_0)) {
                builder.appendChild(XMLElement.ignore(this.factory.resolveQName("persistence-thread-pool")));
            }
            builder.appendChild(XMLElement.ignore(this.factory.resolveQName("remote-command-thread-pool")));
            builder.appendChild(XMLElement.ignore(this.factory.resolveQName("state-transfer-thread-pool")));
            builder.appendChild(XMLElement.ignore(this.factory.resolveQName("transport-thread-pool")));
        }

        ResourceXMLElement.Builder expirationThreadPoolBuilder = this.factory.createBuilder(ScheduledThreadPool.EXPIRATION).withElementLocalName(ResourceXMLElementLocalName.VALUE_KEY).omitIfEmpty();
        if (!this.since(VERSION_10_0)) {
            expirationThreadPoolBuilder.excludeAttribute(ScheduledThreadPool.EXPIRATION.getMinThreads());
            expirationThreadPoolBuilder.ignoreAttribute("max-threads");
        }
        builder.appendChild(expirationThreadPoolBuilder.build());

        if (!this.since(VERSION_10_0)) {
            builder.appendChild(XMLElement.ignore(this.factory.resolveQName("persistence-thread-pool")));
        }

        builder.appendChild(this.createCacheResourceXMLDescriptionBuilder(LocalCacheResourceDescription.INSTANCE).build());
        builder.appendChild(this.createClusteredCacheResourceXMLDescriptionBuilder(InvalidationCacheResourceDescription.INSTANCE).build());
        builder.appendChild(this.createSharedStateCacheResourceXMLDescriptionBuilder(ReplicatedCacheResourceDescription.INSTANCE).build());
        builder.appendChild(this.createSegmentedCacheResourceXMLDescriptionBuilder(DistributedCacheResourceDescription.INSTANCE).build());
        builder.appendChild(this.createSegmentedCacheResourceXMLDescriptionBuilder(ScatteredCacheResourceDescription.INSTANCE).build());

        builder.withOperationTransformation(new BiConsumer<>() {
            @Override
            public void accept(Map<PathAddress, ModelNode> operations, PathAddress operationKey) {
                PathAddress transportKey = operationKey.append(NoTransportResourceDescription.INSTANCE.getPathKey());
                if (!operations.containsKey(transportKey)) {
                    operations.put(transportKey, Util.createAddOperation(operationKey.append(NoTransportResourceDescription.INSTANCE.getPathElement())));
                }
                for (ThreadPool pool : EnumSet.allOf(ThreadPool.class)) {
                    PathAddress poolAddress = operationKey.append(pool.getPathElement());
                    if (!operations.containsKey(poolAddress)) {
                        operations.put(poolAddress, Util.createAddOperation(poolAddress));
                    }
                }
                for (ScheduledThreadPool pool : EnumSet.allOf(ScheduledThreadPool.class)) {
                    PathAddress poolAddress = operationKey.append(pool.getPathElement());
                    if (!operations.containsKey(poolAddress)) {
                        operations.put(poolAddress, Util.createAddOperation(poolAddress));
                    }
                }
            }
        });
        return builder.build();
    }

    ResourceXMLElement.Builder createSegmentedCacheResourceXMLDescriptionBuilder(ResourceDescription description) {
        ResourceXMLElement.Builder builder = this.createSharedStateCacheResourceXMLDescriptionBuilder(description);
        if (!this.since(VERSION_14_0)) {
            builder.ignoreAttribute("consistent-hash-strategy");
        }
        return builder;
    }

    ResourceXMLElement.Builder createSharedStateCacheResourceXMLDescriptionBuilder(ResourceDescription description) {
        ResourceXMLElement.Builder builder = this.createClusteredCacheResourceXMLDescriptionBuilder(description);
        if (this.since(VERSION_4_0)) {
            ResourceXMLElement.Builder partitionHandlingBuilder = this.factory.createBuilder(PartitionHandlingResourceDescription.INSTANCE).omitIfEmpty();
            if (!this.since(VERSION_14_0)) {
                partitionHandlingBuilder.excludeAttributes(Set.of(PartitionHandlingResourceDescription.MERGE_POLICY, PartitionHandlingResourceDescription.WHEN_SPLIT));
                partitionHandlingBuilder.includeAttribute(PartitionHandlingResourceDescription.DeprecatedAttribute.ENABLED.get());
            }
            builder.appendChild(partitionHandlingBuilder.build());
        }

        ResourceXMLElement.Builder stateTransferBuilder = this.factory.createBuilder(StateTransferResourceDescription.INSTANCE).omitIfEmpty();
        if (!this.since(VERSION_4_0)) {
            stateTransferBuilder.ignoreAttribute("enabled");
        }
        builder.appendChild(stateTransferBuilder.build());

        builder.appendChild(this.factory.createBuilder(BackupSitesResourceDescription.INSTANCE).omitIfEmpty()
                .appendChild(this.factory.createBuilder(BackupSiteResourceDescription.INSTANCE).withPathValueAttributeLocalName("site").build())
                .build());
        if (this.since(VERSION_2_0) && !this.since(VERSION_5_0)) {
            builder.appendChild(XMLElement.ignore(this.factory.resolveQName("backup-for")));
        }

        return builder.withOperationTransformation(new BiConsumer<>() {
            @Override
            public void accept(Map<PathAddress, ModelNode> operations, PathAddress operationKey) {
                PathAddress partitionHandlingAddress = operationKey.append(PartitionHandlingResourceDescription.INSTANCE.getPathElement());
                if (!operations.containsKey(partitionHandlingAddress)) {
                    operations.put(partitionHandlingAddress, Util.createAddOperation(partitionHandlingAddress));
                }
                PathAddress stateTransferAddress = operationKey.append(StateTransferResourceDescription.INSTANCE.getPathElement());
                if (!operations.containsKey(stateTransferAddress)) {
                    operations.put(stateTransferAddress, Util.createAddOperation(stateTransferAddress));
                }
                PathAddress backupSitesAddress = operationKey.append(BackupSitesResourceDescription.INSTANCE.getPathElement());
                if (!operations.containsKey(backupSitesAddress)) {
                    operations.put(backupSitesAddress, Util.createAddOperation(backupSitesAddress));
                }
            }
        });
    }

    ResourceXMLElement.Builder createClusteredCacheResourceXMLDescriptionBuilder(ResourceDescription description) {
        ResourceXMLElement.Builder builder = this.createCacheResourceXMLDescriptionBuilder(description);
        if (!InfinispanSubsystemSchema.this.since(VERSION_5_0)) {
            builder.ignoreAttributes(Set.of("mode", "queue-size", "queue-flush-interval"));
            if (!InfinispanSubsystemSchema.this.since(VERSION_4_0)) {
                builder.ignoreAttribute("async-marshalling");
            }
        }
        return builder;
    }

    ResourceXMLElement.Builder createCacheResourceXMLDescriptionBuilder(ResourceDescription description) {
        AttributeDefinition batching = this.since(VERSION_3_0) ? null : new SimpleAttributeDefinitionBuilder("batching", ModelType.BOOLEAN).setRequired(false).setDefaultValue(ModelNode.FALSE).build();
        ResourceXMLElement.Builder builder = this.factory.createBuilder(description);
        if (!InfinispanSubsystemSchema.this.since(VERSION_5_0)) {
            builder.ignoreAttribute("jndi-name");
            if (!InfinispanSubsystemSchema.this.since(VERSION_3_0)) {
                builder.ignoreAttribute("start");
            }
            if (batching != null) {
                builder.includeAttribute(batching);
            }
        }
        if (batching != null) {
            builder.withOperationTransformation(new BiConsumer<>() {
                @Override
                public void accept(Map<PathAddress, ModelNode> operations, PathAddress operationKey) {
                    ModelNode operation = operations.get(operationKey);
                    ModelNode value = operation.remove(batching.getName());
                    if (value != null) {
                        ModelNode transactionOperation = operations.get(operationKey.append(TransactionResourceDescription.INSTANCE.getPathElement()));
                        transactionOperation.get(TransactionResourceDescription.MODE.getName()).set(new ModelNode(TransactionMode.BATCH.name()));
                    }
                }
            });
        }
        if (!this.since(VERSION_12_0)) {
            builder.withLocalNames(Map.of(CacheResourceDescription.MODULES, ModelDescriptionConstants.MODULE));
        }
        builder.appendChild(this.factory.createBuilder(LockingResourceDescription.INSTANCE).omitIfEmpty().build());

        ResourceXMLElement.Builder transactionBuilder = this.factory.createBuilder(TransactionResourceDescription.INSTANCE).omitIfEmpty();
        if (!this.since(VERSION_13_0)) {
            transactionBuilder.excludeAttribute(TransactionResourceDescription.COMPLETE_TIMEOUT);
        }
        builder.appendChild(transactionBuilder.build());

        List<ResourceXMLElement> memoryElements = new LinkedList<>();

        ResourceXMLElement.Builder heapMemoryBuilder = this.factory.createBuilder(HeapMemoryResourceDescription.INSTANCE).omitIfEmpty();
        if (this.since(VERSION_11_0)) {
            heapMemoryBuilder.withElementLocalName(ResourceXMLElementLocalName.VALUE_KEY);
        } else {
            heapMemoryBuilder.excludeAttribute(HeapMemoryResourceDescription.INSTANCE.getSizeUnitAttribute());
            if (this.since(VERSION_5_0)) {
                heapMemoryBuilder.withElementLocalName("object-memory");
            } else {
                heapMemoryBuilder.withElementLocalName("eviction").withLocalNames(Map.of(MemoryResourceDescription.Attribute.SIZE.get(), "max-entries"));
                heapMemoryBuilder.ignoreAttribute("strategy");
            }
        }
        memoryElements.add(heapMemoryBuilder.build());

        if (this.since(VERSION_5_0)) {
            ResourceXMLElement.Builder memoryBuilder = this.factory.createBuilder(OffHeapMemoryResourceDescription.INSTANCE).withElementLocalName(ResourceXMLElementLocalName.VALUE_KEY);
            if (!this.since(VERSION_14_0)) {
                memoryBuilder.ignoreAttribute("capacity");
                if (!this.since(VERSION_11_0)) {
                    memoryBuilder.excludeAttribute(OffHeapMemoryResourceDescription.INSTANCE.getSizeUnitAttribute());
                    memoryBuilder.ignoreAttribute("eviction-type");
                }
            }
            memoryElements.add(memoryBuilder.build());

            if (!this.since(VERSION_11_0)) {
                memoryElements.add(this.factory.createBuilder(OffHeapMemoryResourceDescription.INSTANCE).withElementLocalName("binary-memory")
                        .excludeAttribute(OffHeapMemoryResourceDescription.INSTANCE.getSizeUnitAttribute())
                        .ignoreAttribute("eviction-type")
                        .build());
            }
        }
        builder.appendChild(XMLChoice.of(memoryElements, XMLCardinality.Single.OPTIONAL));

        builder.appendChild(this.factory.createBuilder(ExpirationResourceDescription.INSTANCE).omitIfEmpty().build());

        List<ResourceXMLElement> storeElements = new LinkedList<>();
        storeElements.add(this.createStoreElementBuilder(new CustomStoreResourceDescription<>()).withElementLocalName(ResourceXMLElementLocalName.KEY).build());
        storeElements.add(this.createStoreElementBuilder(FileStoreResourceDescription.INSTANCE).build());
        storeElements.add(this.createStoreElementBuilder(HotRodStoreResourceDescription.INSTANCE).build());
        storeElements.add(this.createStoreElementBuilder(RemoteStoreResourceDescription.INSTANCE).withLocalNames(Map.of(RemoteStoreResourceDescription.SOCKET_BINDINGS, InfinispanSubsystemSchema.this.since(VERSION_4_0) ? RemoteStoreResourceDescription.SOCKET_BINDINGS.getXmlName() : "remote-server")).withParsers(Map.of(RemoteStoreResourceDescription.SOCKET_BINDINGS, InfinispanSubsystemSchema.this.since(VERSION_4_0) ? RemoteStoreResourceDescription.SOCKET_BINDINGS.getParser() : new AttributeElementParser("remote-server") {
            @Override
            public void parseElement(AttributeDefinition attribute, XMLExtendedStreamReader reader, ModelNode model) throws XMLStreamException {
                ParseUtils.requireSingleAttribute(reader, "outbound-socket-binding");
                String name = reader.getAttributeValue(0);
                model.get(attribute.getName()).add(name);
                ParseUtils.requireNoContent(reader);
            }
        })).build());

        storeElements.add(this.createJDBCStoreResourceXMLDescriptionBuilder(JDBCStoreResourceDescription.INSTANCE).withElementLocalName(ResourceXMLElementLocalName.VALUE_KEY)
                .appendChild(this.createTableResourceXMLDescriptionBuilder().withElementLocalName(ResourceXMLElementLocalName.KEY).build())
                .build());

        if (!this.since(VERSION_14_0)) {
            builder.appendChild(this.createJDBCStoreResourceXMLDescriptionBuilder(JDBCStoreResourceDescription.INSTANCE).withElementLocalName("binary-keyed-jdbc-store")
                    .appendChild(this.createTableResourceXMLDescriptionBuilder().withElementLocalName("binary-keyed-table").build())
                    .build());
            builder.appendChild(this.createJDBCStoreResourceXMLDescriptionBuilder(JDBCStoreResourceDescription.INSTANCE).withElementLocalName("mixed-keyed-jdbc-store")
                    .appendChild(this.createTableResourceXMLDescriptionBuilder().withElementLocalName("binary-keyed-table").build())
                    .appendChild(this.createTableResourceXMLDescriptionBuilder().withElementLocalName("string-keyed-table").build())
                    .build());
        }
        builder.appendChild(XMLChoice.of(storeElements, XMLCardinality.Single.OPTIONAL));

        if (!this.since(VERSION_4_0)) {
            builder.appendChild(XMLElement.ignore(this.factory.resolveQName("indexing")));
        }
        return builder.withOperationTransformation(new BiConsumer<>() {
            @Override
            public void accept(Map<PathAddress, ModelNode> operations, PathAddress operationKey) {
                PathAddress transactionAddress = operationKey.append(TransactionResourceDescription.INSTANCE.getPathElement());
                if (!operations.containsKey(transactionAddress)) {
                    operations.put(transactionAddress, Util.createAddOperation(transactionAddress));
                }
                PathAddress memoryKey = operationKey.append(HeapMemoryResourceDescription.INSTANCE.getPathKey());
                if (!operations.containsKey(memoryKey)) {
                    operations.put(memoryKey, Util.createAddOperation(operationKey.append(HeapMemoryResourceDescription.INSTANCE.getPathElement())));
                }
                PathAddress expirationAddress = operationKey.append(ExpirationResourceDescription.INSTANCE.getPathElement());
                if (!operations.containsKey(expirationAddress)) {
                    operations.put(expirationAddress, Util.createAddOperation(expirationAddress));
                }
                PathAddress storeKey = operationKey.append(NoStoreResourceDescription.INSTANCE.getPathKey());
                if (!operations.containsKey(storeKey)) {
                    operations.put(storeKey, Util.createAddOperation(operationKey.append(NoStoreResourceDescription.INSTANCE.getPathElement())));
                }
            }
        });
    }

    ResourceXMLElement.Builder createJDBCStoreResourceXMLDescriptionBuilder(ResourceDescription description) {
        ResourceXMLElement.Builder builder = this.createStoreElementBuilder(JDBCStoreResourceDescription.INSTANCE);
        if (!this.since(VERSION_4_0)) {
            if (!this.since(VERSION_2_0)) {
                builder.excludeAttribute(JDBCStoreResourceDescription.DIALECT);
            }
            builder.withLocalNames(Map.of(JDBCStoreResourceDescription.DATA_SOURCE, "datasource"));
            builder.withOperationTransformation(new UnaryOperator<>() {
                @Override
                public ModelNode apply(ModelNode operation) {
                    // Attempt to convert jndi name to data source name
                    String jndiName = operation.get("datasource").asString();
                    String dataSourceName = jndiName.substring(jndiName.lastIndexOf('/') + 1);
                    operation.get(JDBCStoreResourceDescription.DATA_SOURCE.getName()).set(dataSourceName);
                    return operation;
                }
            });
        }
        return builder.withOperationTransformation(new BiConsumer<>() {
            @Override
            public void accept(Map<PathAddress, ModelNode> operations, PathAddress operationKey) {
                ModelNode operation = operations.get(operationKey);
                PathAddress address = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR));
                PathAddress tableKey = operationKey.append(TableResourceDescription.INSTANCE.getPathKey());
                if (!operations.containsKey(tableKey)) {
                    operations.put(tableKey, Util.createAddOperation(address.append(TableResourceDescription.INSTANCE.getPathElement())));
                }
            }
        });
    }

    ResourceXMLElement.Builder createTableResourceXMLDescriptionBuilder() {
        ResourceXMLElement.Builder builder = this.factory.createBuilder(TableResourceDescription.INSTANCE);
        if (!this.since(VERSION_9_0)) {
            builder.excludeAttributes(Set.of(TableResourceDescription.Attribute.CREATE_ON_START.get(), TableResourceDescription.Attribute.DROP_ON_STOP.get()));
            if (!this.since(VERSION_5_0)) {
                builder.ignoreAttribute("batch-size");
            }
        }
        return builder;
    }

    ResourceXMLElement.Builder createStoreElementBuilder(ResourceDescription description) {
        ResourceXMLElement.Builder builder = this.factory.createBuilder(description).withElementLocalName(ResourceXMLElementLocalName.VALUE_KEY);
        if (!this.since(VERSION_14_0)) {
            builder.excludeAttribute(StoreResourceDescription.Attribute.SEGMENTED.get());
            builder.ignoreAttribute("singleton");
        }
        ResourceXMLElement.Builder writeBehindBuilder = this.factory.createBuilder(StoreWriteBehindResourceDescription.INSTANCE).withElementLocalName(ResourceXMLElementLocalName.KEY_VALUE);
        if (!this.since(VERSION_14_0)) {
            writeBehindBuilder.ignoreAttribute("thread-pool-size");
            if (!this.since(VERSION_4_0)) {
                writeBehindBuilder.ignoreAttributes(Set.of("flush-lock-timeout", "shutdown-timeout"));
            }
        }
        builder.insertChild(writeBehindBuilder.build());

        if (!this.since(VERSION_14_0)) {
            // Apply defaults from legacy schema
            builder.withOperationTransformation(new AttributeOverrideOperationTransformer(Map.of(StoreResourceDescription.Attribute.PASSIVATION.get(), ModelNode.TRUE, StoreResourceDescription.Attribute.PURGE.get(), ModelNode.TRUE)));
        }
        return builder.withOperationTransformation(new BiConsumer<>() {
            @Override
            public void accept(Map<PathAddress, ModelNode> operations, PathAddress operationKey) {
                ModelNode operation = operations.get(operationKey);
                PathAddress address = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR));
                PathAddress writeKey = operationKey.append(StoreWriteThroughResourceDescription.INSTANCE.getPathKey());
                if (!operations.containsKey(writeKey)) {
                    operations.put(writeKey, Util.createAddOperation(address.append(StoreWriteThroughResourceDescription.INSTANCE.getPathElement())));
                }
            }
        });
    }

    ResourceXMLElement createRemoteCacheContainerElement() {
        ResourceXMLElement.Builder builder = this.factory.createBuilder(RemoteCacheContainerResourceDescription.INSTANCE);
        if (!this.since(VERSION_13_0)) {
            builder.excludeAttribute(RemoteCacheContainerResourceDescription.MARSHALLER);
        }
        if (!this.since(InfinispanSubsystemSchema.VERSION_11_0) && (!this.since(InfinispanSubsystemSchema.VERSION_9_1) || this.since(InfinispanSubsystemSchema.VERSION_10_0))) {
            builder.excludeAttribute(RemoteCacheContainerResourceDescription.PROPERTIES);
        }
        if (!this.since(VERSION_12_0)) {
            builder.withLocalNames(Map.of(RemoteCacheContainerResourceDescription.MODULES, ModelDescriptionConstants.MODULE));
        }
        for (ClientThreadPool pool : EnumSet.allOf(ClientThreadPool.class)) {
            builder.insertChild(this.factory.createBuilder(pool).withElementLocalName(ResourceXMLElementLocalName.VALUE_KEY).omitIfEmpty().build());
        }
        builder.insertChild(this.factory.createBuilder(ConnectionPoolResourceDescription.INSTANCE).omitIfEmpty().build());
        builder.appendChild(XMLElement.wrap(this.factory.resolveQName("remote-clusters"), this.factory.createBuilder(RemoteClusterResourceDescription.INSTANCE).build()));
        builder.appendChild(this.factory.createBuilder(SecurityResourceDescription.INSTANCE).omitIfEmpty().build());

        if (!this.since(VERSION_14_0)) {
            builder.withOperationTransformation(new AttributeOverrideOperationTransformer(Set.of(RemoteCacheContainerResourceDescription.MARSHALLER)));
            builder.appendChild(XMLElement.ignore(this.factory.resolveQName("invalidation-near-cache")));
            builder.appendChild(XMLElement.ignore(this.factory.resolveQName("transaction")));
        }
        return builder.withOperationTransformation(new BiConsumer<>() {
            @Override
            public void accept(Map<PathAddress, ModelNode> operations, PathAddress operationKey) {
                for (ClientThreadPool pool : EnumSet.allOf(ClientThreadPool.class)) {
                    PathAddress poolAddress = operationKey.append(pool.getPathElement());
                    if (!operations.containsKey(poolAddress)) {
                        operations.put(poolAddress, Util.createAddOperation(poolAddress));
                    }
                }
                PathAddress connectionPoolAddress = operationKey.append(ConnectionPoolResourceDescription.INSTANCE.getPathKey());
                if (!operations.containsKey(connectionPoolAddress)) {
                    operations.put(connectionPoolAddress, Util.createAddOperation(connectionPoolAddress));
                }
                PathAddress securityAddress = operationKey.append(SecurityResourceDescription.INSTANCE.getPathKey());
                if (!operations.containsKey(securityAddress)) {
                    operations.put(securityAddress, Util.createAddOperation(securityAddress));
                }
            }
        }).build();
    }

    private static class AttributeOverrideOperationTransformer implements UnaryOperator<ModelNode> {
        private final Map<AttributeDefinition, ModelNode> overrides;

        AttributeOverrideOperationTransformer(Set<AttributeDefinition> attributes) {
            this.overrides = attributes.stream().collect(Collectors.toMap(Function.identity(), AttributeDefinition::getDefaultValue));
        }

        AttributeOverrideOperationTransformer(Map<AttributeDefinition, ModelNode> overrides) {
            this.overrides = overrides;
        }

        @Override
        public ModelNode apply(ModelNode operation) {
            for (Map.Entry<AttributeDefinition, ModelNode> override : this.overrides.entrySet()) {
                String attributeName = override.getKey().getName();
                if (!operation.hasDefined(attributeName)) {
                    operation.get(attributeName).set(override.getValue());
                }
            }
            return operation;
        }
    }
}
