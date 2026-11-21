/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.provider;

import org.jboss.as.server.suspend.SuspendableActivityRegistrar;
import org.wildfly.clustering.server.infinispan.CacheContainerGroup;
import org.wildfly.clustering.server.infinispan.CacheContainerGroupMember;
import org.wildfly.clustering.server.infinispan.provider.CacheContainerServiceProviderRegistrar;

/**
 * A {@link CacheContainerServiceProviderRegistrar} decorator that creates suspendable registrations.
 * @author Paul Ferraro
 */
public class SuspendableCacheContainerServiceProviderRegistrar<T> extends AbstractSuspendableServiceProviderRegistrar<T, CacheContainerGroupMember> implements CacheContainerServiceProviderRegistrar<T> {
    private final CacheContainerGroup group;

    SuspendableCacheContainerServiceProviderRegistrar(CacheContainerServiceProviderRegistrar<T> registrar, SuspendableActivityRegistrar activityRegistrar) {
        super(registrar, activityRegistrar);
        this.group = registrar.getGroup();
    }

    @Override
    public CacheContainerGroup getGroup() {
        return this.group;
    }
}
