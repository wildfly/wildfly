/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.group.legacy;

import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.server.local.LocalGroup;
import org.wildfly.clustering.server.local.LocalGroupMember;

/**
 * @author Paul Ferraro
 */
@Deprecated
public interface LegacyLocalGroup extends LegacyGroup<String, LocalGroupMember> {

    @Override
    LocalGroup unwrap();

    @Override
    default Node wrap(LocalGroupMember member) {
        return LegacyLocalGroupMember.wrap(member);
    }

    @Override
    default LocalGroupMember unwrap(Node node) {
        return ((LegacyLocalGroupMember) node).unwrap();
    }

    static LegacyLocalGroup wrap(LocalGroup group) {
        return new LegacyLocalGroup() {
            @Override
            public LocalGroup unwrap() {
                return group;
            }
        };
    }
}
