/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.infinispan.session;

import org.wildfly.clustering.ee.infinispan.GroupedKey;

/**
 * Cache key for the session creation meta data entry.
 * @author Paul Ferraro
 */
public class SessionCreationMetaDataKey extends GroupedKey<String> {

    public SessionCreationMetaDataKey(String id) {
        super(id);
    }
}
