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

package org.jboss.as.naming.remote;

import java.io.IOException;
import static org.jboss.as.naming.NamingLogger.ROOT_LOGGER;
import org.jboss.naming.remote.server.RemoteNamingServerLogger;
import org.jboss.remoting3.Channel;

/**
 * @author John Bailey
 */
class RemoteNamingLogger implements RemoteNamingServerLogger {
    static final RemoteNamingLogger INSTANCE = new RemoteNamingLogger();

    private RemoteNamingLogger() {
    }

    public void failedToSendHeader(final IOException exception) {
        ROOT_LOGGER.failedToSendHeader(exception);
    }

    public void failedToDetermineClientVersion(final IOException exception) {
        ROOT_LOGGER.failedToDetermineClientVersion(exception);
    }

    public void closingChannel(final Channel channel, final Throwable t) {
        ROOT_LOGGER.closingChannel(channel, t);
    }

    public void closingChannelOnChannelEnd(final Channel channel) {
        ROOT_LOGGER.closingChannelOnChannelEnd(channel);
    }

    public void unnexpectedError(final Throwable t) {
        ROOT_LOGGER.unnexpectedError(t);
    }

    public void nullCorrelationId(final Throwable t) {
        ROOT_LOGGER.nullCorrelationId(t);
    }

    public void failedToSendExceptionResponse(final IOException exception) {
        ROOT_LOGGER.failedToSendExceptionResponse(exception);
    }

    public void unexpectedParameterType(final byte expected, final byte actual) {
        ROOT_LOGGER.unexpectedParameterType(expected, actual);
    }
}
