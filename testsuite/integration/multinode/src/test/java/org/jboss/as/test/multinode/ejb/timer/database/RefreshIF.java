/*
 * JBoss, Home of Professional Open Source
 * Copyright 2020, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

package org.jboss.as.test.multinode.ejb.timer.database;

import java.io.Serializable;
import java.util.List;
import javax.ejb.Remote;

@Remote
public interface RefreshIF {
    /**
     * Used to indicate in which node and by which bean the timer is created.
     */
    enum Info {
        /**
         * The timer is created in the client node and by bean 1.
         */
        CLIENT1,

        /**
         * The timer is created in the server node and by bean 1.
         */
        SERVER1,

        /**
         * Indicates that a timer handle should be returned
         */
        RETURN_HANDLE
    }

    /**
     * Creates a timer to expire {@code delay} milliseconds later, with {@code info}
     * as the timer info.
     *
     * @param delay number of milliseconds after which the timer is set to expire
     * @param info timer info
     * @return timer handle for the new timer
     */
    byte[] createTimer(long delay, Serializable info);

    /**
     * Gets all timers after programmatic refresh. Any implementation method
     * should be configured to have an interceptor that enables programmatic
     * timer refresh.
     *
     * @return list of timer info
     */
    List<Serializable> getAllTimerInfoWithRefresh();

    /**
     * Gets all timers after programmatic refresh. Any implementation method
     * should be configured to have an interceptor that enables programmatic
     * timer refresh.
     * <p>
     * This method demonstrates that a bean class can invoke its own business
     * method that enables programmatic timer refresh, without replying on an
     * external client invocation.
     *
     * @return list of timer info
     */
    List<Serializable> getAllTimerInfoWithRefresh2();

    /**
     * Gets all timers without programmatic refresh. Any implementation method
     * should NOT be configured to have an interceptor that enables programmatic
     * timer refresh.
     *
     * @return list of timer info
     */
    List<Serializable> getAllTimerInfoNoRefresh();

    /**
     * Cancels timers of this bean.
     */
    void cancelTimers();

    /**
     * Cancels a timer by its timer handle.
     * @param handle the timer handle for the timer to be cancelled
     */
    void cancelTimer(byte[] handle);
}
