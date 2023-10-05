/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.infinispan.session.attributes;

import org.wildfly.clustering.ee.infinispan.GroupedKey;

/**
 * Cache key for session attributes.
 * @author Paul Ferraro
 */
public class SessionAttributesKey extends GroupedKey<String> {

    public SessionAttributesKey(String id) {
        super(id);
    }
}
