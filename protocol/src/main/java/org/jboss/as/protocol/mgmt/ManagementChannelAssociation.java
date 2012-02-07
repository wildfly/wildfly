/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.protocol.mgmt;

import org.jboss.remoting3.Channel;
import org.jboss.threads.AsyncFuture;

import java.io.IOException;

/**
 * Associates a remoting {@code Channel} to a management client.
 *
 * @author Emanuel Muckenhuber
 */
public interface ManagementChannelAssociation {

    /**
     * Execute a management request.
     *
     * @param request the request
     * @param attachment the attachment
     * @param <T> the result type
     * @param <A> the attachment type
     * @return the created active operation
     * @throws IOException
     */
    <T, A> ActiveOperation<T, A> executeRequest(final ManagementRequest<T, A> request, A attachment) throws IOException;

    /**
     * Execute a management request.
     *
     * @param request the request
     * @param attachment the attachment
     * @param callback the completion listener
     * @param <T> the result type
     * @param <A> the attachment type
     * @return the created active operation
     * @throws IOException
     */
    <T, A> ActiveOperation<T, A> executeRequest(final ManagementRequest<T, A> request, A attachment, ActiveOperation.CompletedCallback<T> callback) throws IOException;

    /**
     * Execute a request based on an existing active operation.
     *
     * @param operationId the operation-id of the existing active operation
     * @param request the request
     * @param <T> the request type
     * @param <A> the attachment type
     * @return the future result
     * @throws IOException
     */
    <T, A> AsyncFuture<T> executeRequest(final Integer operationId, final ManagementRequest<T, A> request) throws IOException;

    /**
     * Execute a request based on an existing active operation.
     *
     * @param operation the active operation
     * @param request the request
     * @param <T> the result type
     * @param <A> the attachment type
     * @return the future result
     * @throws IOException
     */
    <T, A> AsyncFuture<T> executeRequest(final ActiveOperation<T, A> operation, final ManagementRequest<T, A> request) throws IOException;

    /**
     * Get the underlying remoting channel associated with this context.
     *
     * @return the channel
     * @throws IOException
     */
    Channel getChannel() throws IOException;

}
