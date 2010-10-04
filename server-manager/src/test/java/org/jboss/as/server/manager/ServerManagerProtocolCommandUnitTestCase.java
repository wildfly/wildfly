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
package org.jboss.as.server.manager;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertSame;
import static junit.framework.Assert.fail;

import org.jboss.as.server.manager.ServerManagerProtocol.Command;
import org.jboss.as.server.manager.ServerManagerProtocol.ServerManagerToServerProtocolCommand;
import org.junit.Test;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class ServerManagerProtocolCommandUnitTestCase {

    @Test
    public void testSmStop() throws Exception {
        byte[] bytes = ServerManagerToServerProtocolCommand.STOP_SERVER.createCommandBytes(null);
        Command<ServerManagerToServerProtocolCommand> stop = ServerManagerToServerProtocolCommand.readCommand(bytes);
        assertSame(ServerManagerToServerProtocolCommand.STOP_SERVER, stop.getCommand());
        assertEquals(0, stop.getData().length);
    }

    @Test
    public void testSmStart() throws Exception {
        byte[] data = new byte[] {10, 11, 12, 13, 14, 15};
        byte[] bytes = ServerManagerToServerProtocolCommand.START_SERVER.createCommandBytes(data);
        Command<ServerManagerToServerProtocolCommand> start = ServerManagerToServerProtocolCommand.readCommand(bytes);
        assertSame(ServerManagerToServerProtocolCommand.START_SERVER, start.getCommand());

        assertEquals(data.length, start.getData().length);
        for (int i = 0 ; i < data.length ; i++)
            assertEquals(data[i], start.getData()[i]);
    }

    @Test
    public void testFailNoArgsCommandWithData() throws Exception {
        try {
            byte[] data = new byte[] {5, 6, 7};
            ServerManagerToServerProtocolCommand.STOP_SERVER.createCommandBytes(data);
        }catch (Exception ignore) {
            return;
        }
        fail("Should have picked up not expected data");
    }

    @Test
    public void testFailArgsCommandWithNoData() throws Exception {
        try {
            ServerManagerToServerProtocolCommand.START_SERVER.createCommandBytes(null);
        }catch (Exception ignore) {
            return;
        }
        fail("Should have picked up missing data");
    }

    @Test
    public void testParseServerManagerProtocolCommands() {
        for (ServerManagerToServerProtocolCommand cmd : ServerManagerToServerProtocolCommand.values()) {
            byte b = cmd.getId();
            assertEquals(cmd, ServerManagerToServerProtocolCommand.parse(b));
        }
    }
}
