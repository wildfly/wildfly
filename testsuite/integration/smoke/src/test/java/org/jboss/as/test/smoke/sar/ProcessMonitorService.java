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
package org.jboss.as.test.smoke.sar;

import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.logging.Logger;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class ProcessMonitorService implements ProcessMonitorServiceMBean {

    Logger log = Logger.getLogger(ProcessMonitorService.class);

    private ConfigServiceMBean config;

    AtomicBoolean stop = new AtomicBoolean();

    public void setConfig(ConfigServiceMBean config) {
        this.config = config;
    }

    public void start() {
        log.trace("Starting " + config.getExampleName());

        Thread t = new Thread(new Runnable() {

            @Override
            public void run() {
                long starttime = System.currentTimeMillis();
                while (!stop.get()) {
                    double totalmemory = bytesToMb(Runtime.getRuntime().totalMemory());
                    double usedmemory = totalmemory - bytesToMb(Runtime.getRuntime().freeMemory());
                    long seconds = (System.currentTimeMillis() - starttime)/1000;

                    log.trace(config.getExampleName() + "-Montitor: System using " + usedmemory + " Mb of " + totalmemory + " Mb after " + seconds + " seconds");
                    try {
                        Thread.sleep(config.getIntervalSeconds() * 1000);
                    } catch (InterruptedException e) {
                        stop.set(true);
                    }
                }
            }
        });
        t.start();
    }

    public void stop() {
        stop.set(true);
        log.trace("Stopping " + config.getExampleName());
    }

    static double bytesToMb(double d) {
        d = d/(1024*1024);
        d = Math.round(d*100);
        return d/100;
    }



}
