/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller;

import org.jboss.dmr.ModelNode;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface ResultHandler {

    /**
     * Add a result fragment to the final result.
     *
     * @param location the location of the fragment within the final result
     * @param result the result fragment to insert
     */
    void handleResultFragment(String[] location, ModelNode result);

    /**
     * Handle operation completion.  The compensating update for the completed update
     * is passed in; if there is no such possible update, the value is {@code undefined}.
     *
     * @param compensatingOperation the compensating operation object
     */
    void handleResultComplete(ModelNode compensatingOperation);

    /**
     * Handle an operation failure.
     *
     * @param failureDescription the failure description
     */
    void handleFailed(ModelNode failureDescription);

    /**
     * Signify that this operation was cancelled.
     */
    void handleCancellation();
}
