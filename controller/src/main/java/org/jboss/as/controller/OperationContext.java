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

import org.jboss.as.controller.client.ExecutionAttachments;
import org.jboss.as.controller.registry.ModelNodeRegistration;
import org.jboss.dmr.ModelNode;

/**
 * The context passed from the {@link ModelController} to the {@link OperationHandler}s.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface OperationContext extends ExecutionAttachments {

    /**
     * Get the model controller performing this update.
     *
     * @return the controller
     */
    ModelController getController();

    /**
     * Get the model node registry
     *
     * @return the registry
     */
    ModelNodeRegistration getRegistry();

    /**
     * Gets a view of the sub-model that this operation affects, if it does affect
     * a model element.
     *
     * @return the sub-model view
     * @throws IllegalArgumentException if no sub-model is associated with this operation
     */
    ModelNode getSubModel() throws IllegalArgumentException;

    /**
     * Gets a read-only view of the sub-model at the given address.
     *
     * @param the address. Cannot be {@code null}
     *
     * @return the sub-model view
     * @throws IllegalArgumentException if no sub-model is associated with this operation
     */
    ModelNode getSubModel(PathAddress address) throws IllegalArgumentException;

    /**
     * Get access to the runtime context for this operation
     *
     * @return The runtime context if this operation is running in a runtime capable state, otherwise null.
     */
    RuntimeOperationContext getRuntimeContext();
}
