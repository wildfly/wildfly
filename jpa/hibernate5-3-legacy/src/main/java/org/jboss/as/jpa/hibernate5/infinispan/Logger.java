/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.jboss.as.jpa.hibernate5.infinispan;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger.Level;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * @author Paul Ferraro
 */
@MessageLogger(projectCode = "ISNPHIB")
public interface Logger extends BasicLogger {
    Logger LOGGER = org.jboss.logging.Logger.getMessageLogger(Logger.class, "org.jboss.as.jpa.hibernate5.infinispan");

    @LogMessage(level = Level.WARN)
    @Message(id = 1, value = "Region factory \"%s\" is deprecated, please use \"%s\" instead, using <property name=\"%s\">%s</property>")
    void deprecatedRegionFactory(String deprecated, String alternative, String propertyName, String propertyValue);
}
