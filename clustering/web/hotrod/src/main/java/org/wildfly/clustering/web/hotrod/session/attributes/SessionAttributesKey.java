/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.hotrod.session.attributes;

import org.wildfly.clustering.ee.hotrod.RemoteCacheKey;

/**
 * Cache key for session attributes.
 * @author Paul Ferraro
 */
public class SessionAttributesKey extends RemoteCacheKey<String> {

    public SessionAttributesKey(String id) {
        super(id);
    }
}
