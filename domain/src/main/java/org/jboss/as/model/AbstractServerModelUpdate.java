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
 * An update that applies to a server model and/or a running server instance.
 * <p>
 * An update can optionally indicate that when applying it to a running server, a restart of the server
 * is required for the update to take full effect.  However such updates <b>may</b> still make a runtime change
 * as long as that change does not disrupt the current operation of the server.  In addition, the runtime change
 * should not mandatorily depend on the non-runtime component of any updates which in turn require a restart.  If
 * a restart-required change cannot be executed safely at runtime, then it should not be made at all.
 *
 * @param <R> the type of result that is returned by this update type
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public abstract class AbstractServerModelUpdate<R> extends AbstractModelUpdate<ServerModel, R> {

    private static final long serialVersionUID = 1977647714406421073L;

    private final boolean requiresRestart;

    /**
     * Construct a new instance.  The {@code requiresRestart} flag is set to {@code false}.
     */
    protected AbstractServerModelUpdate() {
        this(false);
    }

    /**
     * Construct a new instance.
     *
     * @param requiresRestart {@code true} if this update requires a restart, {@code false} otherwise
     */
    protected AbstractServerModelUpdate(final boolean requiresRestart) {
        this.requiresRestart = requiresRestart;
    }

    /** {@inheritDoc} */
    @Override
    public final Class<ServerModel> getModelElementType() {
        return ServerModel.class;
    }

    /**
     * Determine whether this update requires a restart to take effect.
     *
     * @return {@code true} if a restart is required
     */
    public final boolean requiresRestart() {
        return requiresRestart;
    }

    /** {@inheritDoc} */
    @Override
    protected abstract void applyUpdate(ServerModel element) throws UpdateFailedException;

    /**
     * Apply this update to a running service container.  The given result handler is called with the result of the
     * application.  By default, this method does nothing but report success.
     *
     * @param updateContext the update context
     * @param resultHandler the handler to call back with the result
     * @param param the parameter value to pass to the result handler
     */
    public <P> void applyUpdate(UpdateContext updateContext, UpdateResultHandler<? super R, P> resultHandler, P param) {
        resultHandler.handleSuccess(null, param);
    }

    /**
     * Apply the boot action for this update.  This action is only executed when the update is processed during
     * server startup.  By default, this method simply invokes {@link #applyUpdate(UpdateContext, UpdateResultHandler, Object)}
     * directly, but this behavior should be overriden if a different action must be taken at boot time.
     *
     * @param updateContext the update context
     */
    protected void applyUpdateBootAction(UpdateContext updateContext) {
        applyUpdate(updateContext, UpdateResultHandler.NULL, null);
    }

    /** {@inheritDoc} */
    @Override
    protected final AbstractServerModelUpdate<R> getServerModelUpdate() {
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public abstract AbstractServerModelUpdate<?> getCompensatingUpdate(ServerModel original);
}
