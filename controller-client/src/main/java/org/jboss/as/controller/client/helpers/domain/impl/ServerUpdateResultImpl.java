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

import static org.jboss.as.controller.client.ControllerClientMessages.MESSAGES;

import java.io.Serializable;
import java.util.UUID;

import org.jboss.as.controller.client.helpers.domain.ServerIdentity;
import org.jboss.as.controller.client.helpers.domain.ServerUpdateResult;
import org.jboss.dmr.ModelNode;

/**
 * Default implementation of {@link ServerUpdateResult}.
 *
 * @author Brian Stansberry
 */
class ServerUpdateResultImpl implements ServerUpdateResult, Serializable {

    private static final long serialVersionUID = 5879115765933810032L;

    private final UUID actionId;
    private final ServerIdentity serverId;
    private final UpdateResultHandlerResponse urhr;
    private UpdateResultHandlerResponse rollbackResult;

    ServerUpdateResultImpl(final UUID actionId, final ServerIdentity serverId, final UpdateResultHandlerResponse urhr) {
        assert actionId != null : "actionId is null";
        assert serverId != null : "serverId is null";
        assert urhr != null : "urhr is null";
        this.actionId = actionId;
        this.serverId = serverId;
        this.urhr = urhr;
    }

    @Override
    public Throwable getFailureResult() {
        return urhr.getFailureResult();
    }

    @Override
    public ServerIdentity getServerIdentity() {
        return serverId;
    }

    @Override
    public ModelNode getSuccessResult() {
        return urhr.getSuccessResult();
    }

    @Override
    public UUID getUpdateActionId() {
        return actionId;
    }

    @Override
    public boolean isCancelled() {
        return urhr.isCancelled();
    }

    @Override
    public boolean isRolledBack() {
        return rollbackResult != null || urhr.isRolledBack();
    }

    @Override
    public boolean isTimedOut() {
        return urhr.isTimedOut();
    }

    void markRolledBack(UpdateResultHandlerResponse rollbackResult) {
        this.rollbackResult = rollbackResult;
    }

    @Override
    public boolean isServerRestarted() {
        return urhr.isServerRestarted();
    }

    @Override
    public Throwable getRollbackFailure() {
        if (rollbackResult == null) {
            return null;
        }
        else if (rollbackResult.isCancelled()) {
            return MESSAGES.rollbackCancelled();
        }
        else if (rollbackResult.isRolledBack()) {
            return MESSAGES.rollbackRolledBack();
        }
        else if (rollbackResult.isTimedOut()) {
            return MESSAGES.rollbackTimedOut();
        }
        else {
            return rollbackResult.getFailureResult();
        }
    }

}
