/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.singleton.service;

import java.util.function.Consumer;

import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.server.GroupMember;
import org.wildfly.service.capture.Reference;
import org.wildfly.service.capture.ReferenceValueRegistry;

/**
 * @author Paul Ferraro
 */
public enum ElectedMemberRegistry implements ReferenceValueRegistry<ServiceName, GroupMember> {
    INSTANCE;

    private final ReferenceValueRegistry<ServiceName, GroupMember> registry = ReferenceValueRegistry.newInstance();

    @Override
    public Reference<GroupMember> getReference(ServiceName key) {
        return this.registry.getReference(key);
    }

    @Override
    public Consumer<GroupMember> add(ServiceName key) {
        return this.registry.add(key);
    }

    @Override
    public void remove(ServiceName key) {
        this.registry.remove(key);
    }
}
