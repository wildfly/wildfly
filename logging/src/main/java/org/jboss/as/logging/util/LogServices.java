/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.logging.util;

import org.jboss.as.logging.CommonAttributes;
import org.jboss.msc.service.ServiceName;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class LogServices {

    public static final ServiceName JBOSS_LOGGING = ServiceName.JBOSS.append("logging");

    public static final ServiceName LOGGER = JBOSS_LOGGING.append("logger");

    public static final ServiceName ROOT_LOGGER = JBOSS_LOGGING.append("root-logger", CommonAttributes.ROOT_LOGGER_NAME);

    public static final ServiceName LOGGER_HANDLER = JBOSS_LOGGING.append("logger-handler");

    public static final ServiceName ROOT_LOGGER_HANDLER = JBOSS_LOGGING.append("root-logger-handler");

    public static final ServiceName HANDLER = JBOSS_LOGGING.append("handler");

    public static final ServiceName HANDLER_FILE = JBOSS_LOGGING.append("handler-file");

    private LogServices() {
    }

    public static ServiceName loggerName(final String name) {
        return CommonAttributes.ROOT_LOGGER_NAME.equals(name) ? ROOT_LOGGER : LOGGER.append(name);
    }

    public static ServiceName loggerHandlerName(final String loggerName, final String handlerName) {
        return CommonAttributes.ROOT_LOGGER_NAME.equals(loggerName) ?
                ROOT_LOGGER_HANDLER.append(CommonAttributes.ROOT_LOGGER_NAME, handlerName) :
                LOGGER_HANDLER.append(loggerName, handlerName);
    }

    public static ServiceName handlerName(final String name) {
        return HANDLER.append(name);
    }

    public static ServiceName handlerFileName(final String handlerName) {
        return HANDLER_FILE.append(handlerName);
    }

}
