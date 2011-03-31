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

package org.jboss.as.server;

import org.jboss.as.controller.ModelController;
import org.jboss.msc.service.ServiceRegistry;

/**
 *
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface ServerController extends ModelController {

    /**
     * Get this server's environment.
     *
     * @return the environment
     */
    ServerEnvironment getServerEnvironment();

    /**
     * Get this server's service container registry.
     *
     * @return the container registry
     */
    ServiceRegistry getServiceRegistry();

    /**
     * Get the server controller state.
     *
     * @return the state
     */
    State getState();

    /**
     * The server controller state.
     */
    enum State {
        /**
         * The server is starting up; both boot-time and run-time updates are being processed.
         */
        STARTING,
        /**
         * The server is running; only run-time updates are being processed.
         */
        RUNNING,
        /**
         * The server requires a restart in order to bring further updates into effect because a boot-time update
         * was processed at run time.
         */
        RESTART_REQUIRED,
    }
}
