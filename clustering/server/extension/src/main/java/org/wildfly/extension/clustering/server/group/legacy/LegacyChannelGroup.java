/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.group.legacy;

import org.jgroups.Address;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.server.jgroups.ChannelGroup;
import org.wildfly.clustering.server.jgroups.ChannelGroupMember;

/**
 * @author Paul Ferraro
 */
@Deprecated
public interface LegacyChannelGroup extends LegacyGroup<Address, ChannelGroupMember> {

    @Override
    ChannelGroup unwrap();

    @Override
    default Node wrap(ChannelGroupMember member) {
        return LegacyChannelGroupMember.wrap(member);
    }

    @Override
    default ChannelGroupMember unwrap(Node node) {
        return ((LegacyChannelGroupMember) node).unwrap();
    }

    static LegacyChannelGroup wrap(ChannelGroup group) {
        return new LegacyChannelGroup() {
            @Override
            public ChannelGroup unwrap() {
                return group;
            }
        };
    }
}
