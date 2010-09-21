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

package org.jboss.as.model;

/**
 * The result of applying an update to a running server.
 *
 * @param <P> the type of the parameter to pass to the handler instance
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface UpdateResultHandler<R, P> {

    /**
     * Handle successful application of the update.
     *
     * @param result the update result, if any
     * @param param the parameter passed in to the update method
     */
    void handleSuccess(R result, P param);

    /**
     * Handle a failure to apply the update.
     *
     * @param cause the cause of the failure
     * @param param the parameter passed in to the update method
     */
    void handleFailure(Throwable cause, P param);

    /**
     * Handle a timeout in applying the update.
     *
     * @param param the parameter passed in to the update method
     */
    void handleTimeout(P param);
}
