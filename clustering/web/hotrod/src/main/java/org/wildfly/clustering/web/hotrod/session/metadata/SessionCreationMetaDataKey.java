/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.hotrod.session.metadata;

import org.wildfly.clustering.ee.hotrod.RemoteCacheKey;

/**
 * Cache key for the session creation meta data entry.
 * @author Paul Ferraro
 */
public class SessionCreationMetaDataKey extends RemoteCacheKey<String> {

    public SessionCreationMetaDataKey(String id) {
        super(id);
    }
}
