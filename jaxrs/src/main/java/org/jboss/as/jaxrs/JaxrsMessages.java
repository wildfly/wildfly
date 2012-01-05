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

package org.jboss.as.jaxrs;

import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.logging.Cause;
import org.jboss.logging.Message;
import org.jboss.logging.MessageBundle;
import org.jboss.logging.Messages;

import javax.ws.rs.core.Application;

/**
 * Date: 05.11.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageBundle(projectCode = "JBAS")
public interface JaxrsMessages {

    /**
     * The messages.
     */
    JaxrsMessages MESSAGES = Messages.getBundle(JaxrsMessages.class);

    /**
     * Creates an exception indicating the JAX-RS application class could not be loaded.
     *
     * @param cause the cause of the error.
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 11230, value = "Could not load JAX-RS Application class")
    DeploymentUnitProcessingException cannotLoadApplicationClass(@Cause Throwable cause);

    /**
     * Creates an exception indicating more than one application class found in deployment.
     *
     * @param app1 the first application.
     * @param app2 the second application.
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 11231, value = "More than one Application class found in deployment %s and %s")
    DeploymentUnitProcessingException moreThanOneApplicationClassFound(Class<? extends Application> app1, Class<? extends Application> app2);

    /**
     * A message indicating only one JAX-RS application class is allowed.
     *
     * @param sb a builder with application classes.
     *
     * @return the message.
     */
    @Message(id = 11232, value = "Only one JAX-RS Application Class allowed. %s")
    String onlyOneApplicationClassAllowed(StringBuilder sb);

    /**
     * A message indicating the incorrect mapping config.
     *
     * @param sb a builder with application classes.
     *
     * @return the message.
     */
    @Message(id = 11233, value = "Please use either @ApplicationPath or servlet mapping for url path config.")
    String conflictUrlMapping();

}

