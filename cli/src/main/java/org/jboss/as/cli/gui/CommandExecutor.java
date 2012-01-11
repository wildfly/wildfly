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

import java.io.IOException;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.operation.CommandLineParser;
import org.jboss.as.cli.operation.impl.DefaultCallbackHandler;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;

/**
 * This class takes a command-line cli command and submits it to the server.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2012 Red Hat Inc.
 */
public class CommandExecutor {

    private ModelControllerClient client;
    private DefaultCallbackHandler parsedCmd = new DefaultCallbackHandler(true);
    private CommandContext cmdCtx;
    private CommandLineParser parser;

    public CommandExecutor(CommandContext cmdCtx) {
        this.cmdCtx = cmdCtx;
        this.client = cmdCtx.getModelControllerClient();
        this.parser = cmdCtx.getCommandLineParser();
    }

    public synchronized ModelNode doCommand(String command) throws CommandFormatException, IOException {
        System.out.println("command=" + command);
        parsedCmd.rootNode(0);
        parser.parse(command, parsedCmd);
        ModelNode request = parsedCmd.toOperationRequest(cmdCtx);
        return client.execute(request);
    }

}
