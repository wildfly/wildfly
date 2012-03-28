/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.cli.gui;

import java.awt.Cursor;
import java.io.IOException;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;

/**
 * This class takes a command-line cli command and submits it to the server.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2012 Red Hat Inc.
 */
public class CommandExecutor {

    private ModelControllerClient client;
    private CommandContext cmdCtx;

    public CommandExecutor(CommandContext cmdCtx) {
        this.cmdCtx = cmdCtx;
        this.client = cmdCtx.getModelControllerClient();
    }

    /**
     * Submit a command to the server.
     *
     * @param command The CLI command
     * @return The DMR response as a ModelNode
     * @throws CommandFormatException
     * @throws IOException
     */
    public synchronized ModelNode doCommand(String command) throws CommandFormatException, IOException {
        ModelNode request = cmdCtx.buildRequest(command);
        return execute(command, request);
    }

    public synchronized Response doCommandFullResponse(String command) throws CommandFormatException, IOException {
        ModelNode request = cmdCtx.buildRequest(command);
        ModelNode response = execute(command, request);
        return new Response(command, request, response);
    }

    private ModelNode execute(String command, ModelNode request) throws IOException {
        try {
            if (command.startsWith("deploy")) {
                GuiMain.getMainWindow().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            }
            return client.execute(request);
        } finally {
            if (command.startsWith("deploy")) {
                GuiMain.getMainWindow().setCursor(Cursor.getDefaultCursor());
            }
        }
    }

    public static class Response {
        private String command;
        private ModelNode dmrRequest;
        private ModelNode dmrResponse;

        Response(String command, ModelNode dmrRequest, ModelNode dmrResponse) {
            this.command = command;
            this.dmrRequest = dmrRequest;
            this.dmrResponse = dmrResponse;
        }

        public String getCommand() {
            return command;
        }

        public ModelNode getDmrRequest() {
            return dmrRequest;
        }

        public ModelNode getDmrResponse() {
            return dmrResponse;
        }

    }

}
