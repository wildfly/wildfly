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

package org.jboss.as.mail.extension;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Logger;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.INFO;

/**
 * Date: 05.11.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageLogger(projectCode = "JBAS")
interface MailLogger extends BasicLogger {

    /**
     * A logger with a category of the package name.
     */
    MailLogger ROOT_LOGGER = Logger.getMessageLogger(MailLogger.class, MailLogger.class.getPackage().getName());

    /**
     * Logs an info message indicating a javax.mail.Session was bound into JNDI.
     *
     * @param jndiName the JNDI name under which the session was bound.
     */
    @LogMessage(level = INFO)
    @Message(id = 15400, value = "Bound mail session [%s]")
    void boundMailSession(String jndiName);

    /**
     * Logs an info message indicating a javax.mail.Session was unbound from JNDI.
     *
     * @param jndiName the JNDI name under which the session was bound.
     */
    @LogMessage(level = INFO)
    @Message(id = 15401, value = "Unbound mail session [%s]")
    void unboundMailSession(String jndiName);

    /**
     * Logs a debug message indicating a javax.mail.Session was removed.
     *
     * @param jndiName the JNDI name under which the session had been bound.
     */
    @LogMessage(level = DEBUG)
    @Message(id = 15402, value = "Removed mail session [%s]")
    void removedMailSession(String jndiName);
}
