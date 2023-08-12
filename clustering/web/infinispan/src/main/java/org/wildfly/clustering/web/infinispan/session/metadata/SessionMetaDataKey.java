/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.infinispan.session.metadata;

import org.wildfly.clustering.ee.infinispan.GroupedKey;

/**
 * Cache key for the session meta data entry.
 * @author Paul Ferraro
 */
public class SessionMetaDataKey extends GroupedKey<String> {

    public SessionMetaDataKey(String id) {
        super(id);
    }
}
