/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
