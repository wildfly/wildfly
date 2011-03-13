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

package org.jboss.as.controller.client;

import org.jboss.dmr.ModelNode;

/**
 * Interface for capturing the results of a model operation.
 *
 * @author John Bailey
 */
public interface OperationResult {

    /**
     * Get a handle which may allow a task to be canceled.
     *
     * @return The handle
     */
    Cancellable getCancellable();

    /**
     * Get the compensating operation for the operation run. Note that
     * this call may block until request execution proceeds to the
     * point where a compensating operation is available.
     *
     * @return the compensating operation. May return an undefined node
     *         if there was no compensating operation or the request was
     *         cancelled before the compensating operation was provided.
     */
    ModelNode getCompensatingOperation();
}
