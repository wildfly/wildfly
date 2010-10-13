/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jboss.as.process.support.TestFileUtils;
import org.jboss.as.process.support.TestProcessUtils;
import org.jboss.as.process.support.TestFileUtils.TestFile;
import org.jboss.as.process.support.TestProcessUtils.TestProcessController;
import org.jboss.as.process.support.TestProcessUtils.TestProcessListenerStream;
import org.jboss.as.process.support.TestProcessUtils.TestStreamManager;
import org.junit.After;
import org.junit.Before;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public abstract class AbstractProcessManagerTest {
    ProcessManagerMaster master;

    Set<String> processes = new HashSet<String>();
    Set<String> stoppedProcesses = new HashSet<String>();

    private TestStreamManager testManager;


    @Before
    public void beforeTest() throws IOException {
        System.err.println("------------> beforeTest" );
        testManager = TestProcessUtils
                .createStreamManager(new TestProcessController() {

                    @Override
                    public void stopProcess(String processName) {
                        master.stopProcess(processName);
                    }

                    @Override
                    public void startProcess(String processName) {
                        master.startProcess(processName);
                    }
                });

        master = new ProcessManagerMaster(InetAddress.getLocalHost(), 12967);
        master.start();
        TestFileUtils.cleanFiles();
    }

    @After
    public void afterTest() {
        if (master != null)
            master.shutdown();
        if (testManager != null)
            testManager.shutdown();
        master = null;
        testManager = null;
        processes.clear();
        stoppedProcesses.clear();
        System.err.println("*Test - afterTest() - end");
    }

    protected TestFile addProcess(String processName, Class<?> clazz)  throws UnknownHostException {
        return addProcess(processName, clazz, 0, false);
    }

    protected TestFile addProcess(String processName, Class<?> clazz, int debugPort, boolean suspend) throws UnknownHostException {
        master.addProcess(processName,
                TestProcessUtils.createCommand(processName, clazz.getName(), master.getPort(), debugPort, suspend),
                System.getenv(),
                ".");
        processes.add(processName);
        return TestFileUtils.getOutputFile(processName);
    }

    protected TestFile addProcess(String processName, Class<?> clazz, RespawnPolicy respawnPolicy)  throws UnknownHostException {
        return addProcess(processName, clazz, respawnPolicy, 0, false);
    }

    protected TestFile addProcess(String processName, Class<?> clazz, RespawnPolicy respawnPolicy,
            int debugPort, boolean suspend)  throws UnknownHostException {
        master.addProcess(processName,
                TestProcessUtils.createCommand(processName, clazz.getName(), master.getPort(), debugPort, suspend),
                System.getenv(),
                ".",
                respawnPolicy);
        processes.add(processName);
        return TestFileUtils.getOutputFile(processName);
    }


    protected TestProcessListenerStream startTestProcessListenerAndWait(String name) throws InterruptedException {
        return startTestProcessListenerAndWait(name, null);
    }

    protected TestProcessListenerStream startTestProcessListenerAndWait(String name, Runnable preWait)
            throws InterruptedException {
        // master.startProcess(name);
        processes.add(name);
        stoppedProcesses.remove(name);

        return testManager.createProcessListener(name, preWait);
    }

    protected void startProcess(String name, int waitMs)
            throws InterruptedException {
        master.startProcess(name);
        Thread.sleep(waitMs);
        processes.add(name);
        stoppedProcesses.remove(name);
    }

    protected void stopTestProcessListener(String name) throws InterruptedException {
        testManager.stopProcessListener(name);
    }

    protected void stopTestProcessListener(String name, int waitMs) throws InterruptedException {
        testManager.stopProcessListener(name);
        Thread.sleep(waitMs);
    }

    protected void stopTestProcessListenerAndWait(TestProcessListenerStream listener) throws InterruptedException {
        WaitForStopProcessListener stopListener = new WaitForStopProcessListener();
        master.registerStopProcessListener(listener.getProcessName(), stopListener);
        testManager.stopProcessListener(listener.getProcessName());
        stopListener.waitForStop();
    }

    protected ProcessExitCodeAndShutDownLatch getStopTestProcessListenerLatch(String processName) {
        WaitForStopProcessListener stopListener = new WaitForStopProcessListener();
        master.registerStopProcessListener(processName, stopListener);
        return new ProcessExitCodeAndShutDownLatch(stopListener);
    }

    protected TestProcessListenerStream getTestProcessListener(String name, long timeoutMillis) {
        return testManager.getProcessListener(name, timeoutMillis);
    }

    protected void detachTestProcessListener(String name) {
        testManager.detachProcessListener(name);
    }

    protected void removeProcess(String name) {
        master.removeProcess(name);
    }

    protected void sendStdin(String recipient, String msg) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            new DataOutputStream(out).writeUTF(msg);
            out.flush();
            master.sendStdin(recipient, out.toByteArray());
        } catch (UnsupportedEncodingException e) {
        } catch (IOException e) {
        }
    }

    protected List<String> lazyList(String... strings) {
        List<String> result = new ArrayList<String>();
        for (String str : strings)
            result.add(str);
        return result;
    }

    protected List<String> getProcessNames(boolean onlyStarted) {
        return master.getProcessNames(onlyStarted);
    }

    private class WaitForStopProcessListener implements StopProcessListener {
        final CountDownLatch stopLatch = new CountDownLatch(1);
        int exitCode;

        @Override
        public void processStopped(int exitCode) {
            this.exitCode = exitCode;
            stopLatch.countDown();
        }

        void waitForStop() {
            try {
                stopLatch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    protected static class ProcessExitCodeAndShutDownLatch {
        final WaitForStopProcessListener listener;

        public ProcessExitCodeAndShutDownLatch(WaitForStopProcessListener listener) {
            this.listener = listener;
        }

        public void waitForStop() {
            listener.waitForStop();
        }

        public int getExitCode() {
            return listener.exitCode;
        }

        public boolean await(long timeout, TimeUnit unit) throws InterruptedException{
            return listener.stopLatch.await(timeout, unit);
        }
    }
}
