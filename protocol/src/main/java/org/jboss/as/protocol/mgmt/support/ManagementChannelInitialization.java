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

package org.jboss.as.protocol.mgmt.support;

import org.jboss.remoting3.Channel;

import java.util.concurrent.TimeUnit;

/**
 * @author Emanuel Muckenhuber
 */
public interface ManagementChannelInitialization {

    /**
     * Initialize the management channel and start receiving request.
     *
     * @param channel opened channel
     * @return the channel handler
     */
    ManagementChannelShutdownHandle startReceiving(Channel channel);

    public interface ManagementChannelShutdownHandle {

        /**
         * Don't allow new operations, but still allow requests for existing ones.
         *
         * <p>This method does not wait for previously submitted operations to be
         * completed. Use {@link #awaitCompletion awaitCompletion} to do that.
         * </p>
         */
        void shutdown();

        /**
         * This will attempt to cancel all active operations, without waiting for their completion.
         */
        void shutdownNow();

        /**
         * Await the completion of all currently active operations.
         *
         * @param timeout the timeout
         * @param unit the time unit
         * @return {@code false} if the timeout was reached and there were still active operations
         * @throws InterruptedException
         */
        boolean awaitCompletion(long timeout, TimeUnit unit) throws InterruptedException;

    }

}
