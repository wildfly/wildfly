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

package org.jboss.as.management.client.api;

import org.jboss.dmr.ModelNode;

/**
 * Listener for events provided by a controller as it
 * {@link ControllerClient#execute(ModelNode, ControllerExecutionListener) executes a request}.
 * As the controller executes the request, it may stream sections of the final
 * ModelNode's tree back to the {@link ControllerClient}; the {@code ControllerClient}
 * will assemble those sections into the final return value. An implementation
 * of this interface can be notified of those sections as they arrive from
 * the controller.
 * <p>
 * If a listener instantiated a new "root" {@code ModelNode} in its constructor,
 * and then for each invocation of {@link #handleExecutionEvent(ModelNode, String[])} did the
 * following:
 * <pre>
 * root.get(path).set(node);
 * </pre>
 * when the listener received the {@link #executionComplete()} notification, the
 * "root" object would have the same state as the response value.
 *
 * @author Brian Stansberry
 */
public interface ControllerExecutionListener {

    /**
     * Notification of the arrival of a portion (possibly the entirety) of the
     * ModelNode that represents the response to a request.
     *
     * @param node the newly arrived response data.
     * @param path
     *            array of strings that could be passed to
     *            {@link ModelNode#get(String...) to arrive at the location of
     *            {@code node}. Will not be {@code null} but may be an empty
     *            array, in which case {@code node} represents the root node of
     *            the response
     */
    void handleExecutionEvent(ModelNode node, String[] path);

    /**
     * Notification that no further invocations of {@link ControllerExecutionListener#handleExecutionEvent(ModelNode, String[])}
     * will be received.
     */
    void executionComplete();
}
