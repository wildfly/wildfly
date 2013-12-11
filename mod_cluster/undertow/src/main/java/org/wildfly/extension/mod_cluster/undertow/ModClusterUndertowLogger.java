/**
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.mod_cluster.undertow;


import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * Logging range 21100 - 21199.
 * <p/>
 * For all ranges, see <a href="https://community.jboss.org/wiki/LoggingIds">Logging Ids document</a>.
 *
 * @author Radoslav Husar
 * @version Dec 2013
 * @since 8.0
 */
@MessageLogger(projectCode = "JBAS")
interface ModClusterUndertowLogger extends BasicLogger {

    ModClusterUndertowLogger ROOT_LOGGER = Logger.getMessageLogger(ModClusterUndertowLogger.class, ModClusterUndertowLogger.class.getPackage().getName());

    @LogMessage(level = Logger.Level.INFO)
    @Message(value = "Starting mod_cluster-Undertow integration subsystem", id = 21100)
    void subsystemStarting();

}
