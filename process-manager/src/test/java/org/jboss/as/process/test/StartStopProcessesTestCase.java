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
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

import org.jboss.as.process.AbstractProcessManagerTest;
import org.jboss.as.process.support.LoggingTestRunner;
import org.jboss.as.process.support.StartStopSimpleProcess;
import org.jboss.as.process.support.TestFileUtils;
import org.jboss.as.process.support.TestFileUtils.TestFile;
import org.jboss.as.process.support.TestProcessUtils.TestProcessListenerStream;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * 
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
@RunWith(LoggingTestRunner.class)
public class StartStopProcessesTestCase extends AbstractProcessManagerTest {

    @Test
    public void testStartStopProcess() throws Exception {
        addProcess("Main", StartStopSimpleProcess.class);
        assertTrue(getProcessNames(false).contains("Main"));
        assertFalse(getProcessNames(true).contains("Main"));
        assertEquals(0, TestFileUtils.getNumberOutputFiles());

        TestProcessListenerStream listener = startTestProcessListenerAndWait("Main");
        assertEquals(StartStopSimpleProcess.STARTED, listener.readMessage());
        assertTrue(getProcessNames(true).contains("Main"));

        stopTestProcessListenerAndWait(listener);
        assertEquals(StartStopSimpleProcess.STOPPED, listener.readMessage());
        assertTrue(getProcessNames(false).contains("Main"));
        assertFalse(getProcessNames(true).contains("Main"));

        removeProcess("Main");

        assertNull(getTestProcessListener("Main", 0));
        assertFalse(getProcessNames(false).contains("Main"));
    }

    @Test
    public void testRestartProcess() throws Exception {
        addProcess("Main", StartStopSimpleProcess.class);

        TestProcessListenerStream stream = startTestProcessListenerAndWait("Main");
        assertEquals(StartStopSimpleProcess.STARTED, stream.readMessage());
        assertTrue(getProcessNames(true).contains("Main"));

        stopTestProcessListenerAndWait(stream);
        assertEquals(StartStopSimpleProcess.STOPPED, stream.readMessage());

        TestProcessListenerStream stream2 = startTestProcessListenerAndWait("Main");
        assertEquals(StartStopSimpleProcess.STARTED, stream2.readMessage());

        stopTestProcessListener("Main");
        assertEquals(StartStopSimpleProcess.STOPPED, stream2.readMessage());

        removeProcess("Main");
    }

    @Test
    public void testStartNotAddedProcess() throws Exception {
        startProcess("Nothing", 300);
        assertEquals(0, TestFileUtils.getNumberOutputFiles());
        assertFalse(getProcessNames(false).contains("Nothing"));
    }

    @Test
    public void testStopNotAddedProcess() throws Exception {
        stopTestProcessListener("Nothing", 300);
        assertEquals(0, TestFileUtils.getNumberOutputFiles());
        assertFalse(getProcessNames(false).contains("Nothing"));
    }

    @Test
    public void testStopNotStartedProcess() throws Exception {
        TestFile main = addProcess("Main", StartStopSimpleProcess.class);
        stopTestProcessListener("Main");
        assertFalse(main.exists());
        assertFalse(getProcessNames(false).contains("Nothing"));
    }

    @Test
    public void testRemoveRunningProcess() throws Exception {
        addProcess("Main", StartStopSimpleProcess.class);

        TestProcessListenerStream listener = startTestProcessListenerAndWait("Main");
        assertEquals(StartStopSimpleProcess.STARTED, listener.readMessage());

        removeProcess("Main");
        assertNull(StartStopSimpleProcess.STOPPED, listener.readMessage(100));
        assertTrue(getProcessNames(false).contains("Main"));
    }

    @Test
    public void testStartStopSameProcessTwiceIsIgnored() throws Exception {
        TestFile main = addProcess("Main", StartStopSimpleProcess.class);
        assertEquals(0, TestFileUtils.getNumberOutputFiles());

        TestProcessListenerStream listener = startTestProcessListenerAndWait("Main");
        assertEquals(StartStopSimpleProcess.STARTED, listener.readMessage());

        startProcess("Main", 300);
        main.checkFile(StartStopSimpleProcess.STARTED);

        stopTestProcessListenerAndWait(listener);
        assertEquals(StartStopSimpleProcess.STOPPED, listener.readMessage());
        main.checkFile(StartStopSimpleProcess.STARTED, StartStopSimpleProcess.STOPPED);

        stopTestProcessListener("Main", 200);
        main.checkFile(StartStopSimpleProcess.STARTED, StartStopSimpleProcess.STOPPED);
    }

    @Test
    public void testStartTwoSimilarProcessesWithDifferentNames() throws Exception {
        addProcess("ProcessA", StartStopSimpleProcess.class);
        addProcess("ProcessB", StartStopSimpleProcess.class);
        assertEquals(0, TestFileUtils.getNumberOutputFiles());

        TestProcessListenerStream listenerA = startTestProcessListenerAndWait("ProcessA");
        assertEquals(StartStopSimpleProcess.STARTED, listenerA.readMessage());

        TestProcessListenerStream listenerB = startTestProcessListenerAndWait("ProcessB");
        assertEquals(StartStopSimpleProcess.STARTED, listenerB.readMessage());

        stopTestProcessListener("ProcessB");
        assertEquals(StartStopSimpleProcess.STOPPED, listenerB.readMessage());
        assertEquals(null, listenerA.readMessage(100));

        stopTestProcessListener("ProcessA");
        assertEquals(StartStopSimpleProcess.STOPPED, listenerA.readMessage());

        removeProcess("ProcessA");
        removeProcess("ProcessB");
    }
}
