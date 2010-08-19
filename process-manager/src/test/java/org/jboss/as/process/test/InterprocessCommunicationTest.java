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

import org.jboss.as.process.AbstractProcessManagerTest;
import org.jboss.as.process.support.ReceivingProcess;
import org.jboss.as.process.support.TestFileUtils;
import org.jboss.as.process.support.TestFileUtils.TestFile;
import org.jboss.as.process.support.TestProcessUtils.TestProcessListenerStream;
import org.junit.Test;

/**
 * 
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public abstract class InterprocessCommunicationTest extends AbstractProcessManagerTest {
    protected abstract void sendMessage(String sender, String recipient, String... message) throws InterruptedException;

    protected abstract void broadcastMessage(String sender, String... message) throws InterruptedException;

    @Test
    public void testSendMessage() throws Exception {
        addProcess("Receiver", ReceivingProcess.class/*, 8787, true*/);
        TestProcessListenerStream listener = startTestProcessListenerAndWait("Receiver");

        sendMessage("Test", "Receiver", "One");
        assertEquals("Test-One", listener.readMessage());
        // assertEquals("Test-One\n", TestFileUtils.readFile(receiver));

        sendMessage("Test", "Receiver", "Two", "Three");
        assertEquals("Test-Two", listener.readMessage());
        assertEquals("Test-Three", listener.readMessage());

        stopTestProcessListenerAndWait(listener);
        removeProcess("Receiver");
    }

    @Test
    public void testSendMessageToNonExistantProcess() throws Exception {
        sendMessage("Test", "Receiver", "One");
        assertEquals(0, TestFileUtils.getNumberOutputFiles());
    }

    @Test
    public void testSendMessageToOnlyOneProcess() throws Exception {
        addProcess("ReceiverA", ReceivingProcess.class);
        TestProcessListenerStream listenerA = startTestProcessListenerAndWait("ReceiverA");

        TestFile fileB = addProcess("ReceiverB", ReceivingProcess.class);
        TestProcessListenerStream listenerB = startTestProcessListenerAndWait("ReceiverB");

        sendMessage("Tester", "ReceiverA", "One");
        assertEquals("Tester-One", listenerA.readMessage());
        assertFalse(fileB.exists());
        assertNull(listenerB.readMessage(100));

        sendMessage("Tester", "ReceiverB", "Two", "Three");
        assertEquals("Tester-Two", listenerB.readMessage());
        assertEquals("Tester-Three", listenerB.readMessage());
        assertNull(listenerA.readMessage(100));

        stopTestProcessListenerAndWait(listenerA);
        stopTestProcessListenerAndWait(listenerB);

        removeProcess("ReceiverA");
        removeProcess("ReceiverB");
    }

    @Test
    public void testBroadcastMessages() throws Exception {
        addProcess("ReceiverA", ReceivingProcess.class);
        TestProcessListenerStream listenerA = startTestProcessListenerAndWait("ReceiverA");

        addProcess("ReceiverB", ReceivingProcess.class);
        TestProcessListenerStream listenerB = startTestProcessListenerAndWait("ReceiverB");

        addProcess("ReceiverC", ReceivingProcess.class);
        TestProcessListenerStream listenerC = startTestProcessListenerAndWait("ReceiverC");

        broadcastMessage("Test", "One");
        assertEquals("Test-One", listenerA.readMessage());
        assertEquals("Test-One", listenerB.readMessage());
        assertEquals("Test-One", listenerC.readMessage());

        broadcastMessage("Test", "Two", "Three", "Four", "Five");
        assertEquals("Test-Two", listenerA.readMessage());
        assertEquals("Test-Two", listenerB.readMessage());
        assertEquals("Test-Two", listenerC.readMessage());
        assertEquals("Test-Three", listenerA.readMessage());
        assertEquals("Test-Three", listenerB.readMessage());
        assertEquals("Test-Three", listenerC.readMessage());
        assertEquals("Test-Four", listenerA.readMessage());
        assertEquals("Test-Four", listenerB.readMessage());
        assertEquals("Test-Four", listenerC.readMessage());
        assertEquals("Test-Five", listenerA.readMessage());
        assertEquals("Test-Five", listenerB.readMessage());
        assertEquals("Test-Five", listenerC.readMessage());

        stopTestProcessListenerAndWait(listenerA);
        stopTestProcessListenerAndWait(listenerB);
        stopTestProcessListenerAndWait(listenerC);

        removeProcess("ReceiverA");
        removeProcess("ReceiverB");
        removeProcess("ReceiverC");
    }

    @Test
    public void testBroadcastMessageAddRemoveProcess() throws Exception {
        addProcess("ReceiverA", ReceivingProcess.class);
        TestProcessListenerStream listenerA = startTestProcessListenerAndWait("ReceiverA");
        broadcastMessage("Test", "1");
        assertEquals("Test-1", listenerA.readMessage());

        addProcess("ReceiverB", ReceivingProcess.class);
        TestProcessListenerStream listenerB = startTestProcessListenerAndWait("ReceiverB");
        broadcastMessage("Test", "2");
        assertEquals("Test-2", listenerA.readMessage());
        assertEquals("Test-2", listenerB.readMessage());
        
        addProcess("ReceiverC", ReceivingProcess.class);
        TestProcessListenerStream listenerC = startTestProcessListenerAndWait("ReceiverC");
        broadcastMessage("Test", "3");
        assertEquals("Test-3", listenerA.readMessage());
        assertEquals("Test-3", listenerB.readMessage());
        assertEquals("Test-3", listenerC.readMessage());

        //Ensure that the listeners have the expected process names
        //before stopping
        assertEquals("ReceiverA", listenerA.getProcessName());
        assertEquals("ReceiverB", listenerB.getProcessName());
        assertEquals("ReceiverC", listenerC.getProcessName());

        stopTestProcessListenerAndWait(listenerA);
        broadcastMessage("Test", "4");
        assertNull(listenerA.readMessage(50));
        assertEquals("Test-4", listenerB.readMessage());
        assertEquals("Test-4", listenerC.readMessage());

        removeProcess("ReceiverA");
        stopTestProcessListenerAndWait(listenerB);
        removeProcess("ReceiverB");
        broadcastMessage("Test", "5");
        assertNull(listenerA.readMessage(50));
        assertNull(listenerB.readMessage(50));
        assertEquals("Test-5", listenerC.readMessage());
        
        stopTestProcessListenerAndWait(listenerC);
        removeProcess("ReceiverC");
    }
}
