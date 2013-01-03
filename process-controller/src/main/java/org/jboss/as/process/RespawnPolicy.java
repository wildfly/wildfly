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

package org.jboss.as.process;

import java.util.concurrent.TimeUnit;

/**
 * Policy used to automatically restart a crashed process.
 *
 * @author Emanuel Muckenhuber
 */
interface RespawnPolicy {

    /**
     * Respawn a process.
     *
     * @param count the count of attempted respawns, including this one
     * @param process the process to respawn
     * @param slowRespawn {@code true} if longer delays between respawn attempts are ok
     * @param unlimited {@code true} if there should be no limit on the number of respawn attempts
     */
    void respawn(int count, ManagedProcess process, boolean slowRespawn, boolean unlimited);

    RespawnPolicy NONE = new RespawnPolicy() {

        @Override
        public void respawn(final int count, final ManagedProcess process, boolean slowRespawn, boolean unlimited) {
            ProcessLogger.SERVER_LOGGER.tracef("not trying to respawn process %s.", process.getProcessName());
        }

    };

    RespawnPolicy RESPAWN = new RespawnPolicy() {

        private static final int MAX_NORMAL_WAIT = 30;
        private static final int MAX_SLOW_WAIT = 300;
        private static final int MAX_RESTARTS = 10;
        private final int[] NORMAL_WAITS = { 1, 5, 10, 15, MAX_NORMAL_WAIT };
        private final int[] SLOW_WAITS = { 15, 15, 30, 60, MAX_SLOW_WAIT };

        @Override
        public void respawn(final int count, final ManagedProcess process, boolean slowRespawn, boolean unlimited) {
            if (unlimited || count <= MAX_RESTARTS) {
                try {
                    final int[] waits = slowRespawn ? SLOW_WAITS : NORMAL_WAITS;
                    int waitPeriod;
                    if (count <= waits.length) {
                        waitPeriod = waits[count - 1];
                    } else {
                        waitPeriod = slowRespawn ? MAX_SLOW_WAIT : MAX_NORMAL_WAIT;
                    }
                    ProcessLogger.SERVER_LOGGER.waitingToRestart(waitPeriod, process.getProcessName());
                    TimeUnit.SECONDS.sleep(waitPeriod);
                } catch (InterruptedException e) {
                    return;
                }
                process.respawn();
            }
        }
    };


}
