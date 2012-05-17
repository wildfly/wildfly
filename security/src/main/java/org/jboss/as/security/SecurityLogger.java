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

package org.jboss.as.security;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Cause;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Logger;
import org.jboss.logging.Logger.Level;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

/**
 * This module is using message IDs in the range 13300-13399.
 * <p/>
 * This file is using the subset 13370-13399 for logger messages.
 * <p/>
 * See <a href="http://community.jboss.org/docs/DOC-16810">http://community.jboss.org/docs/DOC-16810</a> for the full
 * list of currently reserved JBAS message id blocks.
 * <p/>
 * Date: 05.11.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageLogger(projectCode = "JBAS")
public interface SecurityLogger extends BasicLogger {

    /**
     * A logger with a category of the package name.
     */
    SecurityLogger ROOT_LOGGER = Logger.getMessageLogger(SecurityLogger.class, SecurityLogger.class.getPackage().getName());

   /** Logs a message indicating the current version of the PicketBox library
    *
    * @param version a {@link String} representing the current version
    */
   @LogMessage(level = Level.INFO)
   @Message(id = 13170, value = "Current PicketBox version=%s")
   void currentVersion(String version);

   /**
    * Logs a message indicating that the security subsystem is being activated
    */
   @LogMessage(level = Level.INFO)
   @Message(id = 13171, value = "Activating Security Subsystem")
   void activatingSecuritySubsystem();

   /**
    * Logs a message indicating that there was an exception while trying to delete the JACC Policy
    * @param t the underlying exception
    */
   @LogMessage(level = Level.WARN)
   @Message(id = 13172, value = "Error deleting JACC Policy")
   void errorDeletingJACCPolicy(@Cause Throwable t);
}