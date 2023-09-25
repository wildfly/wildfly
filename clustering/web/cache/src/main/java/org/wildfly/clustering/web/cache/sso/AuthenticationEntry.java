/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.cache.sso;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Cache entry that store authentication data plus any local context.
 * @author Paul Ferraro
 * @param <A> the identity type
 * @param <L> the local context type
 */
public class AuthenticationEntry<V, L> {

    private final V authentication;
    private final AtomicReference<L> localContext = new AtomicReference<>();

    public AuthenticationEntry(V authentication) {
        this.authentication = authentication;
    }

    public V getAuthentication() {
        return this.authentication;
    }

    public AtomicReference<L> getLocalContext() {
        return this.localContext;
    }
}
