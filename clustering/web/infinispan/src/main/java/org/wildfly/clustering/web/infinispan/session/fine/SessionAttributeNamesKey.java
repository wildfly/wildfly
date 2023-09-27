/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.infinispan.session.fine;

import org.wildfly.clustering.ee.infinispan.GroupedKey;

/**
 * Cache key for session attribute names.
 * @author Paul Ferraro
 */
public class SessionAttributeNamesKey extends GroupedKey<String> {

    public SessionAttributeNamesKey(String id) {
        super(id);
    }
}
