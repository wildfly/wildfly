/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.cache.sso.coarse;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.wildfly.clustering.ee.Mutator;
import org.wildfly.clustering.web.sso.Sessions;

public class CoarseSessions<D, S> implements Sessions<D, S> {

    private final Map<D, S> sessions;
    private final Mutator mutator;

    public CoarseSessions(Map<D, S> sessions, Mutator mutator) {
        this.sessions = sessions;
        this.mutator = mutator;
    }

    @Override
    public Set<D> getDeployments() {
        return Collections.unmodifiableSet(this.sessions.keySet());
    }

    @Override
    public S getSession(D deployment) {
        return this.sessions.get(deployment);
    }

    @Override
    public S removeSession(D deployment) {
        S removed = this.sessions.remove(deployment);
        if (removed != null) {
            this.mutator.mutate();
        }
        return removed;
    }

    @Override
    public boolean addSession(D deployment, S session) {
        boolean added = this.sessions.put(deployment, session) == null;
        if (added) {
            this.mutator.mutate();
        }
        return added;
    }
}
