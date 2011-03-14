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

package org.jboss.as.controller.client.helpers.domain;

/**
 * Exception indicating a given {@link DeploymentPlan} is invalid since it
 * could leave the domain in an invalid state.
 *
 * @author Brian Stansberry
 */
public class InvalidDeploymentPlanException extends Exception {

    private static final long serialVersionUID = 6442943555765667251L;

    /**
     * Constructs a new InvalidDeploymentPlanException with the given message.
     *
     * @param message the message
     */
    public InvalidDeploymentPlanException(String message) {
        super(message);
    }

    /**
     * Constructs a new InvalidDeploymentPlanException with the given message and cause.
     *
     * @param message the message
     * @param cause the cause
     */
    public InvalidDeploymentPlanException(String message, Exception cause) {
        super(message, cause);
    }

}
