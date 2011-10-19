/*
 *
 *  * JBoss, Home of Professional Open Source.
 *  * Copyright 2011, Red Hat, Inc., and individual contributors
 *  * as indicated by the @author tags. See the copyright.txt file in the
 *  * distribution for a full listing of individual contributors.
 *  *
 *  * This is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU Lesser General Public License as
 *  * published by the Free Software Foundation; either version 2.1 of
 *  * the License, or (at your option) any later version.
 *  *
 *  * This software is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  * Lesser General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU Lesser General Public
 *  * License along with this software; if not, write to the Free
 *  * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 */

package org.jboss.as.ejb3;

import org.jboss.ejb.client.SessionID;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Logger;
import org.jboss.logging.Message;

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.WARN;

/**
 * Date: 19.10.2011
 *
 * @author <a href="mailto:Flemming.Harms@gmail.com">Flemming Harms</a>
 */
public interface EjbLogger extends BasicLogger {

    /**
       * Default root level logger with the package name for he category.
       */
      EjbLogger ROOT_LOGGER = Logger.getMessageLogger(EjbLogger.class, EjbLogger.class.getPackage().getName());

    /**
     * Logs an error message indicating an exception occurred while removing the an inactive bean.
     * Initialization continues after logging the error.
     * @param id the session id that could not be removed
     * @param cause the cause of the error.
     */
    @LogMessage(level = ERROR)
    @Message(id = 14100, value = "Exception removing stateful bean %s")
    void errorRemovingStatefulBean(SessionID id, Exception cause);

    /**
     * Logs an warning message indicating the it could not find a EJB for the specific id
     * Initialization continues after logging the warning.
     * @param id the session id that could not be released
     */
    @LogMessage(level = WARN)
    @Message(id = 14101, value = "Could not find stateful bean to release %s")
    void couldNotFindStatefulBean(SessionID id);
}
