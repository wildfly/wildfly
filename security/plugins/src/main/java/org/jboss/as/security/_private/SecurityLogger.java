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

package org.jboss.as.security._private;

import org.jboss.logging.Logger;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * Date: 05.11.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageLogger(projectCode = "WFLYSEC", length = 4)
public interface SecurityLogger extends BasicLogger {

    /**
     * A logger with a category of the package name.
     */
    SecurityLogger ROOT_LOGGER = Logger.getMessageLogger(SecurityLogger.class, "org.jboss.as.security");

    /**
     * Creates an exception indicating that the module name was missing
     * @param name the missing module name
     * @return {@link IllegalArgumentException}
     */
    @Message(id = 6, value = "Missing module name for the %s")
    IllegalArgumentException missingModuleName(String name);

    /**
     * Creates a {@link RuntimeException}
     * @param e the underlying exception
     * @return the exception
     */
    @Message(id = 7, value = "Runtime Exception:")
    RuntimeException runtimeException(@Cause Throwable e);
}
