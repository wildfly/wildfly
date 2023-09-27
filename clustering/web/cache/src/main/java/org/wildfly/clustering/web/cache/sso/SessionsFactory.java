/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.sso;

import java.util.Map;

import org.wildfly.clustering.ee.Creator;
import org.wildfly.clustering.ee.Locator;
import org.wildfly.clustering.ee.Remover;
import org.wildfly.clustering.web.sso.Sessions;

public interface SessionsFactory<V, D, S> extends Creator<String, V, Void>, Locator<String, V>, Remover<String> {
    Sessions<D, S> createSessions(String ssoId, V value);

    Map.Entry<String, V> findEntryContaining(S session);
}
