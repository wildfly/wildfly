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

package org.jboss.system.logging;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageLogger(projectCode = "WFLYSYSJMX", length = 4)
public interface ServiceMBeanLogger extends BasicLogger {

    ServiceMBeanLogger ROOT_LOGGER = Logger.getMessageLogger(ServiceMBeanLogger.class, "org.jboss.system");

    @Message(id = 1, value = "Null method name")
    IllegalArgumentException nullMethodName();

    @Message(id = 2, value = "Unknown lifecyle method %s")
    IllegalArgumentException unknownLifecycleMethod(String methodName);

    @Message(id = 3, value = "Error in destroy %s")
    String errorInDestroy(String description);

    @Message(id = 4, value = "Error in stop %s")
    String errorInStop(String description);

    @Message(id = 5, value = "Initialization failed %s")
    String initializationFailed(String description);

    @Message(id = 6, value = "Starting failed %s")
    String startingFailed(String description);

    @Message(id = 7, value = "Stopping failed %s")
    String stoppingFailed(String description);

    @Message(id = 8, value = "Destroying failed %s")
    String destroyingFailed(String description);

    @Message(id = 9, value = "Initialization failed during postRegister")
    String postRegisterInitializationFailed();
}
