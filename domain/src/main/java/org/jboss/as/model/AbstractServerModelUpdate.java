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

import org.jboss.msc.service.ServiceContainer;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public abstract class AbstractServerModelUpdate<R> extends AbstractModelUpdate<ServerModel, R> {

    private static final long serialVersionUID = 1977647714406421073L;

    /** {@inheritDoc} */
    protected final Class<ServerModel> getModelElementType() {
        return ServerModel.class;
    }

    /**
     * Determine whether this update requires a restart to take effect.  If {@code true}, then
     * {@link #applyUpdate(ServiceContainer, UpdateResultHandler, Object)} should not be invoked unless the server
     * is in the process of starting up.
     *
     * @return {@code true} if a restart is required
     */
    public abstract boolean requiresRestart();

    /** {@inheritDoc} */
    protected abstract void applyUpdate(ServerModel element) throws UpdateFailedException;

    /**
     * Apply this update to a running service container.  The given result handler is called with the result of the
     * application.
     *
     * @param container the container
     * @param resultHandler the handler to call back with the result
     * @param param the parameter value to pass to the result handler
     * @param <P> the result parameter type
     */
    public abstract <P> void applyUpdate(ServiceContainer container, UpdateResultHandler<R, P> resultHandler, P param);

    /** {@inheritDoc} */
    protected final AbstractServerModelUpdate<R> getServerModelUpdate() {
        return this;
    }

    /** {@inheritDoc} */
    protected abstract AbstractServerModelUpdate<?> getCompensatingUpdate(ServerModel original);
}
