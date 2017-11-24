/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

    MODULE("module"),
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
    BINARY_TABLE(StoreResourceDefinition.WILDCARD_PATH, BinaryTableResourceDefinition.PATH),
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
