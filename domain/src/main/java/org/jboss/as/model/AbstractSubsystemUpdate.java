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

import org.jboss.as.deployment.DeployerRegistrationContext;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public abstract class AbstractSubsystemUpdate<E extends AbstractSubsystemElement<E>, R> extends AbstractModelElementUpdate<E> {
    private static final long serialVersionUID = 5066326932283149448L;

    private final String subsystemNamespaceUri;
    private final boolean requiresRestart;

    protected AbstractSubsystemUpdate(final String subsystemNamespaceUri, final boolean restart) {
        this.subsystemNamespaceUri = subsystemNamespaceUri;
        requiresRestart = restart;
    }

    protected AbstractSubsystemUpdate(final String subsystemNamespaceUri) {
        this(subsystemNamespaceUri, false);
    }

    /**
     * Determine whether this update requires a restart to take effect.
     *
     * @return {@code true} if a restart is required
     */
    public final boolean requiresRestart() {
        return requiresRestart;
    }

    /**
     * Get the namespace URI of the subsystem configured by this update.
     *
     * @return the URI
     */
    public final String getSubsystemNamespaceUri() {
        return subsystemNamespaceUri;
    }

    /**
     * Apply this update to a running service container.  The given result handler is called with the result of the
     * application.
     *
     * @param updateContext the update context
     * @param resultHandler the handler to call back with the result
     * @param param the parameter value to pass to the result handler
     */
    protected abstract <P> void applyUpdate(UpdateContext updateContext, UpdateResultHandler<? super R, P> resultHandler, P param);

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
    public abstract AbstractSubsystemUpdate<E, ?> getCompensatingUpdate(E original);
}
