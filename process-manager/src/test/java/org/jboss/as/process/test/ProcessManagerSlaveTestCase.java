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
package org.jboss.as.process.test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.jboss.as.process.AbstractProcessManagerTest;
import org.jboss.as.process.support.LoggingTestRunner;
import org.jboss.as.process.support.ProcessManagerSlaveProcess;
import org.jboss.as.process.support.StartStopSimpleProcess;
import org.jboss.as.process.support.TestProcessUtils.TestProcessListenerStream;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests to test the processes sending data to other processes via the PM
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
@RunWith(LoggingTestRunner.class)
public class ProcessManagerSlaveTestCase extends AbstractProcessManagerTest {

    @Test
    public void testSimpleForwardMessage() throws Exception {
        addProcess("ProcA", ProcessManagerSlaveProcess.class);
        TestProcessListenerStream listenerA = startTestProcessListenerAndWait("ProcA");

        addProcess("ProcB", ProcessManagerSlaveProcess.class);
        TestProcessListenerStream listenerB = startTestProcessListenerAndWait("ProcB");

        sendMessage("Test", "ProcA", "Fwd$ProcB$Hello");

        assertEquals("Test-ProcA-Fwd$ProcB$Hello", listenerA.readMessage());
        assertEquals("ProcA-ProcB-Hello", listenerB.readMessage());

        stopTestProcessListenerAndWait(listenerA);
        stopTestProcessListenerAndWait(listenerB);
        removeProcess("ProcA");
        removeProcess("ProcB");
    }

    @Test
    public void testBiggerForwardMessage() throws Exception {
        addProcess("ProcA", ProcessManagerSlaveProcess.class);
        TestProcessListenerStream listenerA = startTestProcessListenerAndWait("ProcA");

        addProcess("ProcB", ProcessManagerSlaveProcess.class);
        TestProcessListenerStream listenerB = startTestProcessListenerAndWait("ProcB");

        addProcess("ProcC", ProcessManagerSlaveProcess.class);
        TestProcessListenerStream listenerC = startTestProcessListenerAndWait("ProcC");

        addProcess("ProcD", ProcessManagerSlaveProcess.class);
        TestProcessListenerStream listenerD = startTestProcessListenerAndWait("ProcD");

        addProcess("ProcE", ProcessManagerSlaveProcess.class);
        TestProcessListenerStream listenerE = startTestProcessListenerAndWait("ProcE");

        addProcess("ProcF", ProcessManagerSlaveProcess.class);
        TestProcessListenerStream listenerF = startTestProcessListenerAndWait("ProcF");

        sendMessage("Test", "ProcA", "Fwd$ProcB$Fwd$ProcC$Fwd$ProcD$Fwd$ProcE$Fwd$ProcA$Hello");
        assertEquals("Test-ProcA-Fwd$ProcB$Fwd$ProcC$Fwd$ProcD$Fwd$ProcE$Fwd$ProcA$Hello", listenerA.readMessage());
        assertEquals("ProcA-ProcB-Fwd$ProcC$Fwd$ProcD$Fwd$ProcE$Fwd$ProcA$Hello", listenerB.readMessage());
        assertEquals("ProcB-ProcC-Fwd$ProcD$Fwd$ProcE$Fwd$ProcA$Hello", listenerC.readMessage());
        assertEquals("ProcC-ProcD-Fwd$ProcE$Fwd$ProcA$Hello", listenerD.readMessage());
        assertEquals("ProcD-ProcE-Fwd$ProcA$Hello", listenerE.readMessage());
        assertEquals("ProcE-ProcA-Hello", listenerA.readMessage());
        assertNull(listenerA.readMessage(10));
        assertNull(listenerB.readMessage(10));
        assertNull(listenerC.readMessage(10));
        assertNull(listenerD.readMessage(10));
        assertNull(listenerE.readMessage(10));
        assertNull(listenerF.readMessage(10));

        stopTestProcessListenerAndWait(listenerA);
        stopTestProcessListenerAndWait(listenerB);
        stopTestProcessListenerAndWait(listenerC);
        stopTestProcessListenerAndWait(listenerD);
        stopTestProcessListenerAndWait(listenerE);
        stopTestProcessListenerAndWait(listenerF);
        removeProcess("ProcA");
        removeProcess("ProcB");
        removeProcess("ProcC");
        removeProcess("ProcD");
        removeProcess("ProcE");
        removeProcess("ProcF");
    }


