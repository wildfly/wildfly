/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import org.jboss.as.clustering.infinispan.subsystem.remote.ClientThreadPool;
import org.jboss.as.clustering.infinispan.subsystem.remote.ConnectionPoolResourceDefinitionRegistrar;
import org.jboss.as.clustering.infinispan.subsystem.remote.RemoteCacheContainerResourceDefinitionRegistrar;
import org.jboss.as.clustering.infinispan.subsystem.remote.RemoteClusterResourceDefinitionRegistrar;
import org.jboss.as.clustering.infinispan.subsystem.remote.RemoteComponentResourceRegistration;
import org.jboss.as.clustering.infinispan.subsystem.remote.SecurityResourceDefinitionRegistrar;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SubsystemResourceRegistration;
import org.jboss.as.controller.SubsystemSchema;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.persistence.xml.NamedResourceRegistrationXMLElement;
import org.jboss.as.controller.persistence.xml.ResourceRegistrationXMLElement;
import org.jboss.as.controller.persistence.xml.ResourceXMLAll;
import org.jboss.as.controller.persistence.xml.ResourceXMLChoice;
import org.jboss.as.controller.persistence.xml.ResourceXMLElement;
import org.jboss.as.controller.persistence.xml.ResourceXMLElementLocalName;
import org.jboss.as.controller.persistence.xml.ResourceXMLParticleFactory;
import org.jboss.as.controller.persistence.xml.ResourceXMLSequence;
import org.jboss.as.controller.persistence.xml.SingletonResourceRegistrationXMLChoice;
import org.jboss.as.controller.persistence.xml.SingletonResourceRegistrationXMLElement;
import org.jboss.as.controller.persistence.xml.SubsystemResourceRegistrationXMLElement;
import org.jboss.as.controller.persistence.xml.SubsystemResourceXMLSchema;
import org.jboss.as.controller.xml.VersionedNamespace;
import org.jboss.as.controller.xml.XMLCardinality;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.staxmapper.IntVersion;
import org.wildfly.subsystem.resource.AttributeDefinitionProvider;

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
    VERSION_14_0(14, 0), // WildFly 27-35
    VERSION_15_0(15, 0), // WildFly 36-present
    ;
    static final InfinispanSubsystemSchema CURRENT = VERSION_15_0;

    private final ResourceXMLParticleFactory factory = ResourceXMLParticleFactory.newInstance(this);
    private final VersionedNamespace<IntVersion, InfinispanSubsystemSchema> namespace;

    InfinispanSubsystemSchema(int major, int minor) {
        this.namespace = SubsystemSchema.createLegacySubsystemURN(InfinispanSubsystemResourceDefinitionRegistrar.REGISTRATION.getName(), new IntVersion(major, minor));
    }

    @Override
    public VersionedNamespace<IntVersion, InfinispanSubsystemSchema> getNamespace() {
        return this.namespace;
    }

    @Override
    public SubsystemResourceRegistrationXMLElement getSubsystemXMLElement() {
        ResourceXMLSequence content = this.factory.sequence()
                .addElement(this.cacheContainerElement())
                .addElement(this.remoteCacheContainerElement())
                .build();
        return this.factory.subsystemElement(InfinispanSubsystemResourceDefinitionRegistrar.REGISTRATION).withContent(content).build();
    }

    private ResourceRegistrationXMLElement cacheContainerElement() {
        NamedResourceRegistrationXMLElement.Builder builder = this.factory.namedElement(CacheContainerResourceDefinitionRegistrar.REGISTRATION)
                .addAttributes(List.of(CacheContainerResourceDefinitionRegistrar.DEFAULT_CACHE, CacheContainerResourceDefinitionRegistrar.ALIASES, CacheContainerResourceDefinitionRegistrar.MODULES, CacheContainerResourceDefinitionRegistrar.STATISTICS_ENABLED))
                .withCardinality(XMLCardinality.Unbounded.OPTIONAL)
                ;
        if (this.since(VERSION_13_0)) {
            builder.addAttribute(CacheContainerResourceDefinitionRegistrar.MARSHALLER);
        } else {
            if (!this.since(VERSION_12_0)) {
                builder.withLocalNames(Map.of(CacheContainerResourceDefinitionRegistrar.MODULES, "module"));
                if (!this.since(VERSION_5_0)) {
                    builder.ignoreAttributeLocalNames(Set.of("jndi-name"));
                    if (!this.since(VERSION_4_0)) {
                        builder.ignoreAttributeLocalNames(Set.of("listener-executor", "eviction-executor", "replication-queue-executor"));
                        if (!this.since(VERSION_3_0)) {
                            builder.ignoreAttributeLocalNames(Set.of("start"));
                        }
                    }
                }
            }
        }

        ResourceXMLSequence.Builder contentBuilder = this.factory.sequence();

        contentBuilder.addChoice(this.factory.singletonElementChoice()
                .implyIfEmpty(TransportResourceRegistration.NONE, TransportResourceRegistration.WILDCARD.getPathElement())
                .addElement(this.transportElement())
                .build());

        if (this.since(VERSION_11_0)) {
            contentBuilder.addElement(this.threadPoolElement(ThreadPool.BLOCKING));
        }
        if (!this.since(VERSION_14_0)) {
            contentBuilder.addElement(ResourceXMLElement.ignore(this.factory.resolve("async-operations-thread-pool"), XMLCardinality.Single.OPTIONAL));
        }
        contentBuilder.addElement(this.threadPoolElement(ThreadPool.LISTENER));
        if (this.since(VERSION_11_0)) {
            contentBuilder.addElement(this.threadPoolElement(ThreadPool.NON_BLOCKING));
        }
        if (!this.since(VERSION_14_0)) {
            if (!this.since(VERSION_7_0) || this.since(VERSION_10_0)) {
                contentBuilder.addElement(ResourceXMLElement.ignore(this.factory.resolve("persistence-thread-pool"), XMLCardinality.Single.OPTIONAL));
            }
            contentBuilder.addElement(ResourceXMLElement.ignore(this.factory.resolve("remote-command-thread-pool"), XMLCardinality.Single.OPTIONAL));
            contentBuilder.addElement(ResourceXMLElement.ignore(this.factory.resolve("state-transfer-thread-pool"), XMLCardinality.Single.OPTIONAL));
            contentBuilder.addElement(ResourceXMLElement.ignore(this.factory.resolve("transport-thread-pool"), XMLCardinality.Single.OPTIONAL));
        }

        contentBuilder.addElement(this.scheduledThreadPoolElement(ScheduledThreadPool.EXPIRATION));

        if (this.since(VERSION_7_0) && !this.since(VERSION_10_0)) {
            contentBuilder.addElement(ResourceXMLElement.ignore(this.factory.resolve("persistence-thread-pool"), XMLCardinality.Single.OPTIONAL));
        }

        contentBuilder.addChoice(this.factory.choice()
                .withCardinality(XMLCardinality.Unbounded.OPTIONAL)
                .addElement(this.localCacheElement())
                .addElement(this.invalidationCacheElement())
                .addElement(this.replicatedCacheElement())
                .addElement(this.distributedCacheElement())
                .addElement(this.scatteredCacheElement())
                .build());

        return builder.withContent(contentBuilder.build()).build();
    }

    private ResourceRegistrationXMLElement threadPoolElement(ThreadPoolResourceRegistration<?> pool) {
        return this.threadPoolElementBuilder(pool)
                .addAttributes(List.of(pool.getMinThreads(), pool.getMaxThreads(), pool.getQueueLength(), pool.getKeepAlive()))
                .build();
    }

    private ResourceRegistrationXMLElement scheduledThreadPoolElement(ScheduledThreadPoolResourceRegistration<?> pool) {
        SingletonResourceRegistrationXMLElement.Builder builder = this.threadPoolElementBuilder(pool)
                .addAttribute(pool.getKeepAlive())
                ;
        if (this.since(VERSION_10_0)) {
            builder.addAttribute(pool.getMinThreads());
        } else {
            builder.ignoreAttributeLocalNames(Set.of("max-threads"));
        }
        return builder.build();
    }

    private SingletonResourceRegistrationXMLElement.Builder threadPoolElementBuilder(ScheduledThreadPoolResourceRegistration<?> pool) {
        return this.factory.singletonElement(pool)
                .withElementLocalName(ResourceXMLElementLocalName.VALUE_KEY)
                .withCardinality(XMLCardinality.Single.OPTIONAL)
                .implyIfAbsent()
                ;
    }

    private SingletonResourceRegistrationXMLElement transportElement() {
        SingletonResourceRegistrationXMLElement.Builder builder = this.factory.singletonElement(TransportResourceRegistration.JGROUPS)
                .withElementLocalName(ResourceXMLElementLocalName.KEY)
                .addAttribute(JGroupsTransportResourceDefinitionRegistrar.LOCK_TIMEOUT)
                ;
        if (this.since(VERSION_3_0)) {
            builder.addAttribute(JGroupsTransportResourceDefinitionRegistrar.CHANNEL_FACTORY);
        } else {
            AttributeDefinition stack = new SimpleAttributeDefinitionBuilder("stack", ModelType.STRING, true).build();
            AttributeDefinition cluster = new SimpleAttributeDefinitionBuilder("cluster", ModelType.STRING, true).build();
            builder.addAttributes(List.of(stack, cluster));
            builder.withOperationTransformation(new BiConsumer<>() {
                @Override
                public void accept(Map<PathAddress, ModelNode> operations, PathAddress operationKey) {
                    // Handle stack+cluster -> channel transition
                    ModelNode operation = operations.get(operationKey);
                    String stackName = Optional.ofNullable(operation.remove(stack.getName())).map(ModelNode::asString).orElse(null);
                    String clusterName = Optional.ofNullable(operation.remove(cluster.getName())).map(ModelNode::asString).orElse(null);
                    // We need to create a corresponding channel add operation
                    String channel = "ee-" + operationKey.getParent().getLastElement().getValue();
                    operation.get(JGroupsTransportResourceDefinitionRegistrar.CHANNEL_FACTORY.getName()).set(channel);
                    PathAddress subsystemAddress = PathAddress.pathAddress(SubsystemResourceRegistration.of("jgroups").getPathElement());
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
        if (!this.since(VERSION_4_0)) {
            builder.ignoreAttributeLocalNames(Set.of("executor"));
        }
        return builder.build();
    }

    private ResourceRegistrationXMLElement localCacheElement() {
        return this.cacheElementBuilder(CacheResourceRegistration.LOCAL)
                .withContent(this.cacheElementContentBuilder().build())
                .build();
    }

    private ResourceRegistrationXMLElement invalidationCacheElement() {
        return this.clusteredCacheElementBuilder(CacheResourceRegistration.INVALIDATION)
                .withContent(this.cacheElementContentBuilder().build())
                .build();
    }

    private ResourceRegistrationXMLElement replicatedCacheElement() {
        return this.clusteredCacheElementBuilder(CacheResourceRegistration.REPLICATED)
                .withContent(this.sharedStateCacheElementContentBuilder().build())
                .build();
    }

    private ResourceRegistrationXMLElement distributedCacheElement() {
        return this.segmentedCacheElementBuilder(CacheResourceRegistration.DISTRIBUTED)
                .addAttribute(DistributedCacheResourceDefinitionRegistrar.L1_LIFESPAN)
                .provideAttributes(EnumSet.allOf(DistributedCacheResourceDefinitionRegistrar.Attribute.class))
                .withContent(this.sharedStateCacheElementContentBuilder().build())
                .build();
    }

    private ResourceRegistrationXMLElement scatteredCacheElement() {
        return this.segmentedCacheElementBuilder(CacheResourceRegistration.SCATTERED)
                .addAttributes(List.of(ScatteredCacheResourceDefinitionRegistrar.BIAS_LIFESPAN, ScatteredCacheResourceDefinitionRegistrar.INVALIDATION_BATCH_SIZE))
                .withContent(this.sharedStateCacheElementContentBuilder().build())
                .build();
    }

    private NamedResourceRegistrationXMLElement.Builder segmentedCacheElementBuilder(CacheResourceRegistration description) {
        NamedResourceRegistrationXMLElement.Builder builder = this.clusteredCacheElementBuilder(description)
                .addAttributes(List.of(SegmentedCacheResourceDefinitionRegistrar.SEGMENTS))
                ;
        if (!this.since(VERSION_14_0)) {
            builder.ignoreAttributeLocalNames(Set.of("consistent-hash-strategy"));
        }
        return builder;
    }

    private NamedResourceRegistrationXMLElement.Builder clusteredCacheElementBuilder(CacheResourceRegistration description) {
        NamedResourceRegistrationXMLElement.Builder builder = this.cacheElementBuilder(description).addAttribute(ClusteredCacheResourceDefinitionRegistrar.REMOTE_TIMEOUT);
        if (!InfinispanSubsystemSchema.this.since(VERSION_5_0)) {
            builder.ignoreAttributeLocalNames(Set.of("mode", "queue-size", "queue-flush-interval"));
            if (!InfinispanSubsystemSchema.this.since(VERSION_4_0)) {
                builder.ignoreAttributeLocalNames(Set.of("async-marshalling"));
            }
        }
        return builder;
    }

    private NamedResourceRegistrationXMLElement.Builder cacheElementBuilder(CacheResourceRegistration description) {
        NamedResourceRegistrationXMLElement.Builder builder = this.factory.namedElement(description)
                .addAttributes(List.of(CacheResourceDefinitionRegistrar.MODULES, CacheResourceDefinitionRegistrar.STATISTICS_ENABLED))
                ;
        if (!this.since(VERSION_12_0)) {
            builder.withLocalNames(Map.of(CacheResourceDefinitionRegistrar.MODULES, ModelDescriptionConstants.MODULE));
            if (!this.since(VERSION_5_0)) {
                builder.ignoreAttributeLocalNames(Set.of("jndi-name"));
                if (!this.since(VERSION_3_0)) {
                    builder.ignoreAttributeLocalNames(Set.of("start"));
                    AttributeDefinition batching = new SimpleAttributeDefinitionBuilder("batching", ModelType.BOOLEAN).setRequired(false).setDefaultValue(ModelNode.FALSE).build();
                    builder.addAttribute(batching);
                    builder.withOperationTransformation(new BiConsumer<>() {
                        @Override
                        public void accept(Map<PathAddress, ModelNode> operations, PathAddress operationKey) {
                            ModelNode operation = operations.get(operationKey);
                            ModelNode value = operation.remove(batching.getName());
                            if (value != null) {
                                ModelNode transactionOperation = operations.get(operationKey.append(ComponentResourceRegistration.TRANSACTION.getPathElement()));
                                transactionOperation.get(TransactionResourceDefinitionRegistrar.MODE.getName()).set(new ModelNode(TransactionMode.BATCH.name()));
                            }
                        }
                    });
                }
            }
        }
        return builder;
    }

    private ResourceXMLSequence.Builder sharedStateCacheElementContentBuilder() {
        ResourceXMLSequence.Builder builder = this.cacheElementContentBuilder();

        if (this.since(VERSION_4_0)) {
            builder.addElement(this.partitionHandlingElement());
        }

        builder.addElement(this.stateTransferElement());
        builder.addElement(this.backupSitesElement());

        if (this.since(VERSION_2_0) && !this.since(VERSION_5_0)) {
            builder.addElement(ResourceXMLElement.ignore(this.factory.resolve("backup-for"), XMLCardinality.Single.OPTIONAL));
        }
        return builder;
    }

    private ResourceRegistrationXMLElement partitionHandlingElement() {
        SingletonResourceRegistrationXMLElement.Builder builder = this.factory.singletonElement(ComponentResourceRegistration.PARTITION_HANDLING)
                .withCardinality(XMLCardinality.Single.OPTIONAL)
                .implyIfAbsent()
                ;
        if (this.since(VERSION_14_0)) {
            builder.addAttributes(List.of(PartitionHandlingResourceDefinitionRegistrar.MERGE_POLICY, PartitionHandlingResourceDefinitionRegistrar.WHEN_SPLIT));
        } else {
            builder.addAttribute(PartitionHandlingResourceDefinitionRegistrar.ENABLED);
        }
        return builder.build();
    }

    private ResourceRegistrationXMLElement stateTransferElement() {
        SingletonResourceRegistrationXMLElement.Builder builder = this.factory.singletonElement(ComponentResourceRegistration.STATE_TRANSFER)
                .addAttributes(List.of(StateTransferResourceDefinitionRegistrar.TIMEOUT, StateTransferResourceDefinitionRegistrar.CHUNK_SIZE))
                .withCardinality(XMLCardinality.Single.OPTIONAL)
                .implyIfAbsent()
                ;
        if (!this.since(VERSION_4_0)) {
            builder.ignoreAttributeLocalNames(Set.of("enabled"));
        }
        return builder.build();
    }

    private ResourceRegistrationXMLElement backupSitesElement() {
        ResourceXMLElement takeOfflineElement = this.factory.element(this.factory.resolve("take-offline"))
                .addAttributes(List.of(BackupSiteResourceDefinitionRegistrar.AFTER_FAILURES, BackupSiteResourceDefinitionRegistrar.MIN_WAIT))
                .build();
        NamedResourceRegistrationXMLElement siteElement = this.factory.namedElement(BackupSiteResourceDefinitionRegistrar.REGISTRATION)
                .withCardinality(XMLCardinality.Unbounded.OPTIONAL)
                .addAttributes(List.of(BackupSiteResourceDefinitionRegistrar.FAILURE_POLICY, BackupSiteResourceDefinitionRegistrar.STRATEGY, BackupSiteResourceDefinitionRegistrar.TIMEOUT, BackupSiteResourceDefinitionRegistrar.ENABLED))
                .withResourceAttributeLocalName("site")
                .withContent(this.factory.all().withCardinality(XMLCardinality.Single.OPTIONAL).addElement(takeOfflineElement).build())
                .build();

        return this.factory.singletonElement(ComponentResourceRegistration.BACKUP_SITES)
                .withCardinality(XMLCardinality.Single.OPTIONAL)
                .implyIfAbsent()
                .withContent(this.factory.sequence().addElement(siteElement).build())
                .build();
    }

    private ResourceXMLSequence.Builder cacheElementContentBuilder() {
        ResourceXMLSequence.Builder builder = this.factory.sequence().withCardinality(XMLCardinality.Single.OPTIONAL)
                .addElement(this.lockingElement())
                .addElement(this.transactionElement())
                .addChoice(this.memoryChoice())
                .addElement(this.expirationElement())
                .addChoice(this.storeChoice())
                ;

        if (!this.since(VERSION_4_0)) {
            builder.addElement(ResourceXMLElement.ignore(this.factory.resolve("indexing"), XMLCardinality.Single.OPTIONAL));
        }

        return builder;
    }

    private ResourceRegistrationXMLElement lockingElement() {
        return this.factory.singletonElement(ComponentResourceRegistration.LOCKING)
                .addAttributes(Stream.concat(Stream.of(LockingResourceDefinitionRegistrar.ACQUIRE_TIMEOUT, LockingResourceDefinitionRegistrar.ISOLATION), EnumSet.allOf(LockingResourceDefinitionRegistrar.Attribute.class).stream().map(Supplier::get)).toList())
                .implyIfAbsent()
                .build();
    }

    private ResourceRegistrationXMLElement transactionElement() {
        SingletonResourceRegistrationXMLElement.Builder builder = this.factory.singletonElement(ComponentResourceRegistration.TRANSACTION)
                .addAttributes(List.of(TransactionResourceDefinitionRegistrar.LOCKING, TransactionResourceDefinitionRegistrar.MODE, TransactionResourceDefinitionRegistrar.STOP_TIMEOUT))
                .implyIfAbsent()
                ;
        if (this.since(VERSION_13_0)) {
            builder.addAttribute(TransactionResourceDefinitionRegistrar.COMPLETE_TIMEOUT);
        }
        return builder.build();
    }

    private ResourceXMLChoice memoryChoice() {
        ResourceXMLChoice.Builder builder = this.factory.choice()
                .withCardinality(XMLCardinality.Single.OPTIONAL)
                .addElement(this.heapMemoryElement())
                ;
        if (this.since(VERSION_5_0)) {
            if (!this.since(VERSION_11_0)) {
                builder.addElement(this.binaryMemoryElement());
            }
            builder.addElement(this.offHeapMemoryElement());
        }
        return builder.build();
    }

    private ResourceRegistrationXMLElement heapMemoryElement() {
        SingletonResourceRegistrationXMLElement.Builder builder = this.memoryElementBuilder(MemoryResourceRegistration.HEAP).implyIfAbsent();
        if (this.since(VERSION_11_0)) {
            builder.withElementLocalName(ResourceXMLElementLocalName.VALUE_KEY);
            builder.addAttribute(HeapMemoryResourceDefinitionRegistrar.SIZE_UNIT);
        } else {
            if (this.since(VERSION_5_0)) {
                builder.withElementLocalName("object-memory");
            } else {
                builder.withElementLocalName("eviction");
                builder.withLocalNames(Map.of(MemoryResourceDefinitionRegistrar.SIZE, "max-entries"));
                builder.ignoreAttributeLocalNames(Set.of("strategy"));
            }
        }
        return builder.build();
    }

    private ResourceRegistrationXMLElement binaryMemoryElement() {
        return this.memoryElementBuilder(MemoryResourceRegistration.OFF_HEAP)
                .withElementLocalName("binary-memory")
                .ignoreAttributeLocalNames(Set.of("eviction-type"))
                .build();
    }

    private ResourceRegistrationXMLElement offHeapMemoryElement() {
        SingletonResourceRegistrationXMLElement.Builder builder = this.memoryElementBuilder(MemoryResourceRegistration.OFF_HEAP)
                .withElementLocalName(ResourceXMLElementLocalName.VALUE_KEY)
                .addAttribute(MemoryResourceDefinitionRegistrar.SIZE_UNIT)
                ;
        if (!this.since(VERSION_14_0)) {
            if (!this.since(VERSION_11_0)) {
                builder.ignoreAttributeLocalNames(Set.of("eviction-type"));
            }
            builder.ignoreAttributeLocalNames(Set.of("capacity"));
        }
        return builder.build();
    }

    private SingletonResourceRegistrationXMLElement.Builder memoryElementBuilder(ResourceRegistration registration) {
        return this.factory.singletonElement(registration)
                .addAttribute(MemoryResourceDefinitionRegistrar.SIZE)
                .withOperationKey(MemoryResourceRegistration.WILDCARD.getPathElement())
                ;
    }

    private ResourceRegistrationXMLElement expirationElement() {
        return this.factory.singletonElement(ComponentResourceRegistration.EXPIRATION)
                .provideAttributes(EnumSet.allOf(ExpirationResourceDefinitionRegistrar.Attribute.class))
                .implyIfAbsent()
                .build();
    }

    private ResourceXMLChoice storeChoice() {
        SingletonResourceRegistrationXMLChoice.Builder builder = this.factory.singletonElementChoice()
                .implyIfEmpty(StoreResourceRegistration.NONE, StoreResourceRegistration.WILDCARD.getPathElement())
                .addElement(this.customStoreElement())
                .addElement(this.fileStoreElement())
                .addElement(this.remoteStoreElement())
                ;
        if (this.since(VERSION_6_0)) {
            builder.addElement(this.hotrodStoreElement());
        }
        if (this.since(VERSION_5_0)) {
            builder.addElement(this.jdbcStoreElement(null, Collections.singletonList(null)));
        } else {
            builder.addElement(this.jdbcStoreElement("string-keyed-jdbc-store", List.of("string-keyed-table")));
        }
        if (!this.since(VERSION_14_0)) {
            builder.addElement(this.jdbcStoreElement("binary-keyed-jdbc-store", List.of("binary-keyed-table")));
            builder.addElement(this.jdbcStoreElement("mixed-keyed-jdbc-store", List.of("binary-keyed-table", "string-keyed-table")));
        }

        return builder.build();
    }

    private SingletonResourceRegistrationXMLElement customStoreElement() {
        return this.storeBuilder(StoreResourceRegistration.CUSTOM)
                .withElementLocalName(ResourceXMLElementLocalName.KEY)
                .addAttribute(CustomStoreResourceDefinitionRegistrar.CLASS)
                .withContent(this.storeContentBuilder().build())
                .build();
    }

    private SingletonResourceRegistrationXMLElement fileStoreElement() {
        return this.storeBuilder(StoreResourceRegistration.FILE)
                .addAttributes(EnumSet.allOf(FileStoreResourceDefinitionRegistrar.DeprecatedAttribute.class).stream().map(Supplier::get).toList())
                .withContent(this.storeContentBuilder().build())
                .build();
    }

    private SingletonResourceRegistrationXMLElement remoteStoreElement() {
        SingletonResourceRegistrationXMLElement.Builder builder = this.storeBuilder(StoreResourceRegistration.REMOTE)
                .addAttribute(RemoteStoreResourceDefinitionRegistrar.SOCKET_TIMEOUT)
                .addAttributes(EnumSet.allOf(RemoteStoreResourceDefinitionRegistrar.Attribute.class).stream().map(Supplier::get).toList())
                ;
        ResourceXMLSequence.Builder contentBuilder = this.storeContentBuilder();
        if (this.since(VERSION_4_0)) {
            builder.addAttribute(RemoteStoreResourceDefinitionRegistrar.SOCKET_BINDINGS);
        } else {
            ResourceXMLElement remoteServerElement = this.factory.element(this.factory.resolve("remote-server"))
                    .addAttribute(RemoteStoreResourceDefinitionRegistrar.SOCKET_BINDINGS)
                    .withLocalNames(Map.of(RemoteStoreResourceDefinitionRegistrar.SOCKET_BINDINGS, "outbound-socket-binding"))
                    .withCardinality(XMLCardinality.Unbounded.REQUIRED)
                    .build();
            contentBuilder.addElement(remoteServerElement);
        }
        return builder.withContent(contentBuilder.build()).build();
    }

    private SingletonResourceRegistrationXMLElement hotrodStoreElement() {
        return this.storeBuilder(StoreResourceRegistration.HOTROD)
                .addAttributes(HotRodStoreResourceDefinitionRegistrar.CACHE_ATTRIBUTE_GROUP.getAttributes())
                .withContent(this.storeContentBuilder().build())
                .build();
    }

    private SingletonResourceRegistrationXMLElement jdbcStoreElement(String localName, List<String> tableLocalNames) {
        SingletonResourceRegistrationXMLElement.Builder builder = this.storeBuilder(StoreResourceRegistration.JDBC);
        if (localName != null) {
            builder.withElementLocalName(localName);
        }
        if (this.since(VERSION_2_0)) {
            builder.addAttribute(JDBCStoreResourceDefinitionRegistrar.DIALECT);
        }
        if (this.since(VERSION_4_0)) {
            builder.addAttribute(JDBCStoreResourceDefinitionRegistrar.DATA_SOURCE);
        } else {
            AttributeDefinition jndiNameAttribute = new SimpleAttributeDefinitionBuilder("datasource", ModelType.STRING).setRequired(true).build();
            builder.addAttribute(jndiNameAttribute);
            builder.withOperationTransformation(new UnaryOperator<>() {
                @Override
                public ModelNode apply(ModelNode operation) {
                    // Attempt to convert jndi name to data source name
                    String jndiName = operation.remove(jndiNameAttribute.getName()).asString();
                    String dataSourceName = jndiName.substring(jndiName.lastIndexOf('/') + 1);
                    operation.get(JDBCStoreResourceDefinitionRegistrar.DATA_SOURCE.getName()).set(dataSourceName);
                    return operation;
                }
            });
        }

        ResourceXMLSequence.Builder contentBuilder = this.storeContentBuilder();
        for (String tableLocalName : tableLocalNames) {
            contentBuilder.addElement(this.tableElement(tableLocalName));
        }
        return builder.withContent(contentBuilder.build()).build();
    }

    private SingletonResourceRegistrationXMLElement tableElement(String localName) {
        SingletonResourceRegistrationXMLElement.Builder builder = this.factory.singletonElement(TableResourceDefinitionRegistrar.REGISTRATION)
                .addAttributes(EnumSet.of(TableResourceDefinitionRegistrar.Attribute.PREFIX, TableResourceDefinitionRegistrar.Attribute.FETCH_SIZE).stream().map(AttributeDefinitionProvider::get).toList())
                .implyIfAbsent()
                ;
        if (localName != null) {
            builder.withElementLocalName(localName);
        } else {
            builder.withElementLocalName(ResourceXMLElementLocalName.KEY);
        }
        if (!this.since(VERSION_5_0)) {
            builder.ignoreAttributeLocalNames(Set.of("batch-size"));
        }
        if (this.since(VERSION_9_0)) {
            builder.addAttributes(EnumSet.of(TableResourceDefinitionRegistrar.Attribute.CREATE_ON_START, TableResourceDefinitionRegistrar.Attribute.DROP_ON_STOP).stream().map(AttributeDefinitionProvider::get).toList());
        }
        ResourceXMLAll.Builder contentBuilder = this.factory.all().withCardinality(XMLCardinality.Single.OPTIONAL);
        for (TableResourceDefinitionRegistrar.ColumnAttribute column : EnumSet.allOf(TableResourceDefinitionRegistrar.ColumnAttribute.class)) {
            if ((column != TableResourceDefinitionRegistrar.ColumnAttribute.SEGMENT) || this.since(VERSION_10_0)) {
                contentBuilder.addElement(column.get());
            }
        }
        return builder.withContent(contentBuilder.build()).build();
    }

    private SingletonResourceRegistrationXMLElement.Builder storeBuilder(ResourceRegistration registration) {
        SingletonResourceRegistrationXMLElement.Builder builder = this.factory.singletonElement(registration)
                .withCardinality(XMLCardinality.Single.REQUIRED)
                .withOperationKey(StoreResourceRegistration.WILDCARD.getPathElement())
                .withElementLocalName(ResourceXMLElementLocalName.VALUE_KEY)
                .addAttributes(List.of(StoreResourceDefinitionRegistrar.DeprecatedAttribute.FETCH_STATE.get(), StoreResourceDefinitionRegistrar.Attribute.PASSIVATION.get(), StoreResourceDefinitionRegistrar.Attribute.PRELOAD.get(), StoreResourceDefinitionRegistrar.Attribute.PURGE.get(), StoreResourceDefinitionRegistrar.Attribute.SHARED.get()))
                ;
        if (this.since(VERSION_5_0)) {
            builder.addAttribute(StoreResourceDefinitionRegistrar.Attribute.MAX_BATCH_SIZE.get());
        }
        if (this.since(VERSION_14_0)) {
            builder.addAttribute(StoreResourceDefinitionRegistrar.Attribute.SEGMENTED.get());
        } else {
            // If undefined, use default value from legacy schema
            builder.withDefaultValues(Map.of(StoreResourceDefinitionRegistrar.Attribute.PASSIVATION.get(), ModelNode.TRUE, StoreResourceDefinitionRegistrar.Attribute.PURGE.get(), ModelNode.TRUE));
            builder.ignoreAttributeLocalNames(Set.of("singleton"));
        }
        return builder;
    }

    private ResourceXMLSequence.Builder storeContentBuilder() {
        return this.factory.sequence().withCardinality(XMLCardinality.Single.OPTIONAL)
                .addChoice(storeWriteChoice())
                .addElement(StoreResourceDefinitionRegistrar.PROPERTIES)
                ;
    }

    private ResourceXMLChoice storeWriteChoice() {
        SingletonResourceRegistrationXMLElement.Builder builder = this.factory.singletonElement(StoreWriteResourceRegistration.BEHIND)
                .addAttribute(StoreWriteBehindResourceDefinitionRegistrar.MODIFICATION_QUEUE_SIZE)
                .withOperationKey(StoreWriteResourceRegistration.WILDCARD.getPathElement())
                .withElementLocalName(ResourceXMLElementLocalName.KEY_VALUE)
                ;
        if (!this.since(VERSION_11_0)) {
            builder.ignoreAttributeLocalNames(Set.of("thread-pool-size"));
            if (!this.since(VERSION_4_0)) {
                builder.ignoreAttributeLocalNames(Set.of("flush-lock-timeout", "shutdown-timeout"));
            }
        }
        return this.factory.singletonElementChoice()
                .implyIfEmpty(StoreWriteResourceRegistration.THROUGH, StoreWriteResourceRegistration.WILDCARD.getPathElement())
                .addElement(builder.build())
                .build();
    }

    private ResourceRegistrationXMLElement remoteCacheContainerElement() {
        NamedResourceRegistrationXMLElement.Builder builder = this.factory.namedElement(RemoteCacheContainerResourceDefinitionRegistrar.REGISTRATION)
                .addAttributes(List.of(RemoteCacheContainerResourceDefinitionRegistrar.CONNECTION_TIMEOUT, RemoteCacheContainerResourceDefinitionRegistrar.DEFAULT_REMOTE_CLUSTER, RemoteCacheContainerResourceDefinitionRegistrar.MODULES, RemoteCacheContainerResourceDefinitionRegistrar.PROTOCOL_VERSION, RemoteCacheContainerResourceDefinitionRegistrar.SOCKET_TIMEOUT))
                .provideAttributes(EnumSet.allOf(RemoteCacheContainerResourceDefinitionRegistrar.Attribute.class))
                .withCardinality(XMLCardinality.Unbounded.OPTIONAL)
                ;
        if (this.since(VERSION_9_0)) {
            if (this.since(VERSION_13_0)) {
                builder.addAttributes(List.of(RemoteCacheContainerResourceDefinitionRegistrar.MARSHALLER, RemoteCacheContainerResourceDefinitionRegistrar.TRANSACTION_TIMEOUT));
            }
            builder.addAttribute(RemoteCacheContainerResourceDefinitionRegistrar.STATISTICS_ENABLED);
        }
        if (!this.since(VERSION_12_0)) {
            builder.withLocalNames(Map.of(RemoteCacheContainerResourceDefinitionRegistrar.MODULES, ModelDescriptionConstants.MODULE));
        }

        ResourceXMLSequence.Builder contentBuilder = this.factory.sequence();
        for (ClientThreadPool pool : EnumSet.allOf(ClientThreadPool.class)) {
            contentBuilder.addElement(this.threadPoolElement(pool));
        }

        contentBuilder.addElement(this.connectionPoolElement());

        if (!this.since(VERSION_14_0)) {
            contentBuilder.addElement(ResourceXMLElement.ignore(this.factory.resolve("invalidation-near-cache"), XMLCardinality.Single.OPTIONAL));
        }

        if (this.since(VERSION_11_0) || (this.since(VERSION_9_1) && !this.since(VERSION_10_0))) {
            contentBuilder.addElement(RemoteCacheContainerResourceDefinitionRegistrar.PROPERTIES);
        }

        contentBuilder.addElement(this.factory.element(this.factory.resolve("remote-clusters")).withContent(this.factory.sequence().addElement(this.remoteClusterElement()).build()).build());
        contentBuilder.addElement(this.securityElement());

        if (!this.since(VERSION_14_0)) {
            contentBuilder.addElement(ResourceXMLElement.ignore(this.factory.resolve("transaction"), XMLCardinality.Single.OPTIONAL));
        }
        return builder.withContent(contentBuilder.build()).build();
    }

    @SuppressWarnings("deprecation")
    private ResourceRegistrationXMLElement connectionPoolElement() {
        return this.factory.singletonElement(RemoteComponentResourceRegistration.CONNECTION_POOL)
                .implyIfAbsent()
                .provideAttributes(EnumSet.allOf(ConnectionPoolResourceDefinitionRegistrar.Attribute.class))
                .build();
    }

    private ResourceRegistrationXMLElement remoteClusterElement() {
        return this.factory.namedElement(RemoteClusterResourceDefinitionRegistrar.REGISTRATION)
                .withCardinality(XMLCardinality.Unbounded.REQUIRED)
                .addAttribute(RemoteClusterResourceDefinitionRegistrar.SOCKET_BINDINGS)
                .build();
    }

    private ResourceRegistrationXMLElement securityElement() {
        return this.factory.singletonElement(RemoteComponentResourceRegistration.SECURITY)
                .addAttribute(SecurityResourceDefinitionRegistrar.SSL_CONTEXT)
                .implyIfAbsent()
                .build();
    }
}
