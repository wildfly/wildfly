/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.domain.controller.plan;

import static org.jboss.as.domain.controller.DomainControllerMessages.MESSAGES;

import java.util.HashSet;
import java.util.Set;

/**
 * Policy that controls whether concurrently executing updates to server groups
 * can proceed. Acts a parent to the {@link ServerUpdatePolicy} that controls
 * each concurrently executing server group.
 *
 * @author Brian Stansberry
 */
class ConcurrentGroupServerUpdatePolicy {
    private final ConcurrentGroupServerUpdatePolicy predecessor;
    private final Set<String> groups = new HashSet<String>();
    private int responseCount;
    private boolean failed;

    /**
     * Creates a new ConcurrentGroupServerUpdatePolicy.
     *
     * @param predecessor the policy for a set of server group updates that
     *                    were updated prior to this set. May be <code>null</code>
     *                    if there was no previous set
     * @param groups  the names of the server groups that will be concurrently updated.
     *                    Cannot be <code>null</code>
     */
    ConcurrentGroupServerUpdatePolicy(final ConcurrentGroupServerUpdatePolicy predecessor,
                                      final Set<String> groups) {
        this.predecessor = predecessor;
        this.groups.addAll(groups);
    }

    /**
     * Check from another ConcurrentGroupServerUpdatePolicy whose plans are meant to
     * execute once this policy's plans are successfully completed.
     *
     * @return <code>true</code> if the successor can proceed
     */
    private boolean canSuccessorProceed() {

        if (predecessor != null && !predecessor.canSuccessorProceed()) {
            return false;
        }

        synchronized (this) {
            while (responseCount < groups.size()) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
            return !failed;
        }
    }

    /**
     * Check from a child {@link ServerUpdatePolicy} as to whether it can
     * proceed.
     *
     * @return <code>true</code> if the child policy can proceed
     */
    public boolean canChildProceed() {
        return predecessor == null || predecessor.canSuccessorProceed();
    }

    /**
     * Records the result of updating a server group.
     *
     * @param serverGroup the server group's name. Cannot be <code>null</code>
     * @param failed <code>true</code> if the server group update failed;
     *               <code>false</code> if it succeeded
     */
    public void recordServerGroupResult(final String serverGroup, final boolean failed) {

        synchronized (this) {
            if (groups.contains(serverGroup)) {
                responseCount++;
                if (failed) {
                    this.failed = true;
                }
                notifyAll();
            }
            else {
                throw MESSAGES.unknownServerGroup(serverGroup);
            }
        }
    }
}
