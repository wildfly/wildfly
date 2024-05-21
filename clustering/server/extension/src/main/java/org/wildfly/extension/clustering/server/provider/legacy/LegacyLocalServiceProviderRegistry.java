/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.provider.legacy;

import org.wildfly.clustering.server.local.LocalGroupMember;
import org.wildfly.clustering.server.local.provider.LocalServiceProviderRegistrar;
import org.wildfly.extension.clustering.server.group.legacy.LegacyLocalGroup;

/**
 * @author Paul Ferraro
 */
@Deprecated
public interface LegacyLocalServiceProviderRegistry<T> extends LegacyServiceProviderRegistry<T, String, LocalGroupMember> {

    @Override
    LocalServiceProviderRegistrar<T> unwrap();

    @Override
    default LegacyLocalGroup getGroup() {
        return LegacyLocalGroup.wrap(this.unwrap().getGroup());
    }

    static <T> LegacyLocalServiceProviderRegistry<T> wrap(LocalServiceProviderRegistrar<T> registrar) {
        return () -> registrar;
    }
}
