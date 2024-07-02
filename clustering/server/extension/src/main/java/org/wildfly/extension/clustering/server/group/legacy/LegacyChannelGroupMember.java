/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.group.legacy;

import org.jgroups.Address;
import org.wildfly.clustering.server.jgroups.ChannelGroupMember;

/**
 * @author Paul Ferraro
 */
@Deprecated
public interface LegacyChannelGroupMember extends LegacyGroupMember<Address> {

    @Override
    ChannelGroupMember unwrap();

    static LegacyChannelGroupMember wrap(ChannelGroupMember member) {
        return () -> member;
    }
}
