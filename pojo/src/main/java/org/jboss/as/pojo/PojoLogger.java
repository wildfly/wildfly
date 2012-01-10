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

package org.jboss.as.pojo;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Cause;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Logger;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
@MessageLogger(projectCode = "JBAS")
public interface PojoLogger extends BasicLogger {

    /**
     * A logger with a category of the package name.
     */
    PojoLogger ROOT_LOGGER = Logger.getMessageLogger(PojoLogger.class, PojoLogger.class.getPackage().getName());

    /**
     * Log old namespace usage.
     *
     * @param namespace the namespace
     */
    @LogMessage(level = INFO)
    @Message(id = 17000, value = "Found legacy bean/pojo namespace: %s - might be missing some xml features (potential exceptions).")
    void oldNamespace(Object namespace);

    /**
     * Error at uninstall.
     *
     * @param joinpoint the joinpoint
     * @param cause the cause of the error.
     */
    @LogMessage(level = WARN)
    @Message(id = 17001, value = "Ignoring uninstall action on target: %s")
    void ignoreUninstallError(Object joinpoint, @Cause Throwable cause);

    /**
     * Error invoking callback.
     *
     * @param callback the callback
     * @param cause the cause of the error.
     */
    @LogMessage(level = WARN)
    @Message(id = 17002, value = "Error invoking callback: %s")
    void invokingCallback(Object callback, @Cause Throwable cause);

    /**
     * Error at incallback.
     *
     * @param callback the callback
     * @param cause the cause of the error.
     */
    @LogMessage(level = WARN)
    @Message(id = 17003, value = "Error invoking incallback: %s")
    void errorAtIncallback(Object callback, @Cause Throwable cause);

    /**
     * Error at uncallback.
     *
     * @param callback the callback
     * @param cause the cause of the error.
     */
    @LogMessage(level = WARN)
    @Message(id = 17004, value = "Error invoking uncallback: %s")
    void errorAtUncallback(Object callback, @Cause Throwable cause);
}
