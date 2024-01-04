/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.stream.Stream;

import org.jboss.as.clustering.controller.ResourceServiceNameFactory;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.msc.service.ServiceName;

/**
 * Enumerates the configurable cache components
 * @author Paul Ferraro
 */
public enum CacheComponent implements ResourceServiceNameFactory {

    MODULES("modules"),
    EXPIRATION(ExpirationResourceDefinition.PATH),
    LOCKING(LockingResourceDefinition.PATH),
    MEMORY(MemoryResourceDefinition.WILDCARD_PATH),
    PERSISTENCE() {
        @Override
        public ServiceName getServiceName(PathAddress cacheAddress) {
            return StoreResourceDefinition.Capability.PERSISTENCE.getServiceName(cacheAddress.append(StoreResourceDefinition.WILDCARD_PATH));
        }
    },
    STATE_TRANSFER(StateTransferResourceDefinition.PATH),
    PARTITION_HANDLING(PartitionHandlingResourceDefinition.PATH),
    STORE_WRITE(StoreWriteResourceDefinition.WILDCARD_PATH),
    TRANSACTION(TransactionResourceDefinition.PATH),
    STRING_TABLE(StoreResourceDefinition.WILDCARD_PATH, StringTableResourceDefinition.PATH),
    BACKUPS(BackupResourceDefinition.WILDCARD_PATH),
    ;

    private final String[] components;

    CacheComponent() {
        this(Stream.empty());
    }

    CacheComponent(PathElement... paths) {
        this(Stream.of(paths).map(path -> path.isWildcard() ? path.getKey() : path.getValue()));
    }

    CacheComponent(Stream<String> components) {
        this(components.toArray(String[]::new));
    }

    CacheComponent(String... components) {
        this.components = components;
    }

    @Override
    public ServiceName getServiceName(PathAddress cacheAddress) {
        return CacheResourceDefinition.Capability.CONFIGURATION.getServiceName(cacheAddress).append(this.components);
    }
}
