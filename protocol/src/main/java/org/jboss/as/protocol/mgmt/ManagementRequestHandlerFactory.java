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

/**
 * A {@code ManagementRequestHandler} factory.
 *
 * @author Emanuel Muckenhuber
 */
public interface ManagementRequestHandlerFactory {

    /**
     * Try to resolve the request handler for the give header.
     *
     * @param handlers the handlers chain
     * @param header the request header
     * @return the management request handler
     */
    ManagementRequestHandler<?, ?> resolveHandler(RequestHandlerChain handlers, ManagementRequestHeader header);

    /**
     * A chain of multiple request handler.
     */
    public interface RequestHandlerChain {

        /**
         * Create a new active operation. This will generate a new operation-id.
         *
         * @param attachment the optional attachment
         * @param <T> the result type
         * @param <A> the attachment type
         * @return the registered active operation
         */
        <T, A> ActiveOperation<T, A> createActiveOperation(A attachment);

        /**
         * Create a new active operation. This will generate a new operation-id.
         *
         * @param attachment the optional attachment
         * @param callback the completed callback
         * @param <T> the result type
         * @param <A> the attachment type
         * @return the registered active operation
         */
        <T, A> ActiveOperation<T, A> createActiveOperation(A attachment, ActiveOperation.CompletedCallback<T> callback);

        /**
         * Create a new active operation, with a given operation-id.
         *
         * @param id the operation-id
         * @param attachment the attachment
         * @param <T> the result type
         * @param <A> the attachment type
         * @return the registered active operation
         */
        <T, A> ActiveOperation<T, A> registerActiveOperation(Integer id, A attachment);

        /**
         * Create a new active operation, with a given operation-id.
         *
         * @param id the operation-id
         * @param attachment the attachment
         * @param callback the completed callback
         * @param <T> the result type
         * @param <A> the attachment type
         * @return the registered active operation
         */
        <T, A> ActiveOperation<T, A> registerActiveOperation(Integer id, A attachment, ActiveOperation.CompletedCallback<T> callback);

        /**
         * Ask the next factory in the chain to resolve the handler.
         *
         * @return the resolved management request handler
         */
        ManagementRequestHandler<?, ?> resolveNext();

    }

}
