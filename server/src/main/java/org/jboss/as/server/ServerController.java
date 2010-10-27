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

package org.jboss.as.server;

import java.util.List;

import org.jboss.as.model.AbstractServerModelUpdate;
import org.jboss.as.model.ServerModel;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.as.model.UpdateResultHandlerResponse;
import org.jboss.msc.service.ServiceName;

/**
 * The API entry point for a server controller.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface ServerController {

    /**
     * The name at which this controller is installed.
     */
    ServiceName SERVICE_NAME = ServiceName.JBOSS.append("as", "server", "controller");

    /**
     * Get the server model
     *
     * @return the server model
     */
    ServerModel getServerModel();

    /**
     * Apply a series of updates.
     *
     * @param updates the updates
     * @param rollbackOnFailure <code>true</code> if successfully applied updates
     *                          should be rolled back if an update later in the list fails
     * @param modelOnly <code>true</code> if the updates should only be applied
     *                  to the ServerModel and should not be applied
     *                to the runtime, <code>false</code> if they should also
     *                be applied to the runtime. A {@code true} value is only
     *                legal on a standalone server
     *
     * @return the results of the updates
     *
     * @throws IllegalStateException if this is not a standalone server and
     *           {@code modelOnly} is {@code true}
     */
    List<UpdateResultHandlerResponse<?>> applyUpdates(List<AbstractServerModelUpdate<?>> updates,
            boolean rollbackOnFailure, boolean applyToRuntime);

    /**
     * Apply a persistent update.
     *
     * @param update the update to apply
     * @param resultHandler the result handler
     * @param param the result handler parameter
     * @param <R> the result type
     * @param <P> the result handler parameter type
     */
    <R, P> void update(AbstractServerModelUpdate<R> update, UpdateResultHandler<R, P> resultHandler, P param);

    // TODO - runtime-only updates
    // <R, P> void update(Something<R> update, UpdateResultHandler<R, P> resultHandler, P param);

    /**
     * Shut down the server.
     */
    void shutdown();
}
