/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.dispatcher.legacy;

import org.wildfly.clustering.server.local.LocalGroupMember;
import org.wildfly.clustering.server.local.dispatcher.LocalCommandDispatcherFactory;
import org.wildfly.extension.clustering.server.group.legacy.LegacyGroup;
import org.wildfly.extension.clustering.server.group.legacy.LegacyLocalGroup;

/**
 * @author Paul Ferraro
 */
@Deprecated
public interface LegacyLocalCommandDispatcherFactory extends LegacyCommandDispatcherFactory<String, LocalGroupMember> {

    @Override
    LocalCommandDispatcherFactory unwrap();

    @Override
    default LegacyGroup<String, LocalGroupMember> getGroup() {
        return LegacyLocalGroup.wrap(this.unwrap().getGroup());
    }

    static LegacyLocalCommandDispatcherFactory wrap(LocalCommandDispatcherFactory factory) {
        return () -> factory;
    }
}
