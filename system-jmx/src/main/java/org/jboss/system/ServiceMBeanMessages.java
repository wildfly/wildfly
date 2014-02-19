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

package org.jboss.system;

import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;
import org.jboss.logging.Messages;

/**
 * This module is using message IDs in the range 17800-17899. This file is using the subset 17800-17849 for
 * non-logger messages. See http://community.jboss.org/docs/DOC-16810 for the full list of currently reserved
 * JBAS message id blocks.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
@MessageBundle(projectCode = "JBAS")
public interface ServiceMBeanMessages {

    /**
     * The messages.
     */
    ServiceMBeanMessages MESSAGES = Messages.getBundle(ServiceMBeanMessages.class);

    @Message(id = 17800, value = "Null method name")
    IllegalArgumentException nullMethodName();

    @Message(id = 17801, value = "Unknown lifecyle method %s")
    IllegalArgumentException unknownLifecycleMethod(String methodName);

    @Message(id = 17802, value = "Error in destroy %s")
    IllegalArgumentException errorInDestroy(@Cause Throwable cause, String description);

    @Message(id = 17803, value = "Error in stop %s")
    IllegalArgumentException errorInStop(@Cause Throwable cause, String description);

    @Message(id = 17804, value = "Initialization failed %s")
    IllegalArgumentException initializationFailed(@Cause Throwable cause, String description);

    @Message(id = 17805, value = "Starting failed %s")
    IllegalArgumentException startingFailed(@Cause Throwable cause, String description);

    @Message(id = 17806, value = "Stopping failed %s")
    IllegalArgumentException stoppingFailed(@Cause Throwable cause, String description);

    @Message(id = 17807, value = "Destroying failed %s")
    IllegalArgumentException destroyingFailed(@Cause Throwable cause, String description);

    @Message(id = 17808, value = "Initialization failed during postRegister")
    String postRegisterInitializationFailed();
}
