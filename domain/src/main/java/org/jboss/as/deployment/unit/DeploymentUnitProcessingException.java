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

package org.jboss.as.deployment.unit;

import org.jboss.as.deployment.DeploymentException;
import org.jboss.msc.service.Location;

/**
 * An exception which is thrown when deployment unit processing fails.  This can occur as a result of a failure
 * to parse a descriptor, an error transforming a descriptor, an error preparing a deployment item, or other causes.
 */
public class DeploymentUnitProcessingException extends DeploymentException {

    private static final long serialVersionUID = -3242784227234412566L;

    private final Location location;

    /**
     * Constructs a {@code DeploymentUnitProcessingException} with no detail message. The cause is not initialized, and may
     * subsequently be initialized by a call to {@link #initCause(Throwable) initCause}.
     *
     * @param location the location at which the processing error occurred
     */
    public DeploymentUnitProcessingException(final Location location) {
        this.location = location;
    }

    /**
     * Constructs a {@code DeploymentUnitProcessingException} with the specified detail message. The cause is not
     * initialized, and may subsequently be initialized by a call to {@link #initCause(Throwable) initCause}.
     *
     * @param msg the detail message
     */
    public DeploymentUnitProcessingException(final String msg) {
        super(msg);
        this.location = new Location(this.getStackTrace()[0].getFileName(), this.getStackTrace()[0].getLineNumber(), -1, null);
    }

    /**
     * Constructs a {@code DeploymentUnitProcessingException} with the specified detail message. The cause is not
     * initialized, and may subsequently be initialized by a call to {@link #initCause(Throwable) initCause}.
     *
     * @param msg the detail message
     * @param location the location at which the processing error occurred
     */
    public DeploymentUnitProcessingException(final String msg, final Location location) {
        super(msg);
        this.location = location;
    }

    /**
     * Constructs a {@code DeploymentUnitProcessingException} with the specified cause. The detail message is set to:
     * <pre>(cause == null ? null : cause.toString())</pre>
     * (which typically contains the class and detail message of {@code cause}).
     *
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method)
     */
    public DeploymentUnitProcessingException(final Throwable cause) {
        super(cause);
        this.location = new Location(cause.getStackTrace()[0].getFileName(), cause.getStackTrace()[0].getLineNumber(), -1, null);
    }


    /**
     * Constructs a {@code DeploymentUnitProcessingException} with the specified cause. The detail message is set to:
     * <pre>(cause == null ? null : cause.toString())</pre>
     * (which typically contains the class and detail message of {@code cause}).
     *
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method)
     * @param location the location at which the processing error occurred
     */
    public DeploymentUnitProcessingException(final Throwable cause, final Location location) {
        super(cause);
        this.location = location;
    }

    /**
     * Constructs a {@code DeploymentUnitProcessingException} with the specified detail message and cause.
     *
     * @param msg the detail message
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method)
     */
    public DeploymentUnitProcessingException(final String msg, final Throwable cause) {
        super(msg, cause);
        this.location = new Location(cause.getStackTrace()[0].getFileName(), cause.getStackTrace()[0].getLineNumber(), -1, null);
    }

    /**
     * Constructs a {@code DeploymentUnitProcessingException} with the specified detail message and cause.
     *
     * @param msg the detail message
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method)
     * @param location the location at which the processing error occurred
     */
    public DeploymentUnitProcessingException(final String msg, final Throwable cause, final Location location) {
        super(msg, cause);
        this.location = location;
    }

    /**
     * Get the location at which this exception occurred, or {@code null} if it is unknown.
     *
     * @return the location
     */
    public Location getLocation() {
        return location;
    }
}
