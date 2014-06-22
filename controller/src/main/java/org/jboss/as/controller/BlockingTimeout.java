/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.BLOCKING_TIMEOUT;

import org.jboss.as.controller.logging.ControllerLogger;
import org.jboss.dmr.ModelNode;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Encapsulates information about how long management operation execution should block
 * before timing out.
 *
 * @author Brian Stansberry (c) 2014 Red Hat Inc.
 */
class BlockingTimeout {

    public static final String SYSTEM_PROPERTY = "jboss.as.management.blocking.timeout";
    private static final int DEFAULT_TIMEOUT = 300;  // seconds
    private static final String DEFAULT_TIMEOUT_STRING = Long.toString(DEFAULT_TIMEOUT);
    private static final int SHORT_TIMEOUT = 5000;
    private static String sysPropValue;
    private static int defaultValue;

    private final int blockingTimeout;
    private final int shortTimeout;
    private volatile boolean timeoutDetected;

    BlockingTimeout(final ModelNode headerValue) {
        Integer opHeaderValue;
        if (headerValue != null && headerValue.isDefined()) {
            opHeaderValue = headerValue.asInt();
            if (opHeaderValue < 1) {
                throw ControllerLogger.MGMT_OP_LOGGER.invalidBlockingTimeout(opHeaderValue.longValue(), BLOCKING_TIMEOUT);
            }
            blockingTimeout = opHeaderValue * 1000;
        } else {
            blockingTimeout = resolveDefaultTimeout();
        }
        shortTimeout = Math.min(blockingTimeout, SHORT_TIMEOUT);
    }

    private static int resolveDefaultTimeout() {
        String propValue = WildFlySecurityManager.getPropertyPrivileged(SYSTEM_PROPERTY, DEFAULT_TIMEOUT_STRING);
        if (sysPropValue == null || !sysPropValue.equals(propValue)) {
            // First call or the system property changed
            sysPropValue = propValue;
            int number = -1;
            try {
                number = Integer.valueOf(sysPropValue);
            } catch (NumberFormatException nfe) {
                // ignored
            }

            if (number > 0) {
                defaultValue = number * 1000; // seconds to ms
            } else {
                ControllerLogger.MGMT_OP_LOGGER.invalidDefaultBlockingTimeout(sysPropValue, SYSTEM_PROPERTY, DEFAULT_TIMEOUT);
                defaultValue = DEFAULT_TIMEOUT * 1000; // seconds to ms
            }
        }
        return defaultValue;
    }

    /**
     * Gets the maximum period, in ms, a blocking call should block.
     * @return the maximum period. Will be a value greater than zero.
     */
    int getBlockingTimeout() {
        return timeoutDetected ? shortTimeout : blockingTimeout;
    }

    /**
     * Notifies this object that a timeout has occurred, allowing shorter timeouts values
     * to be returned from {@link #getBlockingTimeout()}
     */
    void timeoutDetected() {
        timeoutDetected = true;
    }
}
