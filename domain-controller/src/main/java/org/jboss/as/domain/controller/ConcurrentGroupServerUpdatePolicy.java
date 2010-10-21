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

package org.jboss.as.domain.controller;

import java.util.HashSet;
import java.util.Set;

import org.jboss.as.domain.client.api.deployment.ServerGroupDeploymentPlan;

class ConcurrentGroupServerUpdatePolicy {
    private final ConcurrentGroupServerUpdatePolicy predecessor;
    private final Set<String> groups = new HashSet<String>();
    private int responseCount;
    private boolean failed;

    ConcurrentGroupServerUpdatePolicy(final ConcurrentGroupServerUpdatePolicy predecessor,
                                      final Set<ServerGroupDeploymentPlan> groupPlans) {
        this.predecessor = predecessor;
        for (ServerGroupDeploymentPlan plan : groupPlans) {
            groups.add(plan.getServerGroupName());
        }
    }

    public boolean canSuccessorProceed() {

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

    public boolean canChildProceed() {
        return predecessor == null || predecessor.canSuccessorProceed();
    }

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
                throw new IllegalStateException("Unknown server group " + serverGroup);
            }
        }
    }
}
