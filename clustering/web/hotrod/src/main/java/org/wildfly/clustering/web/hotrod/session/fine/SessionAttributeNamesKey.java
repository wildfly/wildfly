/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.hotrod.session.fine;

import org.wildfly.clustering.ee.hotrod.RemoteCacheKey;

/**
 * Cache key for session attribute names.
 * @author Paul Ferraro
 */
public class SessionAttributeNamesKey extends RemoteCacheKey<String> {

    public SessionAttributeNamesKey(String id) {
        super(id);
    }
}
