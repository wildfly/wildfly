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

package org.jboss.as.controller.client.helpers.standalone;

import static org.jboss.as.controller.client.ControllerClientMessages.MESSAGES;

import java.util.UUID;



/**
 * Abstract superclass of implementations of {@link ServerUpdateActionResult}.
 *
 * @author Brian Stansberry
 */
public abstract class AbstractServerUpdateActionResult<T extends ServerUpdateActionResult> implements ServerUpdateActionResult, java.io.Serializable {

    private static final long serialVersionUID = -4692787126053225682L;

    private final UUID id;
    private Result result;
    private final Throwable deploymentException;
    private T rollbackResult;

    protected AbstractServerUpdateActionResult() {
        this.id = null;
        this.deploymentException = null;
    }

    public AbstractServerUpdateActionResult(UUID id, Result result) {
        this(id, result, null);
    }

    public AbstractServerUpdateActionResult(UUID id, Throwable deploymentException) {
        this(id, Result.FAILED, deploymentException);
    }

    public AbstractServerUpdateActionResult(UUID id, Result result, Throwable deploymentException) {
        if (id == null)
            throw MESSAGES.nullVar("id");
        if (result == null)
            throw MESSAGES.nullVar("result");
        this.id = id;
        this.result = result;
        this.deploymentException = deploymentException;
    }

    @Override
    public UUID getUpdateActionId() {
        return id;
    }

    @Override
    public Throwable getDeploymentException() {
        return deploymentException;
    }

    @Override
    public Result getResult() {
        if (rollbackResult != null) {
            return result == Result.FAILED ? result : Result.ROLLED_BACK;
        }
        return result;
    }

    @Override
    public T getRollbackResult() {
        return rollbackResult;
    }

    protected abstract Class<T> getRollbackResultClass();

    public static <R  extends ServerUpdateActionResult> void installRollbackResult(AbstractServerUpdateActionResult<R> update, ServerUpdateActionResult rollback) {
        R cast = update.getRollbackResultClass().cast(rollback);
        update.rollbackResult = cast;
    }
}
