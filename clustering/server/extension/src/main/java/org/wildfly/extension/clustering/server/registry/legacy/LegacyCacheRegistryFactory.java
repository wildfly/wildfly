/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.registry.legacy;

import java.util.Map;

import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.server.Registration;
import org.wildfly.clustering.server.infinispan.CacheContainerGroup;
import org.wildfly.clustering.server.infinispan.CacheContainerGroupMember;
import org.wildfly.clustering.server.registry.Registry;
import org.wildfly.clustering.server.registry.RegistryFactory;
import org.wildfly.clustering.server.registry.RegistryListener;
import org.wildfly.extension.clustering.server.group.legacy.LegacyCacheContainerGroup;

/**
 * @author Paul Ferraro
 */
@Deprecated
public interface LegacyCacheRegistryFactory<K, V> extends org.wildfly.clustering.registry.RegistryFactory<K, V> {

    RegistryFactory<CacheContainerGroupMember, K, V> unwrap();

    @Override
    default org.wildfly.clustering.registry.Registry<K, V> createRegistry(Map.Entry<K, V> entry) {
        Registry<CacheContainerGroupMember, K, V> registry = this.unwrap().createRegistry(entry);
        LegacyCacheContainerGroup group = LegacyCacheContainerGroup.wrap(CacheContainerGroup.class.cast(registry.getGroup()));
        return new org.wildfly.clustering.registry.Registry<>() {
            @Override
            public org.wildfly.clustering.Registration register(org.wildfly.clustering.registry.RegistryListener<K, V> listener) {
                Registration registration = registry.register(new RegistryListener<>() {
                    @Override
                    public void added(Map<K, V> added) {
                        listener.addedEntries(added);
                    }

                    @Override
                    public void updated(Map<K, V> updated) {
                        listener.updatedEntries(updated);
                    }

                    @Override
                    public void removed(Map<K, V> removed) {
                        listener.removedEntries(removed);
                    }
                });
                return registration::close;
            }

            @Override
            public Group getGroup() {
                return group;
            }

            @Override
            public Map<K, V> getEntries() {
                return registry.getEntries();
            }

            @Override
            public Map.Entry<K, V> getEntry(Node node) {
                return registry.getEntry(group.unwrap(node));
            }

            @Override
            public void close() {
                registry.close();
            }
        };
    }

    static <K, V> LegacyCacheRegistryFactory<K, V> wrap(RegistryFactory<CacheContainerGroupMember, K, V> factory) {
        return () -> factory;
    }
}
