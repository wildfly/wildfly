/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.registry.legacy;

import java.util.Map;

import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.server.Registration;
import org.wildfly.clustering.server.local.LocalGroup;
import org.wildfly.clustering.server.local.LocalGroupMember;
import org.wildfly.clustering.server.registry.Registry;
import org.wildfly.clustering.server.registry.RegistryFactory;
import org.wildfly.clustering.server.registry.RegistryListener;
import org.wildfly.extension.clustering.server.group.legacy.LegacyLocalGroup;

/**
 * @author Paul Ferraro
 */
@Deprecated
public interface LegacyLocalRegistryFactory<K, V> extends org.wildfly.clustering.registry.RegistryFactory<K, V> {

    RegistryFactory<LocalGroupMember, K, V> unwrap();

    @Override
    default org.wildfly.clustering.registry.Registry<K, V> createRegistry(Map.Entry<K, V> entry) {
        Registry<LocalGroupMember, K, V> registry = this.unwrap().createRegistry(entry);
        LegacyLocalGroup group = LegacyLocalGroup.wrap(LocalGroup.class.cast(registry.getGroup()));
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

    static <K, V> LegacyLocalRegistryFactory<K, V> wrap(RegistryFactory<LocalGroupMember, K, V> factory) {
        return () -> factory;
    }
}
