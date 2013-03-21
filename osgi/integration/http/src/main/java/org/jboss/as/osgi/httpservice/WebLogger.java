/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2013, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 * /
 */

package org.jboss.as.osgi.httpservice;

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
@MessageLogger(projectCode = "JBAS")
public interface WebLogger extends BasicLogger {


    /**
     * A logger with the category {@code org.jboss.web}.
     */
    WebLogger WEB_LOGGER = Logger.getMessageLogger(WebLogger.class, "org.jboss.web");

    @LogMessage(level = ERROR)
    @Message(id = 18208, value = "Failed to start context")
    void stopContextFailed(@Cause Throwable cause);

    @LogMessage(level = ERROR)
    @Message(id = 18209, value = "Failed to destroy context")
    void destroyContextFailed(@Cause Throwable cause);

    @LogMessage(level = INFO)
    @Message(id = 18210, value = "Register web context: %s")
    void registerWebapp(String webappPath);

    @Message(id = 18039, value = "Failed to create context")
    String createContextFailed();

    @Message(id = 18040, value = "Failed to start context")
    String startContextFailed();
}
