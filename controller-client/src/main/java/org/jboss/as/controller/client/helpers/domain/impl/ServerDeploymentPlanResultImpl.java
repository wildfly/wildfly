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

package org.jboss.as.controller.client.helpers.domain.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.jboss.as.controller.client.helpers.domain.ServerDeploymentPlanResult;
import org.jboss.as.controller.client.helpers.domain.ServerUpdateResult;

/**
 * Default implementation of {@link ServerDeploymentPlanResult}.
 *
 * @author Brian Stansberry
 */
class ServerDeploymentPlanResultImpl implements ServerDeploymentPlanResult {

    private final String serverName;
    private final Map<UUID, ServerUpdateResult> serverResults = new HashMap<UUID, ServerUpdateResult>();

    ServerDeploymentPlanResultImpl(final String serverName) {
        assert serverName != null : "serverName is null";
        this.serverName = serverName;
    }

    @Override
    public Map<UUID, ServerUpdateResult> getDeploymentActionResults() {
        return Collections.unmodifiableMap(serverResults);
    }

    @Override
    public String getServerName() {
        return serverName;
    }

    ServerUpdateResult getServerUpdateResult(UUID actionId) {
        synchronized (serverResults) {
            return serverResults.get(actionId);
        }
    }

    void storeServerUpdateResult(UUID actionId, ServerUpdateResult result) {
        synchronized (serverResults) {
            serverResults.put(actionId, result);
        }
    }

}
