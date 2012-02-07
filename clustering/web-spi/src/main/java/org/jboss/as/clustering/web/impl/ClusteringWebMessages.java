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

package org.jboss.as.clustering.web.impl;

import org.jboss.logging.Cause;
import org.jboss.logging.Message;
import org.jboss.logging.MessageBundle;
import org.jboss.logging.Messages;

/**
 * Date: 30.08.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageBundle(projectCode = "JBAS")
interface ClusteringWebMessages {

    /**
     * The messages.
     */
    ClusteringWebMessages MESSAGES = Messages.getBundle(ClusteringWebMessages.class);

    /**
     * Creates an exception indicating that an exception occurred while executing the method represented by the
     * {@code methodName} parameter..
     *
     * @param cause      the cause of the error.
     * @param methodName the name of the method the error occurred in.
     *
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 10310, value = "%s: Caught Exception ending batch: ")
    RuntimeException caughtExceptionEndingBatch(@Cause Throwable cause, String methodName);

    /**
     * Creates an exception indicating the {@code Object} represented by the {@code value} parameter does not implement
     * the interface represented by the {@code interfaceName} parameter.
     *
     * @param value         the object that does not implement the interface.
     * @param interfaceName the interface name that should be implemented.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 10311, value = "%s does not implement %s")
    IllegalArgumentException interfaceNotImplemented(Object value, String interfaceName);

    /**
     * Creates an exception indicating the session is not configured to provide session attributes.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 10312, value = "Not configured to provide session attributes")
    IllegalStateException sessionAttributesNotConfigured();
}
