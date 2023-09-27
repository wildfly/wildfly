/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session.fine;

import java.util.UUID;

import org.wildfly.clustering.ee.Key;

/**
 * A key for session attribute entries, identified via session identifier and attribute identifier.
 * @author Paul Ferraro
 */
public interface SessionAttributeKey extends Key<String>, Comparable<SessionAttributeKey> {

    UUID getAttributeId();

    @Override
    default int compareTo(SessionAttributeKey key) {
        int result = this.getId().compareTo(key.getId());
        return (result == 0) ? this.getAttributeId().compareTo(key.getAttributeId()) : result;
    }
}
