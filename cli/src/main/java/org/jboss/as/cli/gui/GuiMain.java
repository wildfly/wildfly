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

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.operation.CommandLineParser;
import org.jboss.as.cli.operation.OperationFormatException;
import org.jboss.as.cli.operation.impl.DefaultCallbackHandler;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2012 Red Hat Inc.
 */
public class GuiMain {

    private CommandContext cmdCtx;
    /** parsed command arguments */
    private DefaultCallbackHandler parsedCmd = new DefaultCallbackHandler(true);

    public GuiMain(CommandContext cmdCtx) {
        this.cmdCtx = cmdCtx;
        ModelControllerClient client = cmdCtx.getModelControllerClient();
        CommandLineParser parser = cmdCtx.getCommandLineParser();
        parsedCmd.rootNode(0);
        try {
            parser.parse("/:read-resource(recursive=true)", parsedCmd);
            ModelNode result = client.execute(parsedCmd.toOperationRequest(cmdCtx));
            System.out.println("result=");
            System.out.println(result.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