    @Test
    public void testBroadcastMessage() throws Exception {
        addProcess("ProcA", ProcessManagerSlaveProcess.class);
        TestProcessListenerStream listenerA = startTestProcessListenerAndWait("ProcA");

        addProcess("ProcB", ProcessManagerSlaveProcess.class);
        TestProcessListenerStream listenerB = startTestProcessListenerAndWait("ProcB");

        addProcess("ProcC", ProcessManagerSlaveProcess.class);
        TestProcessListenerStream listenerC = startTestProcessListenerAndWait("ProcC");

        sendMessage("Test", "ProcA", "Bcst$Hello");

        assertEquals("Test-ProcA-Bcst$Hello", listenerA.readMessage());
        assertEquals("ProcA-ProcB-Hello", listenerB.readMessage());
        assertEquals("ProcA-ProcC-Hello", listenerC.readMessage());
        assertEquals("ProcA-ProcA-Hello", listenerA.readMessage());

        stopTestProcessListenerAndWait(listenerA);
        stopTestProcessListenerAndWait(listenerB);
        stopTestProcessListenerAndWait(listenerC);
        removeProcess("ProcA");
        removeProcess("ProcB");
        removeProcess("ProcC");
    }

    @Test
    public void testSimpleStartStop() throws Exception {
        addProcess("ProcA", ProcessManagerSlaveProcess.class);
        TestProcessListenerStream listenerA = startTestProcessListenerAndWait("ProcA");

        addProcess("ProcB", ProcessManagerSlaveProcess.class);
        TestProcessListenerStream listenerB = startTestProcessListenerAndWait("ProcB");

        sendMessage("Test", "ProcA", "Add$slave$" + StartStopSimpleProcess.class.getName());
        assertEquals("ProcA-Add$slave$" + StartStopSimpleProcess.class.getName(), listenerA.readMessage());

        //Need a short sleep here
        Thread.sleep(500);
        assertTrue(getProcessNames(false).contains("slave"));
        assertFalse(getProcessNames(true).contains("slave"));

        sendMessage("Test", "ProcB", "Start$slave");
        assertEquals("ProcB-Start$slave", listenerB.readMessage());

        //Give process time to start
        TestProcessListenerStream slave = getTestProcessListener("slave", 1000);
        assertNotNull(slave);
        assertTrue(getProcessNames(true).contains("slave"));

        assertEquals(StartStopSimpleProcess.STARTED, slave.readMessage());

        ProcessExitCodeAndShutDownLatch stopLatch = getStopTestProcessListenerLatch("slave");
        sendMessage("Test", "ProcA", "Stop$slave");
        assertEquals("ProcA-Stop$slave", listenerA.readMessage());
        assertEquals(StartStopSimpleProcess.STOPPED, slave.readMessage());
        assertTrue(stopLatch.await(500, TimeUnit.MILLISECONDS));
        assertTrue(getProcessNames(false).contains("slave"));
        assertFalse(getProcessNames(true).contains("slave"));

        sendMessage("Test", "ProcB", "Remove$slave");
        assertEquals("ProcB-Remove$slave", listenerB.readMessage());

        //Need a short sleep here
        Thread.sleep(500);
        assertFalse(getProcessNames(false).contains("slave"));
    }

