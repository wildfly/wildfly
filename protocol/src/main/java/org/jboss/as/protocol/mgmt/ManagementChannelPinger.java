/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.protocol.mgmt;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;



/**
 * Workaround to handle ungraceful shutdown until this is handled better in the remoting layer
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ManagementChannelPinger extends Thread {

    private static final long PING_FREQUENCY = 2000;
    private static final long PING_TIMEOUT = 10000;
    private static volatile ManagementChannelPinger INSTANCE;
    private Set<ManagementChannel> channels = Collections.synchronizedSet(new HashSet<ManagementChannel>());


    private ManagementChannelPinger() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                ManagementChannelPinger.this.interrupt();
            }
        });
        super.setDaemon(true);
    }

    static ManagementChannelPinger getInstance() {
        if (INSTANCE == null) {
            synchronized (ManagementChannelPinger.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ManagementChannelPinger();
                    INSTANCE.start();
                }
            }
        }
        return INSTANCE;
    }

    @Override
    public void run() {
        while (!interrupted()) {
            try {
                Thread.sleep(PING_FREQUENCY);
            } catch (InterruptedException e) {
                break;
            }

            Set<ManagementChannel> channels;
            synchronized (this.channels) {
                channels = new HashSet<ManagementChannel>(this.channels);
            }

            for (ManagementChannel channel : channels) {
                if (interrupted()) {
                    interrupt();
                    break;
                }
                channel.ping(PING_TIMEOUT);
            }

            if (interrupted()) {
                break;
            }
        }
        interrupt();
    }



    void addChannel(ManagementChannel channel) {
        channels.add(channel);
    }

    void removeChannel(ManagementChannel channel) {
        channels.remove(channel);
    }
}
