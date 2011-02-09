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

package org.jboss.as.domain.controller.legacy;

import java.io.Serializable;
import org.jboss.as.model.UpdateFailedException;

/**
 * Response object used to relay information back the DC when an update is passed to a host controller to process.
 *
 * @param <R> the type of result that is returned by this update type
 *
 * @author John Bailey
 */
public class ModelUpdateResponse<R> implements Serializable {
    private static final long serialVersionUID = 5496724566407738382L;

    private final R result;
    private final UpdateFailedException updateException;

    /**
     * Create an instance with a result object
     *
     * @param result The result of the update
     */
    public ModelUpdateResponse(final R result) {
        this.result = result;
        this.updateException = null;
    }

    /**
     * Create an instance with an {@link org.jboss.as.model.UpdateFailedException} to allow the client to know
     * the update failed.
     *
     * @param updateException The update exception
     */
    public ModelUpdateResponse(UpdateFailedException updateException) {
        this.updateException = updateException;
        this.result = null;
    }

    /**
     * Determine if the update was successfully executed.
     *
     * @return true if the update was a success, false if not.
     */
    public boolean isSuccess() {
        return updateException == null;
    }

    /**
     * Get the result of the update execution.
     *
     * @return The result if update was successful, otherwise null.
     */
    public R getResult() {
        return result;
    }

    /**
     * Get the {@link org.jboss.as.model.UpdateFailedException} created by the update.
     *
     * @return The exception if the update failed, otherwise null.
     */
    public UpdateFailedException getUpdateException() {
        return updateException;
    }
}
