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

import org.jboss.as.controller.AbstractModelController;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ResultHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceRegistry;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class NewServerControllerImpl extends AbstractModelController implements NewServerController {

    NewServerControllerImpl() {
        registerOperationHandler(PathAddress.EMPTY_ADDRESS, "shutdown", /*todo*/ null);
        registerOperationHandler(PathAddress.EMPTY_ADDRESS, "restart", /*todo*/ null);
    }

    /**
     * Get this server's environment.
     *
     * @return the environment
     */
    public ServerEnvironment getServerEnvironment() {
        return null;
    }

    /**
     * Get this server's service container registry.
     *
     * @return the container registry
     */
    public ServiceRegistry getServiceRegistry() {
        return null;
    }

    /**
     * Get the server controller state.
     *
     * @return the state
     */
    public State getState() {
        return null;
    }

    /**
     * Execute an operation, possibly asynchronously, sending updates and the final result to the given handler.
     *
     * @param operation the operation to execute
     * @param handler the result handler
     *
     * @return a handle which may be used to cancel the operation
     */
    public Operation execute(final ModelNode operation, final ResultHandler handler) {
        return null;
    }
}
