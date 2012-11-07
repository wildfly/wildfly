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

package org.jboss.as.host.controller;

import java.util.concurrent.TimeUnit;

/**
 * Policy used to automatically try to reconnect to a crashed master HC.
 *
 * @author Emanuel Muckenhuber
 */
interface ReconnectPolicy {

    /**
     * Respawn a process.
     *
     * @param count
     */
    void wait(int count) throws InterruptedException;

    ReconnectPolicy CONNECT = new ReconnectPolicy() {

        @Override
        public void wait(int count) throws InterruptedException {
            final int waitPeriod;
            if (count < 5) {
                waitPeriod = 1;
            } else if (count >= 5 && count < 10) {
                waitPeriod = 3;
            } else if (count >= 10 && count < 15) {
                waitPeriod = 10;
            } else {
                waitPeriod = 20;
            }
            TimeUnit.SECONDS.sleep(waitPeriod);
        }
    };

    ReconnectPolicy RECONNECT = new ReconnectPolicy() {

        private static final int MAX_WAIT = 15;

        @Override
        public void wait(final int count) throws InterruptedException {
            final int waitPeriod = Math.min((count * count), MAX_WAIT);
            TimeUnit.SECONDS.sleep(waitPeriod);
        }
    };

}
