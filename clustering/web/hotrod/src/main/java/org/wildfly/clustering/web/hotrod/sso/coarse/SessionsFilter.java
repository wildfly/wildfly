/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.hotrod.sso.coarse;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Filter/mapper that handles filtering and casting for cache entries containing SSO sessions.
 * @author Paul Ferraro
 */
public class SessionsFilter<D, S> implements Predicate<Map.Entry<?, ?>>, Function<Map.Entry<?, ?>, Map.Entry<CoarseSessionsKey, Map<D, S>>> {

    @SuppressWarnings("unchecked")
    @Override
    public Map.Entry<CoarseSessionsKey, Map<D, S>> apply(Map.Entry<?, ?> entry) {
        return (Map.Entry<CoarseSessionsKey, Map<D, S>>) entry;
    }

    @Override
    public boolean test(Map.Entry<?, ?> entry) {
        return (entry.getKey() instanceof CoarseSessionsKey);
    }
}
