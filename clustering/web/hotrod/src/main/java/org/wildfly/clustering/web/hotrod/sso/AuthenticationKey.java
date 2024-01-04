/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.hotrod.sso;

import org.wildfly.clustering.ee.hotrod.RemoteCacheKey;

/**
 * @author Paul Ferraro
 */
public class AuthenticationKey extends RemoteCacheKey<String> {

    public AuthenticationKey(String id) {
        super(id);
    }
}
