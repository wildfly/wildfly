/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.group.legacy;

import java.net.InetSocketAddress;

import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.server.group.GroupMember;

/**
 * @author Paul Ferraro
 */
@Deprecated
public interface LegacyGroupMember<A extends Comparable<A>> extends Node {

    GroupMember<A> unwrap();

    @Override
    default String getName() {
        return this.unwrap().getName();
    }

    @Override
    default InetSocketAddress getSocketAddress() {
        return null;
    }
}
