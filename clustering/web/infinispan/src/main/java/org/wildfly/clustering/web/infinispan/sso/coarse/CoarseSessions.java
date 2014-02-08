/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.wildfly.clustering.web.infinispan.sso.coarse;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.jboss.as.clustering.infinispan.invoker.Mutator;
import org.wildfly.clustering.web.sso.Sessions;

public class CoarseSessions<D> implements Sessions<D> {

    private final Map<D, String> sessions;
    private final Mutator mutator;

    public CoarseSessions(Map<D, String> sessions, Mutator mutator) {
        this.sessions = sessions;
        this.mutator = mutator;
    }

    @Override
    public Set<D> getDeployments() {
        return Collections.unmodifiableSet(this.sessions.keySet());
    }

    @Override
    public String getSession(D deployment) {
        return this.sessions.get(deployment);
    }

    @Override
    public void removeSession(D deployment) {
        if (this.sessions.remove(deployment) != null) {
            this.mutator.mutate();
        }
    }

    @Override
    public void addSession(D deployment, String id) {
        if (this.sessions.put(deployment, id) == null) {
            this.mutator.mutate();
        }
    }
}
