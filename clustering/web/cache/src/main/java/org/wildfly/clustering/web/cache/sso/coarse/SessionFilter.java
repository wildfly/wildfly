/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.sso.coarse;

import java.util.Map;
import java.util.function.Predicate;

/**
 * Selects SSO sessions entries containing the specified session.
 * @author Paul Ferraro
 * @param <K> the cache key type
 * @param <D> the deployment type
 * @param <S> the session type
 */
public class SessionFilter<K, D, S> implements Predicate<Map.Entry<K, Map<D, S>>> {

    private final S session;

    public SessionFilter(S session) {
        this.session = session;
    }

    public S getSession() {
        return this.session;
    }

    @Override
    public boolean test(Map.Entry<K, Map<D, S>> entry) {
        return entry.getValue().values().contains(this.session);
    }
}
