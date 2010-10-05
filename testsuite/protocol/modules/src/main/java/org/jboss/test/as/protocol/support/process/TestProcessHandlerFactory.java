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
package org.jboss.test.as.protocol.support.process;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jboss.as.process.ManagedProcess.ProcessHandler;
import org.jboss.as.process.ProcessManagerMaster.ProcessHandlerFactory;

/**
 * A process handler factory that differs from the default one by creating
 * process handlers starting server and server manager processes in-process.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class TestProcessHandlerFactory implements ProcessHandlerFactory {

    private final boolean useRealServerManager;
    private final boolean useRealServers;
    private final Map<String, CountDownLatch> processLatches = new HashMap<String, CountDownLatch>();
    private final Map<String, TestProcessHandler> createdProcesses = new HashMap<String, TestProcessHandler>();


    public TestProcessHandlerFactory(boolean useRealServerManager, boolean useRealServers) {
        this.useRealServerManager = useRealServerManager;
        this.useRealServers = useRealServers;
    }

    @Override
    public ProcessHandler createHandler() {
        return new TestProcessHandler(this, useRealServerManager, useRealServers);
    }

    public void resetLatch(String processName) {
        synchronized (this) {
            processLatches.put(processName, new CountDownLatch(1));
        }
    }

    public void addProcessHandler(String processName, TestProcessHandler handler) {
        CountDownLatch latch;
        synchronized (this) {
            latch = processLatches.get(processName);
            if (latch == null) {
                latch = new CountDownLatch(1);
                processLatches.put(processName, latch);
            }
            createdProcesses.put(processName, handler);
        }
        latch.countDown();
    }

    public TestProcessHandler getProcessHandler(String processName) throws InterruptedException {
        CountDownLatch latch;
        synchronized (this) {
            latch = processLatches.get(processName);
            if (latch == null) {
                latch = new CountDownLatch(1);
                processLatches.put(processName, latch);
            }
        }
        if (!latch.await(10, TimeUnit.SECONDS)){
            throw new IllegalStateException("Read timed out");
        }
        synchronized (this) {
            return createdProcesses.get(processName);
        }
    }

}
