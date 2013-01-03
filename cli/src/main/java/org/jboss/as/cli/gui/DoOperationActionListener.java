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

import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.text.BadLocationException;
import org.jboss.as.cli.gui.component.CLIOutput;

/**
 * This class executes whatever command is on the command line.
 * It displays the result in the Output tab and sets "Output" to
 * be the currently selected tab.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2012 Red Hat Inc.
 */
public class DoOperationActionListener extends AbstractAction {

    private CliGuiContext cliGuiCtx;

    private CLIOutput output;

    private LinkedList<String> cmdHistory = new LinkedList<String>();

    public DoOperationActionListener(CliGuiContext cliGuiCtx) {
        this.cliGuiCtx = cliGuiCtx;
        this.output = cliGuiCtx.getOutput();
    }

    public void actionPerformed(ActionEvent ae) {
        String command = cliGuiCtx.getCommandLine().getCmdText().getText();
        try {
            cmdHistory.push(command);
            CommandExecutor.Response response = cliGuiCtx.getExecutor().doCommandFullResponse(command);
            postOutput(response);
        } catch (Exception e) {
            output.postCommandWithResponse(command, e.getMessage());
        } finally {
            cliGuiCtx.getTabs().setSelectedIndex(1); // set to Output tab to view the output
        }
    }

    public List getCmdHistory() {
        return Collections.unmodifiableList(this.cmdHistory);
    }

    private void postOutput(CommandExecutor.Response response) throws BadLocationException {
        boolean verbose = cliGuiCtx.getCommandLine().isVerbose();
        if (verbose) {
            postVerboseOutput(response);
        } else {
            output.postCommandWithResponse(response.getCommand(), response.getDmrResponse().toString());
        }
    }

    private void postVerboseOutput(CommandExecutor.Response response) throws BadLocationException {
        output.postAttributed(response.getDmrResponse().toString() + "\n\n", null);
        output.postAttributed(response.getDmrRequest().toString() + "\n\n", null);
        output.postBold(response.getCommand() + "\n");
    }

}
