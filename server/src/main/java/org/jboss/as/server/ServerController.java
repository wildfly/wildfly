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

import org.jboss.as.model.AbstractServerModelUpdate;
import org.jboss.as.model.UpdateResultHandler;
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
