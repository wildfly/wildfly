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
package org.jboss.as.domain.management.connections.database;


/**
 * The reaper thread maintain the database pool, by monitoring dead connection or unused connection
 *
 * @author <a href="mailto:flemming.harms@gmail.com">Flemming Harms</a>
 */
class ConnectionReaper extends Thread {

    private final DatabaseConnectionPool pool;
    private long delay = 300000;
    private boolean terminate;

    ConnectionReaper(DatabaseConnectionPool pool, long delay) {
        this.pool = pool;
        this.delay = delay;
    }

    @Override
    public void run() {
        while (!terminate) {
            try {
                sleep(getDelay());
            } catch (InterruptedException e) {
            }
            pool.reapConnections();
        }
    }

    /**
     * @return the execution delay in milliseconds
     */
    public long getDelay() {
        return delay;
    }

    /**
     * @param delay - set the execution delay in milliseconds
     */
    public void setDelay(long delay) {
        this.delay = delay;
        notify();
    }


    public void terminate() {
        this.terminate = true;
        interrupt();
    }
}