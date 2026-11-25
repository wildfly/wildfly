/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.group.legacy;

import org.wildfly.clustering.server.local.LocalGroupMember;

/**
 * @author Paul Ferraro
 */
@Deprecated
public interface LegacyLocalGroupMember extends LegacyGroupMember<String> {

    @Override
    LocalGroupMember unwrap();

    static LegacyLocalGroupMember wrap(LocalGroupMember member) {
        return () -> member;
    }
}
