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
package org.jboss.as.process.support;

import java.util.Collections;
import java.util.List;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class ProcessManagerSlaveProcess extends AbstractProcess {
    protected ProcessManagerSlaveProcess(String processName, int port) {
        super(processName, port);
    }

    public static void main(String[] args) {
        if (args.length < 1)
            System.exit(-1);
        ProcessManagerSlaveProcess process = new ProcessManagerSlaveProcess(args[0], getPort(args));
        process.startSlave();
    }

    @Override
    protected void started() {
    }

    @Override
    protected void handleMessage(String sourceProcessName, byte[] message) {
        writeData(sourceProcessName, new String(message), true);
    }

    @Override
    protected void handleMessage(String sourceProcessName, List<String> message) {
        for (String str : message)
            writeData(sourceProcessName, str, false);
    }

    private void writeData(String sourceProcessName, String message, boolean bytes) {

        if (!message.contains("$"))
            writeData(sourceProcessName + "-" + processName + "-" + message);
        if (message.startsWith("Fwd$")) {
            writeData(sourceProcessName + "-" + processName + "-" + message);
            int index = message.indexOf('$', 5);
            String nextProcess = message.substring(4, index);
            message = message.substring(index + 1);
            debug(processName, "Sending data to " + nextProcess + " '" + message + "'");
            if (!bytes)
                sendMessage(nextProcess, Collections.singletonList(message));
            else
                sendMessage(nextProcess, message.getBytes());
        }
        else if (message.startsWith("Bcst$")) {
            writeData(sourceProcessName + "-" + processName + "-" + message);
            message = message.substring(5);
            debug(processName, "broadcasting data '" + message + "'");
            if (!bytes)
                broadcastMessage(Collections.singletonList(message));
            else
                broadcastMessage(message.getBytes());
        }
        else if (message.startsWith("Add$")) {
            int index1 = message.indexOf("$");
            int index2 = message.indexOf("$", index1 + 1);
            String proc = message.substring(index1 + 1, index2);
            String clazz = message.substring(index2 + 1);

            debug(processName, "Adding " + proc + ":" + clazz);
            addProcess(proc, clazz);
            writeData(processName + "-" + message);
        }
        else if (message.startsWith("Start$")) {
            int index = message.indexOf("$");
            String proc = message.substring(index + 1);
            debug(processName, "Starting " + proc + ":" + proc);
            startProcess(proc);
            writeData(processName + "-" + message);
        }
        else if (message.startsWith("Stop$")) {
            int index = message.indexOf("$");
            String proc = message.substring(index + 1);
            debug(processName, "Stopping " + proc + ":" + proc);
            stopProcess(proc);
            writeData(processName + "-" + message);
        }
        else if (message.startsWith("Remove$")) {
            int index = message.indexOf("$");
            String proc = message.substring(index + 1);
            debug(processName, "Removing " + proc + ":" + proc);
            removeProcess(proc);
            writeData(processName + "-" + message);
        }
    }

    @Override
    protected void shutdown() {
    }
}