    @Test
    public void testBroadcastMessageAddRemoveProcess() throws Exception {
        addProcess("ProcA", ProcessManagerSlaveProcess.class);
        TestProcessListenerStream listenerA = startTestProcessListenerAndWait("ProcA");
        sendMessage("Test", "ProcA", "Bcst$Hello1");
        assertEquals("Test-ProcA-Bcst$Hello1", listenerA.readMessage());
        assertEquals("ProcA-ProcA-Hello1", listenerA.readMessage());

        sendMessage("Test", "ProcA", "Add$ProcB$" + ProcessManagerSlaveProcess.class.getName());
        sendMessage("Test", "ProcA", "Start$ProcB");
        assertEquals("ProcA-Add$ProcB$" + ProcessManagerSlaveProcess.class.getName(), listenerA.readMessage());
        assertEquals("ProcA-Start$ProcB", listenerA.readMessage());
        TestProcessListenerStream listenerB = getTestProcessListener("ProcB", 1000);
        sendMessage("Test", "ProcA", "Bcst$Hello2");
        assertEquals("Test-ProcA-Bcst$Hello2", listenerA.readMessage());
        assertEquals("ProcA-ProcA-Hello2", listenerA.readMessage());
        assertEquals("ProcA-ProcB-Hello2", listenerB.readMessage());

        sendMessage("Test", "ProcB", "Add$ProcC$" + ProcessManagerSlaveProcess.class.getName());
        sendMessage("Test", "ProcB", "Start$ProcC");
        assertEquals("ProcB-Add$ProcC$" + ProcessManagerSlaveProcess.class.getName(), listenerB.readMessage());
        assertEquals("ProcB-Start$ProcC", listenerB.readMessage());
        TestProcessListenerStream listenerC = getTestProcessListener("ProcC", 1000);
        sendMessage("Test", "ProcB", "Bcst$Hello3");
        assertEquals("Test-ProcB-Bcst$Hello3", listenerB.readMessage());
        assertEquals("ProcB-ProcA-Hello3", listenerA.readMessage());
        assertEquals("ProcB-ProcB-Hello3", listenerB.readMessage());
        assertEquals("ProcB-ProcC-Hello3", listenerC.readMessage());

        ProcessExitCodeAndShutDownLatch stopLatch = getStopTestProcessListenerLatch("ProcA");
        sendMessage("Test", "ProcC", "Stop$ProcA");
        assertEquals("ProcC-Stop$ProcA", listenerC.readMessage());
        assertTrue(stopLatch.await(500, TimeUnit.MILLISECONDS));
        sendMessage("Test", "ProcB", "Bcst$Hello4");
        assertEquals("Test-ProcB-Bcst$Hello4", listenerB.readMessage());
        assertEquals("ProcB-ProcB-Hello4", listenerB.readMessage());
        assertEquals("ProcB-ProcC-Hello4", listenerC.readMessage());

        stopLatch = getStopTestProcessListenerLatch("ProcC");
        sendMessage("Test", "ProcB", "Stop$ProcC");
        assertEquals("ProcB-Stop$ProcC", listenerB.readMessage());
        assertTrue(stopLatch.await(500, TimeUnit.MILLISECONDS));
        sendMessage("Test", "ProcB", "Bcst$Hello5");
        assertEquals("Test-ProcB-Bcst$Hello5", listenerB.readMessage());
        assertEquals("ProcB-ProcB-Hello5", listenerB.readMessage());

        stopLatch = getStopTestProcessListenerLatch("ProcB");
        sendMessage("Test", "ProcB", "Stop$ProcB");
        assertEquals("ProcB-Stop$ProcB", listenerB.readMessage());
        assertTrue(stopLatch.await(500, TimeUnit.MILLISECONDS));

        assertFalse(getProcessNames(true).contains("ProcA"));
        assertFalse(getProcessNames(true).contains("ProcB"));
        assertFalse(getProcessNames(true).contains("ProcC"));
    }
}
