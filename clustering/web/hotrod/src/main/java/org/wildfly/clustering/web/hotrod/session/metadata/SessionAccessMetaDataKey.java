/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.hotrod.session.metadata;

import org.wildfly.clustering.ee.hotrod.RemoteCacheKey;

/**
 * Cache key for the session access meta data entry.
 * @author Paul Ferraro
 */
public class SessionAccessMetaDataKey extends RemoteCacheKey<String> {

    public SessionAccessMetaDataKey(String id) {
        super(id);
    }
}
