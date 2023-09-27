/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.infinispan.sso;

import org.wildfly.clustering.ee.infinispan.GroupedKey;

/**
 * Cache key for the authentication cache entry.
 * @author Paul Ferraro
 */
public class AuthenticationKey extends GroupedKey<String> {

    public AuthenticationKey(String id) {
        super(id);
    }
}
