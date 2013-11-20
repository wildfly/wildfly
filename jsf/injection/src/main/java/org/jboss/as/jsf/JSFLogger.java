/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.jsf;

import static org.jboss.logging.Logger.Level.WARN;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * Date: 05.11.2011
 * <p/>
 * <p/>
 * 12600-12610
 *
 * @author Stuart Douglas
 */
@MessageLogger(projectCode = "JBAS")
public interface JSFLogger extends BasicLogger {

    /**
     * A logger with a category of the package name.
     */
    JSFLogger ROOT_LOGGER = Logger.getMessageLogger(JSFLogger.class, "org.jboss.as.jsf");

    @LogMessage(level = WARN)
    @Message(id = 12604, value = "WildFlyConversationAwareViewHandler was improperly initialized. Expected ViewHandler parent.")
    void viewHandlerImproperlyInitialized();
}
