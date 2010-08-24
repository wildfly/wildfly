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
package org.jboss.as.process.test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.jboss.as.process.AbstractProcessManagerTest;
import org.jboss.as.process.RespawnPolicy;
import org.jboss.as.process.support.CrashingProcess;
import org.jboss.as.process.support.LoggingTestRunner;
import org.jboss.as.process.support.TestProcessUtils.TestProcessListenerStream;
import org.junit.Test;
import org.junit.runner.RunWith;

import sun.jvmstat.monitor.MonitoredHost;
import sun.jvmstat.monitor.MonitoredVm;
import sun.jvmstat.monitor.MonitoredVmUtil;
import sun.jvmstat.monitor.VmIdentifier;

/**
 * 
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
@RunWith(LoggingTestRunner.class)
public class RespawnCrashedProcessesTestCase extends AbstractProcessManagerTest {

    @Test
    public void testRespawnExitedProcess() throws Exception {
        addProcess("Main", CrashingProcess.class, new TestRespawnPolicy());
        TestProcessListenerStream listener = startTestProcessListenerAndWait("Main");

        ProcessExitCodeAndShutDownLatch stopLatch = getStopTestProcessListenerLatch("Main");
        Object id = getProcessId("Main", CrashingProcess.class);
        
        detachTestProcessListener("Main");
        sendMessage("Test", "Main", lazyList("Exit1"));
        assertEquals("Main-Exit1", listener.readMessage());
        assertTrue(stopLatch.await(1000, TimeUnit.MILLISECONDS));
        assertEquals(1, stopLatch.getExitCode());
        
        //Check the process has been respawned
        listener = getTestProcessListener("Main", 1000);
        assertNotNull(listener);
        assertFalse(id.equals(getProcessId("Main", CrashingProcess.class)));
        assertTrue(id != getProcessId("Main", CrashingProcess.class));
        
        stopTestProcessListenerAndWait(listener);
        removeProcess("Main");
    }
    
    @Test
    public void testRespawnKilledProcess() throws Exception {
        addProcess("KillMe", CrashingProcess.class, new TestRespawnPolicy());
        TestProcessListenerStream listener = startTestProcessListenerAndWait("KillMe");
       
        ProcessExitCodeAndShutDownLatch stopLatch = getStopTestProcessListenerLatch("KillMe");
        Object id = getProcessId("KillMe", CrashingProcess.class);

        detachTestProcessListener("KillMe");
        killProcess("KillMe", CrashingProcess.class);
        assertTrue(stopLatch.await(1000, TimeUnit.MILLISECONDS));
        assertTrue(0 != stopLatch.getExitCode());
        
        //Check the process has been respawned
        listener = getTestProcessListener("KillMe", 1000);
        assertNotNull(listener);
        assertFalse(id.equals(getProcessId("KillMe", CrashingProcess.class)));
        
        stopTestProcessListenerAndWait(listener);
        removeProcess("KillMe");
    }
    
    @Test
    public void testDontRespawnProcessShutdownViaPM() throws Exception {
        addProcess("Main", CrashingProcess.class, new TestRespawnPolicy());
        TestProcessListenerStream listener = startTestProcessListenerAndWait("Main");
        
        stopTestProcessListenerAndWait(listener);
        
        //Check the process has not been respawned
        assertNull(getTestProcessListener("Main", 1000));
    }
    
    @Test
    public void testDontRespawnProcessWithExitCode0() throws Exception {
        addProcess("Main", CrashingProcess.class, new TestRespawnPolicy());
        TestProcessListenerStream listener = startTestProcessListenerAndWait("Main");
        
        ProcessExitCodeAndShutDownLatch stopLatch = getStopTestProcessListenerLatch("Main");
        
        detachTestProcessListener("Main");
        sendMessage("Test", "Main", lazyList("Exit0"));
        assertEquals("Main-Exit0", listener.readMessage());
        assertTrue(stopLatch.await(1000, TimeUnit.MILLISECONDS));
        assertEquals(0, stopLatch.getExitCode());
        
        //Check the process has not been respawned
        assertNull(getTestProcessListener("Main", 1000));
    }
    
    @Test
    public void testDelayInRespawningProcessesBeforeGivingUp() throws Exception {
        addProcess("KillMe", CrashingProcess.class, new TestRespawnPolicy());
        TestProcessListenerStream listener = startTestProcessListenerAndWait("KillMe");
        runDelayInRespawningProcessesBeforeGivingUp(listener);
        
        assertNull(getTestProcessListener("KillMe", 1000));
        
        listener = startTestProcessListenerAndWait("KillMe");
        runDelayInRespawningProcessesBeforeGivingUp(listener);
        
        assertNull(getTestProcessListener("KillMe", 1000));
    }
    
    private void runDelayInRespawningProcessesBeforeGivingUp(TestProcessListenerStream listener) throws Exception{
        long lastRespawn = 0;
        Object lastId = getProcessId("KillMe", CrashingProcess.class);
        
        for (int i = 0 ; i < 3 ; i++) {
            ProcessExitCodeAndShutDownLatch stopLatch = getStopTestProcessListenerLatch("KillMe");

            detachTestProcessListener("KillMe");
            long killStart = System.currentTimeMillis();
            killProcess("KillMe", CrashingProcess.class);
            assertTrue("Failed for " + i, stopLatch.await(1000, TimeUnit.MILLISECONDS));
            assertTrue(0 != stopLatch.getExitCode());
            listener.shutdown();
            
            listener = getTestProcessListener("KillMe", 1000);
            if (i == 2) {
                assertNull(listener);
            }else {
                assertNotNull(listener);
                long respawn = System.currentTimeMillis() - killStart;
                assertTrue(respawn + "<" + lastRespawn, respawn >= lastRespawn);
                lastRespawn = respawn;
                
                Object id = getProcessId("KillMe", CrashingProcess.class);
                assertFalse(lastId.equals(id));
                lastId = id;
            }
        }
        
    }
    
    
    private Object getProcessId(String processName, Class<?> clazz) throws Exception {
        String classname = clazz.getName();
        MonitoredHost local = MonitoredHost.getMonitoredHost("localhost");
        
        //OpenJDK insists this is an Integer, Sun does not seem to care the set can be parameterized with everything 
        @SuppressWarnings("unchecked")
        Set vmList = local.activeVms();
        
        Object found = null;
        for (Object id : vmList) {
            MonitoredVm vm = local.getMonitoredVm(new VmIdentifier("//" + id));
            String main = MonitoredVmUtil.mainClass(vm, true);
            if (main.equals(classname)) {
                String args =  MonitoredVmUtil.mainArgs(vm);
                if (args.contains(processName)) {
                    if (found != null)
                        fail("Found more than one process");
                    found = id;
                }
            }
        }
        if (found != null)
            return found;
        fail("Could not find process");
        return null;
    }
    
    private void killProcess(String processName, Class<?> clazz) throws Exception {
        Object id = getProcessId(processName, clazz);

        String osName = System.getProperty("os.name");
        Process proc;
        if(osName.contains("Windows"))
           proc = Runtime.getRuntime().exec("taskkill /pid " + id);
        else //TODO this only works on *nix
           proc = Runtime.getRuntime().exec("kill -9 " + id);
        
        assertEquals(0, proc.waitFor());
        return;
    }
    
    private static class TestRespawnPolicy implements RespawnPolicy{

        @Override
        public long getTimeOutMs(int retryCount) {
            if (retryCount <= 0)
                throw new IllegalArgumentException();
            switch (retryCount) {
            case 1:
                return 0;
            case 2:
                return 400;
            default:
                return -1;
            }
        }
    }
}
