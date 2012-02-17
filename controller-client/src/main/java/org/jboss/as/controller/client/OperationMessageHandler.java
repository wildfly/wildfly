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

package org.jboss.as.controller.client;

import static org.jboss.as.controller.client.ControllerClientLogger.ROOT_LOGGER;

/**
 * An operation message handler for handling progress reports.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface OperationMessageHandler {
    /**
     * Handle an operation progress report.
     *
     * @param severity the severity of the message
     * @param message the message
     */
    void handleReport(MessageSeverity severity, String message);

    /**
     * An operation message handler which logs to the current system log.
     */
    OperationMessageHandler logging = new OperationMessageHandler() {

        public void handleReport(final MessageSeverity severity, final String message) {
            switch (severity) {
                case ERROR:
                    ROOT_LOGGER.error(message);
                    break;
                case WARN:
                    ROOT_LOGGER.warn(message);
                    break;
                case INFO:
                default:
                    ROOT_LOGGER.trace(message);
                    break;
            }
        }
    };
}
