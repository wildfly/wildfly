/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.provider;

import org.jboss.as.server.suspend.SuspendableActivityRegistrar;
import org.wildfly.clustering.server.local.LocalGroup;
import org.wildfly.clustering.server.local.LocalGroupMember;
import org.wildfly.clustering.server.local.provider.LocalServiceProviderRegistrar;

/**
 * A {@link LocalServiceProviderRegistrar} decorator that creates suspendable registrations.
 * @author Paul Ferraro
 */
public class SuspendableLocalServiceProviderRegistrar<T> extends AbstractSuspendableServiceProviderRegistrar<T, LocalGroupMember> implements LocalServiceProviderRegistrar<T> {
    private final LocalGroup group;

    SuspendableLocalServiceProviderRegistrar(LocalServiceProviderRegistrar<T> registrar, SuspendableActivityRegistrar activityRegistrar) {
        super(registrar, activityRegistrar);
        this.group = registrar.getGroup();
    }

    @Override
    public LocalGroup getGroup() {
        return this.group;
    }
}
