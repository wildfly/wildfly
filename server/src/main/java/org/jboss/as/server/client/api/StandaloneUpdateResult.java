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

package org.jboss.as.server.client.api;

import java.io.Serializable;

import org.jboss.as.model.UpdateFailedException;

/**
 * Wrapper object containing the results of an update operation.  The result either contains a result of type {@code R}
 * or a {@link org.jboss.as.model.UpdateFailedException}.  To determine which to look for the {@code isSuccess()} method
 * can be called.  If the result is {@code true} the result object is present, if not the {@link org.jboss.as.model.UpdateFailedException}
 * is.
 *
 * @author John Bailey
 * @author Emanuel Muckenhuber
 */
public class StandaloneUpdateResult<R> implements Serializable {

    private static final long serialVersionUID = 4320577243229764829L;

    private final R result;
    private final UpdateFailedException failure;

    public StandaloneUpdateResult(final R result, final UpdateFailedException failure) {
        this.result = result;
        this.failure = failure;
    }

    /**
     * Get the update result.
     *
     * @return the result
     */
    public R getResult() {
        return result;
    }

    /**
     * Get the update failure.
     *
     * @return the failure
     */
    public UpdateFailedException getFailure() {
        return failure;
    }

    /**
     * Determine if the update was successfully executed.
     *
     * @return true if the update was a success, false if not.
     */
    public boolean isSuccess() {
        return failure == null;
    }


}
