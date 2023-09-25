/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.cache.sso;

import java.util.Map;

import org.wildfly.clustering.ee.Creator;
import org.wildfly.clustering.ee.Locator;
import org.wildfly.clustering.ee.Remover;
import org.wildfly.clustering.web.sso.SSO;

/**
 * Creates an {@link SSO} from its cache storage value.
 * @author Paul Ferraro
 * @param <V> the cache value type
 */
public interface SSOFactory<AV, SV, A, D, S, L> extends Creator<String, Map.Entry<AV, SV>, A>, Locator<String, Map.Entry<AV, SV>>, Remover<String> {
    SSO<A, D, S, L> createSSO(String id, Map.Entry<AV, SV> value);

    SessionsFactory<SV, D, S> getSessionsFactory();
}
