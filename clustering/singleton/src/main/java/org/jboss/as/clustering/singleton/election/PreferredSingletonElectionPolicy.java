/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.clustering.singleton.election;

import java.util.List;

import org.jboss.as.clustering.ClusterNode;
import org.jboss.as.clustering.singleton.SingletonElectionPolicy;

/**
 * An election policy that always elects a preferred node, and defers to a default policy
 * if the preferred node is not a candidate.  The means of specifying the preferred node is
 * the responsibility of the extending class.
 * @author Paul Ferraro
 */
public class PreferredSingletonElectionPolicy implements SingletonElectionPolicy {
    private final Preference preference;
    private final SingletonElectionPolicy policy;

    public PreferredSingletonElectionPolicy(Preference preference, SingletonElectionPolicy policy) {
        this.preference = preference;
        this.policy = policy;
    }

    @Override
    public ClusterNode elect(List<ClusterNode> candidates) {
        for (ClusterNode candidate: candidates) {
            if (this.preference.preferred(candidate)) {
                return candidate;
            }
        }
        return this.policy.elect(candidates);
    }
}
